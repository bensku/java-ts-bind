package io.github.bensku.tsbind.ast;

import java.util.Objects;
import java.util.Optional;

public class TypeParam {

	public final String name;
	
	public final Optional<TypeRef> extend;
	
	public TypeParam(String name, TypeRef extend) {
		Objects.requireNonNull(name);
		this.name = name;
		this.extend = Optional.ofNullable(extend);
	}
}
