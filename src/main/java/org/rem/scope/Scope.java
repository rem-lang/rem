package org.rem.scope;

import org.rem.parser.ast.AST;
import org.rem.parser.ast.Statement;

import java.util.HashMap;

public class Scope {

  public final AST node;
  public final Scope parent;
  private final HashMap<String, AST> declarations = new HashMap<>();

  public Scope(AST node, Scope parent) {
    this.node = node;
    this.parent = parent;
  }

  public Scope() {
    this(null, null);
  }

  /**
   * Adds a new declaration to this scope.
   */
  public void declare(String identifier, AST node) {
    declarations.put(identifier, node);
  }

  /**
   * Look up the name in the scope and its parents, returning a context comprising the
   * found declaration and the scope in which it occurs, or null if not found.
   */
  public DeclarationContext lookup(String name) {
    AST declaration = declarations.get(name);
    return declaration != null
      ? new DeclarationContext(this, declaration)
      : (parent != null
      ? parent.lookup(name)
      : null);
  }

  /**
   * Look up the given name only in this scope and return the corresponding declaration, or null
   * if not found.
   */
  public AST lookupLocal(String name) {
    return declarations.get(name);
  }

  @Override
  public String toString() {
    return "Scope " + declarations;
  }
}
