package io.github.bensku.tsbind.binding;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import io.github.bensku.tsbind.AstConsumer;
import io.github.bensku.tsbind.ast.Type;

/**
 * Generates TypeScript (.d.ts) declarations.
 *
 */
public class BindingGenerator implements AstConsumer<String> {

	@Override
	public Stream<Result<String>> consume(Stream<Type> types) {
		Map<String, TsModule> modules = new HashMap<>();
		
		types.forEach(type -> {
			// Generate TS class declaration
			// TODO Java-side superclass and interface support
			TsClass c = new TsClass(superclass, interfaces);
			
			TsModule module = modules.computeIfAbsent(moduleName(type), TsModule::new);
			module.addClass(c);
		});
		
		// Put everything to single declaration
		return Stream.of(null);
	}
	
	private String moduleName(Type type) {
		// All parts except the last
		return type.name.substring(0, type.name.length() - type.simpleName.length() - 1);
	}

}
