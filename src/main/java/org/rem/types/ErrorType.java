package org.rem.types;

import org.jspecify.annotations.NonNull;
import org.rem.enums.TypeEnum;
import org.rem.interfaces.IType;

/**
 * Used for reporting errors during type inference and semantic analysis.
 */
public final class ErrorType implements IType {
  private String name;

  public ErrorType(String name) {
    this.name = name;
  }

  @Override
  public TypeEnum type() {
    return TypeEnum.ERROR;
  }

  @Override
  public boolean isPrimitive() {
    return false;
  }

  @Override
  public @NonNull String name() {
    return "unknown type " + name;
  }

  @Override
  public String toString() {
    return name();
  }
}
