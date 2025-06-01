package org.rem.exceptions;

public class LexerException extends RuntimeException {
  private final int line;
  private final int column;

  public LexerException(int line, int column, String message) {
    super(String.format("%s on line %d:%d", message, line, column));
    this.line = line;
    this.column = column;
  }
}
