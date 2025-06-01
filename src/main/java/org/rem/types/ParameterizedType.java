package org.rem.types;

import org.jspecify.annotations.NonNull;
import org.rem.enums.TypeEnum;
import org.rem.interfaces.IType;

public record ParameterizedType(IType outerType, IType innerType) implements IType {

  @Override
  public TypeEnum type() {
    return TypeEnum.PARAMETERIZED;
  }

  @Override
  public @NonNull String name() {
    return outerType.name() + "<" + innerType.name() + ">";
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
