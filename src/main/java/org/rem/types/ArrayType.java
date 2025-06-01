package org.rem.types;

import org.jspecify.annotations.NonNull;
import org.rem.enums.TypeEnum;
import org.rem.interfaces.IType;

public final class ArrayType implements IType {

  private final IType type;
  private final long length;

  public ArrayType(IType type, long length) {
    this.type = type;
    this.length = length;
  }

  public ArrayType(IType type) {
    this(type, 0);
  }

  public IType getType() {
    return type;
  }

  public long getLength() {
    return length;
  }

  @Override
  public TypeEnum type() {
    return TypeEnum.ARRAY;
  }

  @Override
  public @NonNull String name() {
    return type.name() + "[]";
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
