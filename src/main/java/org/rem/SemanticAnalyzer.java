package org.rem;

import norswap.uranium.Attribute;
import norswap.uranium.Reactor;
import norswap.uranium.Rule;
import org.rem.enums.DeclarationKind;
import org.rem.interfaces.IType;
import org.rem.parser.ast.*;
import org.rem.scope.DeclarationContext;
import org.rem.scope.RootScope;
import org.rem.scope.Scope;
import org.rem.types.*;
import org.rem.utils.TypeUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;


// TODO: Detect loops that always evaluate to true without a break statement and warn
// TODO: Detect loops that always evaluate to false and mark it for removal in final compilation
public final class SemanticAnalyzer implements Expression.VoidVisitor, Statement.VoidVisitor, Typed.VoidVisitor {

  private final Reactor R;
  private Scope scope;

  /**
   * Current context for type inference (currently only to infer the type of empty arrays).
   */
  private AST inferenceContext;

  /**
   * Index of the current function argument.
   */
  private int argumentIndex;

  public SemanticAnalyzer(Reactor reactor) {
    this.R = reactor;
    scope = new RootScope(reactor);
  }

  private static String arithmeticError(Expression.Binary node, Object left, Object right) {
    return String.format(
      "Invalid arithemetic operation %s on type %s and %s",
      node.op.literal(), left, right
    );
  }

  private static String bitwiseError(Expression.Binary node, Object left, Object right) {
    return String.format(
      "Invalid bitwise operation %s on type %s and %s",
      node.op.literal(), left, right
    );
  }

  private static boolean isComparableTo(IType a, IType b) {
    if (a == VoidType.INSTANCE || b == VoidType.INSTANCE)
      return false;

    // TODO: Handle other types
    return a.isReference() && b.isReference()
      || a.equals(b)
      || TypeUtil.isNumericType(a) && TypeUtil.isNumericType(b);
  }

  /**
   * Indicates whether a value of type {@code a} can be assigned to a location (variable,
   * parameter, ...) of type {@code b}.
   */
  private static boolean isAssignableTo(IType a, IType b) {
    if (a == VoidType.INSTANCE || b == VoidType.INSTANCE)
      return false;

    if (a.equals(b)) return true;

    // The last check is pretty important because:
    // var a: f64 = 10 # This is valid code. However,
    // var a: i32 = 1.5 # is invalid and not allowed!
    if (TypeUtil.isNumericType(a) && TypeUtil.isNumericType(b) && TypeUtil.max(a, b) == b)
      return true;

    if (a instanceof ArrayType)
      return b instanceof ArrayType
        && isAssignableTo(((ArrayType) a).getType(), ((ArrayType) b).getType());

    return a == NilType.INSTANCE && b.isReference();
  }

  private static boolean isTypeDeclaration(AST decl) {
    if (decl instanceof Statement.Class) return true;

    if (!(decl instanceof BuiltInTypeNode typeNode)) return false;
    return typeNode.kind() == DeclarationKind.TYPE;
  }

  public void analyze(List<Statement> statements) {
    for (Statement statement : statements) {
      statement.accept(this);
    }
  }

  @Override
  public void visitNilExpression(Expression.Nil expr) {
    R.set(expr, "type", NilType.INSTANCE);
  }

  @Override
  public void visitBooleanExpression(Expression.Boolean expr) {
    R.set(expr, "type", BoolType.INSTANCE);
  }

  @Override
  public void visitInt32Expression(Expression.Int32 expr) {
    R.set(expr, "type", I32Type.INSTANCE);
  }

  @Override
  public void visitInt64Expression(Expression.Int64 expr) {
    R.set(expr, "type", I64Type.INSTANCE);
  }

  @Override
  public void visitFloat32Expression(Expression.Float32 expr) {
    R.set(expr, "type", F32Type.INSTANCE);
  }

  @Override
  public void visitFloat64Expression(Expression.Float64 expr) {
    R.set(expr, "type", F64Type.INSTANCE);
  }

