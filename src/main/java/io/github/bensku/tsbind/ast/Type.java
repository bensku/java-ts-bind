package io.github.bensku.tsbind.ast;

import java.util.List;

public class Type extends Member {
	
	/**
	 * Fully qualified name of this type.
	 */
	public final String name;
	
	/**
	 * Simple name of this type.
	 */
	public final String simpleName;
	
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
		// Create simple name; if in default package, -1 from lastIndexOf + 1 == 0, which is ok
		this.simpleName = name.substring(name.lastIndexOf('.') + 1);
		this.members = members;
		this.typeParams = typeParams;
	}

}
