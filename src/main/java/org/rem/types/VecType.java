package org.rem.types;

import org.jspecify.annotations.NonNull;
import org.rem.enums.TypeEnum;
import org.rem.interfaces.IType;

public final class VecType implements IType {

  private final IType type;
  private final int length;

  public VecType(IType type, int length) {
    this.type = type;
    this.length = length;
  }

  public VecType(IType type) {
    this(type, 0);
  }

  public IType getType() {
    return type;
  }

  public int getLength() {
    return length;
  }

  @Override
  public TypeEnum type() {
    return TypeEnum.PARAMETERIZED;
  }

  @Override
  public @NonNull String name() {
    return "Vec<" + type.name() + ">";
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
