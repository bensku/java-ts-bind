package io.github.bensku.tsbind.ast;

import java.util.Collections;
import java.util.List;

public class Setter extends Method {

	public static String setterName(String methodName) {
		if (methodName.startsWith("set")) {
			return methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
		} else {
			return methodName;
		}
	}
	
	private final String originalName;

	public Setter(String name, TypeRef type, String javadoc, boolean isPublic, boolean isStatic, boolean isOverride) {
		super(setterName(name), TypeRef.VOID, List.of(new Parameter(setterName(name), type, false)),
				Collections.emptyList(), javadoc, isPublic, isStatic, isOverride);
		this.originalName = name;
	}
	
	public String originalName() {
		return originalName;
	}
}
