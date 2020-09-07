package io.github.bensku.tsbind.binding;

import java.util.Optional;

public class TsTypeParam {
	
	/**
	 * Name of type parameter.
	 */
	private final String name;
	
	/**
	 * Base type of this parameter (optional).
	 */
	private final Optional<TsType> extendsType;
	
	public TsTypeParam(String name, TsType extendsType) {
		this.name = name;
		this.extendsType = Optional.ofNullable(extendsType);
	}
	
	@Override
	public String toString() {
		return name + (extendsType.isPresent() ? "extends " + extendsType.orElseThrow() : "");
	}
}