  @Override
  public void visitLiteralExpression(Expression.Literal expr) {

  }

  @Override
  public void visitUnaryExpression(Expression.Unary expr) {
    visitExpression(expr.right);

    switch (expr.op.type()) {
      case TILDE: {
        R.rule()
          .using(expr.right, "type")
          .by(r -> {
            IType opType = r.get(0);

            // carry the type of the right expression
            R.set(expr, "type", opType);

            if (!TypeUtil.isIntegerType(opType))
              r.error(String.format("Invalid bitwise operation '%s' on value of type %s", expr.op.literal(), opType), expr);
          });
        break;
      }
      case MINUS: {
        R.rule()
          .using(expr.right, "type")
          .by(r -> {
            IType opType = r.get(0);

            // carry the type of the right expression
            R.set(expr, "type", opType);

            if (!TypeUtil.isNumericType(opType))
              r.error(String.format("Invalid operation '%s' on value of type %s", expr.op.literal(), opType), expr);
          });
        break;
      }
      case BANG: {
        R.set(expr, "type", BoolType.INSTANCE);

        R.rule()
          .using(expr.right, "type")
          .by(r -> {
            IType opType = r.get(0);
            if (!TypeUtil.isBoolean(opType))
              r.error("Cannot negate value of type: " + opType, expr);
          });
        break;
      }
      default:
        throw new IllegalStateException("Unexpected value: " + expr.op);
    }
  }

  @Override
  public void visitBinaryExpression(Expression.Binary expr) {
    visitExpression(expr.left);
    visitExpression(expr.right);

    R.rule(expr, "type")
      .using(expr.left.attr("type"), expr.right.attr("type"))
      .by(r -> {
        IType left = r.get(0);
        IType right = r.get(1);

        // TODO: Handle string concatenation

        if (expr.op.isArithemetic()) {
          if (TypeUtil.isNumericType(left) && TypeUtil.isNumericType(right)) {
            r.set(0, TypeUtil.max(left, right));
          } else {
            r.error(arithmeticError(expr, left, right), expr);
          }
        } else if (expr.op.isBitwise()) {
          if (TypeUtil.isNumericType(left) && TypeUtil.isNumericType(right)) {
            IType maxType = TypeUtil.max(left, right);

            if (TypeUtil.max(maxType, F32Type.INSTANCE) == F32Type.INSTANCE) {
              // bitwise operations only support integers
              r.error(bitwiseError(expr, left, right), expr);
            }

            r.set(0, maxType);
          } else {
            r.error(bitwiseError(expr, left, right), expr);
          }
        }

        // TODO: Handle other operations
      });
  }

  @Override
  public void visitLogicalExpression(Expression.Logical expr) {
    visitExpression(expr.left);
    visitExpression(expr.right);

    R.rule(expr, "type")
      .using(expr.left.attr("type"), expr.right.attr("type"))
      .by(r -> {
        IType left = r.get(0);
        IType right = r.get(1);

        if (expr.op.isLogical()) {
          r.set(0, BoolType.INSTANCE);

          if (!TypeUtil.isBoolean(left))
            r.errorFor("Attempting to perform binary logic on non-boolean type: " + left, expr.left);
          if (!TypeUtil.isBoolean(right))
            r.errorFor("Attempting to perform binary logic on non-boolean type: " + right, expr.right);
        } else if (expr.op.isEquality()) {
          r.set(0, BoolType.INSTANCE);

          if (!isComparableTo(left, right))
            r.errorFor(String.format("Comparison on incomparable types %s and %s", left, right), expr);
        }
      });
  }

  @Override
  public void visitRangeExpression(Expression.Range expr) {

  }

  @Override
  public void visitGroupingExpression(Expression.Grouping expr) {
    visitExpression(expr.expression);

    R.rule(expr, "type")
      .using(expr.expression, "type")
      .by(Rule::copyFirst);
  }

