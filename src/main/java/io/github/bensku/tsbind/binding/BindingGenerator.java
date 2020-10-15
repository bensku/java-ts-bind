package io.github.bensku.tsbind.binding;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import io.github.bensku.tsbind.AstConsumer;
import io.github.bensku.tsbind.ast.TypeDefinition;
import io.github.bensku.tsbind.ast.TypeRef;

/**
 * Generates TypeScript (.d.ts) declarations.
 *
 */
public class BindingGenerator implements AstConsumer<String> {

	static final Set<TypeRef> EXCLUDED_TYPES = new HashSet<>();
	
	static {
		EXCLUDED_TYPES.add(TypeRef.BOOLEAN);
		EXCLUDED_TYPES.add(TypeRef.BYTE);
		EXCLUDED_TYPES.add(TypeRef.SHORT);
		EXCLUDED_TYPES.add(TypeRef.CHAR);
		EXCLUDED_TYPES.add(TypeRef.INT);
		EXCLUDED_TYPES.add(TypeRef.LONG);
		EXCLUDED_TYPES.add(TypeRef.FLOAT);
		EXCLUDED_TYPES.add(TypeRef.DOUBLE);
		EXCLUDED_TYPES.add(TypeRef.STRING);
		EXCLUDED_TYPES.add(TypeRef.OBJECT);
	}
	
	@Override
	public Stream<Result<String>> consume(Stream<TypeDefinition> types) {
		Map<String, TsModule> modules = new HashMap<>();
		
		types.forEach(type -> addType(modules, type));
		
		// Put modules in declarations based on their base packages (tld.domain)
		Map<String, StringBuilder> outputs = new HashMap<>();
		for (TsModule module : modules.values()) {
			String basePkg = getBasePkg(module.name()).replace('.', '_');
			StringBuilder out = outputs.computeIfAbsent(basePkg, key -> new StringBuilder());
			module.write(out);
		}
		return outputs.entrySet().stream().map(entry
				-> new Result<>(entry.getKey() + ".d.ts", entry.getValue().toString()));
	}
	
	private String getBasePkg(String name) {
		int tld = name.indexOf('.');
		if (tld == -1) {
			return name; // Default package?
		}
		int domain = name.indexOf('.', tld + 1);
		if (domain == -1) {
			return name; // Already base package
		}
		return name.substring(0, domain);
	}
	
	private void addType(Map<String, TsModule> modules, TypeDefinition type) {
		if (EXCLUDED_TYPES.contains(type.ref)) {
			return; // Don't generate this type
		}
		
		// Get module for package the class is in, creating if needed
		modules.computeIfAbsent(getModuleName(type.ref), TsModule::new).addType(type);
		
		// Fake inner classes with TS modules
		// Nested types in TS are quite different from Java, so we can't use them
		type.members.stream().filter(member -> (member instanceof TypeDefinition))
				.forEach(innerType -> addType(modules, (TypeDefinition) innerType));
	}
	
	private String getModuleName(TypeRef type) {
		// All parts except the last
		return type.name().substring(0, type.name().length() - type.simpleName().length() - 1);
	}

}
