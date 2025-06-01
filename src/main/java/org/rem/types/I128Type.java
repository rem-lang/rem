package org.rem.types;

import org.rem.enums.TypeEnum;
import org.rem.interfaces.IType;

public final class I128Type implements IType {
  public static I128Type INSTANCE = new I128Type();

  @Override
  public TypeEnum type() {
    return TypeEnum.I128;
  }

  @Override
  public String toString() {
    return name();
  }
}
