package org.rem.compiler;

import norswap.uranium.Reactor;
import org.rem.interfaces.ICompileTarget;
import org.rem.parser.ast.Expression;
import org.rem.parser.ast.Statement;
import org.rem.parser.ast.Typed;

public abstract class BaseCompileTarget<T> implements ICompileTarget<T> {
  protected Reactor R;

  public BaseCompileTarget(Reactor reactor) {
    this.R = reactor;
  }

  @Override
  public T visitNilExpression(Expression.Nil expr) {
    return null;
  }

  @Override
  public T visitBooleanExpression(Expression.Boolean expr) {
    return null;
  }

  @Override
  public T visitInt32Expression(Expression.Int32 expr) {
    return null;
  }

  @Override
  public T visitInt64Expression(Expression.Int64 expr) {
    return null;
  }

  @Override
  public T visitFloat32Expression(Expression.Float32 expr) {
    return null;
  }

  @Override
  public T visitFloat64Expression(Expression.Float64 expr) {
    return null;
  }

  @Override
  public T visitLiteralExpression(Expression.Literal expr) {
    return null;
  }

  @Override
  public T visitUnaryExpression(Expression.Unary expr) {
    return null;
  }

  @Override
  public T visitBinaryExpression(Expression.Binary expr) {
    return null;
  }

  @Override
  public T visitLogicalExpression(Expression.Logical expr) {
    return null;
  }

  @Override
  public T visitRangeExpression(Expression.Range expr) {
    return null;
  }

  @Override
  public T visitGroupingExpression(Expression.Grouping expr) {
    return null;
  }

  @Override
  public T visitIdentifierExpression(Expression.Identifier expr) {
    return null;
  }

  @Override
  public T visitTypedNameExpression(Expression.TypedName expr) {
    return null;
  }

  @Override
  public T visitConditionExpression(Expression.Condition expr) {
    return null;
  }

  @Override
  public T visitCallExpression(Expression.Call expr) {
    return null;
  }

  @Override
  public T visitGetExpression(Expression.Get expr) {
    return null;
  }

  @Override
  public T visitSetExpression(Expression.Set expr) {
    return null;
  }

  @Override
  public T visitIndexExpression(Expression.Index expr) {
    return null;
  }

  @Override
  public T visitSliceExpression(Expression.Slice expr) {
    return null;
  }

  @Override
  public T visitArrayExpression(Expression.Array expr) {
    return null;
  }

  @Override
  public T visitDictExpression(Expression.Dict expr) {
    return null;
  }

  @Override
  public T visitNewExpression(Expression.New expr) {
    return null;
  }

  @Override
  public T visitParentExpression(Expression.Parent expr) {
    return null;
  }

  @Override
  public T visitSelfExpression(Expression.Self expr) {
    return null;
  }

  @Override
  public T visitAssignExpression(Expression.Assign expr) {
    return null;
  }

  @Override
  public T visitUpdateExpression(Expression.Update expression) {
    return null;
  }

  @Override
  public T visitAnonymousExpression(Expression.Anonymous expr) {
    return null;
  }

  @Override
  public T visitExpression(Expression expression) {
    return null;
  }

  @Override
  public T visitEchoStatement(Statement.Echo stmt) {
    return null;
  }

  @Override
  public T visitSimpleStatement(Statement.Simple statement) {
    return null;
  }

  @Override
  public T visitIfStatement(Statement.If stmt) {
    return null;
  }

  @Override
  public T visitForStatement(Statement.For stmt) {
    return null;
  }

  @Override
  public T visitWhileStatement(Statement.While stmt) {
    return null;
  }

  @Override
  public T visitDoWhileStatement(Statement.DoWhile stmt) {
    return null;
  }

  @Override
  public T visitContinueStatement(Statement.Continue stmt) {
    return null;
  }

  @Override
  public T visitBreakStatement(Statement.Break stmt) {
    return null;
  }

  @Override
  public T visitRaiseStatement(Statement.Raise stmt) {
    return null;
  }

  @Override
  public T visitReturnStatement(Statement.Return stmt) {
    return null;
  }

  @Override
  public T visitAssertStatement(Statement.Assert stmt) {
    return null;
  }

  @Override
  public T visitUsingStatement(Statement.Using stmt) {
    return null;
  }

  @Override
  public T visitImportStatement(Statement.Import stmt) {
    return null;
  }

  @Override
  public T visitCatchStatement(Statement.Catch stmt) {
    return null;
  }

  @Override
  public T visitBlockStatement(Statement.Block stmt) {
    return null;
  }

  @Override
  public T visitVarStatement(Statement.Var stmt) {
    return null;
  }

  @Override
  public T visitFunctionStatement(Statement.Function stmt) {
    return null;
  }

  @Override
  public T visitMethodStatement(Statement.Method stmt) {
    return null;
  }

  @Override
  public T visitPropertyStatement(Statement.Property stmt) {
    return null;
  }

  @Override
  public T visitClassStatement(Statement.Class stmt) {
    return null;
  }

  @Override
  public T visitVarListStatement(Statement.VarList stmt) {
    return null;
  }

  @Override
  public T visitStatement(Statement statement) {
    return null;
  }

  @Override
  public T visitVoidTyped(Typed.Void typed) {
    return null;
  }

  @Override
  public T visitIdTyped(Typed.Id typed) {
    return null;
  }

  @Override
  public T visitArrayTyped(Typed.Array typed) {
    return null;
  }

  @Override
  public T visitVectorTyped(Typed.Vector typed) {
    return null;
  }

  @Override
  public T visitMapTyped(Typed.Map typed) {
    return null;
  }

  @Override
  public T visitTyped(Typed typed) {
    return null;
  }

  @Override
  public T visitExternStatement(Statement.Extern statement) {
    return null;
  }

  @Override
  public T visitDecrementExpression(Expression.Decrement expression) {
    return null;
  }

  @Override
  public T visitIncrementExpression(Expression.Increment expression) {
    return null;
  }
}
