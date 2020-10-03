package io.github.bensku.tsbind.ast;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.resolution.types.ResolvedArrayType;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;

public abstract class TypeRef implements AstNode {
	
	public static final Simple OBJECT = new Simple("java.lang.Object");
	public static final Simple VOID = new Simple("void");
	public static final Simple BOOLEAN = new Simple("boolean");
	public static final Simple BYTE = new Simple("byte");
	public static final Simple SHORT = new Simple("short");
	public static final Simple CHAR = new Simple("char");
	public static final Simple INT = new Simple("int");
	public static final Simple LONG = new Simple("long");
	public static final Simple FLOAT = new Simple("float");
	public static final Simple DOUBLE = new Simple("double");
	
	public static TypeRef fromType(ResolvedType type) {
		if (type.isVoid()) {
			return VOID;
		} else if (type.isPrimitive()) {
			switch (type.asPrimitive()) {
			case BOOLEAN:
				return BOOLEAN;
			case BYTE:
				return BYTE;
			case SHORT:
				return SHORT;
			case CHAR:
				return CHAR;
			case INT:
				return INT;
			case LONG:
				return LONG;
			case FLOAT:
				return FLOAT;
			case DOUBLE:
				return DOUBLE;
			default:
				throw new AssertionError();
			}
		} else if (type.isReferenceType()) {
			ResolvedReferenceType reference = type.asReferenceType();
			List<ResolvedType> typeParams = reference.typeParametersValues();
			if (typeParams.isEmpty()) { // No generic type parameters
				return new Simple(reference.getQualifiedName());
			} else {
				List<TypeRef> params = typeParams.stream().map(TypeRef::fromType).collect(Collectors.toList());
				return new Parametrized(new Simple(reference.getQualifiedName()), params);
			}
		} else if (type.isArray()) {
			ResolvedArrayType array = type.asArrayType();
			TypeRef component = fromType(array.getComponentType());
			return new Array(component, array.arrayLevel());
		} else if (type.isWildcard()) {
			if (type.asWildcard().isExtends()) {
				return new Wildcard(fromType(type.asWildcard().getBoundedType()));
			} else { // We can't describe ? super X in TS (AFAIK)
				return OBJECT;
			}
		} else if (type.isTypeVariable()) {
			return fromDeclaration(type.asTypeParameter());
		} else {
			throw new AssertionError("unexpected type: " + type);
		}
	}
	
	public static TypeRef fromDeclaration(ResolvedTypeParameterDeclaration decl) {
		if (decl.hasUpperBound()) {
			return new Parametrized(new Simple(decl.getName()),
					Collections.singletonList(fromType(decl.getUpperBound())));
		} else if (decl.hasLowerBound()) { // We can't describe X super Y in TS (AFAIK)
			return OBJECT;
		} else {
			return new Simple(decl.getName());
		}
	}
	
	public static TypeRef fromDeclaration(String typeName, ResolvedReferenceTypeDeclaration decl) {
		var typeParams = decl.getTypeParameters();
		if (typeParams.isEmpty()) {
			return new Simple(decl.getQualifiedName());
		} else {
			List<TypeRef> params = typeParams.stream().map(TypeRef::fromDeclaration).collect(Collectors.toList());
			return new Parametrized(new Simple(decl.getQualifiedName()), params);
		}
	}
		
	public static class Simple extends TypeRef {
		
		/**
		 * Fully qualified name of the type, excluding array dimensions.
		 */
		private final String name;

		private Simple(String name) {
			this.name = name;
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public int arrayDimensions() {
			return 0;
		}
	}
	
	public static class Wildcard extends TypeRef {

		/**
		 * Type that this generic parameter must extend.
		 */
		private final TypeRef extendedType;

		private Wildcard(TypeRef extendedType) {
			this.extendedType = extendedType;
		}

		@Override
		public String name() {
			return "*";
		}

		@Override
		public int arrayDimensions() {
			return 0;
		}
		
		public TypeRef extendedType() {
			return extendedType;
		}
		
	}
	
	public static class Parametrized extends TypeRef {
		
		/**
		 * Base type that generic type parameters are applied to.
		 */
		private final TypeRef baseType;
		
		/**
		 * Type parameters.
		 */
		private final List<TypeRef> params;

		private Parametrized(TypeRef baseType, List<TypeRef> params) {
			this.baseType = baseType;
			this.params = params;
		}

		@Override
		public String name() {
			return baseType.name();
		}

		@Override
		public int arrayDimensions() {
			return 0;
		}
		
		public TypeRef baseType() {
			return baseType;
		}
		
		public List<TypeRef> typeParams() {
			return params;
		}
	}
	
	public static class Array extends TypeRef {
		
		/**
		 * Array component type.
		 */
		private final TypeRef component;
		
		/**
		 * Array dimensions.
		 */
		private final int dimensions;

		private Array(TypeRef component, int dimensions) {
			this.component = component;
			this.dimensions = dimensions;
		}

		@Override
		public String name() {
			return component.name() + "[]".repeat(dimensions);
		}
		
		public TypeRef componentType() {
			return component;
		}

		@Override
		public int arrayDimensions() {
			return dimensions;
		}
		
		
	}
	
	public abstract String name();
	
	public String simpleName() {
		String name = name();
		return name.substring(name.lastIndexOf('.') + 1);
	}
	
	public abstract int arrayDimensions();
	
}
