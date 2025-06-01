package org.rem.scope;

import norswap.uranium.Reactor;
import org.rem.enums.DeclarationKind;
import org.rem.parser.ast.BuiltInTypeNode;
import org.rem.types.*;

public class RootScope extends Scope {

  // root scope types
  public final BuiltInTypeNode Bool   = builtinType("bool");
  public final BuiltInTypeNode Int8    = builtinType("i8");
  public final BuiltInTypeNode Int16    = builtinType("i16");
  public final BuiltInTypeNode Int32    = builtinType("i32");
  public final BuiltInTypeNode Int64    = builtinType("i64");
  public final BuiltInTypeNode Int128    = builtinType("i128");
  public final BuiltInTypeNode Float32  = builtinType("f32");
  public final BuiltInTypeNode Float64  = builtinType("f64");
  public final BuiltInTypeNode Float128  = builtinType("f128");
  public final BuiltInTypeNode Void   = builtinType("void");
  public final BuiltInTypeNode Type   = builtinType("type");

  public final BuiltInTypeNode True  = builtinName("true");
  public final BuiltInTypeNode False = builtinName("false");
  public final BuiltInTypeNode Nil  = builtinName("nil");

  public RootScope (Reactor reactor) {
    reactor.set(Bool,   "type",       TypeType.INSTANCE);
    reactor.set(Int8,    "type",       TypeType.INSTANCE);
    reactor.set(Int16,    "type",       TypeType.INSTANCE);
    reactor.set(Int32,    "type",       TypeType.INSTANCE);
    reactor.set(Int64,    "type",       TypeType.INSTANCE);
    reactor.set(Int128,    "type",       TypeType.INSTANCE);
    reactor.set(Float32,  "type",       TypeType.INSTANCE);
    reactor.set(Float64,  "type",       TypeType.INSTANCE);
    reactor.set(Float128,  "type",       TypeType.INSTANCE);
    reactor.set(Void,   "type",       TypeType.INSTANCE);
    reactor.set(Type,   "type",       TypeType.INSTANCE);

    reactor.set(Bool,   "declared",   BoolType.INSTANCE);
    reactor.set(Int8,    "declared",    I8Type.INSTANCE);
    reactor.set(Int16,    "declared",    I16Type.INSTANCE);
    reactor.set(Int32,    "declared",    I32Type.INSTANCE);
    reactor.set(Int64,    "declared",    I64Type.INSTANCE);
    reactor.set(Int128,    "declared",    I128Type.INSTANCE);
    reactor.set(Float32,  "declared",  F32Type.INSTANCE);
    reactor.set(Float64,  "declared",  F64Type.INSTANCE);
    reactor.set(Float128,  "declared",  F128Type.INSTANCE);
    reactor.set(Void,   "declared",   VoidType.INSTANCE);
    reactor.set(Type,   "declared",   TypeType.INSTANCE);

    reactor.set(True,  "type",       BoolType.INSTANCE);
    reactor.set(False, "type",       BoolType.INSTANCE);
    reactor.set(Nil,  "type",       NilType.INSTANCE);
  }

  private BuiltInTypeNode rootDeclare(String name, DeclarationKind kind) {
    BuiltInTypeNode node = new BuiltInTypeNode(name, kind);
    declare(name, node);
    return node;
  }

  private BuiltInTypeNode builtinType(String name) {
    return rootDeclare(name, DeclarationKind.TYPE);
  }

  private BuiltInTypeNode builtinName(String name) {
    return rootDeclare(name, DeclarationKind.CONSTANT);
  }
}
