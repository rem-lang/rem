package org.rem.scope;

import org.rem.parser.ast.AST;

/**
 * A pair of a {@link Scope} and a {@link AST} declaring an entry in that scope.
 */
public record DeclarationContext(Scope scope, AST declaration) {
}
