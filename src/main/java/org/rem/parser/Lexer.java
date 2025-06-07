package org.rem.parser;

import org.rem.exceptions.LexerException;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.rem.parser.TokenType.*;

public class Lexer {
  private final static int MAX_INTERPOLATION_NESTING = 8;

  private int line = 1;
  private int current = 0;
  private int start = 0;
  private int lineStart = 0;
  private Token lastToken;
  private final Stack<Character> interpolating = new Stack<>();

  private final Source source;
  public final String sourceString;
  private final List<Token> tokens = new ArrayList<>();

  public Lexer(Source source) {
    this.source = source;
    this.sourceString = source.getContent();
  }

  private final Dictionary<String, TokenType> keywords = createKeywords();

  private Dictionary<String, TokenType> createKeywords() {
    return new Hashtable<>() {
      {
        put("and", AND);
        put("assert", ASSERT);
        put("break", BREAK);
        put("catch", CATCH);
        put("class", CLASS);
        put("const", CONST);
        put("continue", CONTINUE);
        put("def", DEF);
        put("default", DEFAULT);
        put("do", DO);
        put("echo", ECHO);
        put("else", ELSE);
        put("false", FALSE);
        put("finally", FINALLY);
        put("for", FOR);
        put("if", IF);
        put("import", IMPORT);
        put("in", IN);
        put("iter", ITER);
        put("new", NEW);
        put("nil", NIL);
        put("or", OR);
        put("parent", PARENT);
        put("raise", RAISE);
        put("return", RETURN);
        put("self", SELF);
        put("static", STATIC);
        put("true", TRUE);
        put("try", TRY);
        put("using", USING);
        put("var", VAR);
        put("when", WHEN);
        put("while", WHILE);
      }

      @Override
      public synchronized TokenType get(Object key) {
        if (this.containsKey(key)) {
          return super.get(key);
        }

        return IDENTIFIER;
      }
    };
  }

  private boolean isAtEnd() {
    return current >= sourceString.length();
  }

  private boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  private boolean isAlpha(char c) {
    return (c >= 'a' && c <= 'z') ||
      (c >= 'A' && c <= 'Z') ||
      c == '_';
  }

  private boolean isAlphanumeric(char c) {
    return isAlpha(c) || isDigit(c);
  }

  private boolean isBinary(char c) {
    return c == '1' || c == '0';
  }

  private boolean isOctal(char c) {
    return c >= '0' && c <= '7';
  }

  private boolean isHexadecimal(char c) {
    return this.isDigit(c) ||
      (c >= 'a' && c <= 'f') ||
      (c >= 'A' && c <= 'F');
  }

  /**
   * Returns the current token while moving the pointer forward
   */
  private char advance() {
    char val = sourceString.charAt(current);
    current++;

    if (val == '\n') {
      lineStart = current;
      line++;
    }

    return val;
  }

  /**
   * Advances if the current character is the given character
   */
  private boolean match(char c) {
    if (isAtEnd()) return false;
    if (sourceString.charAt(current) != c) return false;
    current++;

    if (c == '\n') {
      lineStart = current;
      line++;
    }

    return true;
  }

  /**
   * Returns the character at the next position without
   * consuming the current one.
   */
  private char next() {
    if (current + 1 >= sourceString.length()) return 0;
    return sourceString.charAt(current + 1);
  }

  /**
   * Returns the character at the next position without
   * consuming the current one.
   */
  private char previous() {
    if (current == 0) return 0;
    return sourceString.charAt(current - 1);
  }

  /**
   * Returns the character at the current position without
   * consuming it.
   */
  private char peek() {
    if (isAtEnd()) return 0;
    return sourceString.charAt(current);
  }

  /**
   * Adds a new token to the list of tokens
   */
  private void addToken(TokenType type) {
    addToken(type, sourceString.subSequence(start, current).toString().trim());
  }

  /**
   * Adds a new token to the list of tokens
   */
  private void addToken(TokenType type, String literal) {
    Token token = new Token(type, literal, line, start);
    lastToken = token;
    tokens.add(token);
  }

