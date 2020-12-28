package io.github.bensku.tsbind.binding;

import io.github.bensku.tsbind.ast.Constructor;
import io.github.bensku.tsbind.ast.Field;
import io.github.bensku.tsbind.ast.Getter;
import io.github.bensku.tsbind.ast.Method;
import io.github.bensku.tsbind.ast.Parameter;
import io.github.bensku.tsbind.ast.Setter;

/**
 * Code generators for different types of class members
 * and simple types needed by them.
 *
 */
public class TsMembers {

	public static final TsGenerator<Field> FIELD = (node, out) -> {
		node.javadoc.ifPresent(out::javadoc);
		out.indent();
		if (node.isStatic) {
			out.print("static ");
		}
		if (node.isFinal) {
			out.print("readonly ");
		}
		out.print("%s: %s;", node.name, node.type);
	};
	
	public static final TsGenerator<Parameter> PARAMETER = (node, out) -> {
		String name;
		// Replace TS/JS reserved keywords that are common parameter names in Java
		switch (node.name) {
			case "function":
				name = "func";
				break;
			case "in":
				name = "in_";
				break;
			case "default":
				name = "default_";
				break;
			case "with":
				name = "with_";
				break;
			default:
				name = node.name;
		}
		if (node.varargs) { // For varargs array, add ...
			out.print("...%s: %s", name, node.type);
		} else {
			out.print("%s: %s", name, node.type);
		}
	};
	
	public static final TsGenerator<Method> METHOD = (node, out) -> {
		node.javadoc.ifPresent(out::javadoc);
		out.indent();
		if (node.isStatic) { // 'static' modified is roughly same in TS and Java
			out.print("static ");
		}
		
		out.print(node.name); // Method name
		// Type parameters go in different place compared to Java
		if (!node.typeParams.isEmpty()) {
			out.print("<");
			out.print(node.typeParams, ", ");
			out.print(">");
		}
		
		// Method parameters (if any)
		out.print("(");
		out.print(node.params, ", ");
		out.print("): ");
		
		out.print(node.returnType).print(";"); // Return type
	};
	
	public static final TsGenerator<Constructor> CONSTRUCTOR = (node, out) -> {
		node.javadoc.ifPresent(out::javadoc);
		out.indent().print("constructor(").print(node.params, ", ").print(");");
	};
	
	public static final TsGenerator<Getter> GETTER = (node, out) -> {
		node.javadoc.ifPresent(out::javadoc);
		out.indent();
		if (node.isStatic) {
			out.print("static ");
		}
		out.print("get %s(): %s;", node.name, node.returnType);
	};
	
	public static final TsGenerator<Setter> SETTER = (node, out) -> {
		node.javadoc.ifPresent(out::javadoc);
		out.indent();
		if (node.isStatic) {
			out.print("static ");
		}
		out.print("set %s(%s);", node.name, node.params.get(0));
	};
}
