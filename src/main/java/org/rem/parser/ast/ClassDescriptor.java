package org.rem.parser.ast;

import org.rem.parser.Token;

public class ClassDescriptor {
  public Token name;
  public Token keyName;
  public Token valueName;
  public ClassDescriptor parent;

  public ClassDescriptor(Token name, Token keyName, Token valueName, ClassDescriptor parent) {
    this.name = name;
    this.keyName = keyName;
    this.valueName = valueName;
    this.parent = parent;
  }
}
