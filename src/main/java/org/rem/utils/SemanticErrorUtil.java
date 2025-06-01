package org.rem.utils;

import norswap.uranium.SemanticError;
import org.rem.parser.Source;
import org.rem.parser.ast.AST;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SemanticErrorUtil {
  public static String trace(SemanticError error, Source source, List<String> lines, String path) {
    AST location = (AST) error.location();
    if(location == null) {
      return error.description;
    } else {
      int startColumn = location.startColumn - source.getLineStart(location.startColumn);
      if(startColumn < 0) startColumn = 0;

      StringBuilder builder = new StringBuilder();
      builder.append(String.format("ERROR: %s at %s:%s:%s", error.description, path, location.startLine, startColumn + 1));

      String line = String.join("\n", lines.subList(location.startLine - 1, location.endLine));
      builder.append(String.format("\n\t%s", line));
      builder.append(String.format("\n\t%s%s", " ".repeat(startColumn), "^"));

      return builder.toString();
    }
  }

  public static String trace(Set<SemanticError> errors, Source source) {
    List<String> strings = new ArrayList<>();

    // TODO improve
    List<String> lines = source.getLines();
    String path = source.getPath();

    for(SemanticError error : errors) {
      strings.add(trace(error, source, lines, path));
    }

    return String.join("\n", strings);
  }
}
