package org.rem.parser;

import org.rem.exceptions.LexerException;
import org.rem.exceptions.ParserException;
import org.rem.parser.ast.*;

import java.util.*;

import static org.rem.parser.TokenType.*;

@SuppressWarnings("StatementWithEmptyBody")
public class Parser {
  private static final Map<TokenType, TokenType> ASSIGNER_ALTS = new HashMap<>() {
    {
      put(PLUS_EQ, PLUS);
      put(MINUS_EQ, MINUS);
      put(PERCENT_EQ, PERCENT);
      put(DIVIDE_EQ, DIVIDE);
      put(MULTIPLY_EQ, MULTIPLY);
      put(FLOOR_EQ, FLOOR);
      put(POW_EQ, POW);
      put(AMP_EQ, AMP);
      put(BAR_EQ, BAR);
      put(TILDE_EQ, TILDE);
      put(XOR_EQ, XOR);
      put(LSHIFT_EQ, LSHIFT);
      put(RSHIFT_EQ, RSHIFT);
      put(URSHIFT_EQ, URSHIFT);
    }

    @Override
    public TokenType get(Object key) {
      return containsKey(key) ? super.get(key) : EQUAL;
    }
  };
  private static final TokenType[] OPERATORS = new TokenType[]{
    PLUS, MINUS, MULTIPLY, POW, DIVIDE, FLOOR, EQUAL_EQ, LESS,
    LSHIFT, GREATER, RSHIFT, URSHIFT, PERCENT, AMP, BAR,
    TILDE, XOR,
  };
  public final Lexer lexer;
  private final List<Token> tokens;
  private int blockCount = 0;
  private int current = 0;
  private int anonymousCount = 0;

  public Parser(Lexer lexer) throws LexerException {
    this.lexer = lexer;
    this.tokens = this.lexer.run();
  }

  //region [Utilities]

  private boolean match(TokenType... tokenTypes) {
    for (TokenType t : tokenTypes) {
      if (check(t)) {
        advance();
        return true;
      }
    }

    return false;
  }

  private boolean matchAssigners() {
    if (isAtEnd()) return false;

    if (peek().isAssignmentOp()) {
      advance();
      return true;
    }

    return false;
  }

  private boolean check(TokenType type) {
    if (isAtEnd() && type != EOF) return false;
    return peek().type() == type;
  }

  private boolean check(TokenType... types) {
    if (isAtEnd()) return false;

    TokenType peeked = peek().type();
    for (TokenType t : types) {
      if (peeked == t && t != EOF) return true;
    }

    return false;
  }

  private Token advance() {
    if (!isAtEnd()) current++;
    return previous();
  }

  private boolean isAtEnd() {
    return peek().type() == EOF;
  }

  private Token peek() {
    return tokens.get(current);
  }

  private Token next() {
    if (current + 1 > tokens.size() - 1) return null;
    return tokens.get(current + 1);
  }

  private Token previous() {
    return tokens.get(current - 1);
  }

  private Token consume(TokenType type, String message) {
    if (check(type)) return advance();
    throw new ParserException(lexer.getSource(), peek(), message);
  }

  private Token consumeAny(String message, TokenType... tokenTypes) {
    for (TokenType t : tokenTypes) {
      if (check(t)) return advance();
    }
    throw new ParserException(lexer.getSource(), peek(), message);
  }

  private void endStatement() {
    if (
      match(EOF)
        || isAtEnd()
        || (blockCount > 0 && check(RBRACE))
    ) return;

    if (match(SEMICOLON)) {
      while (match(NEWLINE, SEMICOLON)) ;
      return;
    }

    consume(NEWLINE, "end of statement expected");

    while (match(NEWLINE, SEMICOLON)) ;
  }

  private void ignoreNewlinesNoSemi() {
    while (match(NEWLINE)) ;
  }

  private void ignoreNewlines() {
    while (match(NEWLINE) || match(SEMICOLON)) ;
  }

  //endregion

  private Typed parseType(String message, boolean mustThrow) {
    return wrap(() -> {
      // The three possible type definitions are:
      // 1. `[]type`
      // 2. `[key]type`
      // 3. `type`
      // These typing can be nested. E.g. `[key][]type`, [][key]type`, etc...
      // However, there are certain exceptions
      // 1. A list cannot be a key, so it's impossible to have `[[]type1]type2` and
      //    its derivatives.

      if(match(LBRACKET)) {
        Expression.Identifier keyType = null;

        if(check(IDENTIFIER)) {
          keyType = wrap(() -> {
            match(IDENTIFIER);
            return new Expression.Identifier(previous());
          });

          consume(RBRACKET, "']' expected after map type declaration");
        } else {
          consume(RBRACKET, "']' expected after list type declaration or key type for map type declaration");
        }

        Typed valueType = parseType("invalid type specified", true);

        if(keyType == null) {
          return new Typed.Array(valueType);
        } else {
          return new Typed.Map(new Typed.Id(keyType), valueType);
        }
      } else if(check(IDENTIFIER)) {
        return new Typed.Id(wrap(() -> {
          match(IDENTIFIER);
          return new Expression.Identifier(previous());
        }));
      } else if(!mustThrow) {
        return null;
      } else {
        throw new ParserException(lexer.getSource(), peek(), message);
      }
    });
  }

