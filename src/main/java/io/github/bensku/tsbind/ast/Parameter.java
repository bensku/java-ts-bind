package io.github.bensku.tsbind.ast;

public class Parameter {
	
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

}
