package org.rem.generators;

import org.rem.compiler.CompileResult;
import org.rem.interfaces.IGenerator;
import org.rem.nodes.Node;

public class JSGenerator implements IGenerator<Node> {
  @Override
  public int generate(CompileResult<Node> result, String outputName) {
    return 0;
  }
}
