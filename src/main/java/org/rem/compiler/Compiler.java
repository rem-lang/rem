package org.rem.compiler;

import norswap.uranium.Reactor;
import norswap.uranium.SemanticError;
import org.rem.SemanticAnalyzer;
import org.rem.exceptions.AnalysisException;
import org.rem.parser.Lexer;
import org.rem.parser.Parser;
import org.rem.parser.Source;
import org.rem.parser.ast.Statement;
import org.rem.registries.CompilerRegistry;
import org.rem.registries.GeneratorRegistry;
import org.rem.utils.SemanticErrorUtil;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class Compiler {

  private final CompileRequest request;
  private final boolean showWarnings;

  public Compiler(CompileRequest request, boolean showWarnings) {
    this.request = request;
    this.showWarnings = showWarnings;
  }

  public CompileResult<?> compile() throws IOException {
    Source source = new Source(request.sourceFile);
    Lexer lexer = new Lexer(source);
    Parser parser = new Parser(lexer);
    List<Statement> parseResult = parser.parse();

    Reactor reactor = new Reactor();
    SemanticAnalyzer analyzer = new SemanticAnalyzer(reactor, showWarnings);
    analyzer.analyze(parseResult);

    reactor.run();
    Set<SemanticError> errors = reactor.errors();

    if (!errors.isEmpty()) {
      throw new AnalysisException(SemanticErrorUtil.trace(errors, source));
    }

    return CompilerRegistry
      .getCompiler(request.compileTarget)
      .compile(parseResult);
  }

  public int generate(CompileResult<?> result) {
    return GeneratorRegistry
      .get(request.compileTarget)
      .generate(result);
  }
}