  private Typed parseType() {
    return parseType("invalid type encountered", true);
  }

  private Typed parseReturnType(String message) {
    Typed typed = parseType(message, false);
    if(typed == null) {
      return new Typed.Void();
    }

    return typed;
  }

  private Expression.TypedName typedName(String message) {
    Expression.Identifier identifier = wrap(() -> {
      consume(IDENTIFIER, message);
      return new Expression.Identifier(previous());
    });

    consume(COLON, "':' expected after variable name '" + identifier.token.literal() + "' declaration");
    return wrap(() -> new Expression.TypedName(identifier, parseType()));
  }

  private Expression.TypedName variableTypedName() {
    Expression.Identifier identifier = wrap(() -> {
      consume(IDENTIFIER, "variable name expected");
      return new Expression.Identifier(previous());
    });

    if(match(COLON)) {
      return wrap(() -> new Expression.TypedName(identifier, parseType()));
    } else {
      return wrap(() -> new Expression.TypedName(identifier, null));
    }
  }

  private Expression.TypedName typedName() {
    return typedName("variable name expected");
  }

  private Expression.Grouping grouping() {
    return wrap(() -> {
      ignoreNewlines();
      var expr = expression();
      ignoreNewlines();
      consume(RPAREN, "')' Expected after expression");
      return new Expression.Grouping(expr);
    });
  }

  private Expression.Call finishCall(Expression callee) {
    return wrap(() -> {
      ignoreNewlines();
      List<Expression> args = new ArrayList<>();

      if (!check(RPAREN)) {
        args.add(expression());

        while (match(COMMA)) {
          ignoreNewlines();
          args.add(expression());
        }
      }

      ignoreNewlines();
      consume(RPAREN, "')' expected after args");
      return new Expression.Call(callee, args);
    });
  }

  private Expression finishIndex(Expression callee) {
    return wrap(() -> {
      ignoreNewlines();
      Expression expression = expression();

      if (match(COMMA)) {
        ignoreNewlines();
        expression = new Expression.Slice(callee, expression, expression());
      } else {
        expression = new Expression.Index(callee, expression);
      }

      ignoreNewlines();
      consume(RBRACKET, "']' expected at end of indexer");
      return expression;
    });
  }

  private Expression finishDot(Expression e) {
    return wrap((expr) -> {
      ignoreNewlines();
      var prop = wrap(() -> new Expression.Identifier(
        consume(IDENTIFIER, "property name expected")
      ));

      if (matchAssigners()) {
        Token token = previous();
        if (token.type() == EQUAL) {
          expr = new Expression.Set(expr, prop, expression());
        } else {
          expr = new Expression.Set(
            (Expression) expr.clone(),
            (Expression.Identifier) prop.clone(),
            new Expression.Binary(
              reflectWrap(expr, prop, new Expression.Get(expr, prop)),
              previous().copyToType(ASSIGNER_ALTS.get(token.type()), previous().literal()),
              assignment()
            )
          );
        }
      } else {
        expr = new Expression.Get(expr, prop);
      }

      return expr;
    }, e);
  }

  private Expression interpolation() {
    return wrap(() -> {
      match(INTERPOLATION);

      Expression expression = wrap(() -> new Expression.Literal(
        previous().copyToType(LITERAL, previous().literal())
      ));

      do {
        expression = new Expression.Binary(
          expression,
          previous().copyToType(PLUS, "+"),
          expression()
        );

      } while ((check(INTERPOLATION) || check(LITERAL)) && !isAtEnd());
      match(INTERPOLATION, LITERAL);

      return expression;
    });
  }

  private Expression newStatement() {
    return wrap(() -> {
      Expression expression = primary();
      List<Expression> arguments = new ArrayList<>();

      consume(LPAREN, "'(' expected after new class instance");
      ignoreNewlines();

      if (!check(RPAREN)) {
        arguments.add(expression());

        while (match(COMMA)) {
          ignoreNewlines();
          arguments.add(expression());
        }
      }

      ignoreNewlines();
      consume(RPAREN, "')' expected after new class instance arguments");

      return new Expression.Call(
        reflectWrap(expression, new Expression.New(expression)),
        arguments
      );
    });
  }

  private Expression parseNumber(String number) {
    try {
      return new Expression.Int32(Integer.parseInt(number));
    } catch (NumberFormatException ignored) {
      try {
        return new Expression.Int64(Long.parseLong(number));
      } catch (NumberFormatException ignored2) {
        try {
          return new Expression.Float32(Float.parseFloat(number));
        } catch (NumberFormatException ignored3) {
          return new Expression.Float64(Double.parseDouble(number));
        }
      }
    }
  }

