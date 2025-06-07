package org.rem.registries;

import norswap.uranium.Reactor;
import org.rem.compiler.targets.c.CCompileTarget;
import org.rem.compiler.targets.js.JSCompileTarget;
import org.rem.compiler.targets.llvm.LLVMCompileTarget;
import org.rem.interfaces.ICompileTarget;

public class CompilerRegistry {
  public static ICompileTarget<?> get(Object key, Reactor reactor) {
    return switch (key.toString().toLowerCase()) {
      case "c" -> new CCompileTarget(reactor);
      case "js" -> new JSCompileTarget(reactor);
      default -> new LLVMCompileTarget(reactor);
    };
  }
}
