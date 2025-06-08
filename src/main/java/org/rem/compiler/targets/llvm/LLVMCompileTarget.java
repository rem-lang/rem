package org.rem.compiler.targets.llvm;

import norswap.uranium.Reactor;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.*;
import org.rem.compiler.BaseCompileTarget;
import org.rem.generators.LLVMGenerator;
import org.rem.interfaces.IGenerator;
import org.rem.interfaces.IType;
import org.rem.parser.TokenType;
import org.rem.parser.ast.Expression;
import org.rem.parser.ast.Statement;
import org.rem.scope.Environment;
import org.rem.types.*;
import org.rem.utils.TypeUtil;

import static org.bytedeco.llvm.global.LLVM.*;

@SuppressWarnings("resource")
public class LLVMCompileTarget extends BaseCompileTarget<LLVMValueRef> {
  private final LLVMContextRef context;
  private final LLVMModuleRef module;
  private LLVMBuilderRef builder = LLVMCreateBuilder();
  private LLVMBasicBlockRef currentBlock;
  private LLVMValueRef currentFunction;
  private Environment<String, LLVMValueRef> env = new Environment<>(null);
  private Environment<LLVMValueRef, LLVMTypeRef> functionTypeRegistry = new Environment<>(null);

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

        try (PointerPointer<LLVMTypeRef> paramsPointer = new PointerPointer<>()) {
          for (IType param : parameters) {
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
    if (statement == null) return null;
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
    var iType = getIType(expr);

    var value = LLVMBuildBitCast(builder, visitExpression(expr.right), getType(expr), "");

    return switch (expr.op.type()) {
      case MINUS -> {
        if (TypeUtil.isFloatType(iType)) {
          yield LLVMBuildFNeg(builder, value, "fneg");
        }
        yield LLVMBuildNeg(builder, value, "neg");
      }
      // TODO: Fix for correctness.
      case BANG -> LLVMBuildNot(builder, value, "not");
      default -> LLVMBuildNot(builder, value, "bnot");
    };
  }

  @Override
  public LLVMValueRef visitBinaryExpression(Expression.Binary expr) {
    var l_IType = getIType(expr.left);
    var r_IType = getIType(expr.right);

    var lValue = visitExpression(expr.left);
    var rValue = visitExpression(expr.right);

    IType lCastType = TypeUtil.max(r_IType, R.get(expr.left, "cast"));
    IType rCastType = TypeUtil.max(R.get(expr.right, "cast"), lCastType);

    // TODO: Handle string concatenation and multiplication
    // TODO: Handle array multiplication

    var maxType = TypeUtil.max(lCastType, rCastType);

    if (lCastType != null && maxType != l_IType) {
      lValue = castNumberToType(lValue, l_IType, maxType);
    }

    if (rCastType != null && maxType != r_IType) {
      rValue = castNumberToType(rValue, r_IType, maxType);
    }

    return switch (expr.op.type()) {
      case PLUS -> {
        if (TypeUtil.isFloatType(maxType)) {
          yield LLVMBuildFAdd(builder, lValue, rValue, "");
        }

        yield LLVMBuildAdd(builder, lValue, rValue, "");
      }
      case MINUS -> {
        if (TypeUtil.isFloatType(maxType)) {
          yield LLVMBuildFSub(builder, lValue, rValue, "");
        }

        yield LLVMBuildSub(builder, lValue, rValue, "");
      }
      case MULTIPLY -> {
        if (TypeUtil.isFloatType(maxType)) {
          yield LLVMBuildFMul(builder, lValue, rValue, "");
        }

        yield LLVMBuildMul(builder, lValue, rValue, "");
      }
      case DIVIDE -> LLVMBuildFDiv(builder, lValue, rValue, "");
      case FLOOR -> {
        if (TypeUtil.isFloatType(maxType)) {
          yield LLVMBuildUDiv(builder, lValue, rValue, "");
        }

        yield LLVMBuildUDiv(builder, lValue, rValue, "");
      }
      default -> throw new RuntimeException("");
    };
  }

  private LLVMValueRef visitLogicalAndOr(Expression.Logical expr) {
    var l_IType = getIType(expr.left);
    var r_IType = getIType(expr.right);
    var op = expr.op.type();

    assert TypeUtil.isBoolean(l_IType) && TypeUtil.isBoolean(r_IType);
    var lValue = visitExpression(expr.left);

    var lhsEndBlock = currentBlockIsInUse();
    var resultOnSkip = LLVMConstInt(llvmType(BoolType.INSTANCE), op == TokenType.AND ? 0 : 1, 0);

    if (lhsEndBlock == null) {
      return resultOnSkip;
    }

    var phiBlock = LLVMCreateBasicBlockInContext(context, op == TokenType.AND ? "and.phi" : "or.phi");
    var rhsBlock = LLVMCreateBasicBlockInContext(context, op == TokenType.AND ? "and.rhs" : "or.rhs");

    if (op == TokenType.AND) {
      LLVMBuildCondBr(builder, lValue, rhsBlock, phiBlock);
    } else {
      LLVMBuildCondBr(builder, lValue, phiBlock, rhsBlock);
    }

    emitBlock(rhsBlock);
    var rValue = visitExpression(expr.right);
    var rhsEndBlock = currentBlockIsInUse();

    if (rhsEndBlock != null) {
      LLVMBuildBr(builder, phiBlock);
    }

    emitBlock(phiBlock);

    if (rhsEndBlock == null) {
      return resultOnSkip;
    }

    return buildPhi(resultOnSkip, lhsEndBlock, rValue, rhsEndBlock);
  }

  @Override
  public LLVMValueRef visitLogicalExpression(Expression.Logical expr) {
    if (expr.op.type() == TokenType.OR || expr.op.type() == TokenType.AND) {
      return visitLogicalAndOr(expr);
    }

    var l_IType = getIType(expr.left);
    var r_IType = getIType(expr.right);

    var lValue = visitExpression(expr.left);
    var rValue = visitExpression(expr.right);

    // TODO: Handle other types equality and non-equality.

    var maxType = TypeUtil.max(l_IType, r_IType);
    if (maxType != l_IType) {
      lValue = castNumberToType(lValue, l_IType, maxType);
    }

    if (maxType != r_IType) {
      rValue = castNumberToType(rValue, r_IType, maxType);
    }

    LLVMValueRef ref;
    if (TypeUtil.isFloatType(maxType)) {
      ref = switch (expr.op.type()) {
        case LESS -> LLVMBuildFCmp(builder, LLVMRealOLT, lValue, rValue, "");
        case LESS_EQ -> LLVMBuildFCmp(builder, LLVMRealOLE, lValue, rValue, "");
        case GREATER -> LLVMBuildFCmp(builder, LLVMRealOGT, lValue, rValue, "");
        case GREATER_EQ -> LLVMBuildFCmp(builder, LLVMRealOGE, lValue, rValue, "");
        case BANG_EQ -> LLVMBuildFCmp(builder, LLVMRealONE, lValue, rValue, "");
        default -> LLVMBuildFCmp(builder, LLVMRealOEQ, lValue, rValue, "");
      };
    } else {
      ref = switch (expr.op.type()) {
        case LESS -> LLVMBuildICmp(builder, LLVMIntSLT, lValue, rValue, "");
        case LESS_EQ -> LLVMBuildICmp(builder, LLVMIntSLE, lValue, rValue, "");
        case GREATER -> LLVMBuildICmp(builder, LLVMIntSGT, lValue, rValue, "");
        case GREATER_EQ -> LLVMBuildICmp(builder, LLVMIntSGE, lValue, rValue, "");
        case BANG_EQ -> LLVMBuildICmp(builder, LLVMIntNE, lValue, rValue, "");
        default -> LLVMBuildICmp(builder, LLVMIntEQ, lValue, rValue, "");
      };
    }

    return LLVMBuildIntCast(builder, ref, llvmType(BoolType.INSTANCE), "");
  }

  @Override
  public LLVMValueRef visitGroupingExpression(Expression.Grouping expr) {
    return visitExpression(expr.expression);
  }

  @Override
  public LLVMValueRef visitIdentifierExpression(Expression.Identifier expr) {
    var value = env.get(expr.token.literal());
    if (value == null) {
      throw new RuntimeException("We should never get here.");
    }

    if (LLVMIsAAllocaInst(value) != null) {
      return LLVMBuildLoad2(builder, LLVMGetAllocatedType(value), value, "");
    }

    return value;
  }

  @Override
  public LLVMValueRef visitAssignExpression(Expression.Assign expr) {

    LLVMValueRef value;
    if (expr.value == null || expr.value instanceof Expression.Nil) {
      value = LLVMGetUndef(getType(expr));
    } else {
      value = visitExpression(expr.value);
    }

    var expression = visitExpression(expr.expression);
    return LLVMBuildStore(builder, value, LLVMGetOperand(expression, 0));
  }

  @Override
  public LLVMValueRef visitUpdateExpression(Expression.Update expr) {
    var iType = getIType(expr.expression);
    var vType = getIType(expr.value);

    var iValue = visitExpression(expr.expression);
    var vValue = visitExpression(expr.value);

    // TODO: Handle string concatenation and multiplication
    // TODO: Handle array multiplication

    if (iType != vType) {
      vValue = castNumberToType(vValue, vType, iType);
    }

    // TODO: Handle bitwise here
    vValue = switch (expr.op.type()) {
      case PLUS_EQ -> {
        if (TypeUtil.isFloatType(iType)) {
          yield LLVMBuildFAdd(builder, iValue, vValue, "");
        }

        yield LLVMBuildAdd(builder, iValue, vValue, "");
      }
      case MINUS_EQ -> {
        if (TypeUtil.isFloatType(iType)) {
          yield LLVMBuildFSub(builder, iValue, vValue, "");
        }

        yield LLVMBuildSub(builder, iValue, vValue, "");
      }
      case MULTIPLY_EQ -> {
        if (TypeUtil.isFloatType(iType)) {
          yield LLVMBuildFMul(builder, iValue, vValue, "");
        }

        yield LLVMBuildMul(builder, iValue, vValue, "");
      }
      case DIVIDE_EQ -> LLVMBuildFDiv(builder, iValue, vValue, "");
      case FLOOR_EQ -> {
        if (TypeUtil.isFloatType(iType)) {
          yield LLVMBuildUDiv(builder, iValue, vValue, "");
        }

        yield LLVMBuildUDiv(builder, iValue, vValue, "");
      }
      default -> throw new RuntimeException("");
    };

    LLVMBuildStore(builder, vValue, LLVMGetOperand(iValue, 0));
    return vValue;
  }

  @Override
  public LLVMValueRef visitIncrementExpression(Expression.Increment expr) {
    var value = visitExpression(expr.expression);

    var iType = getIType(expr);
    var one = LLVMConstInt(llvmType(getIType(expr)), 1, 0);

    LLVMValueRef addition;
    if (TypeUtil.isIntegerType(iType)) {
      addition = LLVMBuildAdd(builder, value, one, "");
    } else {
      addition = LLVMBuildFAdd(builder, value, one, "");
    }

    return LLVMBuildStore(builder, addition, LLVMGetOperand(value, 0));
  }

  @Override
  public LLVMValueRef visitCallExpression(Expression.Call expr) {
    var callee = visitExpression(expr.callee);
    if (callee == null)
      throw new RuntimeException("Something went wrong!");

    DefType type = R.get(expr.callee, "type");
    if (type.getParameterCount() != expr.args.size()) {
      throw new RuntimeException("Should not end up here!");
    }

//    IType returnType = R.get(expr, "type");

    var values = new PointerPointer<LLVMValueRef>(expr.args.size());
    for (int i = 0; i < expr.args.size(); i++) {
      var arg = expr.args.get(i);
      var argument = visitExpression(arg);

      IType rType = type.getParameterTypes()[i];
      IType argType = getIType(arg);

//      System.out.println("ARG: "+arg+"RT: "+rType+", AC: "+argType);

      if (rType != null && argType != rType) {
        if (TypeUtil.isNumericType(argType)) {
          argument = castNumberToType(argument, getIType(arg), rType);
        } else {
          // TODO: Handle non-numeric parameters
          argument = LLVMBuildPointerCast(builder, argument, llvmType(rType), "");
        }
      }

      values.put(i, argument);
    }

    return LLVMBuildCall2(builder, functionTypeRegistry.get(callee), callee, values, expr.args.size(), "");

//    return super.visitCallExpression(expr);
  }

  @Override
  public LLVMValueRef visitSimpleStatement(Statement.Simple statement) {
    return visitExpression(statement.expression);
  }

  @Override
  public LLVMValueRef visitBlockStatement(Statement.Block stmt) {
    for (var statement : stmt.body) {
      visitStatement(statement);
    }

    return super.visitBlockStatement(stmt);
  }

  @Override
  public LLVMValueRef visitVarStatement(Statement.Var stmt) {
    var type = getType(stmt);

    String name = stmt.typedName.name.token.literal();

    var allocation = LLVMBuildAlloca(builder, type, name);
    LLVMValueRef value;

    if (stmt.value != null && !(stmt.value instanceof Expression.Nil)) {
      value = visitExpression(stmt.value);
    } else {
      value = LLVMGetUndef(type);
    }

    env.put(name, allocation);
    return LLVMBuildStore(builder, value, allocation);
  }

  @Override
  public LLVMValueRef visitVarListStatement(Statement.VarList stmt) {
    for (var statement : stmt.declarations) {
      visitStatement(statement);
    }

    return null;
  }

  @Override
  public LLVMValueRef visitExternStatement(Statement.Extern stmt) {
    var returnType = getTypeValue(stmt.returnType);

    var paramTypes = new PointerPointer<LLVMTypeRef>(stmt.parameters.size());
    for (int i = 0; i < stmt.parameters.size(); i++) {
      var param = stmt.parameters.get(i);
      paramTypes.put(i, getType(param));
    }

    var functionType = LLVMFunctionType(returnType, paramTypes, stmt.parameters.size(), stmt.isVariadic ? 1 : 0);
    var function = LLVMAddFunction(module, stmt.name.literal(), functionType);

    if (stmt.name.literal().startsWith("_")) {
      LLVMSetLinkage(function, LLVMInternalLinkage);
    } else {
      LLVMSetLinkage(function, LLVMExternalLinkage);
    }

    // define the function in the parent environment
    env.put(stmt.name.literal(), function);
    functionTypeRegistry.put(function, functionType);

    for (int i = 0; i < stmt.parameters.size(); i++) {
      String name = stmt.parameters.get(i).name.token.literal();

      var param = LLVMGetParam(function, i);
      LLVMSetValueName2(param, name, name.length());
      env.put(name, param);
    }

    return function;
  }

  @Override
  public LLVMValueRef visitIfStatement(Statement.If stmt) {

    IType cType = R.get(stmt.condition, "type");
    assert TypeUtil.isBoolean(cType);

    LLVMValueRef condition = LLVMBuildICmp(
      builder, LLVMIntEQ, visitExpression(stmt.condition),
      LLVMConstInt(llvmType(cType), 0, 0),
      "if.cond"
    );

    if (condition == null) return null;

    var exitBlock = LLVMCreateBasicBlockInContext(context, "if.exit");
    var thenBlock = exitBlock;
    var elseBlock = exitBlock;

    if (stmt.thenBranch != null) {
      thenBlock = LLVMCreateBasicBlockInContext(context, "if.then");
    }

    if (stmt.elseBranch != null) {
      elseBlock = LLVMCreateBasicBlockInContext(context, "if.else");
    }

    boolean exitInUse = true;

    if (LLVMIsAConstantInt(condition) != null && thenBlock != elseBlock) {
      if (LLVMConstIntGetZExtValue(condition) != 0) {
        LLVMBuildBr(builder, thenBlock);
        elseBlock = exitBlock;
      } else {
        LLVMBuildBr(builder, elseBlock);
        thenBlock = exitBlock;
      }
    } else {
      if (thenBlock != elseBlock) {
        LLVMBuildCondBr(builder, condition, thenBlock, elseBlock);
      } else {
        exitInUse = LLVMGetFirstUse(LLVMBasicBlockAsValue(exitBlock)) != null;
        if (exitInUse) {
          LLVMBuildBr(builder, exitBlock);
        }
      }
    }

    if (thenBlock != exitBlock) {
      var previousBlock = currentBlock;
      emitBlock(thenBlock);

      visitStatement(stmt.thenBranch);

      if (LLVMGetBasicBlockTerminator(currentBlock) == null) {
        LLVMBuildBr(builder, exitBlock);
      }

      currentBlock = previousBlock;
    }

    if (elseBlock != exitBlock) {
      var previousBlock = currentBlock;
      emitBlock(elseBlock);

      visitStatement(stmt.elseBranch);

      if (LLVMGetBasicBlockTerminator(currentBlock) == null) {
        LLVMBuildBr(builder, exitBlock);
      }

      currentBlock = previousBlock;
    }

    if (exitInUse) {
      emitBlock(exitBlock);
    }
    return super.visitIfStatement(stmt);
  }

  @Override
  public LLVMValueRef visitWhileStatement(Statement.While stmt) {
    var bodyBlock = !stmt.body.body.isEmpty() ? LLVMCreateBasicBlockInContext(context, "while.body") : null;
    LLVMBasicBlockRef conditionBlock = null;

    var loop = ExpressionUtil.loopTypeForCondition(stmt.condition, false);

    LLVMBasicBlockRef loopStartBlock = bodyBlock;
    if(loop == LoopType.NORMAL) {
      conditionBlock = LLVMCreateBasicBlockInContext(context, "while.cond");
      loopStartBlock = conditionBlock;
    }

    if(conditionBlock == null && bodyBlock == null) {
      return null;
    }

    var exitBlock = LLVMCreateBasicBlockInContext(context, "while.exit");

    stmt.continueBlock = loop == LoopType.NONE ? exitBlock : loopStartBlock;
    stmt.exitBlock = exitBlock;

    if (loop == LoopType.NORMAL) {
      LLVMBuildBr(builder, conditionBlock);

      // Emit the block
      emitBlock(conditionBlock);
      var condition = visitExpression(stmt.condition);

      // If we have a body, conditionally jump to it.
      LLVMBasicBlockRef conditionSuccess = bodyBlock != null ? bodyBlock : conditionBlock;
      // Otherwise, jump to inc or cond depending on what's available.
      LLVMBuildCondBr(builder, condition, conditionSuccess, exitBlock);
    }

    // The optional cond is emitted, so emit the body
    if (bodyBlock != null) {
      // If we have LOOP NONE, then we don't need a new block here
      // since we will just exit. That leaves the infinite loop.
      switch (loop) {
        case NORMAL:
          // If we have LOOP_NORMAL, we already emitted a br to the body.
          // So emit the block
          emitBlock(bodyBlock);
          break;
        case INFINITE:
          // In this case, we have no cond, so we need to emit the br and
          // then the block
          LLVMBuildBr(builder, bodyBlock);
          emitBlock(bodyBlock);
        case NONE:
          // If there is no loop, then we will just fall through and the
          // block is needed.
          bodyBlock = null;
          break;
      }

      // Now emit the body
      visitStatement(stmt.body);
    }

    // Loop back.
    if (loop != LoopType.NONE) {
      LLVMBuildBr(builder, loopStartBlock);
    } else {
      // If the exit block is unused, skip it.
      if (blockIsUnused(exitBlock)) {
        return null;
      }

      LLVMBuildBr(builder, exitBlock);
    }

    // And insert exit block
    emitBlock(exitBlock);
    return super.visitWhileStatement(stmt);
  }

  @Override
  public LLVMValueRef visitDoWhileStatement(Statement.DoWhile stmt) {
    var bodyBlock = !stmt.body.body.isEmpty() ? LLVMCreateBasicBlockInContext(context, "do.body") : null;
    LLVMBasicBlockRef conditionBlock = null;

    var loop = ExpressionUtil.loopTypeForCondition(stmt.condition, true);

    LLVMBasicBlockRef loopStartBlock = bodyBlock;
    if(loop == LoopType.NORMAL) {
      conditionBlock = LLVMCreateBasicBlockInContext(context, "do.cond");
      loopStartBlock = conditionBlock;
    }

    if(conditionBlock == null && bodyBlock == null) {
      return null;
    }

    var exitBlock = LLVMCreateBasicBlockInContext(context, "do.exit");

    stmt.continueBlock = loop == LoopType.NONE ? exitBlock : loopStartBlock;
    stmt.exitBlock = exitBlock;

    if (loop == LoopType.NORMAL) {
      LLVMBuildBr(builder, bodyBlock != null ? bodyBlock : conditionBlock);

      // Emit the block
      emitBlock(conditionBlock);
      var condition = visitExpression(stmt.condition);

      // If we have a body, conditionally jump to it.
      LLVMBasicBlockRef conditionSuccess = bodyBlock != null ? bodyBlock : conditionBlock;
      // Otherwise, jump to inc or cond depending on what's available.
      LLVMBuildCondBr(builder, condition, conditionSuccess, exitBlock);
    }

    // The optional cond is emitted, so emit the body
    if (bodyBlock != null) {
      // If we have LOOP NONE, then we don't need a new block here
      // since we will just exit. That leaves the infinite loop.
      switch (loop) {
        case NORMAL:
          // If we have LOOP_NORMAL, we already emitted a br to the body.
          // So emit the block
          emitBlock(bodyBlock);
          break;
        case INFINITE:
          // In this case, we have no cond, so we need to emit the br and
          // then the block
          LLVMBuildBr(builder, bodyBlock);
          emitBlock(bodyBlock);
        case NONE:
          // If there is no loop, then we will just fall through and the
          // block is needed.
          bodyBlock = null;
          break;
      }

      // Now emit the body
      visitStatement(stmt.body);
    }

    // Loop back.
    if (loop != LoopType.NONE) {
      LLVMBuildBr(builder, loopStartBlock);
    } else {
      // If the exit block is unused, skip it.
      if (blockIsUnused(exitBlock)) {
        return null;
      }

      LLVMBuildBr(builder, exitBlock);
    }

    // And insert exit block
    emitBlock(exitBlock);
    return super.visitDoWhileStatement(stmt);
  }

  @Override
  public LLVMValueRef visitForStatement(Statement.For stmt) {
    visitStatement(stmt.declaration);

    var incrementBlock = stmt.interation == null
      ? null : LLVMCreateBasicBlockInContext(context, "for.inc");

    var bodyBlock = stmt.body.body.isEmpty()
      ? null : LLVMCreateBasicBlockInContext(context, "for.body");

    LLVMBasicBlockRef conditionBlock = null;

    var loop = ExpressionUtil.loopTypeForCondition(stmt.condition, false);

    // This is the starting block to loop back to, and may either be cond, body or inc
    var loopStartBlock = bodyBlock != null ? bodyBlock : incrementBlock;

    // We only emit a cond block if we have a normal loop.
    if (loop == LoopType.NORMAL) {
      loopStartBlock = conditionBlock = LLVMCreateBasicBlockInContext(context, "for.cond");
    }

    // In the case that *none* of the blocks exist.
    if (incrementBlock == null && bodyBlock == null && conditionBlock == null) {
      return null;
    }

    var exitBlock = LLVMCreateBasicBlockInContext(context, "for.exit");

    // Break is straightforward, it always jumps out.
    // For `continue`:
    // 1. If there is inc, jump to condition
    // 2. If this is not looping, jump to the exit, otherwise go to cond/body depending on what the start is.
    LLVMBasicBlockRef continueBlock = incrementBlock;
    if (continueBlock == null) {
      continueBlock = loop == LoopType.NONE ? exitBlock : loopStartBlock;
    }

    stmt.continueBlock = continueBlock;
    stmt.exitBlock = exitBlock;

    // We have a normal loop, so we emit a cond.
    if (loop == LoopType.NORMAL) {
      LLVMBuildBr(builder, conditionBlock);

      // Emit the block
      emitBlock(conditionBlock);
      var condition = visitExpression(stmt.condition);

      // If we have a body, conditionally jump to it.
      LLVMBasicBlockRef conditionSuccess = bodyBlock != null ? bodyBlock : incrementBlock;

      // If there is a while (...) { } we need to set the success to this block
      if (conditionSuccess == null) conditionSuccess = conditionBlock;

      // Otherwise, jump to inc or cond depending on what's available.
      LLVMBuildCondBr(builder, condition, conditionSuccess, exitBlock);
    }

    // The optional cond is emitted, so emit the body
    if (bodyBlock != null) {
      // If we have LOOP_NONE, then we don't need a new block here
      // since we will just exit. That leaves the infinite loop.
      switch (loop) {
        case NORMAL:
          // If we have LOOP_NORMAL, we already emitted a br to the body.
          // So emit the block
          emitBlock(bodyBlock);
          break;
        case INFINITE:
          // In this case, we have no cond, so we need to emit the br and
          // then the block
          LLVMBuildBr(builder, bodyBlock);
          emitBlock(bodyBlock);
        case NONE:
          // If there is no loop, then we will just fall through and the
          // block is needed.
          bodyBlock = null;
          break;
      }

      // Now emit the body
      visitStatement(stmt.body);

      // Did we have a jump to inc yet?
//      if (incrementBlock != null && !blockIsUnused(incrementBlock)) {
      // If so, we emit the jump to the inc block.
      LLVMBuildBr(builder, incrementBlock);
//      } else {
//        incrementBlock = null;
//      }
    }

    if (stmt.interation != null) {
      // We might have neither body nor cond
      // In that case we do a jump from the init.
      if (loopStartBlock == incrementBlock) {
        LLVMBuildBr(builder, incrementBlock);
      }
      if (incrementBlock != null) {
        // Emit the block if it exists.
        // The inc block might also be the end of the body block.
        emitBlock(incrementBlock);
      }

      if (currentBlockIsInUse() != null) {
        visitStatement(stmt.interation);
      }
    }

    // Loop back.
    if (loop != LoopType.NONE) {
      LLVMBuildBr(builder, loopStartBlock);
    } else {
      // If the exit block is unused, skip it.
      if (blockIsUnused(exitBlock)) {
        return null;
      }

      LLVMBuildBr(builder, exitBlock);
    }

    emitBlock(exitBlock);
    return null;
  }

  @Override
  public LLVMValueRef visitReturnStatement(Statement.Return stmt) {
    if (stmt.value != null) {
      var value = visitExpression(stmt.value);
      IType type = R.get(stmt.value, "type");

      IType lCastType = R.get(stmt, "cast");
//      System.out.println("CAST = "+lCastType+", T: " +type);
      if (lCastType != null && lCastType != type) {
        value = castNumberToType(value, type, lCastType);
        type = lCastType;
      }

      if (LLVMIsAStoreInst(value) != null) {
        return LLVMBuildRet(
          builder,
          LLVMBuildLoad2(builder, llvmType(type), LLVMGetOperand(value, 1), "")
        );
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

    var paramTypes = new PointerPointer<LLVMTypeRef>(stmt.parameters.size());
    for (int i = 0; i < stmt.parameters.size(); i++) {
      var param = stmt.parameters.get(i);
      paramTypes.put(i, getType(param));
    }

    var functionType = LLVMFunctionType(returnType, paramTypes, stmt.parameters.size(), stmt.isVariadic ? 1 : 0);
    var function = LLVMAddFunction(module, stmt.name.literal(), functionType);

    env = new Environment<>(env);
    functionTypeRegistry = new Environment<>(functionTypeRegistry);
    currentFunction = function;

    if (stmt.name.literal().startsWith("_")) {
      LLVMSetLinkage(function, LLVMInternalLinkage);
    } else {
      LLVMSetLinkage(function, LLVMExternalLinkage);
    }

    // define the function in the parent environment
    env.getParent().put(stmt.name.literal(), function);
    functionTypeRegistry.getParent().put(function, functionType);

    currentBlock = LLVMAppendBasicBlockInContext(context, function, "entry");
    builder = LLVMCreateBuilderInContext(context);
    LLVMPositionBuilderAtEnd(builder, currentBlock);

    for (int i = 0; i < stmt.parameters.size(); i++) {
      String name = stmt.parameters.get(i).name.token.literal();

      var param = LLVMGetParam(function, i);
      LLVMSetValueName2(param, name, name.length());

      LLVMValueRef paramAllocation = LLVMBuildAlloca(builder, LLVMTypeOf(param), name + ".addr");
      LLVMBuildStore(builder, param, paramAllocation);

      env.put(name, paramAllocation);
    }

    Boolean returns = R.get(stmt.body, "returns");

    visitStatement(stmt.body);
    if (!returns) {
      LLVMBuildRetVoid(builder);
    }

    currentBlock = previousBlock;
    builder = previousBuilder;
    env = env.getParent();
    currentFunction = null;
    functionTypeRegistry = functionTypeRegistry.getParent();

    if (LLVMVerifyFunction(function, LLVMPrintMessageAction) != 0) {
      LLVMInstructionEraseFromParent(function);
      return null;
    }

    return function;
  }

  private LLVMValueRef castNumberToType(LLVMValueRef value, IType currentType, IType targetType) {
    if (currentType == targetType) {
      return value;
    }

    if (TypeUtil.isFloatType(currentType) && TypeUtil.isFloatType(targetType)) {
      if (currentType.less(targetType)) {
        return LLVMBuildFPExt(builder, value, llvmType(targetType), "");
      } else {
        return LLVMBuildFPTrunc(builder, value, llvmType(targetType), "");
      }
    } else if (TypeUtil.isIntegerType(currentType) && TypeUtil.isIntegerType(targetType)) {
      if (currentType.less(targetType)) {
        return LLVMBuildSExtOrBitCast(builder, value, llvmType(targetType), "");
      } else {
        return LLVMBuildTrunc(builder, value, llvmType(targetType), "");
      }
    }

    return TypeUtil.isIntegerType(currentType)
      ? LLVMBuildSIToFP(builder, value, llvmType(targetType), "")
      : LLVMBuildFPToSI(builder, value, llvmType(targetType), "");
  }

  private void emitBlock(LLVMBasicBlockRef block) {
    LLVMAppendExistingBasicBlock(currentFunction, block);
    LLVMPositionBuilderAtEnd(builder, block);
    currentBlock = block;
  }

  private boolean blockIsUnused(LLVMBasicBlockRef block) {
    return LLVMGetFirstInstruction(block) == null
      && LLVMGetFirstUse(LLVMBasicBlockAsValue(block)) == null;
  }

  private LLVMBasicBlockRef currentBlockIsInUse() {
    if (currentBlock != null && blockIsUnused(currentBlock)) {
      LLVMDeleteBasicBlock(currentBlock);
      return currentBlock = null;
    }

    return currentBlock;
  }

  private LLVMValueRef buildPhi(LLVMValueRef val1, LLVMBasicBlockRef block1, LLVMValueRef val2, LLVMBasicBlockRef block2) {
    var retType = LLVMTypeOf(val1);
    var otherType = LLVMTypeOf(val2);

    if (retType != otherType) {
      if (retType == LLVMInt1TypeInContext(context)) {
        val2 = LLVMBuildTrunc(builder, val2, retType, "");
      } else {
        val2 = LLVMBuildZExt(builder, val2, retType, "");
      }
    }

    var phi = LLVMBuildPhi(builder, LLVMTypeOf(val1), "");
    var valuePointer = new PointerPointer<>(2)
      .put(0, val1)
      .put(1, val2);
    var blockPointer = new PointerPointer<>(2)
      .put(0, block1)
      .put(1, block2);
    LLVMAddIncoming(phi, valuePointer, blockPointer, 2);

    return phi;
  }

  private boolean deleteCurrentIfUnused(LLVMBasicBlockRef block) {
    if (block == null || !blockIsUnused(block)) return false;
    LLVMBasicBlockRef prevBlock = LLVMGetPreviousBasicBlock(block);
    LLVMDeleteBasicBlock(block);
    LLVMPositionBuilderAtEnd(builder, prevBlock);
    return true;
  }

  private boolean deleteCurrentBlockIfUnused() {
    if (currentBlock == null || !blockIsUnused(currentBlock)) return false;

    LLVMBasicBlockRef prevBlock = LLVMGetPreviousBasicBlock(currentBlock);
    LLVMDeleteBasicBlock(currentBlock);
    currentBlock = prevBlock;

    LLVMPositionBuilderAtEnd(builder, prevBlock);
    return true;
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
