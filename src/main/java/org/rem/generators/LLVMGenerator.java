package org.rem.generators;

import org.bytedeco.llvm.LLVM.LLVMTargetRef;
import org.bytedeco.llvm.LLVM.LLVMValueRef;
import org.rem.compiler.CompileResult;
import org.rem.compiler.targets.LLVMCompileTarget;
import org.rem.interfaces.IGenerator;

import org.bytedeco.javacpp.BytePointer;

import java.io.IOException;

import static org.bytedeco.llvm.global.LLVM.*;

public class LLVMGenerator implements IGenerator<LLVMValueRef> {
  static final String[] LINKERS = new String[]{"cc", "clang", "gcc"};

  private static final BytePointer error = new BytePointer();

  @Override
  public int generate(CompileResult<LLVMValueRef> result) {
    if(result.getTarget() instanceof LLVMCompileTarget llvmTarget) {
       final var error = new BytePointer();

       LLVMDumpModule(llvmTarget.getModule());

      LLVMInitializeAllTargetInfos();
      LLVMInitializeAllTargets();
      LLVMInitializeAllTargetMCs();
      LLVMInitializeAllAsmParsers();
      LLVMInitializeAllAsmPrinters();

      var target = new LLVMTargetRef();
      LLVMGetTargetFromTriple(LLVMGetDefaultTargetTriple(), target, error);
      if(!error.isNull()) {
        System.err.printf("error: %s\n", error);
      }
      LLVMDisposeMessage(error);

      var machine = LLVMCreateTargetMachine(target, LLVMGetDefaultTargetTriple(), new BytePointer("generic"), LLVMGetHostCPUFeatures(), LLVMCodeGenLevelDefault, LLVMRelocDefault, LLVMCodeModelDefault);

      LLVMSetTarget(llvmTarget.getModule(), LLVMGetDefaultTargetTriple());
      var dataLayout = LLVMCreateTargetDataLayout(machine);
      var datalayoutStr = LLVMCopyStringRepOfTargetData(dataLayout);

      if(!error.isNull()) {
        System.err.printf("error: %s\n", error);
      }
      LLVMSetDataLayout(llvmTarget.getModule(), datalayoutStr);
      LLVMDisposeMessage(datalayoutStr);

      LLVMTargetMachineEmitToFile(machine, llvmTarget.getModule(), "result.o", LLVMObjectFile, error);
      if(!error.isNull()) {
        System.err.printf("error: %s\n", error);
      }
      LLVMDisposeMessage(error);

      return linkToExe("result.o", "result");

    }
    return 0;
  }

  private int linkToExe(String inputPath, String ouputPath) {
    boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

    if(!ouputPath.endsWith(".exe") && isWindows) {
      ouputPath += ".exe";
    }

    try {
      for(var linker : LINKERS) {
        var process = Runtime.getRuntime().exec(new String[]{linker, inputPath, "-o", ouputPath});

        String out;
        try (var inputReader = process.inputReader()) {
          while ((out = inputReader.readLine()) != null) {
            System.out.println(out);
          }
        }

        try (var errReader = process.errorReader()) {
          while ((out = errReader.readLine()) != null) {
            System.err.println(out);
          }
        }
      }
    } catch (IOException e) {
      return 1;
    }

    return 0;
  }
}
