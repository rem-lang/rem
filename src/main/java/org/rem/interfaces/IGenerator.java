package org.rem.interfaces;

import org.rem.compiler.CompileResult;

import java.io.File;

public interface IGenerator<T> {
  int generate(CompileResult<T> result, String outputName);
}
