package org.rem.interfaces;

import org.rem.compiler.CompileResult;

public interface IGenerator<T> {
  int generate(CompileResult<T> result);
}
