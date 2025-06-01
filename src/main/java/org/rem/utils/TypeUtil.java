package org.rem.utils;

import org.rem.enums.TypeEnum;
import org.rem.interfaces.IType;

public class TypeUtil {
  public static IType max(IType a, IType b) {
    return a.type().compareTo(b.type()) > 0 ? a : b;
  }

  public static boolean isIntegerType(IType type) {
    return type.type().compareTo(TypeEnum.I8) >= 0 &&
      type.type().compareTo(TypeEnum.I128) <= 0;
  }

  public static boolean isNumericType(IType type) {
    return type.type().compareTo(TypeEnum.I8) >= 0 &&
      type.type().compareTo(TypeEnum.F128) <= 0;
  }

  public static boolean isNil(IType type) {
    return type.type() == TypeEnum.NIL;
  }

  public static boolean isBoolean(IType type) {
    return type.type() == TypeEnum.BOOL;
  }
}
