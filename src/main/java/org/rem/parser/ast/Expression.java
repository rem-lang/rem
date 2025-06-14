// DO NOT MODIFY DIRECTLY
// This file was generated by the tools.rem.GenerateAst
package org.rem.parser.ast;

import org.rem.parser.Token;
import java.util.List;

public abstract class Expression extends AST {
  public abstract<T> T accept(Visitor<T> visitor);
  public abstract void accept(VoidVisitor visitor);

  @Override public String astName() {
    return "expression";
  }

  public interface Visitor<T> {
    T visitNilExpression(Nil expression);
    T visitBooleanExpression(Boolean expression);
    T visitInt32Expression(Int32 expression);
    T visitInt64Expression(Int64 expression);
    T visitFloat32Expression(Float32 expression);
    T visitFloat64Expression(Float64 expression);
    T visitLiteralExpression(Literal expression);
    T visitUnaryExpression(Unary expression);
    T visitBinaryExpression(Binary expression);
    T visitLogicalExpression(Logical expression);
    T visitRangeExpression(Range expression);
    T visitGroupingExpression(Grouping expression);
    T visitIncrementExpression(Increment expression);
    T visitDecrementExpression(Decrement expression);
    T visitIdentifierExpression(Identifier expression);
    T visitArrayExpression(Array expression);
    T visitTypedNameExpression(TypedName expression);
    T visitAssignExpression(Assign expression);
    T visitUpdateExpression(Update expression);
    T visitConditionExpression(Condition expression);
    T visitCallExpression(Call expression);
    T visitGetExpression(Get expression);
    T visitSetExpression(Set expression);
    T visitIndexExpression(Index expression);
    T visitSliceExpression(Slice expression);
    T visitDictExpression(Dict expression);
    T visitNewExpression(New expression);
    T visitParentExpression(Parent expression);
    T visitSelfExpression(Self expression);
    T visitAnonymousExpression(Anonymous expression);
    T visitExpression(Expression expression);
  }


  public interface VoidVisitor {
    void visitNilExpression(Nil expression);
    void visitBooleanExpression(Boolean expression);
    void visitInt32Expression(Int32 expression);
    void visitInt64Expression(Int64 expression);
    void visitFloat32Expression(Float32 expression);
    void visitFloat64Expression(Float64 expression);
    void visitLiteralExpression(Literal expression);
    void visitUnaryExpression(Unary expression);
    void visitBinaryExpression(Binary expression);
    void visitLogicalExpression(Logical expression);
    void visitRangeExpression(Range expression);
    void visitGroupingExpression(Grouping expression);
    void visitIncrementExpression(Increment expression);
    void visitDecrementExpression(Decrement expression);
    void visitIdentifierExpression(Identifier expression);
    void visitArrayExpression(Array expression);
    void visitTypedNameExpression(TypedName expression);
    void visitAssignExpression(Assign expression);
    void visitUpdateExpression(Update expression);
    void visitConditionExpression(Condition expression);
    void visitCallExpression(Call expression);
    void visitGetExpression(Get expression);
    void visitSetExpression(Set expression);
    void visitIndexExpression(Index expression);
    void visitSliceExpression(Slice expression);
    void visitDictExpression(Dict expression);
    void visitNewExpression(New expression);
    void visitParentExpression(Parent expression);
    void visitSelfExpression(Self expression);
    void visitAnonymousExpression(Anonymous expression);
    void visitExpression(Expression expression);
  }

  public static class Nil extends Expression {
    public Nil() {
    }

    public <T> T accept(Visitor<T> visitor) {
      return visitor.visitNilExpression(this);
    }

    public void accept(VoidVisitor visitor) {
      visitor.visitNilExpression(this);
    }

    @Override public String astName() {
      return "nil expression";
    }
  }

  public static class Boolean extends Expression {
    public final boolean value;

    public Boolean(boolean value) {
      this.value = value;
    }

    public <T> T accept(Visitor<T> visitor) {
      return visitor.visitBooleanExpression(this);
    }

    public void accept(VoidVisitor visitor) {
      visitor.visitBooleanExpression(this);
    }

