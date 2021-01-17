package io.github.bensku.tsbind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.HasAccessSpecifier;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;

import io.github.bensku.tsbind.ast.Constructor;
import io.github.bensku.tsbind.ast.Field;
import io.github.bensku.tsbind.ast.Getter;
import io.github.bensku.tsbind.ast.Member;
import io.github.bensku.tsbind.ast.Method;
import io.github.bensku.tsbind.ast.Parameter;
import io.github.bensku.tsbind.ast.Setter;
import io.github.bensku.tsbind.ast.TypeDefinition;
import io.github.bensku.tsbind.ast.TypeRef;

/**
 * Reads Java source code to produce AST of types and their members.
 * No <i>code</i> is actually analyzed.
 *
 */
public class AstGenerator {

	private final JavaParser parser;
	
	public AstGenerator(JavaParser parser) {
		this.parser = parser;
	}
	
	/**
	 * Parses type AST from source code.
	 * @param source Source unit (single Java file).
	 * @return Parsed type or empty optional if it is not public.
	 */
	public Optional<TypeDefinition> parseType(SourceUnit source) {
		// FIXME don't log errors here, CLI might not be only user in future
		
		ParseResult<CompilationUnit> result = parser.parse(source.code);
		if (!result.isSuccessful()) {
			//throw new IllegalArgumentException("failed to parse given source code: " + result.getProblems());
			System.err.println("failed to parse " + source.name + ": " + result.getProblems());
			return Optional.empty();
		}
		CompilationUnit unit = result.getResult().orElseThrow();
		TypeDeclaration<?> type = unit.findFirst(TypeDeclaration.class).orElseThrow();
		if (type.getAccessSpecifier() == AccessSpecifier.PUBLIC) {
			try {
				return processType(source.name, type);
			} catch (UnsolvedSymbolException e) {
				System.err.println("failed to resolve symbol " + e.getName() + " in " + source.name);
				return Optional.empty();
			}
		} else {
			return Optional.empty();
		}
	}
	
	private List<Parameter> getParameters(ResolvedMethodLikeDeclaration method, Boolean[] nullable) {
		List<Parameter> params = new ArrayList<>(method.getNumberOfParams());
		for (int i = 0; i < method.getNumberOfParams(); i++) {
			ResolvedParameterDeclaration param = method.getParam(i);
			TypeRef type = TypeRef.fromType(param.getType(), nullable[i]);
			params.add(new Parameter(param.getName(), type, param.isVariadic()));
		}
		return params;
	}
	
	private String getJavadoc(Node node) {
		return node.getComment().map(comment -> {
			if (comment.isJavadocComment()) {
				return comment.asJavadocComment().getContent();
			}
			return null;
		}).orElse(null);
	}
	
