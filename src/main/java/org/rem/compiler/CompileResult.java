package org.rem.compiler;

import java.util.List;

public class CompileResult<T> {
  private final List<T> nodes;

  public CompileResult(List<T> nodes) {
    this.nodes = nodes;
  }
}