  private Expression primary() {
    return wrap(() -> {
      if (match(FALSE)) return new Expression.Boolean(false);
      if (match(TRUE)) return new Expression.Boolean(true);
      if (match(NIL)) return new Expression.Nil();
      if (match(SELF)) return new Expression.Self();
      if (match(PARENT)) return new Expression.Parent();
      if (match(NEW)) return newStatement();

      if (check(INTERPOLATION)) return interpolation();

      if (check(BIN_NUMBER, HEX_NUMBER, OCT_NUMBER, REG_NUMBER)) {
        return wrap(() -> {
          match(BIN_NUMBER, HEX_NUMBER, OCT_NUMBER, REG_NUMBER);
          return parseNumber(previous().literal());
        });
      }

      /*if (match(BIG_NUMBER)) {
        return new Expression.BigNumber(previous());
      }*/

      if (check(LITERAL)) {
        return wrap(() -> {
          match(LITERAL);
          return new Expression.Literal(previous());
        });
      }

      if (check(IDENTIFIER)) return wrap(() -> {
        match(IDENTIFIER);
        return new Expression.Identifier(previous());
      });

      if (match(LPAREN)) return grouping();
      if (match(LBRACE)) {
        return dict();
      }
      if (match(LBRACKET)) return list();
      if (match(AT)) return anonymous();

      return null;
    });
  }

  private Expression range() {
    return wrap(() -> {
      Expression expression = primary();

      while (match(RANGE)) {
        ignoreNewlines();
        expression = new Expression.Range(expression, primary());
      }

      return expression;
    });
  }

  private Expression doCall(Expression e) {
    return wrap((expr) -> {
      while (true) {
        if (match(DOT)) {
          expr = finishDot(expr);
        } else if (match(LPAREN)) {
          expr = finishCall(expr);
        } else if (match(LBRACKET)) {
          expr = finishIndex(expr);
        } else {
          break;
        }
      }

      return expr;
    }, e);
  }

  private Expression call() {
    return wrap(() -> doCall(range()));
  }

  private Expression assignExpression() {
    return wrap(() -> {
      Expression expression = call();

      if (match(INCREMENT)) {
        if (expression instanceof Expression.Get get) {
          expression = new Expression.Set(
            (Expression) get.expression.clone(),
            get.name,
            reflectWrap(get, new Expression.Binary(
              get,
              previous().copyToType(PLUS, "+"),
              new Expression.Int32(1)
            ))
          );
        } else {
          expression = new Expression.Assign(
            expression,
            reflectWrap(expression, new Expression.Binary(
              (Expression) expression.clone(),
              previous().copyToType(PLUS, "+"),
              new Expression.Int32(1)
            ))
          );
        }
      } else if (match(DECREMENT)) {
        if (expression instanceof Expression.Get get) {
          expression = new Expression.Set(
            (Expression) get.expression.clone(),
            get.name,
            reflectWrap(get, new Expression.Binary(
              get,
              previous().copyToType(MINUS, "-"),
              new Expression.Int32(1)
            ))
          );
        } else {
          expression = new Expression.Assign(
            expression,
            reflectWrap(expression, new Expression.Binary(
              (Expression) expression.clone(),
              previous().copyToType(MINUS, "-"),
              new Expression.Int32(1)
            ))
          );
        }
      }

      return expression;
    });
  }

  private Expression unary() {
    return wrap(() -> {
      if (match(BANG, MINUS, TILDE)) {
        Token op = previous();
        ignoreNewlines();
        return new Expression.Unary(op, unary());
      }

      return assignExpression();
    });
  }

  private Expression factor() {
    return wrap(() -> {
      Expression expression = unary();

      while (match(MULTIPLY, DIVIDE, PERCENT, POW, FLOOR)) {
        Token op = previous();
        ignoreNewlines();
        expression = new Expression.Binary(expression, op, unary());
      }

      return expression;
    });
  }

  private Expression term() {
    return wrap(() -> {
      Expression expression = factor();

      while (match(PLUS, MINUS)) {
        Token op = previous();
        ignoreNewlines();
        expression = new Expression.Binary(expression, op, factor());
      }

      return expression;
    });
  }

  private Expression shift() {
    return wrap(() -> {
      Expression expression = term();

      while (match(LSHIFT, RSHIFT, URSHIFT)) {
        var op = previous();
        ignoreNewlines();
        expression = new Expression.Binary(expression, op, term());
      }

      return expression;
    });
  }

  private Expression bitAnd() {
    return wrap(() -> {
      Expression expression = shift();

      while (match(AMP)) {
        var op = previous();
        ignoreNewlines();
        expression = new Expression.Binary(expression, op, shift());
      }

      return expression;
    });
  }

