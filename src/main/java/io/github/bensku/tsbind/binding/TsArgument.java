package io.github.bensku.tsbind.binding;

public class TsArgument {

	/**
	 * Name of this argument.
	 */
	private final String name;
	
	/**
	 * Argument type.
	 */
	private final TsType type;
	
	public TsArgument(String name, TsType type) {
		this.name = name;
		this.type = type;
	}
	
	@Override
	public String toString() {
		return name + ": " + type;
	}

	
}