  @Override
  public void visitIdentifierExpression(Expression.Identifier expr) {
    final Scope scope = this.scope;

    // Try to look up immediately. This must succeed for variables, but not necessarily for
    // functions or types. By looking up now, we can report looked-up variables later
    // as being used before being defined.
    String name = expr.token.literal();
    DeclarationContext maybeCtx = scope.lookup(name);

    if (maybeCtx != null) {
      R.set(expr, "ast", maybeCtx.declaration());
      R.set(expr, "scope", maybeCtx.scope());

      R.rule(expr, "type")
        .using(maybeCtx.declaration(), "type")
        .by(Rule::copyFirst);
      return;
    }

    // Re-lookup after the scopes have been built.
    R.rule(expr.attr("ast"), expr.attr("scope"))
      .by(r -> {
        DeclarationContext ctx = scope.lookup(name);
        AST ast = ctx == null ? null : ctx.declaration();

        if (ctx == null) {
          r.errorFor("Could not resolve: " + name,
            expr, expr.attr("ast"), expr.attr("scope"), expr.attr("type"));
        } else {
          r.set(expr, "scope", ctx.scope());
          r.set(expr, "ast", ast);

          if (ast instanceof Statement.Var) {
            r.errorFor("Variable used before declaration: " + name,
              expr, expr.attr("type"));
          } else {
            R.rule(expr, "type")
              .using(ast, "type")
              .by(Rule::copyFirst);
          }
        }
      });
  }

  @Override
  public void visitTypedNameExpression(Expression.TypedName expr) {
    visitTyped(expr.type);

    scope.declare(expr.name.token.literal(), expr);

    R.rule(expr, "type")
      .using(expr.type, "value")
      .by(Rule::copyFirst);
  }

  @Override
  public void visitConditionExpression(Expression.Condition expr) {
    visitExpression(expr.expression);
    visitExpression(expr.falsy);
    visitExpression(expr.truth);

    R.rule(expr, "type")
      .using(expr.expression.attr("type"), expr.truth.attr("type"), expr.falsy.attr("type"))
      .by(r -> {
        IType condition = r.get(0);
        IType truthy = r.get(1);
        IType falsy = r.get(2);

        boolean truthIsNil = TypeUtil.isNil(truthy);
        boolean falseIsNil = TypeUtil.isNil(falsy);

        if (!TypeUtil.isBoolean(condition))
          r.errorFor("Cannot perform conditional check on non-boolean result: " + condition, expr.expression);

        if (truthIsNil && falseIsNil)
          r.errorFor("Both sides of a conditional operation cannot evaluate to nil", expr);

        if (!truthy.type().equals(falsy.type()) && !(truthIsNil || falseIsNil))
          r.errorFor(String.format("Incompatible evaluation results for truth and false condition: %s and %s", truthy, falsy), expr.falsy);

        r.set(0, truthIsNil ? falsy : truthy);
      });
  }

  @Override
  public void visitCallExpression(Expression.Call expr) {
    visitExpression(expr.callee);
    for (var arg : expr.args) {
      visitExpression(arg);
    }

    // TODO: Implement
  }

  @Override
  public void visitGetExpression(Expression.Get expr) {
    visitExpression(expr.expression);
    visitExpression(expr.name);

    // TODO: Implement
  }

  @Override
  public void visitSetExpression(Expression.Set expr) {
    visitExpression(expr.expression);
    visitExpression(expr.name);
    visitExpression(expr.value);

    // TODO: Implement
  }

  @Override
  public void visitIndexExpression(Expression.Index expr) {
    visitExpression(expr.argument);
    visitExpression(expr.callee);

    R.rule()
      .using(expr.argument, "type")
      .by(r -> {
        IType type = r.get(0);

        // TODO: Handle non integer indexing on dictionary maps

        if (!TypeUtil.isIntegerType(type))
          r.error("Indexing an array using a non-Int-valued expression", expr.argument);
      });

    R.rule(expr, "type")
      .using(expr.callee, "type")
      .by(r -> {
        IType type = r.get(0);
        if (type instanceof ArrayType)
          r.set(0, ((ArrayType) type).getType());
        else
          r.error("Trying to index a non-array expression of type " + type, expr);
      });
  }

