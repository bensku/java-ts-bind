package io.github.bensku.tsbind;

/**
 * A single Java source file loaded to memory.
 *
 */
public class SourceUnit {

	/**
	 * Fully qualified class name.
	 */
	public final String name;
	
	/**
	 * Source code.
	 */
	public final String code;
	
	public SourceUnit(String name, String source) {
		this.name = name;
		this.code = source;
	}
}
