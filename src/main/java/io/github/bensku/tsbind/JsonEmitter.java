package io.github.bensku.tsbind;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.github.bensku.tsbind.ast.TypeDefinition;

/**
 * Dumps AST to JSON string.
 *
 */
public class JsonEmitter implements AstConsumer<String> {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	
	@Override
	public Stream<Result<String>> consume(Stream<TypeDefinition> types) {
		return Stream.of(new Result<>("dump.json", GSON.toJson(types.collect(Collectors.toList()))));
	}

}