  private Expression bitXor() {
    return wrap(() -> {
      Expression expression = bitAnd();

      while (match(XOR)) {
        var op = previous();
        ignoreNewlines();
        expression = new Expression.Binary(expression, op, bitAnd());
      }

      return expression;
    });
  }

  private Expression bitOr() {
    return wrap(() -> {
      Expression expression = bitXor();

      while (match(BAR)) {
        var op = previous();
        ignoreNewlines();
        expression = new Expression.Binary(expression, op, bitXor());
      }

      return expression;
    });
  }

  private Expression comparison() {
    return wrap(() -> {
      Expression expression = bitOr();

      while (match(GREATER, GREATER_EQ, LESS, LESS_EQ)) {
        Token op = previous();
        ignoreNewlines();
        expression = new Expression.Logical(expression, op, bitOr());
      }

      return expression;
    });
  }

  private Expression equality() {
    return wrap(() -> {
      Expression expression = comparison();

      while (match(BANG_EQ, EQUAL_EQ)) {
        Token op = previous();
        ignoreNewlines();
        expression = new Expression.Logical(expression, op, comparison());
      }

      return expression;
    });
  }

  private Expression and() {
    return wrap(() -> {
      Expression expression = equality();

      while (match(AND)) {
        Token op = previous();
        ignoreNewlines();
        expression = new Expression.Logical(expression, op, equality());
      }

      return expression;
    });
  }

  private Expression or() {
    return wrap(() -> {
      Expression expression = and();

      while (match(OR)) {
        Token op = previous();
        ignoreNewlines();
        expression = new Expression.Logical(expression, op, and());
      }

      return expression;
    });
  }

  private Expression conditional() {
    return wrap(() -> {
      Expression expression = or();

      if (match(QUESTION)) {
        ignoreNewlines();
        var truth = conditional();
        consume(COLON, "':' expected in ternary operation after truth value");
        ignoreNewlines();
        expression = new Expression.Condition(expression, truth, conditional());
      }

      return expression;
    });
  }

  private Expression assignment() {
    return wrap(() -> {
      final Expression expression = conditional();

      if (matchAssigners()) {
        var type = previous();
        ignoreNewlines();

        if (type.type() == EQUAL) {
          return new Expression.Assign(expression, assignment());
        } else {
          return new Expression.Assign(
            expression,
            wrap(() -> new Expression.Binary(
              (Expression) expression.clone(),
              previous().copyToType(ASSIGNER_ALTS.get(type.type()), type.literal()),
              assignment()
            ))
          );
        }
      }

      return expression;
    });
  }

  private Expression expression() {
    return wrap(this::assignment);
  }

  private Expression dict() {
    return wrap(() -> {
      ignoreNewlines();
      List<Expression> keys = new ArrayList<>();
      List<Expression> values = new ArrayList<>();

      if (!check(RBRACE)) {
        do {
          ignoreNewlines();

          if (!check(RBRACE)) {
            Expression key;
            if (check(IDENTIFIER)) {
              key = wrap(() -> {
                match(IDENTIFIER);
                return new Expression.Literal(previous());
              });
            } else {
              key = expression();
            }
            keys.add(key);
            ignoreNewlines();

            if (!match(COLON)) {
              if (key instanceof Expression.Literal literal) {
                values.add(reflectWrap(literal, new Expression.Identifier(literal.token)));
              } else {
                throw new ParserException(
                  lexer.getSource(),
                  previous(), "missing value in dictionary definition"
                );
              }
            } else {
              ignoreNewlines();
              values.add(expression());
            }

            ignoreNewlines();
          } else {
            break;
          }
        } while (match(COMMA));
      }

      if (keys.size() != values.size()) {
        throw new ParserException(
          lexer.getSource(),
          previous(), "key/value count mismatch dictionary definition"
        );
      }

      ignoreNewlines();
      consume(RBRACE, "'}' expected after dictionary");
      return new Expression.Dict(keys, values);
    });
  }

  private Expression list() {
    return wrap(() -> {
      ignoreNewlines();
      List<Expression> items = new ArrayList<>();

      if (!check(RBRACKET)) {
        do {
          ignoreNewlines();

          if (!check(RBRACKET)) {
            items.add(expression());
            ignoreNewlines();
          } else {
            break;
          }
        } while (match(COMMA));
      }

      ignoreNewlines();
      consume(RBRACKET, "expected ']' at the end of list");
      return new Expression.Array(items);
    });
  }

  private Statement echoStatement() {
    return wrap(() -> {
      Expression val = expression();
      endStatement();
      return new Statement.Echo(val);
    });
  }

  private Statement.Simple expressionStatement(boolean is_iter) {
    return wrap(() -> {
      Expression val = expression();
      if (!is_iter) endStatement();
      return new Statement.Simple(val);
    });
  }