  @Override
  public void visitSliceExpression(Expression.Slice expr) {
    visitExpression(expr.callee);
    visitExpression(expr.lower);
    visitExpression(expr.upper);

    // TODO: Implement
  }

  @Override
  public void visitArrayExpression(Expression.Array expr) {
    for (var item : expr.items) {
      visitExpression(item);
    }

    // TODO: Implement
  }

  @Override
  public void visitDictExpression(Expression.Dict expr) {
    for (var key : expr.keys) {
      visitExpression(key);
    }
    for (var value : expr.values) {
      visitExpression(value);
    }

    // TODO: Implement
  }

  @Override
  public void visitNewExpression(Expression.New expr) {
    visitExpression(expr.expression);
    for (var argument : expr.arguments) {
      visitExpression(argument);
    }

    // TODO: Implement
  }

  @Override
  public void visitParentExpression(Expression.Parent expr) {

    // TODO: Implement
  }

  @Override
  public void visitSelfExpression(Expression.Self expr) {

    // TODO: Implement
  }

  @Override
  public void visitAssignExpression(Expression.Assign expr) {
    visitExpression(expr.value);

    R.rule(expr, "type")
      .using(expr.expression.attr("type"), expr.value.attr("type"))
      .by(r -> {
        IType left = r.get(0);
        IType right = r.get(1);

        r.set(0, r.get(0)); // the type of the assignment is the left-side type
//        r.set(1, r.get(0)); // the type of the assignment is the left-side type

        if (expr.expression instanceof Expression.Identifier
          || expr.expression instanceof Expression.Get
          || expr.expression instanceof Expression.Index) {
          if (!isAssignableTo(right, left))
            r.errorFor("Trying to assign a value to a non-compatible lvalue", expr);
        } else
          r.errorFor("Trying to assign to an non-lvalue expression", expr.expression);
      });
  }

  @Override
  public void visitAnonymousExpression(Expression.Anonymous expr) {
    visitFunctionStatement(expr.function);

    R.rule(expr, "type")
      .using(expr.function.returnType, "value")
      .by(Rule::copyFirst);
  }

  @Override
  public void visitExpression(Expression expression) {
    if (expression == null) return;
    expression.accept(this);
  }

  @Override
  public void visitEchoStatement(Statement.Echo stmt) {
    visitExpression(stmt.value);

    // TODO: Implement
  }

  @Override
  public void visitSimpleStatement(Statement.Simple statement) {
    visitExpression(statement.expression);
  }

  @Override
  public void visitIfStatement(Statement.If stmt) {
    visitExpression(stmt.condition);
    visitStatement(stmt.thenBranch);
    visitStatement(stmt.elseBranch);

    R.rule()
      .using(stmt.condition.attr("type"))
      .by(r -> {
        IType condition = r.get(0);

        if (!TypeUtil.isBoolean(condition))
          r.errorFor("If statement with a non-boolean condition of type:  " + condition, stmt.condition);

        r.set(0, condition);
      });

    Attribute[] deps = getReturnsDependencies(List.of(stmt.thenBranch, stmt.elseBranch));
    R.rule(stmt, "returns")
      .using(deps)
      .by(r -> r.set(0, deps.length == 2 && Arrays.stream(deps).allMatch(r::get)));
  }

  @Override
  public void visitIterStatement(Statement.Iter stmt) {
    visitStatement(stmt.declaration);
    visitExpression(stmt.condition);
    visitStatement(stmt.interation);
    visitStatement(stmt.body);

    // TODO: Implement
  }

  @Override
  public void visitWhileStatement(Statement.While stmt) {
    visitExpression(stmt.condition);
    visitStatement(stmt.body);

    R.rule()
      .using(stmt.condition, "type")
      .by(r -> {
        IType type = r.get(0);
        if (!TypeUtil.isBoolean(type)) {
          r.error("While statement with a non-boolean condition of type: " + type,
            stmt.condition);
        }
      });
  }