    @Override public String astName() {
      return "boolean expression";
    }
  }

  public static class Int32 extends Expression {
    public final int value;

    public Int32(int value) {
      this.value = value;
    }

    public <T> T accept(Visitor<T> visitor) {
      return visitor.visitInt32Expression(this);
    }

    public void accept(VoidVisitor visitor) {
      visitor.visitInt32Expression(this);
    }

    @Override public String astName() {
      return "int32 expression";
    }
  }

  public static class Int64 extends Expression {
    public final long value;

    public Int64(long value) {
      this.value = value;
    }

    public <T> T accept(Visitor<T> visitor) {
      return visitor.visitInt64Expression(this);
    }

    public void accept(VoidVisitor visitor) {
      visitor.visitInt64Expression(this);
    }

    @Override public String astName() {
      return "int64 expression";
    }
  }

  public static class Float32 extends Expression {
    public final Float value;

    public Float32(Float value) {
      this.value = value;
    }

    public <T> T accept(Visitor<T> visitor) {
      return visitor.visitFloat32Expression(this);
    }

    public void accept(VoidVisitor visitor) {
      visitor.visitFloat32Expression(this);
    }

    @Override public String astName() {
      return "float32 expression";
    }
  }

  public static class Float64 extends Expression {
    public final Double value;

    public Float64(Double value) {
      this.value = value;
    }

    public <T> T accept(Visitor<T> visitor) {
      return visitor.visitFloat64Expression(this);
    }

    public void accept(VoidVisitor visitor) {
      visitor.visitFloat64Expression(this);
    }

    @Override public String astName() {
      return "float64 expression";
    }
  }

  public static class Literal extends Expression {
    public final Token token;

    public Literal(Token token) {
      this.token = token;
    }

    public <T> T accept(Visitor<T> visitor) {
      return visitor.visitLiteralExpression(this);
    }

    public void accept(VoidVisitor visitor) {
      visitor.visitLiteralExpression(this);
    }

    @Override public String astName() {
      return "literal expression";
    }
  }

  public static class Unary extends Expression {
    public final Token op;
    public final Expression right;

    public Unary(Token op, Expression right) {
      this.op = op;
      this.right = right;
    }

    public <T> T accept(Visitor<T> visitor) {
      return visitor.visitUnaryExpression(this);
    }

    public void accept(VoidVisitor visitor) {
      visitor.visitUnaryExpression(this);
    }

    @Override public String astName() {
      return "unary expression";
    }
  }

  public static class Binary extends Expression {
    public final Expression left;
    public final Token op;
    public final Expression right;

    public Binary(Expression left, Token op, Expression right) {
      this.left = left;
      this.op = op;
      this.right = right;
    }

    public <T> T accept(Visitor<T> visitor) {
      return visitor.visitBinaryExpression(this);
    }

    public void accept(VoidVisitor visitor) {
      visitor.visitBinaryExpression(this);
    }

    @Override public String astName() {
      return "binary expression";
    }
  }

  public static class Logical extends Expression {
    public final Expression left;
    public final Token op;
    public final Expression right;

    public Logical(Expression left, Token op, Expression right) {
      this.left = left;
      this.op = op;
      this.right = right;
    }

    public <T> T accept(Visitor<T> visitor) {
      return visitor.visitLogicalExpression(this);
    }

    public void accept(VoidVisitor visitor) {
      visitor.visitLogicalExpression(this);
    }

    @Override public String astName() {
      return "logical expression";
    }
  }

  public static class Range extends Expression {
    public final Expression lower;
    public final Expression upper;

    public Range(Expression lower, Expression upper) {
      this.lower = lower;
      this.upper = upper;
    }

    public <T> T accept(Visitor<T> visitor) {
      return visitor.visitRangeExpression(this);
    }

    public void accept(VoidVisitor visitor) {
      visitor.visitRangeExpression(this);
    }

    @Override public String astName() {
      return "range expression";
    }
  }

  public static class Grouping extends Expression {
    public final Expression expression;

    public Grouping(Expression expression) {
      this.expression = expression;
    }