  private Statement.Block block() {
    return wrap(() -> {
      blockCount++;

      List<Statement> val = new ArrayList<>();
      ignoreNewlines();

      while (!check(RBRACE) && !isAtEnd()) {
        val.add(declaration());
      }

      consume(RBRACE, "'}' expected after block");
      blockCount--;

      return new Statement.Block(val);
    });
  }

  private Statement.Block matchBlock(String message) {
    ignoreNewlines();
    consume(LBRACE, message);
    return block();
  }

  private Statement ifStatement() {
    return wrap(() -> {
      Expression expression = expression();
      Statement body = statement();

      if (match(ELSE)) {
        return new Statement.If(expression, body, statement());
      }

      return new Statement.If(expression, body, null);
    });
  }

  private Statement whileStatement() {
    return wrap(() -> new Statement.While(expression(), statement()));
  }

  private Statement doWhileStatement() {
    return wrap(() -> {
      Statement body = statement();
      consume(WHILE, "'while' expected after do body");
      return new Statement.DoWhile(body, expression());
    });
  }

  /*private Statement forStatement() {
    return wrap(() -> {
      consume(IDENTIFIER, "variable name expected");

      // var key = nil
      Statement.Var key = new Statement.Var(previous().copyToType(IDENTIFIER, " key "), null, false);
      // var value = nil
      Statement.Var value = new Statement.Var(previous(), null, false);

      if (match(COMMA)) {
        consume(IDENTIFIER, "variable name expected");
        key = value;
        value = new Statement.Var(previous(), null, false);
      }

      consume(IN, "'in' expected after for statement variables");

      // object
      Expression iterable = expression();

      List<Statement> stmtList = new ArrayList<>();

      // key = object.@key(key)
      stmtList.add(new Statement.Expression(
        new Expression.Assign(
          new Expression.Identifier(key.typedName.name.token),
          new Expression.Call(
            new Expression.Get(
              (Expression) iterable.clone(),
              new Expression.Identifier(previous().copyToType(IDENTIFIER, "@key"))
            ),
            List.of(new Expression.Identifier(key.typedName.name.token))
          )
        )
      ));

      // if key == nil {
      //   break
      // }
      stmtList.add(new Statement.If(
        new Expression.Binary(
          new Expression.Identifier(key.typedName.name.token),
          key.typedName.name.token.copyToType(EQUAL_EQ, "=="),
          new Expression.Nil()
        ),
        new Statement.Break(),
        null
      ));

      // value = object.@value(key)
      stmtList.add(new Statement.Expression(
        new Expression.Assign(
          new Expression.Identifier(value.typedName.name.token),
          new Expression.Call(
            new Expression.Get(
              iterable,
              new Expression.Identifier(previous().copyToType(IDENTIFIER, "@value"))
            ),
            List.of(new Expression.Identifier(key.typedName.name.token))
          )
        )
      ));

      // parse the loop body
      stmtList.add(statement());

      return new Statement.Block(
        List.of(
          key,
          value,
          new Statement.While(new Expression.Boolean(true), new Statement.Block(stmtList))
        )
      );
    });
  }*/

  private Statement assertStatement() {
    return wrap(() -> {
      Expression message = null;
      Expression expression = expression();

      if (match(COMMA)) message = expression();
      return new Statement.Assert(expression, message);
    });
  }

  private Statement usingStatement() {
    return wrap(() -> {
      Expression expression = expression();
      List<Expression> caseLabels = new ArrayList<>();
      List<Statement> caseBodies = new ArrayList<>();
      Statement defaultCase = null;

      consume(LBRACE, "'{' expected after using expression");
      ignoreNewlines();

      var state = 0;

      while (!match(RBRACE) && !check(EOF)) {
        if (match(WHEN, DEFAULT, NEWLINE)) {
          if (state == 1) {
            throw new ParserException(
              lexer.getSource(),
              previous(), "'when' cannot exist after a default"
            );
          }

          if (previous().type() == NEWLINE) {
          } else if (previous().type() == WHEN) {
            List<Expression> tmp_cases = new ArrayList<>();
            do {
              ignoreNewlines();
              tmp_cases.add(expression());
            } while (match(COMMA));

            var stmt = statement();

            for (Expression tmp : tmp_cases) {
              caseLabels.add(tmp);
              caseBodies.add(stmt);
            }
          } else {
            state = 1;
            defaultCase = statement();
          }
        } else {
          throw new ParserException(
            lexer.getSource(),
            previous(), "Invalid using statement"
          );
        }
      }

      return new Statement.Using(expression, caseLabels, caseBodies, defaultCase);
    });
  }