	private Optional<TypeDefinition> processType(String typeName, TypeDeclaration<?> type) {
		ResolvedReferenceTypeDeclaration resolved = type.resolve();		
		TypeRef typeRef = TypeRef.fromDeclaration(typeName, resolved);
		List<Member> members = new ArrayList<>();
		
		// If this is an enum, generate enum constants and compiler-generated methods
		// JavaParser doesn't consider enum constants "members"
		if (type.isEnumDeclaration()) {
			for (EnumConstantDeclaration constant : type.asEnumDeclaration().getEntries()) {
				members.add(new Field(constant.getNameAsString(), typeRef, getJavadoc(constant), true, true, true));
			}
			
			members.add(new Method("valueOf", typeRef,
					List.of(new Parameter("name", TypeRef.STRING, false)),
					List.of(), null, true, true, false));
			members.add(new Method("values", typeRef.makeArray(1), List.of(), List.of(), null, true, true, false));
		}
		
		// Figure out supertypes and interfaces (needed by some members)
		TypeDefinition.Kind typeKind;
		List<TypeRef> superTypes;
		List<TypeRef> interfaces;
		// Overrides of methods from non-public types are not overrides from TS point of view
		Set<String> privateOverrides = new HashSet<>();
		boolean isAbstract = false;
		if (type.isClassOrInterfaceDeclaration()) {
			ClassOrInterfaceDeclaration decl = type.asClassOrInterfaceDeclaration();
			isAbstract = decl.isAbstract();
			typeKind = decl.isInterface() ? TypeDefinition.Kind.INTERFACE : TypeDefinition.Kind.CLASS;
			
			PublicFilterResult extendedResult = filterPublicTypes(decl.getExtendedTypes());
			PublicFilterResult implementedResult = filterPublicTypes(decl.getImplementedTypes());
			superTypes = extendedResult.publicTypes.stream()
					.map(TypeRef::fromType).collect(Collectors.toList());;
			interfaces = implementedResult.publicTypes.stream()
					.map(TypeRef::fromType).collect(Collectors.toList());
			
			extendedResult.privateTypes.forEach(t -> privateOverrides.addAll(getAllMethods(t)));
			implementedResult.privateTypes.forEach(t -> privateOverrides.addAll(getAllMethods(t)));
		} else if (type.isEnumDeclaration()) {
			typeKind = TypeDefinition.Kind.ENUM;
			superTypes = List.of(TypeRef.enumSuperClass(typeRef));
			interfaces = List.of();
		} else {
			typeKind = TypeDefinition.Kind.ANNOTATION;
			superTypes = List.of();
			interfaces = List.of();
		}
		
		// Lombok setter/getter support
		boolean lombokGetter = type.isAnnotationPresent("Getter");
		boolean lombokSetter = type.isAnnotationPresent("Setter");
		
		// Handle normal members
		for (BodyDeclaration<?> member : type.getMembers()) {
			boolean isPublic = isPublic(type, member);
			if (member.isFieldDeclaration()) {
				// Even private fields may need Lombok getter/setter
				try {
					processField(members, member.asFieldDeclaration(), typeKind == TypeDefinition.Kind.INTERFACE, isPublic, lombokGetter, lombokSetter);
				} catch (UnsolvedSymbolException e) {
					// Allow symbol lookup to fail on private fields
					if (isPublic) {
						throw e;
					}
				}
			}	
			if (!isPublic) {
				// For now, only private fields are needed
				// Work as if other non-public members did not exist
				continue; // Neither implicitly or explicitly public
			}
			
			// Process type depending on what it is
			if (member.isTypeDeclaration()) {
				// Recursively process an inner type
				TypeDeclaration<?> inner = member.asTypeDeclaration();
				processType(typeName + "." + inner.getNameAsString(), inner).ifPresent(members::add);
			} else if (member.isConstructorDeclaration()) {
				ResolvedConstructorDeclaration constructor = member.asConstructorDeclaration().resolve();
				Boolean[] nullable = member.asConstructorDeclaration().getParameters().stream()
						.map(param -> param.isAnnotationPresent("Nullable")).toArray(Boolean[]::new);
				// Constructor might be generic, but AFAIK TypeScript doesn't support that
				// (constructors of generic classes are, of course, supported)
				// Private constructors are not yet needed, so they won't exist
				members.add(new Constructor(constructor.getName(), getParameters(constructor, nullable), getJavadoc(member), true));
			} else if (member.isMethodDeclaration()) {
				members.add(processMethod(member.asMethodDeclaration(), privateOverrides));
			}
		}
		
		// Lombok autogenerated constructors
		// AllArgsConstructor uses ALL instance fields
		// RequiredArgsConstructor only for final and (TODO, not implemented) non-null fields
		if (type.isAnnotationPresent("AllArgsConstructor")) {
			List<Parameter> params = members.stream()
					.filter(member -> !member.isStatic && member instanceof Field) // Only instance fields
					.map(member -> (Field) member)
					.map(field -> new Parameter(field.name, field.type, false))
					.collect(Collectors.toList());
			members.add(new Constructor(type.getNameAsString(), params, null, true));
		}
		if (type.isAnnotationPresent("RequiredArgsConstructor")) {
			List<Parameter> params = members.stream()
					.filter(member -> !member.isStatic && member instanceof Field)
					.map(member -> (Field) member)
					.filter(field -> field.isFinal)
					.map(field -> new Parameter(field.name, field.type, false))
					.collect(Collectors.toList());
			members.add(new Constructor(type.getNameAsString(), params, null, true));
		}
		
		// Create type definition
		String javadoc = getJavadoc(type);
		return Optional.of(new TypeDefinition(javadoc, type.isStatic(), typeRef, typeKind, isAbstract,
				superTypes, interfaces, members));
	}

