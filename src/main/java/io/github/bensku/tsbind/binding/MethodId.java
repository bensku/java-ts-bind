package io.github.bensku.tsbind.binding;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import io.github.bensku.tsbind.ast.Method;

public class MethodId {
	String name;
	boolean isPublic;
	List<String> paramTypes;
	
	MethodId(Method method) {
		this.name = method.name();
		this.isPublic = method.isPublic;
		this.paramTypes = method.params.stream().map(param -> param.type)
				.map(type -> TsTypes.primitiveName(type).orElse(type.name()))
				.collect(Collectors.toList());
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, isPublic, paramTypes);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		MethodId other = (MethodId) obj;
		return Objects.equals(name, other.name) && Objects.equals(isPublic, other.isPublic)
				&& Objects.equals(paramTypes, other.paramTypes);
	}
}
