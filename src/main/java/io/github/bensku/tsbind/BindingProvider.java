package io.github.bensku.tsbind;

import java.nio.file.Path;

import io.github.bensku.tsbind.ast.Type;

public interface BindingProvider {

	void add(Type type);
	
	void generate(Path dir);
}
