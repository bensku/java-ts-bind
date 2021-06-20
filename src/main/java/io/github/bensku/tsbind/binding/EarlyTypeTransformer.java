package io.github.bensku.tsbind.binding;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import io.github.bensku.tsbind.ast.Member;
import io.github.bensku.tsbind.ast.Method;
import io.github.bensku.tsbind.ast.TypeDefinition;
import io.github.bensku.tsbind.ast.TypeRef;

/**
 * Performs early type transformations. They are required e.g. when the pass
 * might add new used types that could affect imports.
 * 
 * Early transform passes can and will mutate the contents of types!
 *
 */
public class EarlyTypeTransformer {
	
	private final Map<String, TypeDefinition> typeTable;
	
	public EarlyTypeTransformer(Map<String, TypeDefinition> typeTable) {
		this.typeTable = typeTable;
	}

	private void visitSupertypes(TypeDefinition type, Consumer<TypeDefinition> visitor) {
		// Call visitor only on supertypes, not the type initially given as parameter
		for (TypeRef ref : type.superTypes) {
			TypeDefinition def = typeTable.get(ref.name());
			if (def != null) {
				visitor.accept(def);
				visitSupertypes(def, visitor);
			}
		}
		for (TypeRef ref : type.interfaces) {
			TypeDefinition def = typeTable.get(ref.name());
			if (def != null) {
				visitor.accept(def);
				visitSupertypes(def, visitor);
			}
		}
	}
	
	/**
	 * TypeScript removes inherited overloads unless they're re-specified.
	 * As such, we copy them to classes that should inherit them.
	 */
	public void addMissingOverloads(TypeDefinition type) {
		// Figure out what methods we already have
		Set<MethodId> methods = new HashSet<>();
		for (Member member : type.members) {
			if (member instanceof Method) {
				methods.add(new MethodId((Method) member));
			}
		}
		
		// Visit supertypes and interfaces to see what we're missing
		visitSupertypes(type, parent -> {
			for (Member member : parent.members) {
				if (member instanceof Method && type.hasMember(member.name())) {
					// We have a member with same name
					// If it has different signature, we need to copy the missing overload
					if (!methods.contains(new MethodId((Method) member))) {
						type.members.add(member);
					}
				}
			}
		});
	}
}
