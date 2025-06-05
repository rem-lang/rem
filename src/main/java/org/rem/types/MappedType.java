package org.rem.types;

import org.jspecify.annotations.NonNull;
import org.rem.enums.TypeEnum;
import org.rem.interfaces.IType;

public record MappedType(IType keyType, IType valueType) implements IType {

  @Override
  public TypeEnum type() {
    return TypeEnum.MAPPED;
  }

  @Override
  public @NonNull String name() {
    return "[" + keyType.name() + "]" + valueType.name();
  }

  @Override
  public boolean isPrimitive() {
    return false;
  }

  @Override
  @NonNull
  public String toString() {
    return name();
  }

  @Override
  public boolean isAssignableFrom(IType type) {
    if (type instanceof MappedType(IType rKeyType, IType rValueType)) {
      return keyType.isAssignableFrom(rKeyType) && valueType.isAssignableFrom(rValueType);
    }
    return false;
  }

  @Override
  public boolean isAssignableTo(IType type) {
    if (type instanceof MappedType(IType rKeyType, IType rValueType)) {
      return rKeyType.isAssignableTo(this.keyType) && rValueType.isAssignableTo(this.valueType);
    }
    return false;
  }
}
