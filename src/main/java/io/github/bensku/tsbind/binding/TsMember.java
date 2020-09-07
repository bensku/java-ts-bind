package io.github.bensku.tsbind.binding;

public abstract class TsMember implements TsElement {

	/**
	 * Name of this class member
	 */
	protected final String name;
	
	protected TsMember(String name) {
		this.name = name;
	}
}
