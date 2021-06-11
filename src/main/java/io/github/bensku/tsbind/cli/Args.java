package io.github.bensku.tsbind.cli;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;
import com.google.common.base.Function;

import io.github.bensku.tsbind.AstConsumer;
import io.github.bensku.tsbind.JsonEmitter;
import io.github.bensku.tsbind.binding.BindingGenerator;

public class Args {

	public enum OutputFormat {
		JSON((args) -> new JsonEmitter()),
		TS_TYPES((args) -> new BindingGenerator(args.index));
		
		public final Function<Args, AstConsumer<String>> consumerSource;
		
		OutputFormat(Function<Args, AstConsumer<String>> consumer) {
			this.consumerSource = consumer;
		}
	}
	
	@Parameter(names = "--format")
	public OutputFormat format = OutputFormat.TS_TYPES;
	
	@Parameter(names = "--in")
	public List<Path> in;
	
	@Parameter(names = "--symbols")
	public List<Path> symbols = new ArrayList<>();
	
	@Parameter(names = "--repo")
	public List<String> repos = new ArrayList<>();

	@Parameter(names = "--artifact")
	public List<String> artifacts = new ArrayList<>();
	
	@Parameter(names = "--offset")
	public String offset = "";
	
	@Parameter(names = "--include")
	public List<String> include = List.of("");
	
	@Parameter(names = "--exclude")
	public List<String> exclude = List.of();
	
	@Parameter(names = "--blacklist")
	public List<String> blacklist = List.of();
	
	@Parameter(names = "--out")
	public Path out = Path.of("");
	
	@Parameter(names = "--packageJson")
	public Path packageJson;
	
	@Parameter(names = "--index")
	public boolean index;
}
