package io.github.bensku.tsbind.binding;

import io.github.bensku.tsbind.ast.TypeRef;

public class TsTypes {

	public static final TsGenerator<TypeRef.Simple> SIMPLE = (node, out) -> {
		if (node == TypeRef.VOID) {
			out.print("void");
		} else if (node == TypeRef.BOOLEAN) {
			out.print("boolean");
		} else if (node == TypeRef.BYTE || node == TypeRef.SHORT 
				|| node == TypeRef.INT || node == TypeRef.FLOAT
				|| node == TypeRef.LONG || node == TypeRef.DOUBLE) {
			// Closest TS type of most primitives is number
			// FIXME GraalJS can't implicitly convert between all of these
			out.print("number");
		} else if (node == TypeRef.STRING || node == TypeRef.CHAR) {
			out.print("string");
		} else if (node == TypeRef.OBJECT) {
			// Allow autoboxing JS boolean and number to Object
			// Also helps with generic inheritance
			out.print("any");
		} else {
			out.printType(node);
		}
	};
	
	public static final TsGenerator<TypeRef.Wildcard> WILDCARD = (node, out) -> {
		// Print upper bound; ? extends X is not possible (and hopefully not needed) in TS
		out.print(node.extendedType());
	};
	
	public static final TsGenerator<TypeRef.Parametrized> PARAMETRIZED = (node, out) -> {
		out.print(node.baseType());
		out.print("<").print(node.typeParams(), ", ").print(">");
	};
	
	public static final TsGenerator<TypeRef.Array> ARRAY = (node, out) -> {
		out.print(node.baseType()).print("[]".repeat(node.arrayDimensions()));
	};
	
	public static final TsGenerator<TypeRef.Nullable> NULLABLE = (node, out) -> {
		out.print(node.nullableType()).print(" | null");
	};
}