  /**
   * Skips block comments
   */
  private void skipBlockComments() {
    int nesting = 1;
    while (nesting > 0) {
      if (isAtEnd()) {
        throw new LexerException(line, current - start, "Unclosed block comment");
      }

      // fix line number changes within a block comment.
      if(previous() == '\n') {
        lineStart = current;
      }

      // internal comment open
      if (peek() == '/' && next() == '*') {
        advance();
        advance();
        nesting++;
      }

      // comment close
      else if (peek() == '*' && next() == '/') {
        advance();
        advance();
        nesting--;
      } else advance();
    }
  }

  /**
   * Skips whitespace and comments
   */
  private void skipWhitespace() {
    while (true) {
      char c = peek();

      switch (c) {

        // whitespace
        case ' ':
        case '\r':
        case '\t': {
          advance();
          break;
        }

        // single line comment
        case '#': {
          while (peek() != '\n' && !isAtEnd()) advance();
          break;
        }

        case '/': {
          if (next() == '*') {
            advance();
            advance();
            skipBlockComments();
            break;
          } else {
            return;
          }
        }

        default:
          return;
      }
    }
  }

  /**
   * Parses a decorator name
   */
  private void decorator() {
    while (isAlphanumeric(peek())) advance();
    addToken(DECORATOR);
  }

  /**
   * Parses a string surrounded by the quote c.
   */
  private void string(char c) {
    while (peek() != c && !isAtEnd()) {
      if (peek() == '$' && next() == '{' && previous() != '\\') {  // interpolation started

        if (interpolating.size() < MAX_INTERPOLATION_NESTING) {
          interpolating.push(c);
          current++;
          addToken(INTERPOLATION, getUnquotedString(c));
          current++;
          return;
        }

        throw new LexerException(
          line, current - start,
          "Maximum interpolation nesting exceeded"
        );
      }

      if (peek() == '\\' && (next() == c || next() == '\\')) advance();
      advance();
    }

    if (isAtEnd()) {
      throw new LexerException(line, current, String.format("Unterminated string on line %d", line));
    }

    match(c);
    addToken(LITERAL, getUnquotedString(c));
  }

  /**
   * Parses a valid Blade number
   */
  private void number() {
    if (previous() == '0') {
      if (match('b')) {   // binary number
        while (isBinary(peek())) advance();
        addToken(BIN_NUMBER);
        return;
      } else if (match('c')) {  // octal number
        while (isOctal(peek())) advance();
        addToken(OCT_NUMBER);
        return;
      } else if (match('x')) {  // hex number
        while (isHexadecimal(peek())) advance();
        addToken(HEX_NUMBER);
        return;
      }
    }

    while (isDigit(peek()) || peek() == '_') advance();

    if(peek() == 'n') {
      // we've encountered a big integer
      advance();
      addToken(BIG_NUMBER, sourceString.subSequence(start, current - 1).toString().replace("_", "").trim());
      return;
    }

    if (peek() == '.' && isDigit(next())) {

      do advance();
      while (isDigit(peek()) || peek() == '_');

      // E or e are only a valid case followed: by a digit and
      // occurring after a dot.
      if ((peek() == 'e' || peek() == 'E') && (next() == '+' || next() == '-')) {
        advance();

        do advance();
        while (isDigit(peek()));
      }
    }

    addToken(REG_NUMBER, sourceString.subSequence(start, current).toString().replace("_", "").trim());
  }

  /**
   * Scans identifiers and keywords
   */
  private void identifier() {
    while (isAlphanumeric(peek())) advance();

    String text = sourceString.subSequence(start, current).toString().trim();
    addToken(keywords.get(text), text);
  }

