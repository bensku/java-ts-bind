package io.github.bensku.tsbind.ast;

import java.util.Collections;

public class Setter extends Method {

	public static String setterName(String methodName) {
		return methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
	}
	
	public final String originalName;

	public Setter(String name, TypeRef type, String javadoc, boolean isOverride) {
		super(setterName(name), TypeRef.VOID, Collections.singletonList(new Parameter(setterName(name), type)),
				Collections.emptyList(), javadoc, false, isOverride);
		this.originalName = name;
	}
}
