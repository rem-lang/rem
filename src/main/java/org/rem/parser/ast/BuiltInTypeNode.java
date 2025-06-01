package org.rem.parser.ast;

import org.rem.enums.DeclarationKind;

public class BuiltInTypeNode extends Statement {
  private final String name;
  private final DeclarationKind kind;

  public BuiltInTypeNode(String name, DeclarationKind kind) {
    this.name = name;
    this.kind = kind;
  }

  public String getName() {
    return name;
  }

  public DeclarationKind kind() {
    return kind;
  }

  @Override
  public <T> T accept(Visitor<T> visitor) {
    return null;
  }

  @Override
  public void accept(VoidVisitor visitor) {
  }

  @Override
  public String astName() {
    return name;
  }
}