  @Override
  public void visitDoWhileStatement(Statement.DoWhile stmt) {
    visitStatement(stmt.body);
    visitExpression(stmt.condition);

    R.rule()
      .using(stmt.condition, "type")
      .by(r -> {
        IType type = r.get(0);
        if (!TypeUtil.isBoolean(type)) {
          r.error("Do-While statement with a non-boolean condition of type: " + type,
            stmt.condition);
        }
      });
  }

  @Override
  public void visitContinueStatement(Statement.Continue stmt) {

    // TODO: Implement
  }

  @Override
  public void visitBreakStatement(Statement.Break stmt) {

    // TODO: Implement
  }

  @Override
  public void visitRaiseStatement(Statement.Raise stmt) {
    visitExpression(stmt.exception);

    // TODO: Implement
  }

  @Override
  public void visitReturnStatement(Statement.Return stmt) {
    visitExpression(stmt.value);

    R.set(stmt, "returns", true);

    Statement.Function function = currentFunction();
    if (function == null) // top-level return
      return;

    if (stmt.value == null) {
      R.rule()
        .using(function.returnType, "value")
        .by(r -> {
          IType returnType = r.get(0);
          if (returnType != VoidType.INSTANCE)
            r.error("Return without value in a function with a return type", stmt);
        });
    } else {
      R.rule()
        .using(function.returnType.attr("value"), stmt.value.attr("type"))
        .by(r -> {
          IType formal = r.get(0);
          IType actual = r.get(1);

          if (formal == VoidType.INSTANCE) {
            r.error("Return with value in a void function", stmt);
          } else if (!isAssignableTo(actual, formal)) {
            r.errorFor(String.format(
                "Incompatible return type, expected %s but got %s", formal, actual),
              stmt.value
            );
          }
        });
    }
  }

  @SuppressWarnings("StatementWithEmptyBody")
  @Override
  public void visitAssertStatement(Statement.Assert stmt) {
    visitExpression(stmt.expression);
    visitExpression(stmt.message);

    R.rule()
      .using(stmt.expression.attr("type"), stmt.message.attr("type"))
      .by(r -> {
        IType expressionType = r.get(0);
        IType messageType = r.get(1);

        if(expressionType != BoolType.INSTANCE) {
          r.errorFor("Assert statement with non-boolean expression", stmt.expression);
        } else {
          // TODO: Ensure message is one of String or Error
        }
      });
  }

  @Override
  public void visitUsingStatement(Statement.Using stmt) {

    // TODO: Implement
  }

  @Override
  public void visitImportStatement(Statement.Import stmt) {

    // TODO: Implement
  }

  @Override
  public void visitCatchStatement(Statement.Catch stmt) {
    visitStatement(stmt.body);
    visitStatement(stmt.catchBody);
    visitStatement(stmt.finallyBody);

    // TODO: Implement
  }

  @Override
  public void visitBlockStatement(Statement.Block stmt) {
    for (var statement : stmt.body) {
      visitStatement(statement);
    }

    Scope previousScope = scope;
    scope = new Scope(stmt, scope);
    R.set(stmt, "scope", scope);

    Attribute[] deps = getReturnsDependencies(stmt.body);
    R.rule(stmt, "returns")
      .using(deps)
      .by(r -> r.set(0, deps.length != 0 && Arrays.stream(deps).anyMatch(r::get)));

    scope = previousScope;
  }

  @Override
  public void visitVarStatement(Statement.Var stmt) {
    visitExpression(stmt.typedName);
    visitExpression(stmt.value);

    this.inferenceContext = stmt;
    String name = stmt.typedName.name.token.literal();

    scope.declare(name, stmt);
    R.set(stmt, "scope", scope);

    R.rule(stmt, "type")
      .using(stmt.typedName, "type")
      .by(Rule::copyFirst);

    R.rule()
      .using(stmt.typedName.attr("type"), stmt.value.attr("type"))
      .by(r -> {
        IType expected = r.get(0);
        IType actual = r.get(1);

        if (!isAssignableTo(actual, expected)) {
          r.error(
            String.format(
              "Incompatible initializer type provided for variable `%s`: expected %s but got %s",
              name, expected, actual
            ),
            stmt.value
          );
        }
      });
  }

