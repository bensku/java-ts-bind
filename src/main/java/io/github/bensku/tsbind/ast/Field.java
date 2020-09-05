package io.github.bensku.tsbind.ast;

import java.util.Objects;

public class Field extends Member {
	
	/**
	 * Field name.
	 */
	public final String name;

	/**
	 * Type of the field.
	 */
	public final TypeRef type;
	
	public Field(String name, TypeRef type, String javadoc, boolean isStatic) {
		super(javadoc, isStatic);
		Objects.requireNonNull(name);
		Objects.requireNonNull(type);
		this.name = name;
		this.type = type;
	}
}
