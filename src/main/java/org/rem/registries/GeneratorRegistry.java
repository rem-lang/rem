package org.rem.registries;

import org.rem.generators.*;
import org.rem.interfaces.IGenerator;

import java.util.HashMap;
import java.util.Map;

public class GeneratorRegistry {
  public static <T> IGenerator<?> get(Object key, T result) {
    return switch (key.toString().toLowerCase()) {
      case "c" -> new CGenerator();
      case "js" -> new JSGenerator();
      default -> new LLVMGenerator();
    };
  }
}
