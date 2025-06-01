package org.rem.parser;

import org.jspecify.annotations.NonNull;

import java.util.List;

import static org.rem.parser.TokenType.*;
import static org.rem.parser.TokenType.AMP;
import static org.rem.parser.TokenType.AND;
import static org.rem.parser.TokenType.BANG_EQ;
import static org.rem.parser.TokenType.BAR;
import static org.rem.parser.TokenType.DIVIDE;
import static org.rem.parser.TokenType.EQUAL_EQ;
import static org.rem.parser.TokenType.FLOOR;
import static org.rem.parser.TokenType.GREATER;
import static org.rem.parser.TokenType.GREATER_EQ;
import static org.rem.parser.TokenType.LESS;
import static org.rem.parser.TokenType.LESS_EQ;
import static org.rem.parser.TokenType.LSHIFT;
import static org.rem.parser.TokenType.OR;
import static org.rem.parser.TokenType.PERCENT;
import static org.rem.parser.TokenType.POW;
import static org.rem.parser.TokenType.RSHIFT;
import static org.rem.parser.TokenType.TILDE;
import static org.rem.parser.TokenType.URSHIFT;
import static org.rem.parser.TokenType.XOR;

public record Token(TokenType type, String literal, int line, int offset) {

  private static final List<TokenType> ARITHEMETIC_OPS = List.of(
    PLUS, MINUS, MULTIPLY, POW, DIVIDE, PERCENT, FLOOR
  );

  private static final List<TokenType> BITWISE_OPS = List.of(
    LSHIFT, RSHIFT, URSHIFT, AMP, BAR, TILDE, XOR
  );

  private static final List<TokenType> COMPARISON_OPS = List.of(
    LESS, LESS_EQ, GREATER, GREATER_EQ
  );

  private static final List<TokenType> LOGICAL_OPS = List.of(
    AND, OR
  );

  private static final List<TokenType> EQUALITY_OPS = List.of(
    EQUAL_EQ, BANG_EQ
  );

  private static final List<TokenType> ASSIGNING_OPS = List.of(
    EQUAL, PLUS_EQ, MINUS_EQ, PERCENT_EQ, DIVIDE_EQ, MULTIPLY_EQ,
    FLOOR_EQ, POW_EQ, AMP_EQ, BAR_EQ, TILDE_EQ, XOR_EQ,
    LSHIFT_EQ, RSHIFT_EQ, URSHIFT_EQ
  );

  @Override
  @NonNull
  public String toString() {
    return String.format("<ast::Token type=%s literal='%s' line=%d offset=%d>", type, literal, line, offset);
  }

  public Token copyToType(TokenType type, String literal) {
    return new Token(type, literal, line, offset);
  }

  public Token copyToType(TokenType type) {
    return new Token(type, literal, line, offset);
  }

  public boolean isArithemetic() {
    return ARITHEMETIC_OPS.contains(this.type);
  }

  public boolean isBitwise() {
    return BITWISE_OPS.contains(this.type);
  }

  public boolean isComparison() {
    return COMPARISON_OPS.contains(this.type);
  }

  public boolean isLogical() {
    return LOGICAL_OPS.contains(this.type);
  }

  public boolean isEquality() {
    return EQUALITY_OPS.contains(this.type);
  }

  public boolean isAssignmentOp() {
    return ASSIGNING_OPS.contains(this.type);
  }
}
