package io.github.bensku.tsbind.binding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.bensku.tsbind.AstConsumer;
import io.github.bensku.tsbind.ast.Constructor;
import io.github.bensku.tsbind.ast.Field;
import io.github.bensku.tsbind.ast.Getter;
import io.github.bensku.tsbind.ast.Member;
import io.github.bensku.tsbind.ast.Method;
import io.github.bensku.tsbind.ast.Setter;
import io.github.bensku.tsbind.ast.TypeDefinition;
import io.github.bensku.tsbind.ast.TypeRef;

/**
 * Generates TypeScript (.d.ts) declarations.
 *
 */
public class BindingGenerator implements AstConsumer<String> {

	@Override
	public Stream<Result<String>> consume(Stream<TypeDefinition> types) {
		Map<String, TsModule> modules = new HashMap<>();
		
		types.forEach(type -> addType(modules, type));
		
		// Put everything to single declaration
		return Stream.of(null);
	}
	
	private void addType(Map<String, TsModule> modules, TypeDefinition type) {
		// Generate TS class declaration
		TsType superclass = null;
		// Collect interfaces to explicitly mutable list
		List<TsType> interfaces = type.interfaces.stream().map(TsType::fromJava)
				.collect(Collectors.toCollection(ArrayList::new));
		if (type.superTypes.size() > 1) {
			// Java interfaces are generated as TS classes (TS interfaces are VERY different)
			// However, in Java interfaces can 'extend' multiple interfaces
			// In TS, we'll have to go with 'implements', even though it is a bit misleading
			interfaces.addAll(TsType.fromJava(type.superTypes));
		} else {
			superclass = TsType.fromJava(type.superTypes.get(0));
		}
		TsClass c = new TsClass(superclass, interfaces);
		
		addMembers(modules, c, type.members);
		
		// Get module for package the class is in, creating if needed
		TsModule module = modules.computeIfAbsent(getModuleName(type), TsModule::new);
		module.addClass(c);
	}
	
	private String getModuleName(Type type) {
		// All parts except the last
		return type.name.substring(0, type.name.length() - type.simpleName.length() - 1);
	}
	
	private void addMembers(Map<String, TsModule> modules, TsClass owner, List<Member> members) {
		for (Member member : members) {
			if (member instanceof Field) {
				Field field = (Field) member;
				owner.add(new TsField(field.name, TsType.fromJava(field.type)));
			} else if (member instanceof Method) {
				if (member instanceof Constructor) {
					
				} else {
					Method method = (Method) member;
					if (method.isOverride) {
						// We're just generating declarations
						// TODO what if override loosens restrictions on types?
						continue;
					}
					TsMethod.Type type;
					if (method instanceof Getter) {
						type = TsMethod.Type.GETTER;
					} else if (method instanceof Setter) {
						type = TsMethod.Type.SETTER;
					} else {
						type = method.isStatic ? TsMethod.Type.STATIC : TsMethod.Type.METHOD;
					}
					owner.add(new TsMethod(method.name, type, TsType.fromJava(method.returnType), , arguments));
				}
			} else if (member instanceof Type) {
				// TS doesn't really have inner classes like Java
				// But we can mimic them with modules!
				addType(modules, (Type) member);
			}
		}
	}

}
