package io.github.bensku.tsbind.binding;

import java.util.Optional;

import io.github.bensku.tsbind.ast.TypeParam;

public class TsTypeParam {
	
	public static TsTypeParam fromJava(TypeParam param) {
		return new TsTypeParam(param.name, param.extend.map(TsType::fromJava).orElse(null));
	}
	
	/**
	 * Name of type parameter.
	 */
	private final String name;
	
	/**
	 * Base type of this parameter (optional).
	 */
	private final Optional<TsType> extendsType;
	
	private TsTypeParam(String name, TsType extendsType) {
		this.name = name;
		this.extendsType = Optional.ofNullable(extendsType);
	}
	
	@Override
	public String toString() {
		return name + (extendsType.isPresent() ? "extends " + extendsType.orElseThrow() : "");
	}
}
