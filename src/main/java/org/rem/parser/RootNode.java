package org.rem.parser;

import org.rem.interfaces.IBaseVisitor;
import org.rem.interfaces.IBaseVoidVisitor;
import org.rem.parser.ast.AST;
import org.rem.parser.ast.Statement;

import java.util.List;

public class RootNode extends AST {
  private final List<Statement> statementList;

  public RootNode(List<Statement> statementList) {
    this.statementList = statementList;
  }

  public void visit(IBaseVisitor<?> baseVisitor) {
    for(var stmt : statementList) {
      stmt.accept(baseVisitor);
    }
  }

  public void visit(IBaseVoidVisitor voidVisitor) {
    for(var stmt : statementList) {
      stmt.accept(voidVisitor);
    }
  }
}
