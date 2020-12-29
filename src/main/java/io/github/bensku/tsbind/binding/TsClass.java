package io.github.bensku.tsbind.binding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
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
	
	private static class MethodId {
		String name;
		List<String> paramTypes;
		
		MethodId(Method method) {
			this.name = method.name();
			this.paramTypes = method.params.stream().map(param -> param.type)
					.map(type -> TsTypes.primitiveName(type).orElse(type.name()))
					.collect(Collectors.toList());
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, paramTypes);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			MethodId other = (MethodId) obj;
			return Objects.equals(name, other.name) && Objects.equals(paramTypes, other.paramTypes);
		}
	}
	
	private static class Members {
		
		private final TypeDefinition type;
		private final List<Member> members;
		
		/**
		 * Not for printing, we just need to access some data.
		 */
		private final TsEmitter emitter;
		
		public Members(TypeDefinition type, TsEmitter emitter) {
			this.type = type;
			this.members = type.members;
			this.emitter = emitter;
		}
		
		private void visitSupertypes(TypeDefinition type, Consumer<TypeDefinition> visitor) {
			// Call visitor only on supertypes, not the type initially given as parameter
			for (TypeRef ref : type.superTypes) {
				Optional<TypeDefinition> def = emitter.resolveType(ref);
				def.ifPresent(d -> {
					visitor.accept(d);
					visitSupertypes(d, visitor);
				});
			}
			for (TypeRef ref : type.interfaces) {
				Optional<TypeDefinition> def = emitter.resolveType(ref);
				def.ifPresent(d -> {
					visitor.accept(d);
					visitSupertypes(d, visitor);
				});
			}
		}
		
		/**
		 * TypeScript removes inherited overloads unless they're re-specified.
		 * We do exactly that.
		 */
		public void addMissingOverloads() {
			// Figure out what methods we already have
			Set<MethodId> methods = new HashSet<>();
			for (Member member : members) {
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
							members.add(member);
						}
					}
				}
			});
		}

		
		/**
		 * Resolves the type which might contain the overridden method.
		 * @param type Root type.
		 * @param method Overriding method.
		 * @return Type definition, if found.
		 */
		private Optional<TypeDefinition> resolveOverrideSource(TypeRef type, Method method) {
			Optional<TypeDefinition> opt = emitter.resolveType(type);
			if (opt.isEmpty()) {
				return Optional.empty(); // Nothing here...
			}
			TypeDefinition def = opt.orElseThrow();
			
			// Check if type we're checking now has it
			if (def.hasMember(method.name())) {
				return Optional.of(def);
			}
			
			// No? Recursively check supertypes and interfaces, maybe they have it
			for (TypeRef parent : def.superTypes) {
				Optional<TypeDefinition> result = resolveOverrideSource(parent, method);
				if (result.isPresent()) {
					return result;
				}
			}
			for (TypeRef parent : def.interfaces) {
				Optional<TypeDefinition> result = resolveOverrideSource(parent, method);
				if (result.isPresent()) {
					return result;
				}
			}
			return Optional.empty(); // Didn't find it
		}
				
		/**
		 * Finds an interface method that the given method overrides.
		 * @param member Method to find overrides for.
		 * @return Overridden member, if found.
		 */
		private Optional<Member> resolveInterfaceOverride(Method method) {
			if (!method.isOverride) {
				return Optional.empty();
			}
			// Don't iterate over supertypes, only interfaces requested
			for (TypeRef parent : type.interfaces) {
				Optional<TypeDefinition> result = resolveOverrideSource(parent, method);
				if (result.isPresent()) {
					for (Member m : result.get().members) {
						if (m.getClass().equals(method.getClass()) && m.name().equals(method.name())) {
							return Optional.of(m); // Same name, same type -> found it!
						}
					}
				}
			}
			return Optional.empty();
		}
		
		/**
		 * Manually copy inherited Javadoc from superclasses that our class
		 * (not interface) can't extend.
		 */
		public void fixInheritDoc() {
			// TODO Javadoc with overrides of overrides
			for (Member member : members) {
				if (!member.isStatic && member instanceof Method && member.javadoc.isEmpty()) {
					resolveInterfaceOverride((Method) member).ifPresent(override -> {
						override.javadoc.ifPresent(doc -> member.javadoc = Optional.of(doc));
					});
				}
			}
		}
		
		/**
		 * Many Java types are emitted as 'number', which can cause strange
		 * duplicates to appear in TS types. This pass removes them.
		 */
		public void removeDuplicates() {
			Set<MethodId> methods = new HashSet<>();
			Iterator<Member> it = members.iterator();
			while (it.hasNext()) {
				Member member = it.next();
				if (member instanceof Method) {
					MethodId id = new MethodId((Method) member);
					if (methods.contains(id)) {
						it.remove(); // Duplicate, remove it
					} else {
						methods.add(id); // First occurrance
					}
				}
			}
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
				// Do not touch other kinds of conflicts - overloaded normal methods are ok
				
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
		
		// If this is Iterable, make types extending this iterable
		if (node.name().equals("java.lang.Iterable")) {
			out.println("  [Symbol.iterator](): globalThis.Iterator<T>;");
		}
		
		// Prepare to emit members
		Members members = new Members(node, out);
		members.addMissingOverloads();
		members.fixInheritDoc();
		members.removeDuplicates();
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
