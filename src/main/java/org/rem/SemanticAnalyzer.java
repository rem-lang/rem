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
import java.util.stream.IntStream;


// TODO: Detect loops that always evaluate to true without a break statement and warn
// TODO: Detect loops that always evaluate to false and mark it for removal in final compilation
@SuppressWarnings("StatementWithEmptyBody")
public final class SemanticAnalyzer implements Expression.VoidVisitor, Statement.VoidVisitor, Typed.VoidVisitor {

  private final Reactor R;
  private final boolean showWarnings;
  private Scope scope;

  /**
   * Current context for type inference (currently only to infer the type of empty arrays).
   */
  private AST inferenceContext;
  private AST methodContext;

  public SemanticAnalyzer(Reactor reactor, boolean showWarnings) {
    this.R = reactor;
    this.showWarnings = showWarnings;
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

    return a.isReference() && b.isReference()
      || a.equals(b)
      || TypeUtil.isNumericType(a) && TypeUtil.isNumericType(b)
      || a.type().compareTo(b.type()) == 0;
  }

  /**
   * Returns the common type between both types, or {@code null} if no such supertype
   * exists.
   */
  private static IType getCommonType(IType a, IType b) {
    if (a == VoidType.INSTANCE || b == VoidType.INSTANCE)
      return null;
    if (a.isAssignableTo(b))
      return b;
    if (b.isAssignableTo(a))
      return a;
    else
      return null;
  }

