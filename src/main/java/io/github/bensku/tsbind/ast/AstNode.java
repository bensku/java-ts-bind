package io.github.bensku.tsbind.ast;

import java.util.function.Consumer;

public interface AstNode {

	/**
	 * Walks all AST nodes under this node, including this.
	 * @param visitor Visitor to be called for node found..
	 */
	void walk(Consumer<AstNode> visitor);
}