  private void doFunctionVisit(Statement statement, String name, Typed returnType, List<Expression.TypedName> parameters, Statement.Block body, boolean isVariadic) {
    scope.declare(name, statement);
    Scope previousScope = scope;

    scope = new Scope(statement, scope);
    R.set(statement, "scope", scope);

    Attribute[] dependencies = new Attribute[parameters.size() + 1];
    dependencies[0] = returnType.attr("value");

    for (int i = 0; i < parameters.size(); i++) {
      Expression.TypedName typedParam = parameters.get(i);
      visitTypedNameExpression(typedParam);
      dependencies[i + 1] = typedParam.attr("type");
    }

    visitTyped(returnType);

    R.rule(statement, "type")
      .using(dependencies)
      .by(r -> {
        IType[] paramTypes = new IType[parameters.size()];

        for (int i = 0; i < paramTypes.length; ++i) {
          paramTypes[i] = r.get(i + 1);
        }

        r.set(0, new DefType(r.get(0), isVariadic, paramTypes));
      });

    visitStatement(body);

    R.rule()
      .using(body.attr("returns"), returnType.attr("value"))
      .by(r -> {
        boolean returns = r.get(0);
        IType rType = r.get(1);
        if (!returns && rType != VoidType.INSTANCE) {
          r.error("Missing return in function", statement);
        }

        // NOTE: The returned value presence and type are checked in visitReturnStatement().
      });

    scope = previousScope;
  }

  @Override
  public void visitFunctionStatement(Statement.Function stmt) {
    doFunctionVisit(
      stmt,
      stmt.name.literal(),
      stmt.returnType,
      stmt.parameters,
      stmt.body,
      stmt.isVariadic
    );
  }

  @Override
  public void visitMethodStatement(Statement.Method stmt) {
    doFunctionVisit(
      stmt,
      stmt.name.literal(),
      stmt.returnType,
      stmt.parameters,
      stmt.body,
      stmt.isVariadic
    );
  }

  @Override
  public void visitPropertyStatement(Statement.Property stmt) {

    // TODO: Implement
  }

  @Override
  public void visitClassStatement(Statement.Class stmt) {

    // TODO: Set the "kind" info (regular, parameterized, mapped) on the statement itself once supported in the parser.
    scope.declare(stmt.descriptor.name.literal(), stmt);
    R.set(stmt, "type", TypeType.INSTANCE);
    R.set(stmt, "declared", new ClassType(stmt));
  }

  @Override
  public void visitVarListStatement(Statement.VarList stmt) {
    for (var variable : stmt.declarations) {
      visitStatement(variable);
    }
  }

  @Override
  public void visitStatement(Statement statement) {
    if (statement == null) return;
    statement.accept(this);
  }

  @Override
  public void visitIdTyped(Typed.Id typed) {
    final String name = typed.name;

    R.rule()
      .by(r -> {
        // type declarations may occur after use
        DeclarationContext ctx = scope.lookup(name);
        AST ast = ctx == null ? null : ctx.declaration();

        if (ctx == null) {
          r.errorFor(
            String.format("Cannot resolve type '%s'", name),
            typed,
            typed.attr("value")
          );
        } else if (!isTypeDeclaration(ast)) {
          r.errorFor(
            String.format(
              "%s did not resolve to a type declaration but to a %s",
              name, ast.astName()
            ),
            typed,
            typed.attr("value")
          );
        } else {
          R.rule(typed.attr("value"))
            .using(ast.attr("declared"))
            .by(Rule::copyFirst);
        }
      });
  }

