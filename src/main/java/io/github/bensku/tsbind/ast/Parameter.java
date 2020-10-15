package io.github.bensku.tsbind.ast;

import java.util.function.Consumer;

public class Parameter implements AstNode {
	
	/**
	 * Name of the parameter.
	 */
	public final String name;

	/**
	 * Parameter type.
	 */
	public final TypeRef type;

	public Parameter(String name, TypeRef type) {
		this.name = name;
		this.type = type;
	}

	@Override
	public void walk(Consumer<AstNode> visitor) {
		visitor.accept(this);
		type.walk(visitor);
	}

}
