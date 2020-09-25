package io.github.bensku.tsbind.binding;

import java.util.List;
import java.util.stream.Collectors;

import io.github.bensku.tsbind.ast.TypeRef;

public class TsType {

	public static final TsType BOOLEAN = new TsType("boolean", "boolean");
	public static final TsType NUMBER = new TsType("number", "number");
	public static final TsType STRING = new TsType("string", "string");
	
	public static TsType fromJava(TypeRef type) {
		// TODO try to shorten name aliases if there are no conflicts?
		return new TsType(type.simpleName, type.name.replace('.', '_'));
	}
	
	public static List<TsType> fromJava(List<TypeRef> types) {
		return types.stream().map(TsType::fromJava).collect(Collectors.toList());
	}
	
	/**
	 * Simple name of this type. If this represents a Java type, this is
	 * {@link Class#getSimpleName()}.
	 */
	public final String simpleName;
	
	/**
	 * Name to use in emitted TypeScript bindings.
	 */
	public final String nameAlias;
	
	private TsType(String simpleName, String nameAlias) {
		this.simpleName = simpleName;
		this.nameAlias = nameAlias;
	}
	
	@Override
	public String toString() {
		return nameAlias;
	}
	
}
