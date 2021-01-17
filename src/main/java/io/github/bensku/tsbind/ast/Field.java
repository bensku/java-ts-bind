package io.github.bensku.tsbind.ast;

import java.util.Objects;
import java.util.function.Consumer;

public class Field extends Member {
	
	/**
	 * Field name.
	 */
	public final String name;

	/**
	 * Type of the field.
	 */
	public final TypeRef type;
	
	/**
	 * If this field is final (readonly).
	 */
	public final boolean isFinal;
	
	public Field(String name, TypeRef type, String javadoc, boolean isPublic, boolean isStatic, boolean isFinal) {
		super(javadoc, isPublic, isStatic);
		Objects.requireNonNull(name);
		Objects.requireNonNull(type);
		this.name = name;
		this.type = type;
		this.isFinal = isFinal;
	}

	@Override
	public void walk(Consumer<AstNode> visitor) {
		visitor.accept(this);
		type.walk(visitor);
	}

	@Override
	public String name() {
		return name;
	}
}
