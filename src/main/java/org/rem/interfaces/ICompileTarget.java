package org.rem.interfaces;

import org.rem.parser.ast.Statement;
import org.rem.compiler.CompileResult;
import org.rem.parser.ast.Typed;

import java.util.ArrayList;
import java.util.List;

public interface ICompileTarget<T> extends IBaseVisitor<T>, Typed.Visitor<T> {

  default CompileResult<T> compile(List<Statement> statementList) {
    List<T> nodes = new ArrayList<>();

    // 1. Add all functions first.
    // This allows using functions in statements (such as other functions)
    // before being defined
    for (Statement statement : statementList) {
      if (statement instanceof Statement.Function function) {
        nodes.add(visitFunctionStatement(function));
      }
    }

    // 2. Add everything not a function after
    for (Statement statement : statementList) {
      if (statement != null && !(statement instanceof Statement.Function)) {
        nodes.add(visitStatement(statement));
      }
    }

    return new CompileResult<>(nodes);
  }
}
