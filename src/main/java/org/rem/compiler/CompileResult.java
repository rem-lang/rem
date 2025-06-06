package org.rem.compiler;

import org.rem.interfaces.ICompileTarget;

import java.util.List;

public class CompileResult<T> {
  private final ICompileTarget<T> target;
  private final List<T> nodes;

  public CompileResult(ICompileTarget<T> target, List<T> nodes) {
    this.target = target;
    this.nodes = nodes;
  }

  public ICompileTarget<T> getTarget() {
    return target;
  }

  public List<T> getNodes() {
    return nodes;
  }
}
