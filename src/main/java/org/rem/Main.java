package org.rem;

import org.rem.compiler.CompileRequest;
import org.rem.compiler.Compiler;
import org.rem.exceptions.AnalysisException;

import java.io.File;

public class Main {

  public static void main(String[] args) {
    if (args.length > 0) {
      String file = args[0];

      // TODO: Make configurable
      boolean showWarnings = true;

      try {
        Compiler compiler = new Compiler(
          new CompileRequest(
            new File(file),
            "llvm"
          ),
          showWarnings
        );

        System.exit(compiler.generate(compiler.compile()));
      } catch (AnalysisException e) {
        System.err.println(e.getMessage());
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      System.out.println("Usage: rem <file>");
    }
  }
}
