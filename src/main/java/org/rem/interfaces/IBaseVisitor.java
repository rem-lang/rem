package org.rem.interfaces;

import org.rem.parser.ast.Expression;
import org.rem.parser.ast.Statement;

public interface IBaseVisitor<T> extends Expression.Visitor<T>, Statement.Visitor<T> {
}
