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
		out.print("%s: %s;", node.name, node.type);
	};
	
	public static final TsGenerator<Parameter> PARAMETER = (node, out) -> {
		out.print("%s: %s", node.name, node.type);
	};
	
	public static final TsGenerator<Method> METHOD = (node, out) -> {
		if (node.isOverride && !node.javadoc.isPresent()) {
			return; // Method inherited, no need to specify again
		}
		
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
		out.indent().print("get %s(): %s;", node.name, node.returnType);
	};
	
	public static final TsGenerator<Setter> SETTER = (node, out) -> {
		node.javadoc.ifPresent(out::javadoc);
		out.indent().print("set %s(%s): void;", node.name, node.params.get(0));
	};
}
