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
}
