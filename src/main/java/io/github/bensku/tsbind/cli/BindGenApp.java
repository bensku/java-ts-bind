package io.github.bensku.tsbind.cli;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.beust.jcommander.JCommander;
import com.github.javaparser.JavaParser;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import io.github.bensku.tsbind.AstConsumer.Result;
import io.github.bensku.tsbind.AstGenerator;
import io.github.bensku.tsbind.SourceUnit;
import io.github.bensku.tsbind.ast.TypeDefinition;

public class BindGenApp {
	
	public static void main(String... argv) throws IOException {
		// Parse command-line arguments
		Args args = new Args();
		JCommander.newBuilder().addObject(args).build().parse(argv);
		
		// Create path to root of input files we have
		Path inputDir;
		if (Files.isDirectory(args.inputPath)) {
			inputDir = args.inputPath.resolve(args.offset);
		} else { // Try to open a zip file
			inputDir = FileSystems.newFileSystem(args.inputPath, null).getPath("/").resolve(args.offset);
		}
		
		// Prepare for AST generation
		JavaParser parser = setupParser();
		AstGenerator astGenerator = new AstGenerator(parser);
		
		// Walk over input Java source files
		Predicate<String> included = Pattern.compile(args.include).asMatchPredicate();
		try (Stream<Path> files = Files.walk(inputDir)
				.filter(Files::isRegularFile)
				.filter(f -> f.getFileName().toString().endsWith(".java"))
				.filter(f -> !f.getFileName().toString().equals("package-info.java"))
				.filter(path -> included.test(inputDir.relativize(path).toString()))) {
			Stream<TypeDefinition> types = files.map(path -> {
				String name = inputDir.relativize(path).toString().replace('/', '.');
				name = name.substring(0, name.length() - 5); // Strip .java
				try {
					return new SourceUnit(name, Files.readString(path));
				} catch (IOException e) {
					// TODO handle this better
					throw new RuntimeException(e);
				}
			}).map(astGenerator::parseType)
			.flatMap(Optional::stream);
			
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
	
	private static JavaParser setupParser() {
		CombinedTypeSolver typeSolver = new CombinedTypeSolver();
		typeSolver.add(new ReflectionTypeSolver());
		
		JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
		JavaParser parser = new JavaParser();
		parser.getParserConfiguration().setSymbolResolver(symbolSolver);
		return parser;
	}
}