  @Override
  public void visitArrayTyped(Typed.Array typed) {
    DeclarationContext ctx = scope.lookup(typed.name);
    AST ast = ctx == null ? null : ctx.declaration();

    R.rule()
      .by(r -> {
        if (ctx == null) {
          r.errorFor(
            String.format("Cannot resolve type '%s'", typed.name),
            typed,
            typed.attr("value")
          );
        } else if (!isTypeDeclaration(ast)) {
          r.errorFor(
            String.format(
              "%s did not resolve to a type declaration but to a %s declaration",
              typed.name, ast.astName()
            ),
            typed,
            typed.attr("value")
          );
        } else {
          R.rule(typed, "value")
            .using(ast, "declared")
            .by(r1 -> {
              r1.set(0, new ArrayType(r1.get(0)));
            });
        }
      });
  }

  @Override
  public void visitParameterizedTyped(Typed.Parameterized typed) {
    visitTyped(typed.innerType);

    String name = typed.name;
    DeclarationContext ctx = scope.lookup(name);
    AST ast = ctx == null ? null : ctx.declaration();

    R.rule()
      .by(r -> {

        if (ctx == null) {
          r.errorFor(
            String.format("Cannot resolve type '%s'", name),
            typed,
            typed.attr("value")
          );
          return;
        }

        R.rule(typed, "value")
          .using(ast.attr("declared"), typed.innerType.attr("value"))
          .by(r1 -> {

            IType astType = r1.get(0);
            IType innerType = r1.get(1);

            // TODO: Correctly check that the astType is actually a type that can hold a parameter.
            if (!isTypeDeclaration(ast)/* || !(astType instanceof ParameterizedType)*/) {
              r1.errorFor(
                String.format(
                  "Expected a parameterized type but got %s",
                  name
                ),
                typed,
                typed.attr("value")
              );
            } else {
              r1.set(0, new ParameterizedType(astType, innerType));
            }
          });
      });
  }

  @Override
  public void visitMapTyped(Typed.Map typed) {
    visitTyped(typed.keyType);
    visitTyped(typed.valueType);

    String name = typed.name;
    DeclarationContext ctx = scope.lookup(name);
    AST ast = ctx == null ? null : ctx.declaration();

    R.rule()
      .by(r -> {
        if (ctx == null) {
          r.errorFor(
            String.format("Cannot resolve type '%s'", name),
            typed,
            typed.attr("value")
          );
        } else {
          R.rule(typed, "value")
            .using(ast.attr("declared"), typed.keyType.attr("value"), typed.valueType.attr("value"))
            .by(r1 -> {
              IType astType = r1.get(0);
              IType keyType = r1.get(1);
              IType valueType = r1.get(2);

              // TODO: Correctly check that the astType is actually a type that can hold a map.
              if (!isTypeDeclaration(ast) /*|| !(astType instanceof MappedType)*/) {
                r.errorFor(
                  String.format(
                    "Expected a parameterized type but got %s",
                    name
                  ),
                  typed,
                  typed.attr("value")
                );
              } else {
                r1.set(0, new MappedType(astType, keyType, valueType));
              }
            });
        }
      });
  }

  @Override
  public void visitTyped(Typed typed) {
    typed.accept(this);
  }

  private boolean isReturnContainer(AST node) {
    // TODO: Determine if raise and assert nodes should be return containers
    return node instanceof Statement.Block
      || node instanceof Statement.If
      || node instanceof Statement.Return
      || node instanceof Statement.While
      || node instanceof Statement.DoWhile
      || node instanceof Statement.Using;
  }

  /**
   * Get the dependencies necessary to compute the "returns" attribute of the parent.
   */
  private Attribute[] getReturnsDependencies(List<? extends AST> children) {
    return children.stream()
      .filter(Objects::nonNull)
      .filter(this::isReturnContainer)
      .map(it -> it.attr("returns"))
      .toArray(Attribute[]::new);
  }

  private Statement.Function currentFunction() {
    Scope scope = this.scope;
    while (scope != null) {
      AST node = scope.node;
      if (node instanceof Statement.Function)
        return (Statement.Function) node;
      scope = scope.parent;
    }

    return null;
  }
}
