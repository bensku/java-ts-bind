package io.github.bensku.tsbind.cli;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import io.github.bensku.tsbind.AstConsumer.Result;
import io.github.bensku.tsbind.AstGenerator;
import io.github.bensku.tsbind.SourceUnit;
import io.github.bensku.tsbind.ast.TypeDefinition;
import io.github.bensku.tsbind.binding.EarlyTypeTransformer;

public class BindGenApp {
	
	public static void main(String... argv) throws IOException, InterruptedException {
		// Parse command-line arguments
		Args args = new Args();
		JCommander.newBuilder().addObject(args).build().parse(argv);
		
		if (args.packageJson != null) {
			args = new GsonBuilder()
					.registerTypeAdapter(Path.class, new TypeAdapter<Path>() {

						@Override
						public void write(JsonWriter out, Path value) throws IOException {
							out.value(value.toString());
						}

						@Override
						public Path read(JsonReader in) throws IOException {
							return Path.of(in.nextString());
						}
					})
					.create()
					.fromJson(Files.readString(args.packageJson), PackageJson.class).tsbindOptions;
			if (args == null) {
				throw new IllegalArgumentException("missing tsbindOptions in --packageJson");
			}
		}
		
		// Download the --artifact from Maven if provided
		List<Path> inputPaths;
		if (!args.artifacts.isEmpty()) {
			MavenResolver resolver = new MavenResolver(Files.createTempDirectory("tsbind"), args.repos);
			args.repos.add("https://repo1.maven.org/maven2"); // Maven central as last resort
			
			// Add all artifacts to input paths and symbols
			inputPaths = new ArrayList<>();
			for (String artifact : args.artifacts) {
				System.out.println("Resolving Maven artifact " + artifact);
				MavenResolver.ArtifactResults results = resolver.downloadArtifacts(artifact, true);
				inputPaths.add(results.sourceJar);
				args.symbols.addAll(results.symbols);
			}
		} else {
			inputPaths = args.in;
		}
		System.out.println("Generating types for " + inputPaths + " to " + args.out);
		
		// Prepare for AST generation
		JavaParser parser = setupParser(args.symbols);
		AstGenerator astGenerator = new AstGenerator(parser, args.blacklist);
		
		// Walk over input Java source files
		String offset = args.offset;
		List<String> include = args.include;
		List<String> exclude = args.exclude;
		Path outDir = args.out;
		
		try (Stream<Path> files = inputPaths.stream().map(t -> {
				if (Files.isDirectory(t)) {
					return t;
				} else {
					// Path should be zip file, access it as directory
					try {
						return FileSystems.newFileSystem(t, (ClassLoader) null).getPath("/");
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			}).map(t -> t.resolve(offset)) // Apply offset argument
			.flatMap(t -> {
				// Walk all files from ALL inputs
				try {
					// Filter here, because we need to relativize to each input directory
					return Files.walk(t)
							.filter(f -> isIncluded(t.relativize(f).toString().replace(File.separatorChar, '.'),
									include, exclude));
				} catch (IOException e) {
					throw new RuntimeException(e); // Thanks, Java
				}
			})
			// Filter out files that are not likely contain Java source code
			.filter(Files::isRegularFile)
			.filter(f -> f.getFileName().toString().endsWith(".java"))
			.filter(f -> !f.getFileName().toString().equals("package-info.java"))) {
			Map<String, TypeDefinition> types = new HashMap<>();
			files.map(path -> {
				try {
					return new SourceUnit(path.toString(), Files.readString(path));
				} catch (IOException e) {
					// TODO handle this better
					throw new RuntimeException(e);
				}
			}).map(astGenerator::parseType)
			.flatMap(Optional::stream).forEach(type -> {
				System.out.println("Parsed type " + type.name());
				types.put(type.name(), type);
			});
			
			// Apply early transformation passes that need all types
			EarlyTypeTransformer earlyTransform = new EarlyTypeTransformer(types);
			for (TypeDefinition def : types.values()) {
				earlyTransform.addMissingOverloads(def);
			}
			
			Stream<Result<String>> results = args.format.consumerSource.apply(args)
					.consume(types);
			results.forEach(result -> {
				System.out.println("Writing module " + result.name);
				try {
					Files.writeString(outDir.resolve(result.name), result.result);
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
