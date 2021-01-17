package io.github.bensku.tsbind.ast;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

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
	 * If this type is abstract.
	 */
	public final boolean isAbstract;
	
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
	
	/**
	 * Members that this type has.
	 */
	private final Set<String> memberNames;

	public TypeDefinition(String javadoc, boolean isStatic, TypeRef ref, Kind kind, boolean isAbstract,
			List<TypeRef> superTypes, List<TypeRef> interfaces, List<Member> members) {
		super(javadoc, true, isStatic); // Private types are not processed at all
		this.ref = ref;
		this.kind = kind;
		this.isAbstract = isAbstract;
		this.superTypes = superTypes;
		this.interfaces = interfaces;
		this.members = members;
		this.memberNames = new HashSet<>();
		members.stream().map(Member::name).forEach(memberNames::add);
	}
	
	public boolean hasMember(String name) {
		return memberNames.contains(name);
	}

	@Override
	public void walk(Consumer<AstNode> visitor) {
		visitor.accept(this);
		visitor.accept(ref);
		superTypes.forEach(type -> type.walk(visitor));
		interfaces.forEach(type -> type.walk(visitor));
		members.forEach(member -> {
			// Don't walk into inner types, they go to separate TS modules
			if (!(member instanceof TypeDefinition)) {
				member.walk(visitor);
			}
		});
	}

	@Override
	public String name() {
		return ref.name();
	}

}
