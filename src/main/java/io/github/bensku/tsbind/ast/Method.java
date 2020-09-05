package io.github.bensku.tsbind.ast;

import java.util.List;

public class Method extends Member {

	/**
	 * Name of the method.
	 */
	public final String name;

	/**
	 * Return type of the method.
	 */
	public final TypeRef returnType;
	
	/**
	 * Parameters of the method
	 */
	public final List<Parameter> params;
	
	public final List<TypeParam> typeParams;
	
	public Method(String name, TypeRef returnType, List<Parameter> params, List<TypeParam> typeParams, String javadoc, boolean isStatic) {
		super(javadoc, isStatic);
		this.name = name;
		this.returnType = returnType;
		this.params = params;
		this.typeParams = typeParams;
	}
}
