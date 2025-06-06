package org.rem.registries;

import norswap.uranium.Reactor;
import org.rem.compiler.targets.CCompileTarget;
import org.rem.compiler.targets.JSCompileTarget;
import org.rem.compiler.targets.LLVMCompileTarget;
import org.rem.interfaces.ICompileTarget;

import java.util.HashMap;
import java.util.Map;

public class CompilerRegistry {
  public static ICompileTarget<?> get(Object key, Reactor reactor) {
    return switch (key.toString().toLowerCase()) {
      case "c" -> new CCompileTarget(reactor);
      case "js" -> new JSCompileTarget(reactor);
      default -> new LLVMCompileTarget(reactor);
    };
  }
}
