package io.github.bensku.tsbind.ast;

import java.util.Collections;
import java.util.List;

public class Constructor extends Method {

	public Constructor(String name, TypeRef returnType, List<Parameter> params, String javadoc) {
		super(name, returnType, params, Collections.emptyList(), javadoc, false, false);
	}

	
}
