package org.rem.interfaces;

import org.rem.parser.ast.Expression;
import org.rem.parser.ast.Statement;

public interface IBaseVoidVisitor extends Expression.VoidVisitor, Statement.VoidVisitor {
}
