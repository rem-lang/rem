package org.rem.types;

import org.rem.enums.TypeEnum;
import org.rem.interfaces.IType;

public final class I8Type implements IType {
  public static I8Type INSTANCE = new I8Type();

  @Override
  public TypeEnum type() {
    return TypeEnum.I8;
  }

  @Override
  public String toString() {
    return name();
  }
}