  private Statement importStatement() {
    return wrap(() -> {
      List<String> path = new ArrayList<>();
      List<Token> elements = new ArrayList<>();

      while (!match(NEWLINE, EOF, LBRACE)) {
        advance();
        path.add(previous().literal());
      }

      Token importsAll = null;
      int selectCount = 0;

      if (previous().type() == LBRACE) {
        var scan = true;

        while (!check(RBRACE) && scan) {
          ignoreNewlines();
          elements.add(consumeAny("identifier expected", IDENTIFIER, MULTIPLY));

          selectCount++;
          if (previous().type() == MULTIPLY) {
            if (importsAll != null) {
              throw new ParserException(
                lexer.getSource(),
                importsAll, "cannot repeat select all"
              );
            }

            importsAll = previous();
          }

          if (!match(COMMA)) {
            scan = false;
          }
          ignoreNewlines();
        }

        consume(RBRACE, "'}' expected at end of selective import");
      }

      if (importsAll != null && selectCount > 1) {
        throw new ParserException(
          lexer.getSource(),
          importsAll, "cannot import selected items and all at the same time"
        );
      }

      return new Statement.Import(String.join("", path), elements, false);
    });
  }

  private Statement catchStatement() {
    return wrap(() -> {
      Statement.Block body = matchBlock("'{' expected after try");
      Statement.Block catchBody = null;
      Statement.Block finallyBody = null;

      Expression.Identifier exception_var = null;
      if (match(CATCH)) {
        exception_var = wrap(() -> {
          consume(IDENTIFIER, "exception variable expected");
          return new Expression.Identifier(previous());
        });

        catchBody = matchBlock("'{' expected after catch variable name");
      }

      if (exception_var == null && !check(FINALLY)) {
        throw new ParserException(lexer.getSource(), peek(), "try must declare at least one of `catch` or `finally`");
      }

      if (match(FINALLY)) {
        finallyBody = matchBlock("'{' expected after finally");
      }

      return new Statement.Catch(body, catchBody, finallyBody, exception_var);
    });
  }

  private Statement iterStatement() {
    return wrap(() -> {
      if (check(LPAREN)) {
        match(LPAREN);
      }

      Statement declaration = null;
      if (!check(SEMICOLON)) {
        if (check(VAR)) {
          consume(VAR, "variable declaration expected");
        }
        declaration = varDeclaration(false);
      }
      consume(SEMICOLON, "';' expected");
      ignoreNewlinesNoSemi();

      Expression condition = null;
      if (!check(SEMICOLON)) {
        condition = expression();
      }
      consume(SEMICOLON, "';' expected");
      ignoreNewlinesNoSemi();

      Statement.Simple iterator = null;
      if (!check(LBRACE) && !check(RPAREN)) {
        do {
          iterator = expressionStatement(true);
          ignoreNewlines();
        } while (match(COMMA));
      }

      if (check(RPAREN)) {
        match(RPAREN);
      }

      Statement.Block body = matchBlock("'{' expected at beginning of iter block");
      return new Statement.Iter(declaration, condition, iterator, body);
    });
  }

  private Statement statement() {
    return wrap(() -> {
      ignoreNewlines();

      Statement result;

      if (match(CATCH) || match(FINALLY)) {
        throw new ParserException(
          lexer.getSource(), previous(),
          "`catch` and `finally` are only valid in `try` context"
        );
      }

      if (match(ECHO)) {
        result = echoStatement();
      } else if (match(IF)) {
        result = ifStatement();
      } else if (match(WHILE)) {
        result = whileStatement();
      } else if (match(DO)) {
        result = doWhileStatement();
      } else if (match(ITER)) {
        result = iterStatement();
      } /*else if (match(FOR)) {
        result = forStatement();
      }*/ else if (match(USING)) {
        result = usingStatement();
      } else if (match(CONTINUE)) {
        result = new Statement.Continue();
      } else if (match(BREAK)) {
        result = new Statement.Break();
      } else if (match(RETURN)) {
        result = new Statement.Return(expression());
      } else if (match(ASSERT)) {
        result = assertStatement();
      } else if (match(RAISE)) {
        result = new Statement.Raise(expression());
      } else if (match(LBRACE)) {
        result = block();
      } else if (match(IMPORT)) {
        result = importStatement();
      } else if (match(TRY)) {
        result = catchStatement();
      } else {
        result = expressionStatement(false);
      }

      ignoreNewlines();

      return result;
    });
  }

