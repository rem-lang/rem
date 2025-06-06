package org.rem.interfaces;

import org.jspecify.annotations.NonNull;
import org.rem.enums.TypeEnum;

public interface IType extends Comparable<IType> {
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

  default boolean less(IType other) {
    return this.compareTo(other) < 0;
  }

  default boolean lessOrGreater(IType other) {
    return this.less(other) || this.greater(other);
  }

  default boolean lessOrEqual(IType other) {
    return this.compareTo(other) <= 0;
  }

  default boolean greater(IType other) {
    return this.compareTo(other) > 0;
  }

  default boolean greaterOrEqual(IType other) {
    return this.compareTo(other) >= 0;
  }

  default IType asReference() {
    return this;
  }

  @Override
  default int compareTo(IType o) {
    return type().compareTo(o.type());
  }
}
