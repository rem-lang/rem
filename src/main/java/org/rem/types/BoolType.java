package org.rem.types;

import org.rem.enums.TypeEnum;
import org.rem.interfaces.IType;

public final class BoolType implements IType {
  public static BoolType INSTANCE = new BoolType();

  @Override
  public TypeEnum type() {
    return TypeEnum.BOOL;
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
    return type.type() == TypeEnum.BOOL;
  }
}