	private static class PublicFilterResult {
		public final List<ResolvedReferenceType> publicTypes = new ArrayList<>();
		public final List<ResolvedReferenceType> privateTypes = new ArrayList<>();
	}
	
	private PublicFilterResult filterPublicTypes(List<ClassOrInterfaceType> types) {
		PublicFilterResult result = new PublicFilterResult();
		for (ClassOrInterfaceType type : types) {
			ResolvedReferenceType resolved = type.resolve();
			if (isPublic(resolved.getTypeDeclaration().orElse(null))) {
				result.publicTypes.add(resolved);
			} else {
				result.privateTypes.add(resolved);
			}
		}
		return result;
	}
	
	private Set<String> getAllMethods(ResolvedReferenceType type) {
		if (type == null) {
			return Collections.emptySet();
		}
		ResolvedReferenceTypeDeclaration decl = type.getTypeDeclaration().orElse(null);
		if (decl == null) {
			return Collections.emptySet();
		}
		Set<String> names = type.getAllMethods().stream().map(method -> method.getName())
				.collect(Collectors.toCollection(HashSet::new));
		if (decl.isClass()) {
			decl.asClass().getSuperClass().ifPresent(c
					-> names.addAll(getAllMethods(c)));
			decl.asClass().getInterfaces().forEach(i
					-> names.addAll(getAllMethods(i)));
		} else if (decl.isInterface()) {
			decl.asInterface().getInterfacesExtended().forEach(i
					-> names.addAll(getAllMethods(i)));
		}
		return names;
	}
	
	private boolean isPublic(TypeDeclaration<?> type, BodyDeclaration<?> member) {
		// JPMS is ignored for now, would need to parse module infos for that
		AccessSpecifier access = (member instanceof NodeWithModifiers<?>)
				? ((NodeWithModifiers<?>) member).getAccessSpecifier() : AccessSpecifier.PACKAGE_PRIVATE;
		// Members specified as public are ALWAYS public
		if (access == AccessSpecifier.PUBLIC) {
			return true;
		}
		// Default ("package private") access in interfaces is public
		if (access == AccessSpecifier.PACKAGE_PRIVATE && type.isClassOrInterfaceDeclaration()) {
			return type.asClassOrInterfaceDeclaration().isInterface();
		}
		// Enum constants are handled separately, JavaParser doesn't consider them members
		
		return false; // No reason to consider member public
	}
	
	private boolean isPublic(ResolvedReferenceTypeDeclaration type) {
		if (type instanceof HasAccessSpecifier) {
			return ((HasAccessSpecifier) type).accessSpecifier() == AccessSpecifier.PUBLIC;
		}
		return false;
	}
	
