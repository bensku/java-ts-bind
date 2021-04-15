package io.github.bensku.tsbind.binding;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jsoup.Jsoup;

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
	
	/**
	 * Cached current indentation (for performance).
	 */
	private String indentStr;
	
	public class Indenter implements AutoCloseable {

		@Override
		public void close() {
			indentLevel--;
			indentStr = indentation.repeat(indentLevel);
		}
		
	}
	
	/**
	 * Used for try-with-resources indentation.
	 */
	private final Indenter indenter;
	
	/**
	 * Names of Java types in currently emitted module.
	 */
	private final Map<TypeRef, String> typeNames;
	
	/**
	 * Code generators for different AST node types.
	 */
	private final Map<Class<?>, TsGenerator<?>> generators;
	
	/**
	 * Type lookup table.
	 */
	private final Map<String, TypeDefinition> types;
	
	public TsEmitter(String indentation, Map<TypeRef, String> typeNames, Map<String, TypeDefinition> types) {
		this.output = new StringBuilder();
		this.indentation = indentation;
		this.indenter = new Indenter();
		this.indentStr = "";
		this.typeNames = typeNames;
		this.generators = new HashMap<>();
		this.types = types;
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
		addGenerator(TypeRef.Nullable.class, TsTypes.NULLABLE);
		
		addGenerator(Field.class, TsMembers.FIELD);
		addGenerator(Parameter.class, TsMembers.PARAMETER);
		addGenerator(Method.class, TsMembers.METHOD);
		addGenerator(Constructor.class, TsMembers.CONSTRUCTOR);
		addGenerator(Getter.class, TsMembers.GETTER);
		addGenerator(Setter.class, TsMembers.SETTER);
	}
	
	public Optional<TypeDefinition> resolveType(TypeRef ref) {
		// TODO inner class support
		return Optional.ofNullable(types.get(ref.name()));
	}
	
	public Indenter startBlock() {
		indentLevel++;
		indentStr = indentation.repeat(indentLevel);
		return indenter;
	}
	
	public TsEmitter print(String str) {
		output.append(str);
		return this;
	}
	
	public TsEmitter indent() {
		print(indentStr);
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
		print(indentStr).print(node).print("\n");
		return this;
	}
	
	public TsEmitter print(List<? extends AstNode> list, String delimiter) {
		for (int i = 0; i < list.size() - 1; i++) {
			print(list.get(i));
			output.append(delimiter);
		}
		if (!list.isEmpty()) {
			print(list.get(list.size() - 1));
		}
		return this;
	}
	
	private String processJavadoc(String doc) {
		// Strip HTML out; TODO markdown generation
		String text = Jsoup.parseBodyFragment(doc).wholeText()
				.replace("*/", "* /"); // No surprise Javadoc ends
		
		// Replace {@code ...} with Markdown code blocks
		StringBuilder sb = new StringBuilder();
		int start = 0;
		int end = -1;
		while (true) {
			start = text.indexOf("{@code", end + 1);
			if (start == -1) {
				break;
			}
			sb.append(text.substring(end + 1, start)); // Text between previous and this code block
			int codeStart = start + "{@code".length() + 1;
			end = text.indexOf('}', codeStart);
			String code = text.substring(codeStart, end);
			if (code.contains("\n")) {
				// Multiline code blocks are not yet supported
				// (it can be difficult to detect the ending })
				sb.append(text.substring(start, end + 1));
			} else {
				sb.append('`').append(code).append('`');
			}
		}
		sb.append(text.substring(end + 1)); // Rest of Javadoc
		
		return sb.toString();
	}
	
	private void javadocContent(String line) {
		indent().print(" ").println(line);
	}
	
	public TsEmitter javadoc(String doc) {
		doc = processJavadoc(doc);
		indent().println("/**");
		doc.lines().map(String::stripLeading).filter(line -> !line.isEmpty()).forEach(this::javadocContent);
		indent().println("*/");
		return this;
	}
	
	public TsEmitter printType(TypeRef type) {
		String name = typeNames.get(type);
		print(name != null ? name : type.name().replace('.', '_'));
		return this;
	}
	
	@Override
	public String toString() {
		return output.toString();
	}
}
