package org.rem.compiler;

import java.io.File;

public class CompileRequest {
  public final String moduleName;
  public final String outputPath;
  public final String compileTarget;
  public final File sourceFile;

  public CompileRequest(File sourceFile, String compileTarget, String moduleName, String outputPath) {
    this.sourceFile = sourceFile;
    this.compileTarget = compileTarget;
    this.moduleName = moduleName;
    this.outputPath = outputPath;
  }

  public CompileRequest(File sourceFile, String compileTarget, String moduleName) {
    this(sourceFile, compileTarget, moduleName, sourceFile.getAbsolutePath().replace("[.]r$", ".o"));
  }

  public CompileRequest(File sourceFile, String compileTarget) {
    this(sourceFile, compileTarget, "__main__");
  }
}
