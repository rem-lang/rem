package org.rem.registries;

import org.rem.compiler.targets.CCompileTarget;
import org.rem.compiler.targets.JSCompileTarget;
import org.rem.compiler.targets.LLVMCompileTarget;
import org.rem.interfaces.ICompileTarget;

import java.util.HashMap;
import java.util.Map;

public class CompilerRegistry {
  private static final Map<String, ICompileTarget<?>> compilers = new HashMap<>() {
    @Override
    public ICompileTarget<?> get(Object key) {
      return switch (key.toString().toLowerCase()) {
        case "c" -> new CCompileTarget();
        case "js" -> new JSCompileTarget();
        default -> new LLVMCompileTarget();
      };
    }
  };

  public static ICompileTarget<?> getCompiler(String name) {
    return compilers.get(name);
  }
}
