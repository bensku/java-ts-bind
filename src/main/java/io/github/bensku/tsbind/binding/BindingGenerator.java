package io.github.bensku.tsbind.binding;

import java.util.stream.Stream;

import io.github.bensku.tsbind.ast.Type;

/**
 * Generates TypeScript (.d.ts) declarations.
 *
 */
public class BindingGenerator implements AstConsumer<String> {

	@Override
	public Stream<Result<String>> consume(Stream<Type> types) {
		// TODO
		
		// Put everything to single declaration
		return Stream.of(null);
	}

}
