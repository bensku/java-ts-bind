package io.github.bensku.tsbind.binding;

import java.util.ArrayList;
import java.util.List;

public class TsModule implements TsElement {

	/**
	 * Module name. Usually a Java package name, but with inner classes, might
	 * be fully qualified name of the outer class instead.
	 */
	private final String name;
	
	/**
	 * Classes in this module.
	 */
	private final List<TsClass> classes;
	
	public TsModule(String name) {
		this.name = name;
		this.classes = new ArrayList<>();
	}
	
	public void addClass(TsClass type) {
		classes.add(type);
	}

	@Override
	public void emit(TsEmitter out) {
		out.println("declare module '%s' {", name);
		classes.forEach(c -> c.emit(out)); // Don't indent classes in module
		out.println("}");
	}
}
