package org.rem.types;

import org.rem.enums.TypeEnum;
import org.rem.interfaces.IType;

public final class VoidType implements IType {
  public static VoidType INSTANCE = new VoidType();

  @Override
  public TypeEnum type() {
    return TypeEnum.VOID;
  }

  @Override
  public boolean isPrimitive() {
    return false;
  }

  @Override
  public String toString() {
    return name();
  }
}
