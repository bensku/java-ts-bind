package io.github.bensku.tsbind.binding;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TsModule {

	/**
	 * Module name. Usually a Java package name, but with inner classes, might
	 * be fully qualified name of the outer class instead.
	 */
	private final String name;
	
	/**
	 * Emitter used for content of this module.
	 */
	private final TsEmitter emitter;
	
	public TsModule(String name) {
		this.name = name;
		this.emitter = new TsEmitter("  ");
	}
	
	public String name() {
		return name;
	}
	
	public TsEmitter emitter() {
		return emitter;
	}
	
	public void write(StringBuilder sb) {
		sb.append("declare module '").append(name).append("' {\n");
		
		class Import {
			final String from;
			final Set<String> names;
			
			Import(String from) {
				this.from = from;
				this.names = new HashSet<>();
			}
		}
		
		// Figure out import declarations
		Map<String, Import> imports = new HashMap<>();
		for (String fqn : emitter.imports()) {
			String from = fqn.substring(0, fqn.lastIndexOf('.'));
			String simpleName = fqn.substring(from.length() + 1);
			imports.computeIfAbsent(from, n -> new Import(from)).names.add(simpleName);
		}
		
		// Emit import lines
		for (Import line : imports.values()) {
			sb.append("import { ");
			String prefix = line.from.replace('.', '_') + "_";
			sb.append(line.names.stream().map(name -> name + " as " + prefix + name)
					.collect(Collectors.joining(", ")));
			sb.append(" } from '").append(line.from).append("';\n");
		}
		
		// Append already generated classes etc.
		sb.append(emitter.toString());
		
		sb.append("\n}\n");
	}
}
