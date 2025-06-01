package org.rem.generators;

import org.rem.compiler.CompileResult;
import org.rem.interfaces.IGenerator;

import org.bytedeco.javacpp.BytePointer;

import java.io.IOException;

public class LLVMGenerator implements IGenerator {
  static final String[] LINKERS = new String[]{"clang", "gcc", "cc"};

  private static final BytePointer error = new BytePointer();

  @Override
  public int generate(CompileResult<?> compileResult) {
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
