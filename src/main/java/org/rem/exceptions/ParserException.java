package org.rem.exceptions;


import org.rem.parser.Source;
import org.rem.parser.Token;

public class ParserException extends RuntimeException {
  private final Source source;
  private final int line;
  private final int offset;

  public ParserException(Source source, Token token, String message) {
    super(message + " on line " + token.line() + ":" + source.getLineColumn(token.offset()));
    this.source = source;
    this.line = token.line();
    this.offset = token.offset();
  }
}