	private Method processMethod(MethodDeclaration member, Set<String> privateOverrides) {
		boolean isPublic = true; // Private methods are not yet needed, so they won't exist
		
		ResolvedMethodDeclaration method = member.asMethodDeclaration().resolve();
		boolean nullableReturn = member.isAnnotationPresent("Nullable");
		Boolean[] nullableParams = member.getParameters().stream()
				.map(param -> param.isAnnotationPresent("Nullable")).toArray(Boolean[]::new);
		
		String name = method.getName();
		TypeRef returnType = TypeRef.fromType(method.getReturnType(), nullableReturn);
		String methodDoc = getJavadoc(member);
		boolean override = !privateOverrides.contains(name) && member.getAnnotationByClass(Override.class).isPresent();
		// boolean getters and setters are kept as regular methods to prevent confusing naming
		if (name.length() > 3 && name.startsWith("get") && returnType != TypeRef.VOID
				&& returnType != TypeRef.BOOLEAN && method.getNumberOfParams() == 0
				&& method.getTypeParameters().isEmpty()) {
			// GraalJS will make this getter work, somehow
			return new Getter(name, returnType, methodDoc, isPublic, method.isStatic(), override);
		} else if (name.length() > 3 && name.startsWith("set") && method.getNumberOfParams() == 1
				&& TypeRef.fromType(method.getParam(0).getType()) != TypeRef.BOOLEAN
				&& method.getTypeParameters().isEmpty()) {
			// GraalJS will make this setter work, somehow
			return new Setter(name, TypeRef.fromType(method.getParam(0).getType(),
					nullableParams[0]), methodDoc, isPublic, method.isStatic(), override);
		} else { // Normal method
			// Resolve type parameters and add to member list
			return new Method(name, returnType, getParameters(method, nullableParams),
					method.getTypeParameters().stream().map(TypeRef::fromDeclaration).collect(Collectors.toList()),
					methodDoc, isPublic, method.isStatic(), override);
		}
	}
	
	private void processField(List<Member> members, FieldDeclaration member, boolean isInterface,
			boolean isPublic, boolean lombokGetter, boolean lombokSetter) {
		FieldDeclaration field = member.asFieldDeclaration();
		boolean nullable = field.isAnnotationPresent("Nullable");
		NodeList<VariableDeclarator> vars = field.getVariables();
		boolean isStatic = isInterface || field.isStatic();
		boolean isFinal = isInterface || field.isFinal();
		if (vars.size() == 1) {
			FieldProps props = new FieldProps(field.resolve(), getJavadoc(member),
					nullable, isPublic, isStatic, isFinal, lombokGetter, lombokSetter);
			processFieldValue(members, props);
		} else { // Symbol solver can't resolve this for us
			for (VariableDeclarator var : vars) {
				FieldProps props = new FieldProps(var.resolve(), getJavadoc(member),
						nullable, isPublic, isStatic, isFinal, lombokGetter, lombokSetter);
				processFieldValue(members, props);
			}
		}
	}
	
	private class FieldProps {
		ResolvedValueDeclaration value;
		String javadoc;
		boolean nullable;
		boolean isPublic;
		boolean isStatic;
		boolean isFinal;
		boolean lombokGetter, lombokSetter;
		
		FieldProps(ResolvedValueDeclaration value, String javadoc, boolean nullable, boolean isPublic,
				boolean isStatic, boolean isFinal, boolean lombokGetter, boolean lombokSetter) {
			this.value = value;
			this.javadoc = javadoc;
			this.nullable = nullable;
			this.isPublic = isPublic;
			this.isStatic = isStatic;
			this.isFinal = isFinal;
			this.lombokGetter = lombokGetter;
			this.lombokSetter = lombokSetter;
		}
	}
	
	private void processFieldValue(List<Member> members, FieldProps props) {
		TypeRef type = TypeRef.fromType(props.value.getType(), props.nullable);
		// Add normal field to AST
		members.add(new Field(props.value.getName(), type, props.javadoc,
				props.isPublic, props.isStatic, props.isFinal));
		
		// Generate public getter/setter pair for field (Lombok)
		if (props.lombokGetter) {
			members.add(new Getter(props.value.getName(), type, props.javadoc, true, props.isStatic, false));
		}
		if (props.lombokSetter) {
			members.add(new Setter(props.value.getName(), type, props.javadoc, true, props.isStatic, false));
		}
	}
}
