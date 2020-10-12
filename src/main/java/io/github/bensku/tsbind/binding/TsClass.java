package io.github.bensku.tsbind.binding;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.github.bensku.tsbind.ast.TypeDefinition;
import io.github.bensku.tsbind.ast.TypeRef;

public class TsClass implements TsGenerator<TypeDefinition> {

	public static final TsClass INSTANCE = new TsClass();
	
	private TsClass() {}
	
	private void emitName(String name, TypeRef type, TsEmitter out) {
		// Need specialized handling, because we DON'T want package name here
		out.print(name);
		if (type instanceof TypeRef.Parametrized) {
			out.print("<").print(((TypeRef.Parametrized) type).typeParams(), ", ").print(">");
		}
	}
	
	@Override
	public void emit(TypeDefinition node, TsEmitter out) {
		node.javadoc.ifPresent(out::javadoc);
		// Class declaration, including superclass and interfaces
		
		// We can't use TS 'implements', because TS interfaces
		// don't support e.g getters/setters
		// Instead, we translate Java implements to TS extends
		List<TypeRef> superTypes = new ArrayList<>(node.superTypes);
		superTypes.addAll(node.interfaces);
		boolean mixinTrick = false;
		if (superTypes.size() < 2) { // At most one superclass/interface
			out.print("export class ");
			emitName(node.ref.simpleName(), node.ref, out);
			if (!superTypes.isEmpty()) {
				out.print(" extends %s", superTypes.get(0));
			}
		} else { // More than one superclass/interface
			// Emit class content as hidden (not exported) class to be extended by mixin
			out.print("class ");
			emitName(node.ref.simpleName() + "_Impl", node.ref, out);
			mixinTrick = true; // Emit mixin after class declaration
		}
		out.println(" {");
		
		// Emit class members with some indentation
		try (var none = out.startBlock()) {
			// TODO use stream for printing to avoid unnecessary list creation in hot path
			out.print(node.members.stream()
					.filter(member -> !(member instanceof TypeDefinition))
					.collect(Collectors.toList()), "\n");
		}
		out.println("\n}");
		
		// Emit supertypes/interfaces as mixin interface+class
		if (mixinTrick) {
			out.print("export interface ");
			emitName(node.ref.simpleName(), node.ref, out);
			out.print(" extends ");
			emitName(node.ref.simpleName() + "_Impl", node.ref, out);
			out.print(", ");
			out.print(superTypes, ", ");
			out.println("{}");
			out.print("export class ");
			emitName(node.ref.simpleName(), node.ref, out);
			out.println(" {}");
		}
	}

}
