package org.rem.utils;

import org.rem.enums.TypeEnum;
import org.rem.interfaces.IType;
import org.rem.types.F128Type;
import org.rem.types.F32Type;
import org.rem.types.I128Type;
import org.rem.types.I8Type;

public class TypeUtil {
  public static IType max(IType a, IType b) {
    return a.greaterOrEqual(b) ? a : b;
  }

  public static boolean isIntegerType(IType type) {
    return type.greaterOrEqual(I8Type.INSTANCE) && type.lessOrEqual(I128Type.INSTANCE);
  }

  public static boolean isFloatType(IType type) {
    return type.greaterOrEqual(F32Type.INSTANCE) &&
      type.lessOrEqual(F128Type.INSTANCE);
  }

  public static boolean isNumericType(IType type) {
    return type.greaterOrEqual(I8Type.INSTANCE) &&
      type.lessOrEqual(F128Type.INSTANCE);
  }

  public static boolean isNil(IType type) {
    return type.type() == TypeEnum.NIL;
  }

  public static boolean isBoolean(IType type) {
    return type.type() == TypeEnum.BOOL;
  }

  public static boolean isArray(IType type) {
    return type.type() == TypeEnum.ARRAY;
  }

  public static boolean isMap(IType type) {
    return type.type() == TypeEnum.MAPPED;
  }
}
