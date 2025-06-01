package org.rem.interfaces;

import org.jspecify.annotations.NonNull;
import org.rem.enums.TypeEnum;

public interface IType {
  default boolean isPrimitive() {
    return true;
  }

  default boolean isReference() {
    return !isPrimitive();
  }

  TypeEnum type();

  @NonNull
  default String name() {
    return type().toString().toLowerCase();
  }
}
