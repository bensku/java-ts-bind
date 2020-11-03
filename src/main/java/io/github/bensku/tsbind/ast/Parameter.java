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
	
	/**
	 * If this is a varargs parameter.
	 */
	public final boolean varargs;

	public Parameter(String name, TypeRef type, boolean varargs) {
		this.name = name;
		this.type = type;
		this.varargs = varargs;
	}

	@Override
	public void walk(Consumer<AstNode> visitor) {
		visitor.accept(this);
		type.walk(visitor);
	}

}
