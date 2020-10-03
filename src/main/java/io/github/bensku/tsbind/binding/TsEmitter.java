package io.github.bensku.tsbind.binding;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.bensku.tsbind.ast.AstNode;
import io.github.bensku.tsbind.ast.Constructor;
import io.github.bensku.tsbind.ast.Field;
import io.github.bensku.tsbind.ast.Getter;
import io.github.bensku.tsbind.ast.Method;
import io.github.bensku.tsbind.ast.Parameter;
import io.github.bensku.tsbind.ast.Setter;
import io.github.bensku.tsbind.ast.TypeDefinition;
import io.github.bensku.tsbind.ast.TypeRef;

public class TsEmitter {
	
	/**
	 * Output string (builder).
	 */
	private final StringBuilder output;
	
	/**
	 * String to be repeated once per indentation level at start of each line.
	 */
	private final String indentation;
	
	/**
	 * Indentation level.
	 */
	private int indentLevel;
	
	public class Indenter implements AutoCloseable {

		@Override
		public void close() {
			indentLevel--;
		}
		
	}
	
	/**
	 * Used for try-with-resources indentation.
	 */
	private final Indenter indenter;
	
	/**
	 * Java imports in classes 
	 */
	private final Set<String> imports;
	
	/**
	 * Code generators for different AST node types.
	 */
	private final Map<Class<?>, TsGenerator<?>> generators;
	
	public TsEmitter(String indentation) {
		this.output = new StringBuilder();
		this.indentation = indentation;
		this.indenter = new Indenter();
		this.imports = new HashSet<>();
		this.generators = new HashMap<>();
		registerGenerators();
	}
	
	private <T extends AstNode> void addGenerator(Class<T> type, TsGenerator<T> generator) {
		generators.put(type, generator);
	}
	
	private void registerGenerators() {
		addGenerator(TypeDefinition.class, TsClass.INSTANCE);
		addGenerator(TypeRef.Simple.class, TsTypes.SIMPLE);
		addGenerator(TypeRef.Wildcard.class, TsTypes.WILDCARD);
		addGenerator(TypeRef.Parametrized.class, TsTypes.PARAMETRIZED);
		addGenerator(TypeRef.Array.class, TsTypes.ARRAY);
		
		addGenerator(Field.class, TsMembers.FIELD);
		addGenerator(Parameter.class, TsMembers.PARAMETER);
		addGenerator(Method.class, TsMembers.METHOD);
		addGenerator(Constructor.class, TsMembers.CONSTRUCTOR);
		addGenerator(Getter.class, TsMembers.GETTER);
		addGenerator(Setter.class, TsMembers.SETTER);
	}
	
	public Indenter indent() {
		indentLevel++;
		return indenter;
	}
	
	public Set<String> imports() {
		return imports;
	}
	
	public TsEmitter print(String str) {
		output.append(str);
		return this;
	}
	
	public TsEmitter print(String fmt, Object... args) {
		int start = 0;
		int next = -2;
		for (int i = 0; (next = fmt.indexOf("%s", start)) >= 0; i++) {
			output.append(fmt.substring(start, next));
			start = next + 2;
			Object arg = args[i];
			if (arg instanceof AstNode) {
				print((AstNode) arg);
			} else {
				output.append(arg);
			}
		}
		output.append(fmt.substring(start));
		return this;
	}
	
	public TsEmitter println(String str) {
		print(str);
		output.append('\n');
		return this;
	}
	
	public TsEmitter println(String fmt, Object... args) {
		print(fmt, args);
		output.append('\n');
		return this;
	}
	
	public TsEmitter print(AstNode node) {
		@SuppressWarnings("unchecked") // addGenerator is type-safe
		TsGenerator<AstNode> generator = (TsGenerator<AstNode>) generators.get(node.getClass());
		if (generator == null) {
			throw new UnsupportedOperationException("unsupported node type " + node.getClass());
		}
		generator.emit(node, this);
		return this;
	}
	
	public TsEmitter println(AstNode node) {
		output.append(indentation.repeat(indentLevel));
		print(node);
		output.append('\n');
		return this;
	}
	
	public TsEmitter print(List<? extends AstNode> list, String delimiter) {
		boolean indent = delimiter.equals("\n");
		for (int i = 0; i < list.size() - 1; i++) {
			if (indent) {
				output.append(indentation.repeat(indentLevel));
			}
			print(list.get(i));
			output.append(delimiter);
		}
		if (!list.isEmpty()) {
			if (indent) {
				output.append(indentation.repeat(indentLevel));
			}
			print(list.get(list.size() - 1));
		}
		return this;
	}
	
	@Override
	public String toString() {
		return output.toString();
	}
}
