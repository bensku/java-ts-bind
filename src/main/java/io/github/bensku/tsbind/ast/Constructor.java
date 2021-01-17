package io.github.bensku.tsbind.ast;

import java.util.Collections;
import java.util.List;

public class Constructor extends Method {

	public Constructor(String name, List<Parameter> params, String javadoc, boolean isPublic) {
		super(name, TypeRef.VOID, params, Collections.emptyList(), javadoc, isPublic, false, false);
	}

	
}
