package org.rem.types;

import org.jspecify.annotations.NonNull;
import org.rem.enums.TypeEnum;
import org.rem.interfaces.IType;
import org.rem.parser.ast.AST;
import org.rem.parser.ast.Statement;

public final class ClassType implements IType {

  private final Statement.Class declaration;
  private final ClassType superClass;

  public ClassType(Statement.Class declaration, ClassType superClass) {
    this.declaration = declaration;
    this.superClass = superClass;
  }

  public ClassType(Statement.Class declaration) {
    this(declaration, null);
  }

  public Statement.Class getDeclaration() {
    return declaration;
  }

  public ClassType getSuperClass() {
    return superClass;
  }

  @Override
  public TypeEnum type() {
    return TypeEnum.CLASS;
  }

  @Override
  public @NonNull String name() {
    return declaration.name.literal();
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
  public boolean isAssignableFrom(IType type) {
    if(type == this || type == NilType.INSTANCE) return true;
    if(!(type instanceof ClassType classType)) return false;

    // check the same class
    if(declaration == classType.declaration) return true;

    // check if type is a subclass of this class
    for(ClassType sClass = classType.superClass; sClass != null; sClass = sClass.superClass) {
      if(sClass == this) {
        return true;
      }
    }

    return false;
  }

  @Override
  public boolean isAssignableTo(IType type) {
    if(type == this) return true;
    if(!(type instanceof ClassType classType)) return false;

    // check the same class
    if(declaration == classType.declaration) return true;

    // check if type is this class's super class.
    for(ClassType sClass = superClass; sClass != null; sClass = sClass.superClass) {
      if(sClass == classType) {
        return true;
      }
    }

    return false;
  }
}
