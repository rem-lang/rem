package org.rem.types;

import org.jspecify.annotations.NonNull;
import org.rem.enums.TypeEnum;
import org.rem.interfaces.IType;

import java.util.Arrays;

public final class DefType implements IType {

  private final IType returnType;
  private final IType[] parameterTypes;
  private final boolean isVariadic;

  public DefType(IType returnType, boolean isVariadic, IType... parameterTypes) {
    this.returnType = returnType;
    this.parameterTypes = parameterTypes;
    this.isVariadic = isVariadic;
  }

  public DefType(IType returnType, IType... parameterTypes) {
    this(returnType, false, parameterTypes);
  }

  public DefType(IType type) {
    this(type, new IType[0]);
  }

  public IType getReturnType() {
    return returnType;
  }

  public IType[] getParameterTypes() {
    return parameterTypes;
  }

  public boolean isVariadic() {
    return isVariadic;
  }

  @Override
  public TypeEnum type() {
    return TypeEnum.DEF;
  }

  @Override
  public @NonNull String name() {
    StringBuilder builder = new StringBuilder();
    builder.append("(");

    for(int i = 0; i < parameterTypes.length; i++) {
      builder.append(parameterTypes[i].name());
      if(i < parameterTypes.length - 1) {
        builder.append(", ");
      }
    }

    builder.append(") -> ");

    return builder.append(returnType.name()).toString();
  }

  @Override
  public boolean isPrimitive() {
    return false;
  }

  @Override
  public String toString() {
    return name();
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) return true;
    if(!(obj instanceof DefType type)) return false;

    return returnType.equals(type.returnType) && Arrays.equals(parameterTypes, type.parameterTypes);
  }

  @Override
  public boolean isAssignableTo(IType type) {
    return equals(type);
  }

  @Override
  public boolean isAssignableFrom(IType type) {
    return isAssignableTo(type);
  }
}
