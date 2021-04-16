package io.github.bensku.tsbind.cli;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.beust.jcommander.JCommander;
import com.github.javaparser.JavaParser;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import io.github.bensku.tsbind.AstConsumer.Result;
import io.github.bensku.tsbind.AstGenerator;
import io.github.bensku.tsbind.SourceUnit;
import io.github.bensku.tsbind.ast.TypeDefinition;

public class BindGenApp {
	
	public static void main(String... argv) throws IOException, InterruptedException {
		// Parse command-line arguments
		Args args = new Args();
		JCommander.newBuilder().addObject(args).build().parse(argv);
		
		// Download the --artifact from Maven if provided
		Path inputPath;
		if (args.artifact != null) {
			MavenResolver resolver = new MavenResolver(args.repo);
			inputPath = resolver.downloadSources(args.artifact);
		} else {
			inputPath = args.inputPath;
		}
		
		// Create path to root of input files we have
		Path inputDir;
		if (Files.isDirectory(inputPath)) {
			inputDir = inputPath.resolve(args.offset);
		} else { // Try to open a zip file
			inputDir = FileSystems.newFileSystem(inputPath, null).getPath("/").resolve(args.offset);
		}
		
		// Prepare for AST generation
		JavaParser parser = setupParser(args.symbolSources);
		AstGenerator astGenerator = new AstGenerator(parser, args.blacklist);
		
		// Walk over input Java source files
		try (Stream<Path> files = Files.walk(inputDir)
				.filter(Files::isRegularFile)
				.filter(f -> f.getFileName().toString().endsWith(".java"))
				.filter(f -> !f.getFileName().toString().equals("package-info.java"))
				.filter(f -> isIncluded(inputDir.relativize(f).toString().replace(File.separatorChar, '.'),
						args.include, args.exclude))) {
			Map<String, TypeDefinition> types = new HashMap<>();
			files.map(path -> {
				String name = inputDir.relativize(path).toString().replace('/', '.');
				name = name.substring(0, name.length() - 5); // Strip .java
				try {
					return new SourceUnit(name, Files.readString(path));
				} catch (IOException e) {
					// TODO handle this better
					throw new RuntimeException(e);
				}
			}).map(astGenerator::parseType)
			.flatMap(Optional::stream).forEach(type -> types.put(type.name(), type));
			
			Stream<Result<String>> results = args.format.consumer.consume(types);
			results.forEach(result -> {
				try {
					Files.writeString(args.outDir.resolve(result.name), result.result);
				} catch (IOException e) {
					// TODO handle this better
					throw new RuntimeException(e);
				}
			});
		}
	}
	
	private static boolean isIncluded(String name, List<String> includes, List<String> excludes) {
		boolean include = false;
		for (String prefix : includes) {
			if (name.startsWith(prefix)) {
				include = true;
				break;
			}
		}
		if (!include) {
			return false; // Not included
		}
		for (String prefix : excludes) {
			if (name.startsWith(prefix)) {
				return false; // Included but excluded
			}
		}
		return true; // Included, not excluded
	}
	
	private static JavaParser setupParser(List<Path> symbolSources) throws IOException {
		CombinedTypeSolver typeSolver = new CombinedTypeSolver();
		typeSolver.add(new ReflectionTypeSolver());
		for (Path jar : symbolSources) {
			typeSolver.add(new JarTypeSolver(jar));
		}
		
		JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
		JavaParser parser = new JavaParser();
		parser.getParserConfiguration().setSymbolResolver(symbolSolver);
		return parser;
	}
}
