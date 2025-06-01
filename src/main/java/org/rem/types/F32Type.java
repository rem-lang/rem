package org.rem.types;

import org.rem.enums.TypeEnum;
import org.rem.interfaces.IType;

public final class F32Type implements IType {
  public static F32Type INSTANCE = new F32Type();

  @Override
  public TypeEnum type() {
    return TypeEnum.F32;
  }

  @Override
  public String toString() {
    return name();
  }
}
