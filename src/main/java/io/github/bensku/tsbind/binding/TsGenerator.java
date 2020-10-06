package io.github.bensku.tsbind.binding;

import io.github.bensku.tsbind.ast.AstNode;

public interface TsGenerator<T extends AstNode> {

	void emit(T node, TsEmitter out);
}
