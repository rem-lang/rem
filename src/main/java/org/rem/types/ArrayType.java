package org.rem.types;

import org.jspecify.annotations.NonNull;
import org.rem.enums.TypeEnum;
import org.rem.interfaces.IType;

public final class ArrayType implements IType {

  private IType type;
  private long length;

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

  public void setLength(long length) {
    this.length = length;
  }

  @Override
  public TypeEnum type() {
    return TypeEnum.ARRAY;
  }

  @Override
  public @NonNull String name() {
    return "[]" + type.name();
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
  public boolean isAssignableTo(IType type) {
    return type.type() == TypeEnum.ARRAY && ((ArrayType) type).getType().isAssignableFrom(this.type);
  }

  @Override
  public boolean isAssignableFrom(IType type) {
    return isAssignableTo(type) || type == NilType.INSTANCE;
  }

  public void setType(IType type) {
    this.type = type;
  }
}
