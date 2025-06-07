package org.rem.compiler.targets.js;

import norswap.uranium.Reactor;
import org.rem.compiler.BaseCompileTarget;
import org.rem.generators.JSGenerator;
import org.rem.interfaces.IGenerator;
import org.rem.nodes.Node;

public class JSCompileTarget extends BaseCompileTarget<Node> {
  public JSCompileTarget(Reactor reactor) {
    super(reactor);
  }

  @Override
  public IGenerator<Node> getGenerator() {
    return new JSGenerator();
  }
}
