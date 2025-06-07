package tools.rem;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class GenerateAst {

  public static void main(String[] args) throws IOException {
    if (args.length < 1) {
      System.err.println("Usage: generate_ast <output_dir> [type?]");
      System.exit(1);
    }

    String outputDir = args[0].trim();
    String type = "all";
    if(args.length == 2) {
      type = args[1].trim();
    }

    if (type.equalsIgnoreCase("expr")) {
      defineAst(outputDir, "Expression", EXPR_DEFINITION);
    } else if (type.equalsIgnoreCase("stmt")) {
      defineAst(outputDir, "Statement", STMT_DEFINITION);
    }  else if (type.equalsIgnoreCase("type")) {
      defineAst(outputDir, "Typed", TYPE_DEFINITION);
    } else {
      defineAst(outputDir, "Typed", TYPE_DEFINITION);
      defineAst(outputDir, "Expression", EXPR_DEFINITION);
      defineAst(outputDir, "Statement", STMT_DEFINITION);
    }
  }

  private static final List<String> TYPE_DEFINITION = Arrays.asList(
    "Void             :",
    "Id               : Expression.Identifier name",
    "Array            : Typed type",
    "Map              : Typed keyType, Typed valueType"
  );

  private static final List<String> EXPR_DEFINITION = Arrays.asList(
    "Nil                :",
    "Boolean            : boolean value",
    "Int32              : int value",
    "Int64              : long value",
    "Float32            : Float value",
    "Float64            : Double value",
//    "BigNumber          : Token token",
    "Literal            : Token token",
    "Unary              : Token op, Expression right",
    "Binary             : Expression left, Token op, Expression right",
    "Logical            : Expression left, Token op, Expression right",
    "Range              : Expression lower, Expression upper",
    "Grouping           : Expression expression",
    "Increment          : Expression expression",
    "Decrement          : Expression expression",
    "Identifier         : Token token",
    "TypedName          : Identifier name, Typed type",
    "Condition          : Expression expression, Expression truth, Expression falsy",
    "Call               : Expression callee, List<Expression> args",
    "Get                : Expression expression, Identifier name",
    "Set                : Expression expression, Identifier name, Expression value",
    "Index              : Expression callee, Expression argument",
    "Slice              : Expression callee, Expression lower, Expression upper",
    "Array              : List<Expression> items",
    "Dict               : List<Expression> keys, List<Expression> values",
    "New                : Expression expression",
    "Parent             :",
    "Self               :",
    "Assign             : Expression expression, Expression value",
    "Anonymous          : Statement.Function function"
  );

  private static final List<String> STMT_DEFINITION = Arrays.asList(
    "Echo       : Expression value",
    "Simple     : Expression expression",
    "If         : Expression condition, Statement thenBranch, Statement elseBranch",
    "For        : Statement declaration, Expression condition, Simple interation, Block body  : Object continueBlock, Object exitBlock",
    "While      : Expression condition, Block body",
    "DoWhile    : Block body, Expression condition",
    "Continue   :",
    "Break      :",
    "Raise      : Expression exception",
    "Return     : Expression value",
    "Assert     : Expression expression, Expression message",
    "Using      : Expression expression, List<Expression> caseLabels, List<Statement> caseBodies, Statement defaultCase",
    "Import     : String path, List<Token> elements, boolean all",
    "Catch      : Block body, Block catchBody, Block finallyBody, Expression.Identifier name",
    "Block      : List<Statement> body",
    "Var        : Expression.TypedName typedName, Expression value, boolean isConstant",
    "Extern     : Token name, List<Expression.TypedName> parameters, Typed returnType, boolean isVariadic",
    "Function   : Token name, List<Expression.TypedName> parameters, Typed returnType, Statement.Block body, boolean isVariadic",
    "Method     : Token name, List<Expression.TypedName> parameters, Typed returnType, Statement.Block body, boolean isVariadic, boolean isStatic",
    "Property   : Expression.TypedName name, Expression value, boolean isStatic, boolean isConstant",
    "Class      : Token name, Expression.Identifier superclass, List<Property> properties, List<Method> methods, List<Method> operators",
    "VarList    : List<Statement> declarations"
  );

  private static void defineAst(String outputDir, String baseName, List<String> types) throws IOException {

    String path = outputDir + "/" + baseName + ".java";
    PrintWriter writer = new PrintWriter(path, StandardCharsets.UTF_8);

    writer.println("// DO NOT MODIFY DIRECTLY");
    writer.println("// This file was generated by the tools.rem.GenerateAst");
    writer.println("package org.rem.parser.ast;");
    writer.println();
    writer.println("import org.rem.parser.Token;");
    writer.println("import java.util.List;");
    writer.println();
    writer.println("public abstract class " + baseName + " extends AST {");

    // The base accept method.
    writer.println("  public abstract<T> T accept(Visitor<T> visitor);");
    writer.println("  public abstract void accept(VoidVisitor visitor);");
    writer.println();
    writer.println("  @Override public String astName() {");
    writer.println("    return \"" + baseName.toLowerCase(Locale.ROOT) + "\";");
    writer.println("  }");

    defineVisitor(writer, baseName, types);

    writer.println();
    defineVoidVisitor(writer, baseName, types);

    // The AST classes
    for (String type : types) {
      String[] brokenType = type.split(":");

      String className = brokenType[0].trim();

      String fields = "";
      String setters = "";
      if (brokenType.length > 1) {
        fields = brokenType[1].trim();
        if(brokenType.length > 2) {
          setters = brokenType[2].trim();
        }
      }

      defineType(writer, baseName, className, fields, setters);
    }

    writer.println("}");
    writer.close();
  }

  private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
    writer.println();
    writer.println("  public interface Visitor<T> {");
    for (String type : types) {
      String typeName = type.split(":")[0].trim();
      writer.println("    T visit" + typeName + baseName + "(" + typeName
          + " " + baseName.toLowerCase() + ");");
    }
    writer.println("    T visit" + baseName + "(" + baseName + " " + baseName.toLowerCase() + ");");
    writer.println("  }");
  }

  private static void defineVoidVisitor(PrintWriter writer, String baseName, List<String> types) {
    writer.println();
    writer.println("  public interface VoidVisitor {");
    for (String type : types) {
      String typeName = type.split(":")[0].trim();
      writer.println("    void visit" + typeName + baseName + "(" + typeName
          + " " + baseName.toLowerCase() + ");");
    }
    writer.println("    void visit" + baseName + "(" + baseName + " " + baseName.toLowerCase() + ");");
    writer.println("  }");
  }

  private static void defineType(PrintWriter writer, String baseName, String className, String fieldsList, String setterList) {
    writer.println();
    writer.println("  public static class " + className + " extends " + baseName + " {");

    String[] fields = null;
    if (!fieldsList.isEmpty()) {
      fields = fieldsList.split(", ");

      // Fields.
      for (String field : fields) {
        writer.println("    public final " + field.trim() + ";");
      }
      writer.println();
    }

    if(!setterList.isEmpty()) {
      // Setters
      for (String setter : setterList.split(", ")) {
        writer.println("    public " + setter.trim() + ";");
      }
      writer.println();
    }

    // Constructor.
    writer.println("    public " + className + "(" + fieldsList + ") {");

    if (!fieldsList.isEmpty()) {
      // Store parameters in fields.
      for (String field : fields) {
        String name = field.split(" ")[1].trim();
        writer.println("      this." + name + " = " + name + ";");
      }
    }
    writer.println("    }");

    // Visitor pattern.
    writer.println();
    writer.println("    public <T> T accept(Visitor<T> visitor) {");
    writer.println("      return visitor.visit" + className + baseName + "(this);");
    writer.println("    }");
    writer.println();
    writer.println("    public void accept(VoidVisitor visitor) {");
    writer.println("      visitor.visit" + className + baseName + "(this);");
    writer.println("    }");
    writer.println();
    writer.println("    @Override public String astName() {");
    writer.println("      return \"" + className.toLowerCase(Locale.ROOT) +  " " + baseName.toLowerCase(Locale.ROOT) + "\";");
    writer.println("    }");

    writer.println("  }");

    System.out.println(className + " AST class generated.");
  }
}
