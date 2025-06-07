package org.rem.compiler.targets.llvm;

import org.rem.parser.ast.Expression;

public final class ExpressionUtil {
  public static LoopType loopTypeForCondition(Expression cond, boolean isDoWhile) {
    if(cond == null) {
      if(isDoWhile) return LoopType.NONE;
      return LoopType.INFINITE;
    }

    if(cond instanceof Expression.Boolean bool) {
      return bool.value ? LoopType.INFINITE : LoopType.NONE;
    }

    return LoopType.NORMAL;
  }
}
