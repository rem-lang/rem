package org.rem.parser;

import org.rem.parser.ast.AST;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Source {
  private final String path;
  private final String content;
  private final List<String> lines;

  public Source(String path, String content) {
    this.path = path;
    this.content = content;
    this.lines = content.lines().toList();
  }

  public Source(File file) throws IOException {
    this(file.getAbsolutePath(), Files.readString(Path.of(file.getAbsolutePath())));
  }

  public String getPath() {
    return path;
  }

  public String getContent() {
    return content;
  }

  public List<String> getLines() {
    return lines;
  }

  public int getLineStart(int offset) {
    if(offset >= content.length()) {
      return content.length() - 1;
    }

    for(var i = offset; i >= 0; i--) {
      if(content.charAt(i) == '\n') {
        return i + 1;
      }
    }

    return offset;
  }

  public int getLineColumn(int column) {
    int startColumn = column - getLineStart(column);
    if(startColumn < 0) startColumn = 0;
    return startColumn + 1;
  }
}