  private Statement varDeclaration(boolean isConstant) {
    return wrap(() -> {
      Expression.TypedName name = variableTypedName();

      Statement declaration;
      if (match(EQUAL)) {
        Expression value = expression();
        if (value == null) {
          throw new ParserException(lexer.getSource(), previous(), "incomplete variable declaration");
        }

        declaration = (Statement.Var) reflectWrap(name, value, new Statement.Var(name, value, isConstant));
      } else if(name.type == null) {
        throw new ParserException(lexer.getSource(), peek(), "Type or value must be declared");
      } else {
        if (isConstant) {
          throw new ParserException(lexer.getSource(), peek(), "constant value not declared");
        }

        declaration = (Statement.Var) reflectWrap(name, new Statement.Var(name, new Expression.Nil(), false));
      }

      if (check(COMMA)) {
        List<Statement> declarations = new ArrayList<>();
        declarations.add(declaration);

        while (match(COMMA)) {
          ignoreNewlines();
          name = variableTypedName();

          if (match(EQUAL)) {
            Expression value = expression();
            if (value == null) {
              throw new ParserException(lexer.getSource(), previous(), "incomplete variable declaration");
            }

            declarations.add((Statement) reflectWrap(name, value, new Statement.Var(name, value, isConstant)));
          } else if(name.type == null) {
            throw new ParserException(lexer.getSource(), peek(), "Type or value must be declared");
          } else {
            if (isConstant) {
              throw new ParserException(lexer.getSource(), peek(), "constant value not declared");
            }
            declarations.add((Statement) reflectWrap(name, new Statement.Var(name, new Expression.Nil(), false)));
          }
        }

        return new Statement.VarList(declarations);
      }

      return declaration;
    });
  }

  private boolean functionArgs(List<Expression.TypedName> params) {
    ignoreNewlines();
    boolean isVariadic = false;

    while (check(IDENTIFIER, TRI_DOT)) {
      if (previous().type() == TRI_DOT) {
        isVariadic = true;
        params.add(wrap(() -> new Expression.TypedName(
          wrap(() -> {
            consume(IDENTIFIER, "variable parameter name expected");
            return new Expression.Identifier(previous());
          }), null)));
        break;
      }

      params.add(typedName());

      if (!check(RPAREN)) {
        consume(COMMA, "',' expected between function arguments");
        ignoreNewlines();
      }
    }

    return isVariadic;
  }

  private Expression anonymous() {
    return wrap(() -> {
      int startOffset = previous().offset();
      int startLine = previous().line();

      Token nameCompatToken = previous();

      List<Expression.TypedName> params = new ArrayList<>();
      boolean isVariadic = false;

      if (check(LPAREN)) {
        consume(LPAREN, "expected '(' at start of anonymous function");

        if (!check(RPAREN)) {
          isVariadic = functionArgs(params);
        }

        consume(RPAREN, "expected ')' after anonymous function parameters");
      }

      Typed returnType = parseReturnType("missing return type after anonymous function arguments");

      var body = matchBlock("'{' expected after function declaration");

      Statement.Function function = new Statement.Function(
        nameCompatToken.copyToType(IDENTIFIER, "@anon" + (anonymousCount++)),
        params, returnType, body, isVariadic
      );
      function.startLine = startLine;
      function.startColumn = startOffset;
      function.endLine = previous().line();
      function.endColumn = previous().offset();

      return new Expression.Anonymous(function);
    });
  }

  private Statement defDeclaration() {
    return wrap(() -> {
      consume(IDENTIFIER, "function name expected");
      Token name = previous();
      List<Expression.TypedName> params = new ArrayList<>();

      consume(LPAREN, "'(' expected after function name");
      boolean isVariadic = functionArgs(params);
      consume(RPAREN, "')' expected after function arguments");

      Typed returnType = parseReturnType("missing return type after function arguments");

      var body = matchBlock("'{' expected after function declaration");

      return new Statement.Function(name, params, returnType, body, isVariadic);
    });
  }

  private Statement.Property classField(boolean isStatic, boolean isConst) {
    return wrap(() -> {
      Expression.TypedName name = typedName("class property name expected");

      Expression value = null;
      if (match(EQUAL)) value = expression();

      endStatement();
      ignoreNewlines();

      return new Statement.Property(name, value, isStatic, isConst);
    });
  }

  private Statement.Method classOperator() {
    return wrap(() -> {
      consumeAny("non-assignment operator expected", OPERATORS);
      var name = previous();

      List<Expression.TypedName> params = new ArrayList<>();

      params.add(new Expression.TypedName(new Expression.Identifier(previous().copyToType(IDENTIFIER, "__arg__")), null));

      var body = matchBlock("'{' expected after operator declaration");

      return new Statement.Method(name, params, null, body, false, false);
    });
  }

  private Statement.Method method(boolean isStatic) {
    return wrap(() -> {
      consumeAny("method name expected", IDENTIFIER, DECORATOR);
      Token name = previous();

      List<Expression.TypedName> params = new ArrayList<>();

      consume(LPAREN, "'(' expected after method name");
      boolean isVariadic = functionArgs(params);
      consume(RPAREN, "')' expected after method arguments");

      Typed returnType = parseReturnType("missing return type after method arguments");

      var body = matchBlock("'{' expected after method declaration");

      return new Statement.Method(name, params, returnType, body, isStatic, isVariadic);
    });
  }

