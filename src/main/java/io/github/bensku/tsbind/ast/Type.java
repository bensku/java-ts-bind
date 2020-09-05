package io.github.bensku.tsbind.ast;

import java.util.List;

public class Type extends Member {
	
	/**
	 * Fully qualified name of this type.
	 */
	public final String name;
	
	/**
	 * List of members (methods, fields, inner types) of this type.
	 */
	public final List<Member> members;
	
	/**
	 * Type parameters (generics).
	 */
	public final List<TypeParam> typeParams;
	
	public Type(String name, List<Member> members, List<TypeParam> typeParams, String javadoc, boolean isStatic) {
		super(javadoc, isStatic);
		this.name = name;
		this.members = members;
		this.typeParams = typeParams;
	}
}
