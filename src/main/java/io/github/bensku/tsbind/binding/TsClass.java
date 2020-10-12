package io.github.bensku.tsbind.binding;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.github.bensku.tsbind.ast.TypeDefinition;
import io.github.bensku.tsbind.ast.TypeRef;

public class TsClass implements TsGenerator<TypeDefinition> {

	public static final TsClass INSTANCE = new TsClass();
	
	private TsClass() {}
	
	private void emitName(TypeRef type, TsEmitter out) {
		// Need specialized handling, because we DON'T want package name here
		out.print(type.simpleName());
		if (type instanceof TypeRef.Parametrized) {
			out.print("<").print(((TypeRef.Parametrized) type).typeParams(), ", ").print(">");
		}
	}
	
	@Override
	public void emit(TypeDefinition node, TsEmitter out) {
		node.javadoc.ifPresent(out::javadoc);
		// Class declaration, including superclass and interfaces
		out.print("declare export class ");
		emitName(node.ref, out);
		if (node.superTypes.size() == 1) { // Exactly 1 superclass -> TS extends
			out.print(" extends %s", node.superTypes.get(0));
			if (!node.interfaces.isEmpty()) {
				out.print(" implements ");
				out.print(node.interfaces, ", ");
			}
		} else { // Interfaces can have many superclasses
			// We declare only classes because TS interfaces are VERY different from Java
			// Just changing extends -> implements looks strange, but should be safe
			List<TypeRef> tsInterfaces = new ArrayList<>(node.superTypes);
			tsInterfaces.addAll(node.interfaces);
			if (!tsInterfaces.isEmpty()) {
				out.print(" implements ");
				out.print(tsInterfaces, ", ");
			}
		}
		out.println(" {");
		
		// Emit class members with some indentation
		try (var none = out.startBlock()) {
			// TODO use stream for printing to avoid unnecessary list creation in hot path
			out.print(node.members.stream().filter(member -> !(member instanceof TypeDefinition)).collect(Collectors.toList()), "\n");
		}
		out.println("\n}");
	}

}