    public <T> T accept(Visitor<T> visitor) {
      return visitor.visitGroupingExpression(this);
    }

    public void accept(VoidVisitor visitor) {
      visitor.visitGroupingExpression(this);
    }

    @Override public String astName() {
      return "grouping expression";
    }
  }

  public static class Increment extends Expression {
    public final Expression expression;

    public Increment(Expression expression) {
      this.expression = expression;
    }

    public <T> T accept(Visitor<T> visitor) {
      return visitor.visitIncrementExpression(this);
    }

    public void accept(VoidVisitor visitor) {
      visitor.visitIncrementExpression(this);
    }

    @Override public String astName() {
      return "increment expression";
    }
  }

  public static class Decrement extends Expression {
    public final Expression expression;

    public Decrement(Expression expression) {
      this.expression = expression;
    }

    public <T> T accept(Visitor<T> visitor) {
      return visitor.visitDecrementExpression(this);
    }

    public void accept(VoidVisitor visitor) {
      visitor.visitDecrementExpression(this);
    }

    @Override public String astName() {
      return "decrement expression";
    }
  }

  public static class Identifier extends Expression {
    public final Token token;

    public Identifier(Token token) {
      this.token = token;
    }

    public <T> T accept(Visitor<T> visitor) {
      return visitor.visitIdentifierExpression(this);
    }

    public void accept(VoidVisitor visitor) {
      visitor.visitIdentifierExpression(this);
    }

    @Override public String astName() {
      return "identifier expression";
    }
  }

  public static class Array extends Expression {
    public final List<Expression> items;

    public Array(List<Expression> items) {
      this.items = items;
    }

    public <T> T accept(Visitor<T> visitor) {
      return visitor.visitArrayExpression(this);
    }

    public void accept(VoidVisitor visitor) {
      visitor.visitArrayExpression(this);
    }

    @Override public String astName() {
      return "array expression";
    }
  }

  public static class TypedName extends Expression {
    public final Identifier name;
    public final Typed type;

    public TypedName(Identifier name, Typed type) {
      this.name = name;
      this.type = type;
    }

    public <T> T accept(Visitor<T> visitor) {
      return visitor.visitTypedNameExpression(this);
    }

    public void accept(VoidVisitor visitor) {
      visitor.visitTypedNameExpression(this);
    }

    @Override public String astName() {
      return "typedname expression";
    }
  }

  public static class Assign extends Expression {
    public final Expression expression;
    public final Expression value;

    public Assign(Expression expression, Expression value) {
      this.expression = expression;
      this.value = value;
    }

    public <T> T accept(Visitor<T> visitor) {
      return visitor.visitAssignExpression(this);
    }

    public void accept(VoidVisitor visitor) {
      visitor.visitAssignExpression(this);
    }

    @Override public String astName() {
      return "assign expression";
    }
  }

  public static class Update extends Expression {
    public final Expression expression;
    public final Token op;
    public final Expression value;

    public Update(Expression expression, Token op, Expression value) {
      this.expression = expression;
      this.op = op;
      this.value = value;
    }

    public <T> T accept(Visitor<T> visitor) {
      return visitor.visitUpdateExpression(this);
    }

    public void accept(VoidVisitor visitor) {
      visitor.visitUpdateExpression(this);
    }

    @Override public String astName() {
      return "update expression";
    }
  }

  public static class Condition extends Expression {
    public final Expression expression;
    public final Expression truth;
    public final Expression falsy;

    public Condition(Expression expression, Expression truth, Expression falsy) {
      this.expression = expression;
      this.truth = truth;
      this.falsy = falsy;
    }

    public <T> T accept(Visitor<T> visitor) {
      return visitor.visitConditionExpression(this);
    }

    public void accept(VoidVisitor visitor) {
      visitor.visitConditionExpression(this);
    }

    @Override public String astName() {
      return "condition expression";
    }
  }

  public static class Call extends Expression {
    public final Expression callee;
    public final List<Expression> args;

    public Call(Expression callee, List<Expression> args) {
      this.callee = callee;
      this.args = args;
    }

    public <T> T accept(Visitor<T> visitor) {
      return visitor.visitCallExpression(this);
    }

