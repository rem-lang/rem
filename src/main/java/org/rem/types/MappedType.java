package org.rem.types;

import org.jspecify.annotations.NonNull;
import org.rem.enums.TypeEnum;
import org.rem.interfaces.IType;

public record MappedType(IType outerType, IType keyType, IType valueType) implements IType {

  @Override
  public TypeEnum type() {
    return TypeEnum.MAPPED;
  }

  @Override
  public @NonNull String name() {
    return outerType.name() + "<" + keyType.name() + ", " + valueType.name() + ">";
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
}