  private Statement classDeclaration() {
    return wrap(() -> {
      List<Statement.Property> properties = new ArrayList<>();
      List<Statement.Method> methods = new ArrayList<>();
      List<Statement.Method> operators = new ArrayList<>();

      consume(IDENTIFIER, "class name expected");
      Token name = previous();
      Expression.Identifier superclass = null;

      if (match(LESS)) {
        superclass = wrap(() -> {
          consume(IDENTIFIER, "super class name expected");
          return new Expression.Identifier(previous());
        });
      }

      ignoreNewlines();
      consume(LBRACE, "'{' expected after class declaration");
      ignoreNewlines();

      while (!check(RBRACE) && !check(EOF)) {
        boolean isStatic;

        ignoreNewlines();

        if (match(STATIC)) isStatic = true;
        else {
          isStatic = false;
        }

        if (check(VAR)) {
          properties.add(wrap(() -> {
            match(VAR);
            return classField(isStatic, false);
          }));
        } else if (check(CONST)) {
          properties.add(wrap(() -> {
            match(CONST);
            return classField(isStatic, true);
          }));
        } else if (check(DEF)) {
          operators.add(wrap(() -> {
            match(DEF);
            return classOperator();
          }));
          ignoreNewlines();
        } else {
          methods.add(method(isStatic));
          ignoreNewlines();
        }
      }

      boolean hasConstructor = methods.stream().anyMatch(m -> m.name.literal().equals("@new"));
      if(!hasConstructor) {
        // create default constructor here
        methods.add(new Statement.Method(
          previous().copyToType(IDENTIFIER, "@new"),
          List.of(),
          wrap(Typed.Void::new),
          wrap(() -> new Statement.Block(List.of())),
          false,
          false
        ));
      }

      consume(RBRACE, "'{' expected at end of class definition");
      return new Statement.Class(
        name,
        superclass,
        properties,
        methods,
        operators
      );
    });
  }

  private Statement externDeclaration(boolean isStatic) {
    return wrap(() -> {
      consume(IDENTIFIER, "function name expected");
      Token name = previous();
      List<Expression.TypedName> params = new ArrayList<>();

      consume(LPAREN, "'(' expected after function name");
      boolean isVariadic = functionArgs(params);
      consume(RPAREN, "')' expected after function arguments");

      Typed returnType = parseReturnType("missing return type after function arguments");

      endStatement();
      return new Statement.Extern(name, params, returnType, isVariadic);
    });
  }

  private Statement declaration() {
    return wrap(() -> {
      ignoreNewlines();

      Statement result;

      if (match(VAR)) {
        result = varDeclaration(false);
        endStatement();
      } else if (match(CONST)) {
        result = varDeclaration(true);
        endStatement();
      } else if (match(DEF)) {
        result = defDeclaration();
      } else if(blockCount == 0 && check(DECORATOR) && peek().literal().equals("@def")) {
        match(DECORATOR);
        boolean isStatic = match(STATIC);
        result = externDeclaration(isStatic);

      } else if (match(CLASS)) {
        result = classDeclaration();
      } else if (match(LBRACE)) {
        if (!check(NEWLINE) && blockCount == 0) {
          result = new Statement.Simple(doCall(dict()));
        } else {
          result = block();
        }
      } else {
        result = statement();
      }

      ignoreNewlines();
      return result;
    });
  }

  public List<Statement> parse() {
    List<Statement> result = new ArrayList<>();

    while (!isAtEnd()) {
      var x = declaration();
      result.add(x);
    }

    return result;
  }

  @Override
  public String toString() {
    return String.format(
      "<rem::Parser path='%s' tokens=%d>",
      lexer.getSource().getPath(),
      tokens.size()
    );
  }

  private <T extends AST> T wrap(Callback<T> callback, T ast) {
    int startLine = peek().line();
    int startOffset = peek().offset();

    T result = callback.run(ast);

    if (result != null && !result.wrapped) {
      int endLine = previous().line();
      int endOffset = previous().offset();

      result.startLine = startLine;
      result.startColumn = startOffset;
      result.endLine = endLine;
      result.endColumn = endOffset;

      // mark ad wrapped
      result.wrapped = true;
    }

    return result;
  }

  private <T extends AST> T wrap(FlatCallback<T> callback) {
    return wrap((ignore) -> callback.run(), null);
  }

  private <T extends AST> T reflectWrap(T template, T value) {
    value.wrapped = true;
    value.startLine = template.startLine;
    value.startColumn = template.startColumn;
    value.endLine = template.endLine;
    value.endColumn = template.endColumn;
    return value;
  }

  private <T extends AST> T reflectWrap(T startTemplate, T endTemplate, T value) {
    value.wrapped = true;
    value.startLine = startTemplate.startLine;
    value.startColumn = startTemplate.startColumn;
    value.endLine = endTemplate.endLine;
    value.endColumn = endTemplate.endColumn;
    return value;
  }

  interface FlatCallback<T> {
    T run();
  }

  interface Callback<T> {
    T run(T ast);
  }
}
