package org.rem.compiler.targets;

import norswap.uranium.Reactor;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.*;
import org.rem.compiler.BaseCompileTarget;
import org.rem.generators.LLVMGenerator;
import org.rem.interfaces.IGenerator;
import org.rem.interfaces.IType;
import org.rem.parser.ast.Expression;
import org.rem.parser.ast.Statement;
import org.rem.types.*;
import org.rem.utils.TypeUtil;

import static org.bytedeco.llvm.global.LLVM.*;

@SuppressWarnings("resource")
public class LLVMCompileTarget extends BaseCompileTarget<LLVMValueRef> {
  private final LLVMContextRef context;
  private LLVMBuilderRef builder = LLVMCreateBuilder();
  private final LLVMModuleRef module;
  private LLVMBasicBlockRef currentBlock;

  public LLVMCompileTarget(Reactor reactor) {
    super(reactor);
    this.context = LLVMContextCreate();
    module = LLVMModuleCreateWithNameInContext("__main__", context);
  }

  @Override
  public IGenerator<LLVMValueRef> getGenerator() {
    return new LLVMGenerator();
  }

  public LLVMModuleRef getModule() {
    return module;
  }

  private LLVMTypeRef llvmType(LLVMContextRef context, IType type) {
    return switch (type.type()) {
      case VOID -> LLVMVoidTypeInContext(context);
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

  private LLVMTypeRef llvmType(IType type) {
    return llvmType(context, type);
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

  @Override
  public LLVMValueRef visitUnaryExpression(Expression.Unary expr) {
    var type = getType(expr);
    var value = LLVMBuildBitCast(builder, visitExpression(expr.right), type, "");

    return switch (expr.op.type()) {
      case MINUS -> LLVMBuildNeg(builder, value, "");
      // TODO: Fix for correctness.
      case BANG -> LLVMBuildNot(builder, value, "");
      default -> LLVMBuildNot(builder, value, "");
    };
  }

  @Override
  public LLVMValueRef visitBinaryExpression(Expression.Binary expr) {
    var l_IType = getIType(expr.left);
    var r_IType = getIType(expr.right);

    var lValue = visitExpression(expr.left);
    var rValue = visitExpression(expr.right);

    IType lCastType = R.get(expr.left, "cast");
//    System.out.println("E: "+expr.op.literal()+", LC: "+lCastType+", L:"+l_IType);
    if(lCastType != null && lCastType != l_IType) {
      lValue = TypeUtil.isIntegerType(l_IType)
        ? LLVMBuildSIToFP(builder, lValue, llvmType(lCastType), "")
        : LLVMBuildFPToSI(builder, lValue, llvmType(lCastType), "");
    }

    IType rCastType = R.get(expr.right, "cast");
//    System.out.println("E: "+expr.op.literal()+", RC: "+rCastType+", R:"+r_IType);
    if(rCastType != null && rCastType != r_IType) {
      rValue = TypeUtil.isIntegerType(l_IType)
        ? LLVMBuildSIToFP(builder, rValue, llvmType(rCastType), "")
        : LLVMBuildFPToSI(builder, rValue, llvmType(rCastType), "");
    }

    var maxType = TypeUtil.max(l_IType, r_IType);

    /*if(l_IType.lessOrGreater(maxType)) {
      lValue = TypeUtil.isFloatType(maxType)
        ? LLVMBuildFPCast(builder, lValue, llvmType(maxType), "")
        : LLVMBuildIntCast(builder, lValue, llvmType(maxType), "");
    }

    if(r_IType.lessOrGreater(maxType)) {
      rValue = TypeUtil.isFloatType(maxType)
        ? LLVMBuildFPCast(builder, rValue, llvmType(maxType), "")
        : LLVMBuildIntCast(builder, rValue, llvmType(maxType), "");
    }*/

    return switch (expr.op.type()) {
      case PLUS -> {
        if(TypeUtil.isFloatType(maxType)) {
          yield LLVMBuildFAdd(builder, lValue, rValue, "");
        }

        yield LLVMBuildAdd(builder, lValue, rValue, "");
      }
      case MINUS -> {
        if(TypeUtil.isFloatType(maxType)) {
          yield LLVMBuildFSub(builder, lValue, rValue, "");
        }

        yield LLVMBuildSub(builder, lValue, rValue, "");
      }
      case MULTIPLY -> {
        if(TypeUtil.isFloatType(maxType)) {
          yield LLVMBuildFMul(builder, lValue, rValue, "");
        }

        yield LLVMBuildMul(builder, lValue, rValue, "");
      }
      case DIVIDE -> LLVMBuildFDiv(builder, lValue, rValue, "");
      case FLOOR -> {
        if(TypeUtil.isFloatType(maxType)) {
          yield LLVMBuildUDiv(builder, lValue, rValue, "");
        }

        yield LLVMBuildUDiv(builder, lValue, rValue, "");
      }
      default -> throw new RuntimeException("");
    };
  }

  @Override
  public LLVMValueRef visitGroupingExpression(Expression.Grouping expr) {
    return visitExpression(expr.expression);
  }

  @Override
  public LLVMValueRef visitSimpleStatement(Statement.Simple statement) {
    return visitExpression(statement.expression);
  }

  @Override
  public LLVMValueRef visitBlockStatement(Statement.Block stmt) {
    for(var statement : stmt.body) {
      visitStatement(statement);
    }

    return super.visitBlockStatement(stmt);
  }

  @Override
  public LLVMValueRef visitReturnStatement(Statement.Return stmt) {
    if(stmt.value != null) {
      var value = visitExpression(stmt.value);
      IType type = R.get(stmt, "type");

      IType lCastType = R.get(stmt, "cast");
//      System.out.println("CAST = "+lCastType);
      if(lCastType != null && lCastType != type) {
        value = LLVMBuildBitCast(builder, value, llvmType(lCastType), "");
      }

      return LLVMBuildRet(builder, value);
    } else {
      return LLVMBuildRetVoid(builder);
    }
  }

  @Override
  public LLVMValueRef visitFunctionStatement(Statement.Function stmt) {
    var previousBuilder = builder;
    var previousBlock = currentBlock;

    var returnType = getTypeValue(stmt.returnType);

    var paramTypes = new PointerPointer();
    for (int i = 0; i < stmt.parameters.size(); i++) {
      var param = stmt.parameters.get(i);
      paramTypes.put(i, getType(param));
    }

    var functionType = LLVMFunctionType(returnType, paramTypes, stmt.parameters.size(), stmt.isVariadic ? 1 : 0);
    var function = LLVMAddFunction(module, stmt.name.literal(), functionType);

    currentBlock = LLVMAppendBasicBlockInContext(context, function, "entry");
    builder = LLVMCreateBuilderInContext(context);
    LLVMPositionBuilderAtEnd(builder, currentBlock);

    visitStatement(stmt.body);

    LLVMVerifyFunction(function, LLVMPrintMessageAction);

    currentBlock = previousBlock;
    builder = previousBuilder;

    return function;
  }

  private <T> LLVMTypeRef getType(T item) {
    return llvmType(context, R.get(item, "type"));
  }

  private <T> LLVMTypeRef getTypeValue(T item) {
    return llvmType(context, R.get(item, "value"));
  }

  private <T> IType getIType(T item) {
    return R.get(item, "type");
  }

  private <T> IType getITypeValue(T item) {
    return R.get(item, "value");
  }
}
