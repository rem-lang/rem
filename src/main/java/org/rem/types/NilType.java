package org.rem.types;

import org.rem.enums.TypeEnum;
import org.rem.interfaces.IType;

public final class NilType implements IType {
  public static NilType INSTANCE = new NilType();

  @Override
  public TypeEnum type() {
    return TypeEnum.NIL;
  }

  @Override
  public String toString() {
    return name();
  }
}
