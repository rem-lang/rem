package org.rem.types;

import org.rem.enums.TypeEnum;
import org.rem.interfaces.IType;

public final class I32Type implements IType {
  public static I32Type INSTANCE = new I32Type();

  @Override
  public TypeEnum type() {
    return TypeEnum.I32;
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
    return type.type().compareTo(TypeEnum.I8) >= 0 && type.type().compareTo(TypeEnum.I128) <= 0;
  }
}
