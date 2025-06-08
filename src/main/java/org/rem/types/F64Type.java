package org.rem.types;

import org.rem.enums.TypeEnum;
import org.rem.interfaces.IType;

public final class F64Type implements IType {
  public static F64Type INSTANCE = new F64Type();

  @Override
  public TypeEnum type() {
    return TypeEnum.F64;
  }

  @Override
  public String toString() {
    return name();
  }

  @Override
  public boolean isAssignableFrom(IType type) {
    return isAssignableTo(type) || type == NilType.INSTANCE;
  }

  @Override
  public boolean isAssignableTo(IType type) {
    return type.type().compareTo(TypeEnum.F32) >= 0 && type.type().compareTo(TypeEnum.F128) <= 0;
  }
}
