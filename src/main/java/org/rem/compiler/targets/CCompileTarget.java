package org.rem.compiler.targets;

import norswap.uranium.Reactor;
import org.rem.compiler.BaseCompileTarget;
import org.rem.generators.CGenerator;
import org.rem.interfaces.IGenerator;
import org.rem.interfaces.IType;
import org.rem.nodes.Node;
import org.rem.types.DefType;

public class CCompileTarget extends BaseCompileTarget<Node> {
  public CCompileTarget(Reactor reactor) {
    super(reactor);
  }

  private String cType(IType type, String name) {
    return switch (type.type()) {
      case BOOL -> "bool";
      case I8 -> "int8_t";
      case I16 -> "int16_t";
      case I32 -> "int";
      case I64 -> "int64_t";
      case F32 -> "float";
      case F64 -> "double";
      case F128 -> "long double";
      case VOID -> "void";
      case DEF -> {
        DefType defType = (DefType) type;
        IType[] parameters = defType.getParameterTypes();

        StringBuilder builder = new StringBuilder();

        builder.append(cType(defType.getReturnType()));
        builder.append(" (*");
        builder.append(name);
        builder.append(") ");

        builder.append("(");

        for(int i = 0; i < parameters.length; i++) {
          builder.append(cType(parameters[i]));
          if(i < parameters.length - 1) {
            builder.append(", ");
          }
        }

        builder.append(")");

        yield  builder.toString();
      }
      case CLASS -> type.name();
      default -> "";
    };
  }

  private String cType(IType type) {
    return cType(type, "");
  }

  @Override
  public IGenerator<Node> getGenerator() {
    return new CGenerator();
  }
}
