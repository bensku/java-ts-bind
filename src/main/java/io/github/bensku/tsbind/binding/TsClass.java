package io.github.bensku.tsbind.binding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.bensku.tsbind.ast.Getter;
import io.github.bensku.tsbind.ast.Member;
import io.github.bensku.tsbind.ast.Method;
import io.github.bensku.tsbind.ast.Setter;
import io.github.bensku.tsbind.ast.TypeDefinition;
import io.github.bensku.tsbind.ast.TypeRef;

public class TsClass implements TsGenerator<TypeDefinition> {

	public static final TsClass INSTANCE = new TsClass();
	
	private TsClass() {}
	
	private class Members {
		
		private final List<Member> members;
		
		public Members(TypeDefinition type) {
			this.members = type.members;
		}
		
		/**
		 * Transforms a TS getter/setter at given index to a normal method.
		 * If the member there is not an accessor, nothing is done.
		 * @param index Index.
		 */
		private void invalidateGetSet(int index) {
			Member member = members.get(index);
			if (member instanceof Getter) {
				Getter getter = (Getter) member;
				Method method = new Method(getter.originalName, getter.returnType, getter.params,
						getter.typeParams, getter.javadoc.orElse(null), getter.isStatic, getter.isOverride);
				members.set(index, method);
			} else if (member instanceof Setter) {
				Setter setter = (Setter) member;
				Method method = new Method(setter.originalName, setter.returnType, setter.params,
						setter.typeParams, setter.javadoc.orElse(null), setter.isStatic, setter.isOverride);
				members.set(index, method);
			} // other kinds of conflicts we don't touch
		}
		
		/**
		 * Resolves name conflicts caused by getters/setters.
		 */
		public void resolveConflicts() {
			// Figure out members with same names
			Map<String, List<Integer>> indices = new HashMap<>();
			for (int i = 0; i < members.size(); i++) {
				Member member = members.get(i);
				indices.computeIfAbsent(member.name(), n -> new ArrayList<>()).add(i);
			}
			
			// Resolve conflicts with getters/setters
			for (Map.Entry<String, List<Integer>> entry : indices.entrySet()) {
				List<Integer> conflicts = entry.getValue();
				if (conflicts.size() == 1) {
					continue; // No conflict exists
				} else if (conflicts.size() == 2) {
					Member first = members.get(conflicts.get(0));
					Member second = members.get(conflicts.get(1));
					if ((first instanceof Getter && second instanceof Setter)
							|| first instanceof Setter && second instanceof Getter) {
						continue; // Getter/setter pair, no conflict
					}
				}
				
				// Transform getters and setters back to normal methods
				for (int index : conflicts) {
					invalidateGetSet(index);
				}
			}
		}
				
		public Stream<Member> stream() {
			return members.stream()
					.filter(member -> !(member instanceof TypeDefinition));
		}
	}
	
	@Override
	public void emit(TypeDefinition node, TsEmitter out) {
		node.javadoc.ifPresent(out::javadoc);
		// Class declaration, including superclass and interfaces
		
		out.print("export class ");
		emitName(node.ref.simpleName(), node.ref, out);
		
		// We can't use TS 'implements', because TS interfaces
		// don't support e.g getters/setters
		// Instead, we translate Java implements to TS extends
		List<TypeRef> superTypes = new ArrayList<>(node.superTypes);
		superTypes.addAll(node.interfaces);
		boolean mixinTrick = false;
		if (!superTypes.isEmpty()) {
			// At least one supertype; there may be more, but we'll use mixin trick for that
			// (we still want to extend even just one type to get @inheritDoc)
			out.print(" extends %s", superTypes.get(0));
		}
		if (superTypes.size() > 1) {
			mixinTrick = true; // Trick multiple inheritance
		}
		out.println(" {");
		
		// Prepare to emit members
		Members members = new Members(node);
		members.resolveConflicts();
		
		// Emit class members with some indentation
		try (var none = out.startBlock()) {
			// TODO use stream for printing to avoid unnecessary list creation in hot path
			Stream<Member> stream = members.stream();
			out.print(stream.collect(Collectors.toList()), "\n");
		}
		
		out.println("\n}");
		
		// Emit supertypes/interfaces to interface that is merged with class
		if (mixinTrick) {
			out.print("export interface ");
			emitName(node.ref.simpleName(), node.ref, out);
			out.print(" extends ");
			out.print(superTypes, ", ");
			out.println(" {}");
		}
	}
	
	private void emitName(String name, TypeRef type, TsEmitter out) {
		// Needs specialized handling, because we DON'T (always) want package name here
		out.print(name);
		if (type instanceof TypeRef.Parametrized) {
			out.print("<").print(((TypeRef.Parametrized) type).typeParams(), ", ").print(">");
		}
	}

}
