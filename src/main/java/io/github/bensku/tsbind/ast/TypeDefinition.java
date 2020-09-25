package io.github.bensku.tsbind.ast;

import java.util.List;

public class TypeDefinition extends Member {
	
	/**
	 * Type reference to the defined type.
	 */
	public final TypeRef ref;
	
	public enum Kind {
		CLASS,
		INTERFACE,
		ENUM,
		ANNOTATION
	}
	
	/**
	 * What kind of type this is.
	 */
	public final Kind kind;
	
	/**
	 * Superclasses (and interfaces, if this is an interface) of this type.
	 */
	public final List<TypeRef> superTypes;
	
	/**
	 * Interfaces of this type (if any).
	 */
	public final List<TypeRef> interfaces;
	
	/**
	 * List of members (methods, fields, inner types) of this type.
	 */
	public final List<Member> members;

	public TypeDefinition(String javadoc, boolean isStatic, TypeRef ref, Kind kind, List<TypeRef> superTypes,
			List<TypeRef> interfaces, List<Member> members) {
		super(javadoc, isStatic);
		this.ref = ref;
		this.kind = kind;
		this.superTypes = superTypes;
		this.interfaces = interfaces;
		this.members = members;
	}
	
	

}
