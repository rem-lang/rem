package org.rem.types;

import org.rem.enums.TypeEnum;
import org.rem.interfaces.IType;

public final class I64Type implements IType {
  public static I64Type INSTANCE = new I64Type();

  @Override
  public TypeEnum type() {
    return TypeEnum.I64;
  }

  @Override
  public String toString() {
    return name();
  }
}
