package io.github.bensku.tsbind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;

import io.github.bensku.tsbind.ast.Constructor;
import io.github.bensku.tsbind.ast.Field;
import io.github.bensku.tsbind.ast.Getter;
import io.github.bensku.tsbind.ast.Member;
import io.github.bensku.tsbind.ast.Method;
import io.github.bensku.tsbind.ast.Parameter;
import io.github.bensku.tsbind.ast.Setter;
import io.github.bensku.tsbind.ast.Type;
import io.github.bensku.tsbind.ast.TypeParam;
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
	 * @return Parsed type.
	 */
	public Type parseType(SourceUnit source) {
		ParseResult<CompilationUnit> result = parser.parse(source.code);
		if (!result.isSuccessful()) {
			throw new IllegalArgumentException("failed to parse given source code: " + result.getProblems());
		}
		CompilationUnit unit = result.getResult().orElseThrow();
		return processType(source.name, unit.findFirst(TypeDeclaration.class).orElseThrow());
	}
	
	private ResolvedType getBaseType(ResolvedType type) {
		if (type.isArray()) {
			return getBaseType(type.asArrayType().getComponentType());
		} else {
			return type;
		}
	}
	
	private TypeRef convertType(ResolvedType type) {
		return new TypeRef(getBaseType(type).describe(), type.arrayLevel());
	}
	
	private TypeParam convertTypeParam(ResolvedTypeParameterDeclaration param) {
		return new TypeParam(param.getName(), param.hasLowerBound() ? convertType(param.getLowerBound()) : null);
	}
	
	private List<TypeParam> convertTypeParams(List<ResolvedTypeParameterDeclaration> params) {
		return params.stream().map(this::convertTypeParam).collect(Collectors.toList());
	}
	
	private List<Parameter> getParameters(ResolvedMethodLikeDeclaration method) {
		List<Parameter> params = new ArrayList<>(method.getNumberOfParams());
		for (int i = 0; i < method.getNumberOfParams(); i++) {
			ResolvedParameterDeclaration param = method.getParam(i);
			params.add(new Parameter(param.getName(), convertType(param.getType())));
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
	
	private Type processType(String typeName, TypeDeclaration<?> type) {
		List<Member> members = new ArrayList<>();
		
		for (BodyDeclaration<?> member : type.getMembers()) {
			// Process type depending on what it is
			if (member.isClassOrInterfaceDeclaration()) {
				// Recursively process an inner type
				ClassOrInterfaceDeclaration inner = member.asClassOrInterfaceDeclaration();
				members.add(processType(typeName + "." + inner.getNameAsString(), inner));
			} else if (member.isConstructorDeclaration()) {
				ResolvedConstructorDeclaration constructor = member.asConstructorDeclaration().resolve();
				if (constructor.accessSpecifier() == AccessSpecifier.PUBLIC) {
					// Constructor might be generic, but AFAIK TypeScript doesn't support that
					// (constructors of generic classes are, of course, supported)
					members.add(new Constructor(constructor.getName(), TypeRef.VOID, getParameters(constructor),
							Collections.emptyList(), getJavadoc(member)));
				}
			} else if (member.isMethodDeclaration()) {
				ResolvedMethodDeclaration method = member.asMethodDeclaration().resolve();
				if (method.accessSpecifier() != AccessSpecifier.PUBLIC) {
					continue;
				}
				
				String name = method.getName();
				TypeRef returnType = convertType(method.getReturnType());
				String methodDoc = getJavadoc(member);
				boolean override = member.getAnnotationByClass(Override.class).isPresent();
				// TODO check if GraalJS works with "is" for boolean getter too
				if (name.startsWith("get") && !method.getReturnType().isVoid()
						&& method.getNumberOfParams() == 0 && method.getTypeParameters().isEmpty()) {
					// GraalJS will make this getter work, somehow
					members.add(new Getter(name, returnType, methodDoc, override));
				} else if (name.startsWith("set") && method.getReturnType().isVoid()
						&& method.getNumberOfParams() == 1 && method.getTypeParameters().isEmpty()) {
					// GraalJS will make this setter work, somehow
					members.add(new Setter(name, returnType, methodDoc, override));
				} else { // Normal method
					// Resolve type parameters and add to member list
					members.add(new Method(name, returnType, getParameters(method),
							convertTypeParams(method.getTypeParameters()), methodDoc,
							method.isStatic(), override));
				}
			} else if (member.isFieldDeclaration()) {
				ResolvedFieldDeclaration field = member.asFieldDeclaration().resolve();
				if (field.accessSpecifier() == AccessSpecifier.PUBLIC) {
					members.add(new Field(field.getName(), convertType(field.getType()), getJavadoc(member), field.isStatic()));
				}
			}
		}
		
		List<TypeParam> typeParams = convertTypeParams(type.resolve().getTypeParameters());
		String javadoc = getJavadoc(type);
		return new Type(typeName, members, typeParams, javadoc, type.isStatic());
	}
}
