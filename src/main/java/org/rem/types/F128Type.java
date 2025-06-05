package org.rem.types;

import org.rem.enums.TypeEnum;
import org.rem.interfaces.IType;

public final class F128Type implements IType {
  public static F128Type INSTANCE = new F128Type();

  @Override
  public TypeEnum type() {
    return TypeEnum.F128;
  }

  @Override
  public String toString() {
    return name();
  }

  @Override
  public boolean isAssignableFrom(IType type) {
    return isAssignableTo(type);
  }

  @Override
  public boolean isAssignableTo(IType type) {
    return type.type().compareTo(TypeEnum.F32) >= 0 && type.type().compareTo(TypeEnum.F128) <= 0;
  }
}