    public void accept(VoidVisitor visitor) {
      visitor.visitCallExpression(this);
    }

    @Override public String astName() {
      return "call expression";
    }
  }

  public static class Get extends Expression {
    public final Expression expression;
    public final Identifier name;

    public Get(Expression expression, Identifier name) {
      this.expression = expression;
      this.name = name;
    }

    public <T> T accept(Visitor<T> visitor) {
      return visitor.visitGetExpression(this);
    }

    public void accept(VoidVisitor visitor) {
      visitor.visitGetExpression(this);
    }

    @Override public String astName() {
      return "get expression";
    }
  }

  public static class Set extends Expression {
    public final Expression expression;
    public final Identifier name;
    public final Expression value;

    public Set(Expression expression, Identifier name, Expression value) {
      this.expression = expression;
      this.name = name;
      this.value = value;
    }

    public <T> T accept(Visitor<T> visitor) {
      return visitor.visitSetExpression(this);
    }

    public void accept(VoidVisitor visitor) {
      visitor.visitSetExpression(this);
    }

    @Override public String astName() {
      return "set expression";
    }
  }

  public static class Index extends Expression {
    public final Expression callee;
    public final Expression argument;

    public Index(Expression callee, Expression argument) {
      this.callee = callee;
      this.argument = argument;
    }

    public <T> T accept(Visitor<T> visitor) {
      return visitor.visitIndexExpression(this);
    }

    public void accept(VoidVisitor visitor) {
      visitor.visitIndexExpression(this);
    }

    @Override public String astName() {
      return "index expression";
    }
  }

  public static class Slice extends Expression {
    public final Expression callee;
    public final Expression lower;
    public final Expression upper;

    public Slice(Expression callee, Expression lower, Expression upper) {
      this.callee = callee;
      this.lower = lower;
      this.upper = upper;
    }

    public <T> T accept(Visitor<T> visitor) {
      return visitor.visitSliceExpression(this);
    }

    public void accept(VoidVisitor visitor) {
      visitor.visitSliceExpression(this);
    }

    @Override public String astName() {
      return "slice expression";
    }
  }

  public static class Dict extends Expression {
    public final List<Expression> keys;
    public final List<Expression> values;

    public Dict(List<Expression> keys, List<Expression> values) {
      this.keys = keys;
      this.values = values;
    }

    public <T> T accept(Visitor<T> visitor) {
      return visitor.visitDictExpression(this);
    }

    public void accept(VoidVisitor visitor) {
      visitor.visitDictExpression(this);
    }

    @Override public String astName() {
      return "dict expression";
    }
  }

  public static class New extends Expression {
    public final Expression expression;

    public New(Expression expression) {
      this.expression = expression;
    }

    public <T> T accept(Visitor<T> visitor) {
      return visitor.visitNewExpression(this);
    }

    public void accept(VoidVisitor visitor) {
      visitor.visitNewExpression(this);
    }

    @Override public String astName() {
      return "new expression";
    }
  }

  public static class Parent extends Expression {
    public Parent() {
    }

    public <T> T accept(Visitor<T> visitor) {
      return visitor.visitParentExpression(this);
    }

    public void accept(VoidVisitor visitor) {
      visitor.visitParentExpression(this);
    }

    @Override public String astName() {
      return "parent expression";
    }
  }

  public static class Self extends Expression {
    public Self() {
    }

    public <T> T accept(Visitor<T> visitor) {
      return visitor.visitSelfExpression(this);
    }

    public void accept(VoidVisitor visitor) {
      visitor.visitSelfExpression(this);
    }

    @Override public String astName() {
      return "self expression";
    }
  }

  public static class Anonymous extends Expression {
    public final Statement.Function function;

    public Anonymous(Statement.Function function) {
      this.function = function;
    }

    public <T> T accept(Visitor<T> visitor) {
      return visitor.visitAnonymousExpression(this);
    }

    public void accept(VoidVisitor visitor) {
      visitor.visitAnonymousExpression(this);
    }

    @Override public String astName() {
      return "anonymous expression";
    }
  }
}
