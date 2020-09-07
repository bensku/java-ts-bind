package io.github.bensku.tsbind.binding;

import java.util.List;
import java.util.stream.Collectors;

public class TsMethod extends TsMember {

	enum Type {
		STATIC,
		METHOD,
		GETTER,
		SETTER
	}
	
	/**
	 * Method type.
	 */
	private final Type methodType;
	
	/**
	 * Return type of method.
	 */
	private final TsType returnType;
	
	/**
	 * Type parameters of method.
	 */
	private final List<TsTypeParam> typeParams;
	
	/**
	 * Argument names and types.
	 */
	private final List<TsArgument> arguments;
	
	public TsMethod(String name, Type methodType, TsType returnType, List<TsTypeParam> typeParams, List<TsArgument> arguments) {
		super(name);
		this.methodType = methodType;
		this.returnType = returnType;
		this.typeParams = typeParams;
		this.arguments = arguments;
	}

	@Override
	public void emit(TsEmitter out) {
		// Add prefix for declaration if method type needs it
		switch (methodType) {
		case STATIC:
			out.print("static ");
			break;
		case GETTER:
			out.print("get ");
			break;
		case SETTER:
			out.print("set ");
			break;
		default:
			break;
		}
		
		out.print(name); // Method (or getter, setter, whatever) name
		if (!typeParams.isEmpty()) { // Need to emit some type parameters for method!
			out.print("<%s>", typeParams.stream().map(TsTypeParam::toString).collect(Collectors.joining(", ")));
		}
		
		// Arguments (names, types) and return type
		out.println("(%s): %s;", arguments.stream().map(TsArgument::toString)
				.collect(Collectors.joining(", ")), returnType);
	}

}