  /**
   * The private scanning helper
   */
  private void scan() {
    skipWhitespace();
    start = current;

    if (isAtEnd()) {
      return;
    }

    var c = advance();

    switch (c) {
      case '(':
        addToken(LPAREN);
        break;
      case ')':
        addToken(RPAREN);
        break;
      case '[':
        addToken(LBRACKET);
        break;
      case ']':
        addToken(RBRACKET);
        break;
      case '{':
        addToken(LBRACE);
        break;
      case '}': {
        if (!interpolating.isEmpty()) {
          string(interpolating.pop());
        } else {
          addToken(RBRACE);
        }
        break;
      }
      case ',':
        addToken(COMMA);
        break;
      case ';':
        addToken(SEMICOLON);
        break;
      case '@': {
        if (!isAlpha(peek())) {
          addToken(AT);
        } else {
          decorator();
        }

        break;
      }
      case '.': {
        if (match('.')) {
          addToken(match('.') ? TRI_DOT : RANGE);
        } else {
          addToken(DOT);
        }

        break;
      }
      case '-': {
        if (match('-')) {
          addToken(DECREMENT);
        } else if (match('=')) {
          addToken(MINUS_EQ);
        } else {
          addToken(MINUS);
        }

        break;
      }
      case '+': {
        if (match('+')) {
          addToken(INCREMENT);
        } else if (match('=')) {
          addToken(PLUS_EQ);
        } else {
          addToken(PLUS);
        }

        break;
      }
      case '*': {
        if (match('*')) {
          addToken(match('=') ? POW_EQ : POW);
        } else {
          addToken(match('=') ? MULTIPLY_EQ : MULTIPLY);
        }

        break;
      }
      case '/': {
        if (match('/')) {
          addToken(match('=') ? FLOOR_EQ : FLOOR);
        } else {
          addToken(match('=') ? DIVIDE_EQ : DIVIDE);
        }

        break;
      }
      case '\\':
        addToken(BACKSLASH);
        break;
      case ':':
        addToken(COLON);
        break;
      case '<': {
        if (match('<')) {
          addToken(match('=') ? LSHIFT_EQ : LSHIFT);
        } else {
          addToken(match('=') ? LESS_EQ : LESS);
        }

        break;
      }
      case '>': {
        if (match('>')) {
          if (match('>')) {
            addToken(match('=') ? URSHIFT_EQ : URSHIFT);
          } else {
            addToken(match('=') ? RSHIFT_EQ : RSHIFT);
          }
        } else {
          addToken(match('=') ? GREATER_EQ : GREATER);
        }

        break;
      }
      case '!':
        addToken(match('=') ? BANG_EQ : BANG);
        break;
      case '=':
        addToken(match('=') ? EQUAL_EQ : EQUAL);
        break;
      case '%':
        addToken(match('=') ? PERCENT_EQ : PERCENT);
        break;
      case '&':
        addToken(match('=') ? AMP_EQ : AMP);
        break;
      case '|':
        addToken(match('=') ? BAR_EQ : BAR);
        break;
      case '^':
        addToken(match('=') ? XOR_EQ : XOR);
        break;
      case '~':
        addToken(match('=') ? TILDE_EQ : TILDE);
        break;
      case '?':
        addToken(QUESTION);
        break;

      // newline token
      case '\n':
        addToken(NEWLINE);
        break;

      case '\'':
        string('\'');
        break;
      case '"':
        string('"');
        break;

      default: {
        if (isDigit(c)) {
          number();
        } else if (isAlpha(c)) {
          identifier();
        } else {
          throw new LexerException(line, current - start, String.format("Unexpected character '%c'", c));
        }
        break;
      }
    }
  }

  /**
   * Scans the source and returns a list of tokens.
   *
   * @return list[Token]
   */
  public List<Token> run() {
    while (!isAtEnd()) {
      skipWhitespace();

      scan();
    }

    addToken(EOF, "");
    return tokens;
  }

  private String getUnquotedString(char quote) {
    Charset UTF_8 = StandardCharsets.UTF_8;
    String escaped = sourceString.subSequence(start + 1, current - 1).toString()
      .replace("\\0", "\0")
      .replace("\\$", "$")
      .replace("\\'", quote == '\'' || quote == '}' ? "'" : "\\'")
      .replace("\\\"", quote == '\"' || quote == '}' ? "'" : "\\\"")
      .replace("\\b", "\b")
      .replace("\\f", "\f")
      .replace("\\n", "\n")
      .replace("\\r", "\r")
      .replace("\\t", "\t")
      .replace("\\\\", "\\")
      .replace("\\n", "\n");

    // ensure we align to UTF8
    return UTF_8.decode(UTF_8.encode(escaped)).toString();
  }

  public Source getSource() {
    return source;
  }
}
