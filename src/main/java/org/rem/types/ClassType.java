package org.rem.types;

import org.jspecify.annotations.NonNull;
import org.rem.enums.TypeEnum;
import org.rem.interfaces.IType;
import org.rem.parser.ast.Statement;

public final class ClassType implements IType {

  private final Statement.Class declaration;

  public ClassType(Statement.Class declaration) {
    this.declaration = declaration;
  }

  public Statement.Class getDeclaration() {
    return declaration;
  }

  @Override
  public TypeEnum type() {
    return TypeEnum.CLASS;
  }

  @Override
  public @NonNull String name() {
    return declaration.descriptor.name.literal();
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
