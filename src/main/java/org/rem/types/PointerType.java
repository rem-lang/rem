package org.rem.types;

import org.jspecify.annotations.NonNull;
import org.rem.enums.TypeEnum;
import org.rem.interfaces.IType;

public final class PointerType implements IType {

  private IType type;

  public PointerType(IType type) {
    this.type = type;
  }

  public IType getType() {
    return type;
  }

  @Override
  public TypeEnum type() {
    return TypeEnum.POINTER;
  }

  @Override
  public @NonNull String name() {
    return type.name();
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
    return this == type;
  }

  @Override
  public boolean isAssignableFrom(IType type) {
    return this == type || this.type.isAssignableFrom(type) || type == NilType.INSTANCE;
  }

  public void setType(IType type) {
    this.type = type;
  }
}
