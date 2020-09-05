package io.github.bensku.tsbind.ast;

import java.util.List;

public class Constructor extends Method {

	public Constructor(String name, TypeRef returnType, List<Parameter> params, List<TypeParam> typeParams,
			String javadoc) {
		super(name, returnType, params, typeParams, javadoc, false);
	}

	
}
