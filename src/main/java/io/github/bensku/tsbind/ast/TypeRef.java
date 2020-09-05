package io.github.bensku.tsbind.ast;

public class TypeRef {
	
	public static final TypeRef VOID = new TypeRef("void", 0);

	/**
	 * Fully qualified name of the class, excluding array dimensions.
	 */
	public final String name;
	
	/**
	 * Array dimensions of this type.
	 */
	public final int arrayDimensions;
	
	public TypeRef(String name, int arrayDimensions) {
		this.name = name;
		this.arrayDimensions = arrayDimensions;
	}
}
