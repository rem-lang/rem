package org.rem.types;

import org.jspecify.annotations.NonNull;
import org.rem.enums.TypeEnum;
import org.rem.interfaces.IType;

public final class VectorType implements IType {

  private IType type;
  private int length;

  public VectorType(IType type, int length) {
    this.type = type;
    this.length = length;
  }

  public VectorType(IType type) {
    this(type, 0);
  }

  public IType getType() {
    return type;
  }

  public int getLength() {
    return length;
  }

  public void setLength(int length) {
    this.length = length;
  }

  @Override
  public TypeEnum type() {
    return TypeEnum.VECTOR;
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
    return type.type() == TypeEnum.VECTOR && ((VectorType) type).getType().isAssignableFrom(this.type);
  }

  @Override
  public boolean isAssignableFrom(IType type) {
    return isAssignableTo(type) || type == NilType.INSTANCE;
  }

  public void setType(IType type) {
    this.type = type;
  }
}
