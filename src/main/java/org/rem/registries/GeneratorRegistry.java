package org.rem.registries;

import org.rem.generators.*;
import org.rem.interfaces.IGenerator;

import java.util.HashMap;
import java.util.Map;

public class GeneratorRegistry {
  private static final Map<String, IGenerator> generators = new HashMap<>() {
    @Override
    public IGenerator get(Object key) {
      return switch (key.toString().toLowerCase()) {
        case "c" -> new CGenerator();
        case "js" -> new JSGenerator();
        default -> new LLVMGenerator();
      };
    }
  };

  public static IGenerator get(String name) {
    return generators.get(name);
  }
}
