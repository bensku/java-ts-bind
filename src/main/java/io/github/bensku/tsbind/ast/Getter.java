package io.github.bensku.tsbind.ast;

import java.util.Collections;

public class Getter extends Method {
	
	public static String getterName(String methodName) {
		if (methodName.startsWith("is")) {
			return methodName.substring(2, 3).toLowerCase() + methodName.substring(3);
		} else {
			return methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
		}
	}
	
	public final String originalName;

	public Getter(String name, TypeRef type, String javadoc, boolean isStatic, boolean isOverride) {
		super(getterName(name), type, Collections.emptyList(), Collections.emptyList(), javadoc, isStatic, isOverride);
		this.originalName = name;
	}

}