  private static boolean isTypeDeclaration(AST declaration) {
    if (declaration instanceof Statement.Class) return true;

    if (!(declaration instanceof BuiltInTypeNode typeNode)) return false;
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

    // TODO: Implement
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
              r.error(
                String.format("Invalid bitwise operation '%s' on value of type %s", expr.op.literal(), opType),
                expr
              );
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

        // TODO: Handle operator overloading.
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

    // TODO: Implement
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
          r.errorFor(
            "Could not resolve: " + name,
            expr, expr.attr("ast"), expr.attr("scope"), expr.attr("type")
          );
        } else {
          r.set(expr, "scope", ctx.scope());
          r.set(expr, "ast", ast);

          if (ast instanceof Statement.Var) {
            r.errorFor(
              "Variable used before declaration: " + name,
              expr, expr.attr("type")
            );
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
    scope.declare(expr.name.token.literal(), expr);

    if (expr.type == null) return;

    visitTyped(expr.type);

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
          r.errorFor(
            String.format(
              "Incompatible evaluation results for truth and false condition: %s and %s",
              truthy,
              falsy
            ), expr.falsy
          );

        r.set(0, truthIsNil ? falsy : truthy);
      });
  }

  @Override
  public void visitCallExpression(Expression.Call expr) {
    inferenceContext = expr;

    Attribute[] dependencies = new Attribute[expr.args.size() + 1];
    dependencies[0] = expr.callee.attr("type");
    for (int i = 0; i < expr.args.size(); i++) {
      Expression arg = expr.args.get(i);

      dependencies[i + 1] = arg.attr("type");
      R.set(arg, "index", i);
      visitExpression(arg);
    }

    visitExpression(expr.callee);

    R.rule(expr, "type")
      .using(dependencies)
      .by(r -> {
        IType maybeFunType = r.get(0);

        if (!(maybeFunType instanceof DefType funType)) {
          r.error("Cannot call non-function: " + expr.callee, expr.callee);
          return;
        }

        r.set(0, funType.getReturnType());

        IType[] params = funType.getParameterTypes();
        List<Expression> args = expr.args;

        // TODO: Handle variadic arguments
        if (params.length != args.size()) {
          r.errorFor(
            String.format(
              "Wrong number of arguments, expected %d but got %d argument(s)",
              params.length,
              args.size()
            ),
            expr
          );
        }

        int checkedArgs = Math.min(params.length, args.size());

        for (int i = 0; i < checkedArgs; ++i) {
          IType argType = r.get(i + 1);
          IType paramType = params[i];

          if (!argType.isAssignableFrom(paramType)) {
            Expression expression = expr.args.get(i);
            if (expression instanceof Expression.Dict dict && !dict.keys.isEmpty()) {
              expression = dict.keys.getFirst();
            } else if (expression instanceof Expression.Array array && !array.items.isEmpty()) {
              expression = array.items.getFirst();
            }

            r.errorFor(
              String.format(
                "Incompatible argument provided for argument %d. Expected %s but got %s",
                i, paramType, argType
              ),
              expression
            );
          }
        }
      });
  }

  @Override
  public void visitGetExpression(Expression.Get expr) {
    visitExpression(expr.expression);

    R.rule()
      .using(expr.expression, "type")
      .by(r -> {
        IType type = r.get(0);

        if (type instanceof ArrayType) {
          // FIXME: May need to ditch this when if decide that arrays do not have fields.
          if (expr.name.token.literal().equals("length")) {
            R.rule(expr, "type")
              .by(rr -> rr.set(0, I32Type.INSTANCE));
          } else {
            r.errorFor(
              "Cannot access a non-length field on an array", expr,
              expr.attr("type")
            );
          }
          return;
        }

        if (!(type instanceof ClassType classType)) {
          r.errorFor(
            "Cannot access a field on a value of type " + type,
            expr,
            expr.attr("type")
          );
          return;
        }

        Statement.Class aClass = classType.getDeclaration();
        String getName = expr.name.token.literal();

        // check properties first
        for (Statement.Property property : aClass.properties) {
          if (!property.name.name.token.literal().equals(getName)) continue;

          R.rule(expr, "type")
            .using(property, "type")
            .by(Rule::copyFirst);

          return;
        }

        // check methods next
        for (Statement.Method method : aClass.methods) {
          if (!method.name.literal().equals(getName)) continue;

          R.rule(expr, "type")
            .using(method, "type")
            .by(Rule::copyFirst);

          return;
        }

        r.errorFor(
          String.format("Cannot access unknown field or method '%s' on class `%s`", getName, aClass.name.literal()),
          expr, expr.attr("type")
        );
      });
  }

  @Override
  public void visitSetExpression(Expression.Set expr) {
    visitExpression(expr.expression);
    visitExpression(expr.name);
    visitExpression(expr.value);

    R.rule()
      .using(expr.expression.attr("type"), expr.value.attr("type"))
      .by(r -> {
        IType iType = r.get(0);
        IType valueType = r.get(1);

        if (iType instanceof ArrayType) {
          // FIXME: May need to ditch this when if decide that arrays do not have fields.
          if (expr.name.token.literal().equals("length")) {
            r.errorFor("Cannot set length of array element directly", expr.name);
          } else {
            r.errorFor(
              "Cannot access a non-length field on an array", expr,
              expr.attr("type")
            );
          }
          return;
        }

        if (!(iType instanceof ClassType classType)) {
          r.errorFor(
            "Cannot update a field on a value of type " + iType,
            expr,
            expr.attr("type")
          );
          return;
        }

        Statement.Class aClass = classType.getDeclaration();
        String getName = expr.name.token.literal();

        final String className = aClass.name.literal();

        for (Statement.Property property : aClass.properties) {
          if (!property.name.name.token.literal().equals(getName)) continue;

          R.rule(expr, "type")
            .using(property, "type")
            .by(r1 -> {
              IType type = r1.get(0);

              if (!type.isAssignableFrom(valueType)) {
                r1.errorFor(
                  String.format(
                    "Cannot assign value of type `%s` to field '%s' of type `%s` in class `%s`",
                    valueType, getName, type, className
                  ),
                  expr.value
                );
              }

              r1.set(0, type);
            });

          return;
        }

        r.errorFor(
          String.format("Cannot access unknown field '%s' on class `%s`", getName, className),
          expr, expr.attr("type")
        );
      });
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
          r.error("Indexing an array using a value that's not assignable to `i32`", expr.argument);
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

    if (expr.items.isEmpty()) {
      final AST context = this.inferenceContext;

      if (context instanceof Statement.Var) {
        R.rule(expr, "type")
          .using(context, "type")
          .by(Rule::copyFirst);
      } else if (context instanceof Expression.Call) {
        R.rule(expr, "type")
          .using(((Expression.Call) context).callee.attr("type"), expr.attr("index"))
          .by(r -> {
            DefType funType = r.get(0);
            r.set(0, funType.getParameterTypes()[(int) r.get(1)]);
          });
      }
      return;
    }

    Attribute[] dependencies =
      expr.items.stream().map(it -> it.attr("type")).toArray(Attribute[]::new);

    R.rule(expr, "type")
      .using(dependencies)
      .by(r -> {
        IType[] types = IntStream.range(0, dependencies.length).<IType>mapToObj(r::get)
          .distinct().toArray(IType[]::new);

        IType supertype = null;
        for (int i = 0; i < types.length; i++) {
          IType type = types[i];

          if (type == VoidType.INSTANCE) {
            // We report the error but compute a type for the array from the other elements.
            r.errorFor("`void` value in array literal.", expr.items.get(i));
          } else if (type == NilType.INSTANCE) {
            // We report the error but compute a type for the array from the other elements.
            r.errorFor("`nil` value in array literal.", expr.items.get(i));
          } else if (supertype == null) {
            supertype = type;
          } else {
            supertype = getCommonType(supertype, type);
            if (supertype == null) {
              r.error("Could not find common type for items in array.", expr);
              return;
            }
          }
        }

        if (supertype == null) {
          r.error("Could not find common non-void type for items in array.", expr);
        } else {
          r.set(0, new ArrayType(supertype, expr.items.size()));
        }
      });
  }

  @Override
  public void visitDictExpression(Expression.Dict expr) {
    for (var key : expr.keys) {
      visitExpression(key);
    }

    if (expr.keys.isEmpty()) {
      final AST context = this.inferenceContext;

      if (context instanceof Statement.Var) {
        R.rule(expr, "type")
          .using(context, "type")
          .by(Rule::copyFirst);
      } else if (context instanceof Expression.Call) {
        Integer index = R.get(expr, "index");
        if (index == null) {
          R.rule(expr, "type")
            .by(r -> {
              r.errorFor("Cannot infer type for sub dictionary literal with no index.", expr);
              r.set(0, new MappedType(VoidType.INSTANCE, VoidType.INSTANCE));
            });
          return;
        }

        R.rule(expr, "type")
          .using(((Expression.Call) context).callee.attr("type"), expr.attr("index"))
          .by(r -> {
            DefType funType = r.get(0);
            r.set(0, funType.getParameterTypes()[(int) r.get(1)]);
          });
      }
      return;
    }

    for (var value : expr.values) {
      visitExpression(value);
    }

    Attribute[] keyDependencies =
      expr.keys.stream().map(it -> it.attr("type")).toArray(Attribute[]::new);

    Attribute[] valueDependencies =
      expr.values.stream().map(it -> it.attr("type")).toArray(Attribute[]::new);

    R.rule()
      .using(keyDependencies)
      .by(r -> {
        IType[] keyTypes = IntStream.range(0, keyDependencies.length).<IType>mapToObj(r::get)
          .distinct().toArray(IType[]::new);

        IType keyType = null;
        for (int i = 0; i < keyTypes.length; i++) {
          IType type = keyTypes[i];

          if (type == VoidType.INSTANCE) {
            // We report the error but compute a type for the array from the other elements.
            r.errorFor("`void` key in dictionary literal.", expr.keys.get(i));
          } else if (type == NilType.INSTANCE) {
            // We report the error but compute a type for the array from the other elements.
            r.errorFor("`nil` key in dictionary literal.", expr.keys.get(i));
          } else if (keyType == null) {
            keyType = type;
          } else {
            keyType = getCommonType(keyType, type);
            if (keyType == null) {
              r.error("Could not find common type for dictionary keys.", expr.keys.get(i));
              return;
            }
          }
        }

        if (keyType == null) {
          r.error("Could not find common non-void type for items in array.", expr.keys.getFirst());
        } else {

          final IType sharedKeyType = keyType;

          R.rule(expr, "type")
            .using(valueDependencies)
            .by(r1 -> {
              IType[] valueTypes = IntStream.range(0, valueDependencies.length).<IType>mapToObj(r1::get)
                .distinct().toArray(IType[]::new);

              IType valueType = null;
              for (int i = 0; i < valueTypes.length; i++) {
                IType type = valueTypes[i];

                if (valueType == null) {
                  valueType = type;
                } else {
                  valueType = getCommonType(valueType, type);
                  if (valueType == null) {
                    r.error("Could not find common type for dictionary values.", expr.values.get(i));
                    return;
                  }
                }
              }

              if (valueType == null) {
                r1.error("Could not find common non-void type for values in dictionary.", expr.values.getFirst());
              } else {
                r1.set(0, new MappedType(sharedKeyType, valueType));
              }
            });
        }
      });
  }

  @Override
  public void visitNewExpression(Expression.New expr) {
    visitExpression(expr.expression);

    R.rule()
      .using(expr.expression, "ast")
      .by(r -> {
        Statement declaration = r.get(0);

        if (!(declaration instanceof Statement.Class klass)) {
          String description =
            "Cannot create instance of non-class type: " + declaration;
          r.errorFor(description, expr, expr.attr("type"));
          return;
        }

        Statement.Method constructor = klass.methods.stream()
          .filter(m -> m.name.literal().equals("@new"))
          .findFirst()
          .orElse(null);

        if (constructor != null) {
          R.rule(expr, "type")
            .using(constructor.attr("type"))
            .by(Rule::copyFirst);
        } else {
          R.rule(expr, "type")
            .using(declaration, "type")
            .by(r1 -> {
              r1.set(0, new DefType(r1.get(0)));
            });
        }
      });
  }

  @Override
  public void visitParentExpression(Expression.Parent expr) {
    if (methodContext != null) {
      R.rule(expr, "type")
        .using(methodContext, "class")
        .by(r -> {
          ClassType type = r.get(0);
          ClassType superClass = type.getSuperClass();

          if (superClass == null) {
            r.errorFor(
              String.format(
                "Class %s does not declare `parent` as it has no superclass",
                type
              ), expr
            );

            r.set(0, type);
          } else {
            r.set(0, superClass);
          }
        });
    } else {
      R.rule(expr, "type")
        .by(r -> {
          r.errorFor("Cannot declare variable `parent` outside of a non-static class method", expr);
          r.set(0, VoidType.INSTANCE);
        });
    }
  }

  @Override
  public void visitSelfExpression(Expression.Self expr) {
    if (methodContext != null) {
      R.rule(expr, "type")
        .using(methodContext, "class")
        .by(Rule::copyFirst);
    } else {
      R.rule(expr, "type")
        .by(r -> {
          r.errorFor("Cannot declare variable `self` outside of a non-static class method", expr);
          r.set(0, VoidType.INSTANCE);
        });
    }
  }

  @Override
  public void visitAssignExpression(Expression.Assign expr) {
    visitExpression(expr.expression);
    visitExpression(expr.value);

    R.rule(expr, "type")
      .using(expr.expression.attr("type"), expr.value.attr("type"))
      .by(r -> {
        IType left = r.get(0);
        IType right = r.get(1);

        r.set(0, r.get(0)); // the type of the assignment is the left-side type
//        r.set(1, r.get(0)); // the type of the assignment is the left-side type

        if (expr.expression instanceof Expression.Self) {
          r.errorFor("Cannot assign value to `self`", expr.expression);
        } else if (expr.expression instanceof Expression.Parent) {
          r.errorFor("Cannot assign value to `parent`", expr.expression);
        } else if (expr.expression instanceof Expression.Identifier
          || expr.expression instanceof Expression.Get
          || expr.expression instanceof Expression.Index) {
          if (!right.isAssignableTo(left)) {
            r.errorFor(String.format("Error assigning a value of type of %s to %s", right.name(), left.name()), expr);
          }
        } else {
          r.errorFor("Trying to assign to an non-lvalue expression", expr.expression);
        }
      });
  }

  @Override
  public void visitAnonymousExpression(Expression.Anonymous expr) {
    visitFunctionStatement(expr.function);

    R.rule(expr, "type")
      .using(expr.function, "type")
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

    R.rule(stmt, "type")
      .using(stmt.value, "type")
      .by(Rule::copyFirst);
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
          r.error(
            "While statement with a non-boolean condition of type: " + type,
            stmt.condition
          );
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
          r.error(
            "Do-While statement with a non-boolean condition of type: " + type,
            stmt.condition
          );
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
    if (function == null) { // top-level return or method
      Statement.Method method = currentMethod();
      if (method == null) // top-level return
        return;

      boolean isConstructor = method.name.literal().equals("@new");

      if (stmt.value == null) {
        R.rule()
          .using(method.returnType, "value")
          .by(r -> {
            IType returnType = r.get(0);

            if (returnType != VoidType.INSTANCE) {
              r.error("Return without value in a method with a return type", stmt);
            }
          });
      } else {
        R.rule()
          .using(method.returnType.attr("value"), stmt.value.attr("type"))
          .by(r -> {
            IType expected = r.get(0);
            IType actual = r.get(1);

            if (expected == VoidType.INSTANCE && !isConstructor) {
              r.error("Return with value in a void method", stmt);
            } else if (isConstructor && expected != VoidType.INSTANCE) {
              r.error("Cannot return value from constructor", stmt);
            } else if (!actual.isAssignableTo(expected)) {
              r.errorFor(
                String.format(
                  "Incompatible return type, expected %s but got %s", expected, actual),
                stmt.value
              );
            }
          });
      }
    } else {
      boolean isConstructor = function.name.literal().equals("@new");

      if (stmt.value == null) {
        R.rule()
          .using(function.returnType, "value")
          .by(r -> {
            IType returnType = r.get(0);

            if (returnType != VoidType.INSTANCE) {
              r.error("Return without value in a function with a return type", stmt);
            }
          });
      } else {
        R.rule()
          .using(function.returnType.attr("value"), stmt.value.attr("type"))
          .by(r -> {
            IType expected = r.get(0);
            IType actual = r.get(1);

            if (expected == VoidType.INSTANCE && !isConstructor) {
              r.error("Return with value in a void function", stmt);
            } else if (!actual.isAssignableTo(expected)) {
              r.errorFor(
                String.format(
                  "Incompatible return type, expected %s but got %s", expected, actual),
                stmt.value
              );
            }
          });
      }
    }
  }

  @Override
  public void visitAssertStatement(Statement.Assert stmt) {
    visitExpression(stmt.expression);
    visitExpression(stmt.message);

    R.rule()
      .using(stmt.expression.attr("type"), stmt.message.attr("type"))
      .by(r -> {
        IType expressionType = r.get(0);
        IType messageType = r.get(1);

        if (expressionType != BoolType.INSTANCE) {
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

    scope = new Scope(stmt, scope);
    R.set(stmt, "scope", scope);

    Attribute[] deps = getReturnsDependencies(stmt.body);
    R.rule(stmt, "returns")
      .using(deps)
      .by(r -> r.set(0, deps.length != 0 && Arrays.stream(deps).anyMatch(r::get)));

    scope = scope.parent;
  }

  @Override
  public void visitVarStatement(Statement.Var stmt) {
    this.inferenceContext = stmt;

    visitExpression(stmt.typedName);
    visitExpression(stmt.value);

    String name = stmt.typedName.name.token.literal();

    if (stmt.typedName.type == null) {
      R.rule(stmt.typedName, "type")
        .using(stmt.value, "type")
        .by(Rule::copyFirst);
    }

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

        if (!actual.isAssignableTo(expected)) {

          if (TypeUtil.isArray(expected) && TypeUtil.isArray(actual)) {
            IType expectedElem = ((ArrayType) expected).getType();
            IType actualElem = ((ArrayType) actual).getType();

            if (TypeUtil.isIntegerType(expectedElem) && TypeUtil.isIntegerType(actualElem)) {
              // TODO: Check this attr in the compiler and perform a casting per-element if necessary.
              R.rule(stmt, "cast")
                .by(r1 -> {
                  r1.set(0, expectedElem);
                });
              return;
            }
          }

          if (TypeUtil.isMap(expected) && TypeUtil.isMap(actual)) {
            IType expectedElem = ((MappedType) expected).keyType();
            IType actualElem = ((MappedType) actual).keyType();

            if (TypeUtil.isIntegerType(expectedElem) && TypeUtil.isIntegerType(actualElem)) {
              // TODO: Check this attr in the compiler and perform a casting per-element if necessary.
              R.rule(stmt, "cast-key")
                .by(r1 -> {
                  r1.set(0, expectedElem);
                });
              return;
            }
          }

          r.error(
            String.format(
              "Incompatible initializer type provided for variable `%s`: expected %s but got %s",
              name, expected, actual
            ),
            stmt
          );
        } else if (expected instanceof ArrayType expectedArrayType && actual instanceof ArrayType actualArrayType1) {
          // If actual has a defined length, update the type to contain the actual size of the array.
          if (expectedArrayType.getLength() == 0 && actualArrayType1.getLength() > 0) {
            expectedArrayType.setLength(actualArrayType1.getLength());
          }
        }
      });
  }

  private void doFunctionVisit(Statement statement, String name, Typed returnType, List<Expression.TypedName> parameters, Statement.Block body, boolean isVariadic, boolean isMethod) {
    scope.declare(name, statement);

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

        IType rType = r.get(0);
        if (name.equals("@new") && isMethod) {
          if (rType != VoidType.INSTANCE) {
            r.errorFor("Cannot return value from constructor", statement);
            // continue assigning type
          } else {
            rType = R.get(statement, "class");
          }
        }

        for (int i = 0; i < paramTypes.length; ++i) {
          paramTypes[i] = r.get(i + 1);
        }

        r.set(0, new DefType(rType, isVariadic, paramTypes));
      });

    methodContext = isMethod ? (((Statement.Method) statement).isStatic ? null : statement) : null;
    visitStatement(body);
    methodContext = null;

    R.rule()
      .using(body.attr("returns"), returnType.attr("value"))
      .by(r -> {
        boolean returns = r.get(0);
        IType rType = r.get(1);

        if (!returns && rType != VoidType.INSTANCE) {
          r.error(String.format("Missing return in function. Expecting %s", rType), returnType);
        }

        // NOTE: The returned value presence and type are checked in visitReturnStatement().
      });

    scope = scope.parent;
  }

  private void doFunctionVisit(Statement statement, String name, Typed returnType, List<Expression.TypedName> parameters, Statement.Block body, boolean isVariadic) {
    doFunctionVisit(statement, name, returnType, parameters, body, isVariadic, false);
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
      stmt.isVariadic,
      true
    );
  }

  @Override
  public void visitPropertyStatement(Statement.Property stmt) {
    visitExpression(stmt.value);
    visitTypedNameExpression(stmt.name);

    R.rule(stmt, "type")
      .using(stmt.name.attr("type"), stmt.value.attr("type"))
      .by(r -> {
        IType nameType = r.get(0);
        IType valueType = r.get(1);

        if (!nameType.isAssignableFrom(valueType)) {
          r.errorFor(
            String.format(
              "Cannot assign value of type %s to field '%s' of type %s",
              valueType, stmt.name.name.token.literal(), nameType
            ),
            stmt.value
          );
        }

        // go ahead and set the type
        r.set(0, nameType);
      });
  }

  @Override
  public void visitClassStatement(Statement.Class stmt) {

    scope.declare(stmt.name.literal(), stmt);

    if (stmt.superclass != null) {
      String superClassName = stmt.superclass.token.literal();
      DeclarationContext ctx = scope.lookup(superClassName);
      AST ast = ctx == null ? null : ctx.declaration();

      if (ctx == null) {
        R.rule()
          .by(r -> r.errorFor(
            String.format("Cannot resolve class '%s'", superClassName),
            stmt.superclass,
            stmt.superclass.attr("value")
          ));
      } else if (!isTypeDeclaration(ast)) {
        R.rule()
          .by(r -> r.errorFor(
            String.format(
              "%s did not resolve to a class declaration but to a %s",
              superClassName, ast.astName()
            ),
            stmt.superclass,
            stmt.superclass.attr("value")
          ));
      }

      IType superClass = R.get(ast, "declared");

      ClassType rClass;
      if (!(superClass instanceof ClassType classType)) {
        rClass = new ClassType(stmt);
      } else {
        rClass = new ClassType(stmt, classType);
      }

      R.set(stmt, "type", TypeType.INSTANCE);
      R.set(stmt, "declared", rClass);

      for (Statement.Method method : stmt.methods) {
        R.set(method, "class", rClass);
        visitMethodStatement(method);
      }

      for (Statement.Property property : stmt.properties) {
        visitPropertyStatement(property);
      }
    } else {
      ClassType rClass = new ClassType(stmt);

      R.set(stmt, "type", TypeType.INSTANCE);
      R.set(stmt, "declared", rClass);

      for (Statement.Method method : stmt.methods) {
        R.set(method, "class", rClass);
        visitMethodStatement(method);
      }

      for (Statement.Property property : stmt.properties) {
        visitPropertyStatement(property);
      }
    }
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
  public void visitVoidTyped(Typed.Void typed) {
    R.set(typed, "value", VoidType.INSTANCE);
  }

  @Override
  public void visitIdTyped(Typed.Id typed) {
    final String name = typed.name.token.literal();

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
    visitTyped(typed.type);

    R.rule(typed, "value")
      .using(typed.type, "value")
      .by(r -> {
        IType type = r.get(0);

        if (type == VoidType.INSTANCE) {
          r.errorFor("Arrays cannot be of void type", typed.type);
        }

        r.set(0, new ArrayType(type));
      });
  }

  @Override
  public void visitMapTyped(Typed.Map typed) {
    visitTyped(typed.keyType);
    visitTyped(typed.valueType);

    R.rule(typed, "value")
      .using(typed.keyType.attr("value"), typed.valueType.attr("value"))
      .by(r -> {
        IType keyType = r.get(0);
        IType valueType = r.get(1);

        if (keyType == VoidType.INSTANCE || valueType == VoidType.INSTANCE) {
          r.errorFor("Map type cannot have void key or value type", typed);
        }

        r.set(0, new MappedType(keyType, valueType));
      });
  }

  @Override
  public void visitTyped(Typed typed) {
    if (typed == null) return;
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

  private Statement.Method currentMethod() {
    Scope scope = this.scope;
    while (scope != null) {
      AST node = scope.node;
      if (node instanceof Statement.Method)
        return (Statement.Method) node;
      scope = scope.parent;
    }

    return null;
  }
}
