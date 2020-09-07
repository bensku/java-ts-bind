package io.github.bensku.tsbind.binding;

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
	
	private final Indenter indenter;
	
	public TsEmitter(String indentation) {
		this.output = new StringBuilder();
		this.indentation = indentation;
		this.indenter = new Indenter();
	}
	
	public Indenter indent() {
		indentLevel++;
		return indenter;
	}
	
	public TsEmitter print(String str) {
		output.append(indentation.repeat(indentLevel)).append(str);
		return this;
	}
	
	public TsEmitter print(String fmt, Object... args) {
		print(String.format(fmt, args));
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
	
	@Override
	public String toString() {
		return output.toString();
	}
}
