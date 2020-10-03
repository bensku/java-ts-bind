package io.github.bensku.tsbind.binding;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import io.github.bensku.tsbind.AstConsumer;
import io.github.bensku.tsbind.ast.TypeDefinition;
import io.github.bensku.tsbind.ast.TypeRef;

/**
 * Generates TypeScript (.d.ts) declarations.
 *
 */
public class BindingGenerator implements AstConsumer<String> {

	@Override
	public Stream<Result<String>> consume(Stream<TypeDefinition> types) {
		Map<String, TsModule> modules = new HashMap<>();
		
		types.forEach(type -> addType(modules, type));
		
		// Put everything to single declaration
		StringBuilder sb = new StringBuilder();
		for (TsModule module : modules.values()) {
			module.write(sb);
		}
		return Stream.of(new Result<>("types.d.ts", sb.toString()));
	}
	
	private void addType(Map<String, TsModule> modules, TypeDefinition type) {
		// Get module for package the class is in, creating if needed
		TsModule module = modules.computeIfAbsent(getModuleName(type.ref), TsModule::new);
		module.emitter().print(type);
	}
	
	private String getModuleName(TypeRef type) {
		// All parts except the last
		return type.name().substring(0, type.name().length() - type.simpleName().length() - 1);
	}

}
