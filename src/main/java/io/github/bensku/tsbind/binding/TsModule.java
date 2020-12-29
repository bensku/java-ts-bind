package io.github.bensku.tsbind.binding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.bensku.tsbind.ast.TypeDefinition;
import io.github.bensku.tsbind.ast.TypeRef;

public class TsModule {

	/**
	 * Module name. Usually a Java package name, but with inner classes, might
	 * be fully qualified name of the outer class instead.
	 */
	private final String name;
	
	/**
	 * Types in this module.
	 */
	private final List<TypeDefinition> types;
	
	public TsModule(String name) {
		this.name = name;
		this.types = new ArrayList<>();
	}
	
	public String name() {
		return name;
	}
	
	public void addType(TypeDefinition type) {
		types.add(type);
	}
	
	public void write(Map<String, TypeDefinition> typeTable, StringBuilder sb) {
		sb.append("declare module '").append(name).append("' {\n");
		
		class Import {
			/**
			 * Module name to import from.
			 */
			final String from;
			/**
			 * Simple names of types in their own module mapped to names that
			 * should be used in this module. In many cases, these are equal.
			 */
			final Map<String, String> names;
			
			Import(String from) {
				this.from = from;
				this.names = new HashMap<>();
			}
		}
		
		// Figure out type names and import declarations
		Map<TypeRef, String> typeNames = findTypeNames();
		Map<String, Import> imports = new HashMap<>();
		typeNames.forEach((type, name) -> {
			String fqn = type.name();
			String from = fqn.substring(0, fqn.lastIndexOf('.'));
			if (!this.name.equals(from)) { // Import only from other modules, not this
				imports.computeIfAbsent(from, n -> new Import(from)).names.put(type.simpleName(), name);
			}
		});
		
		// Emit import lines
		for (Import line : imports.values()) {
			sb.append("import { ");
			sb.append(line.names.entrySet().stream().map(entry -> {
				if (entry.getKey().equals(entry.getValue())) {
					return entry.getKey();
				} else {
					return entry.getKey() + " as " + entry.getValue();
				}
			}).collect(Collectors.joining(", ")));
			sb.append(" } from '").append(line.from).append("';\n");
		}
		
		// Generate classes of this module
		TsEmitter emitter = new TsEmitter("  ", typeNames, typeTable);
		types.forEach(emitter::print);
		sb.append(emitter.toString());
		
		sb.append("\n}\n");
	}
	
	private Map<TypeRef, String> findTypeNames() {
		Map<TypeRef, String> typeNames = new HashMap<>();
		Set<String> simpleNames = new HashSet<>();
		types.forEach(def -> def.walk(node -> {
			if (!(node instanceof TypeRef)) {
				return; // Not a type reference
			}
			TypeRef type = (TypeRef) node;
			type = type.baseType();
			if (BindingGenerator.EXCLUDED_TYPES.contains(type)) {
				// Excluded types are transformed to TS primitives and NOT generated
				return; // We certainly can't import them
			}
			if (!type.name().contains(".")) {
				// TODO JavaParser can distinguish T and a.b.Foo, try to use that knowledge
				return; // Generic type reference (never imported, ignore it)
			}
			if (typeNames.containsKey(type)) {
				return; // Same type used again, this is fine
			}
			
			// On name collision, fall back to fully qualified names
			String simple = type.simpleName();
			if (simpleNames.contains(simple)) {
				typeNames.put(type, type.name().replace('.', '_'));
			} else { // Otherwise, just use the simple name
				typeNames.put(type, simple);
				simpleNames.add(simple); // Reserve this simple name
			}
		}));
		return typeNames;
	}
}
