package org.rem.compiler.targets;

import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.LLVMContextRef;
import org.bytedeco.llvm.LLVM.LLVMTypeRef;
import org.bytedeco.llvm.LLVM.LLVMValueRef;
import org.rem.compiler.BaseCompileTarget;
import org.rem.interfaces.IType;
import org.rem.parser.ast.Expression;
import org.rem.parser.ast.Statement;
import org.rem.types.*;

import static org.bytedeco.llvm.global.LLVM.*;

public class LLVMCompileTarget extends BaseCompileTarget<LLVMValueRef> {
  LLVMContextRef context;

  public LLVMCompileTarget() {
    this.context = LLVMContextCreate();
  }

  private LLVMTypeRef llvmType(LLVMContextRef context, IType type) {
    return switch (type.type()) {
      case BOOL -> LLVMInt1TypeInContext(context);
      case I8 -> LLVMInt8TypeInContext(context);
      case I16 -> LLVMInt16TypeInContext(context);
      case I32 -> LLVMInt32TypeInContext(context);
      case I64 -> LLVMInt64TypeInContext(context);
      case I128 -> LLVMInt128TypeInContext(context);
      case F32 -> LLVMFloatTypeInContext(context);
      case F64 -> LLVMDoubleTypeInContext(context);
      case F128 -> LLVMFP128TypeInContext(context);
      case ARRAY -> {
        ArrayType vecType = (ArrayType) type;
        yield LLVMArrayType2(llvmType(context, vecType.getType()), vecType.getLength());
      }
      case PARAMETERIZED -> {
        VecType vecType = (VecType) type;
        yield LLVMVectorType(llvmType(context, vecType.getType()), vecType.getLength());
      }
      case DEF -> {
        DefType defType = (DefType) type;
        IType[] parameters = defType.getParameterTypes();
        IType returnType = defType.getReturnType();

        try(PointerPointer<LLVMTypeRef> paramsPointer = new PointerPointer<>()) {
          for(IType param : parameters) {
            paramsPointer.put(llvmType(context, param));
          }

          yield LLVMFunctionType(
            llvmType(context, returnType),
            paramsPointer,
            parameters.length,
            defType.isVariadic() ? 1 : 0
          );
        }
      }
      // TODO: Handle classes here...
//      case CLASS -> {
//        ClassType classType = (ClassType) type;
//
//
//      }
      default -> LLVMVoidType();
    };
  }

  @Override
  public LLVMValueRef visitExpression(Expression expression) {
    return expression.accept(this);
  }

  @Override
  public LLVMValueRef visitStatement(Statement statement) {
    return statement.accept(this);
  }

  @Override
  public LLVMValueRef visitBooleanExpression(Expression.Boolean expr) {
    return LLVMConstInt(llvmType(context, BoolType.INSTANCE), expr.value ? 1 : 0, 0);
  }

  @Override
  public LLVMValueRef visitInt32Expression(Expression.Int32 expr) {
    return LLVMConstInt(llvmType(context, I32Type.INSTANCE), expr.value, expr.value < 0 ? 1 : 0);
  }

  @Override
  public LLVMValueRef visitInt64Expression(Expression.Int64 expr) {
    return LLVMConstInt(llvmType(context, I64Type.INSTANCE), expr.value, expr.value < 0 ? 1 : 0);
  }

  @Override
  public LLVMValueRef visitFloat32Expression(Expression.Float32 expr) {
    return LLVMConstReal(llvmType(context, F32Type.INSTANCE), expr.value);
  }

  @Override
  public LLVMValueRef visitFloat64Expression(Expression.Float64 expr) {
    return LLVMConstReal(llvmType(context, F64Type.INSTANCE), expr.value);
  }
}
