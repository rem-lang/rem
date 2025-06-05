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

  default boolean isAssignableTo(IType type) {
    return false;
  }

  default boolean isAssignableFrom(IType type) {
    return false;
  }

  default IType asReference() {
    return this;
  }
}
