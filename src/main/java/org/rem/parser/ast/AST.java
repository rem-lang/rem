package org.rem.parser.ast;

import norswap.uranium.Attribute;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;

public class AST implements Cloneable {
  public int startLine = 1;
  public int endLine = 1;
  public int startColumn = 0;
  public int endColumn = 0;
  public boolean wrapped = false;

  public final Attribute attr (String name) {
    return new Attribute(this, name);
  }

  private Field[] getFields() {
    return Arrays.stream(this.getClass().getFields())
      .filter(f -> Modifier.isPublic(f.getModifiers()))
      .toArray(Field[]::new);
  }

  public String astName() {
    return "ast";
  }

  @Override
  public AST clone() {
    try {
      AST clone = (AST) super.clone();
      // TODO: copy mutable state here, so the clone can't change the internals of the original
      return clone;
    } catch (CloneNotSupportedException e) {
      throw new AssertionError();
    }
  }
}
