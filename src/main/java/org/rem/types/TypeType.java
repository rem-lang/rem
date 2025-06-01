package org.rem.types;

import org.rem.enums.TypeEnum;
import org.rem.interfaces.IType;

public final class TypeType implements IType {
  public static TypeType INSTANCE = new TypeType();

  @Override
  public TypeEnum type() {
    return TypeEnum.TYPE;
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
