package io.github.bensku.tsbind.binding;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TsClass implements TsElement {

	/**
	 * Member fields and methods of this class.
	 */
	private final List<TsMember> members;
	
	/**
	 * Superclass in TypeScript.
	 */
	private final Optional<TsType> superclass;
	
	/**
	 * Interfaces in TypeScript. While all Java interfaces are declared as
	 * TS classes, they are always used with 'implements' - even when
	 * 'extends' would be used for interfaces extending each other in Java.
	 */
	private final List<TsType> interfaces;
	
	/**
	 * Type parameters.
	 */
	private final List<TsTypeParam> typeParams;
	
	public TsClass(TsType superclass, List<TsType> interfaces, List<TsTypeParam> typeParams) {
		this.members = new ArrayList<>();
		this.superclass = Optional.ofNullable(superclass);
		this.interfaces = interfaces;
		this.typeParams = typeParams;
	}
	
	public void add(TsMember member) {
		members.add(member);
	}
	
	@Override
	public void emit(TsEmitter out) {
		// Class declaration, including superclass and interfaces
		out.print("declare export class %s ");
		superclass.ifPresent(sup -> out.print("extends %s ", sup));
		if (!interfaces.isEmpty()) {
			out.print("implements %s ", interfaces.stream().map(TsType::toString).collect(Collectors.joining(", ")));
		}
		out.println("{");
		
		// Emit class members with some indentation
		try (var none = out.indent()) {
			members.forEach(member -> member.emit(out));
		}
		out.println("}");
	}

}
