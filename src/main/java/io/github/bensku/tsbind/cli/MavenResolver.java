package io.github.bensku.tsbind.cli;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

/**
 * A very simple Maven resolver. This currently lacks support for Maven BOM
 * and variable substitutions in pom.xml, but should be enough to pull
 * dependencies for small libraries.
 *
 */
public class MavenResolver {

	private Path tempDir;
	private HttpClient client;
	private List<String> repos;
	
	/**
	 * Creates a new Maven resolver.
	 * @param tempDir Temporary directory where downloads should be placed.
	 * @param repos Maven repository URLs, in order of preference.
	 */
	public MavenResolver(Path tempDir, List<String> repos) {
		this.tempDir = tempDir;
		this.client = HttpClient.newHttpClient();
		this.repos = repos;
	}
	
	private static class ArtifactNotFoundException extends RuntimeException {
		
		private static final long serialVersionUID = 1L;

		public ArtifactNotFoundException(String msg) {
			super(msg);
		}
	}
	
	private String getSnapshotUrl(String group, String artifact, String version) throws InterruptedException, IOException {
		// Try each repo in order they were specified
		for (String repo : repos) {
			URI uri = URI.create(repo + "/" + group.replace('.', '/')
					+ "/" + artifact + "/" + version + "/maven-metadata.xml");
			HttpResponse<String> response = client.send(HttpRequest.newBuilder(uri).GET().build(), BodyHandlers.ofString());
			if (response.statusCode() == 200) {
				// Got a response, let's hope it is valid XML
				return getSnapshotUrl(repo, response.body());
			}
		}
		throw new ArtifactNotFoundException("cannot find artifact " + group + ":" + artifact + ":" + version);
	}
	
	private String getSnapshotUrl(String repo, String metadataStr) {
		Document doc = Jsoup.parse(metadataStr, "", Parser.xmlParser());
		Element metadata = doc.selectFirst("metadata");
		Elements snapshots = metadata.selectFirst("versioning")
				.selectFirst("snapshotVersions")
				.select("snapshotVersion");
		
		// Find the source jar and take its snapshot identifier
		String snapshotVersion = null;
		for (Element snapshot : snapshots) {
			Element classifier = snapshot.selectFirst("classifier");
			if (classifier != null && classifier.text().equals("sources")) {
				snapshotVersion = snapshot.selectFirst("value").text();
				break;
			}
		}
		if (snapshotVersion == null) {
			throw new IllegalArgumentException("source jar not found");
		}
		
		// Create download URL for source jar
		String group = metadata.selectFirst("groupId").text();
		String artifact = metadata.selectFirst("artifactId").text();
		String version = metadata.selectFirst("version").text();
		return repo + "/" + group.replace('.', '/')
				+ "/" + artifact + "/" + version
				+ "/" + artifact + "-" + snapshotVersion;
	}
	
	private String getReleaseUrl(String group, String artifact, String version) throws IOException, InterruptedException {
		for (String repo : repos) {
			String baseUrl = repo + "/" + group.replace('.', '/')
					+ "/" + artifact + "/" + version
					+ "/" + artifact + "-" + version;
			URI testUri = URI.create(baseUrl + ".pom");
			HttpResponse<String> response = client.send(HttpRequest.newBuilder(testUri)
					.GET().build(), BodyHandlers.ofString());
			if (response.statusCode() == 200) {
				// Got .pom, artifact should exist in this repository
				// TODO reuse this .pom to avoid downloading twice
				return baseUrl;
			}
		}
		throw new ArtifactNotFoundException("cannot find artifact " + group + ":" + artifact + ":" + version);

	}
	
	/**
	 * Parses the given .pom file contents and gets a list of dependencies.
	 * @param pomXml .pom XML content.
	 * @return Dependency coordinates.
	 */
	private List<String> getDependencies(String pomXml) {
		List<String> deps = new ArrayList<>();
		
		Document doc = Jsoup.parse(pomXml, "", Parser.xmlParser());
		// Select last <dependencies> so we don't hit one under <dependencyManagement>
		Element depsTag = doc.selectFirst("project").select("dependencies").last();
		if (depsTag == null) {
			return deps; // No dependencies
		}
		Elements dependencies = depsTag.select("dependency");
		if (dependencies == null) {
			return deps; // Also no dependencies
		}
		for (Element dependency : dependencies) {
			Element group = dependency.selectFirst("groupId");
			Element artifact = dependency.selectFirst("artifactId");
			Element version = dependency.selectFirst("version");
			if (group == null || artifact == null) {
				throw new AssertionError("groupId: " + group + ", artifactId: " + artifact);
			}
			Element scopeEl = dependency.selectFirst("scope");
			if (scopeEl != null) {
				String scope = scopeEl.text();
				if (!scope.equals("compile") && !scope.equals("provided")) {
					continue; // Not referenced from main source code
				}
			}
			if (version == null) {
				System.out.println(group.text() + ":" + artifact.text() + ": unknown version, Maven BOM?");
				continue;
			}
			String dep = group.text() + ":" + artifact.text() + ":" + version.text();
			if (dep.contains("${")) {
				System.out.println(dep + ": variables not supported");
				continue;
			}
			deps.add(dep);
		}
		return deps;
	}
	
	public static class ArtifactResults {
		
		/**
		 * Source jar. Null when {@link MavenResolver#downloadArtifacts(String, boolean)}
		 * is given false as {@code source} parameter.
		 */
		public final Path sourceJar;
		
		/**
		 * Binary jars that contain symbols needed to parse the source jar.
		 */
		public final List<Path> symbols;
		
		private ArtifactResults(Path sourceJar, List<Path> symbols) {
			this.sourceJar = sourceJar;
			this.symbols = symbols;
		}
	}
	
	private Path download(URI uri, String name) throws IOException, InterruptedException {
		name = name.replace(':', '-'); // Double colon is trouble on Windows
		Path path = tempDir.resolve(name);
		if (Files.exists(path)) {
			return path; // no need to download it again
		}
		HttpResponse<Path> response = client.send(HttpRequest.newBuilder(uri)
				.GET().build(), BodyHandlers.ofFile(path));
		if (response.statusCode() != 200) {
			throw new IOException("failed to GET " + uri + ": HTTP " + response.statusCode());
		}
		return path;
	}
	
	/**
	 * Downloads an artifact and its (non-test and supported) dependencies.
	 * @param coordinates Coordinates in Gradle format, i.e.
	 * {@code group:artifact:version}.
	 * @param source If source jar should be downloaded. If this is false,
	 * {@link ArtifactResults#sourceJar} is also null.
	 * @return Paths to symbol jars and, optionally, the source jar.
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public ArtifactResults downloadArtifacts(String coordinates, boolean source) throws InterruptedException, IOException {
		String[] parts = coordinates.split(":");
		String group = parts[0];
		String artifact = parts[1];
		String version = parts[2];
		
		// For snapshots, we need to figure out the subfolder for latest upload
		// (for releases: just find the repository it exists in)
		String baseUrl;
		if (version.contains("SNAPSHOT")) {
			baseUrl = getSnapshotUrl(group, artifact, version);
		} else {
			baseUrl = getReleaseUrl(group, artifact, version);
		}
		
		List<Path> symbols = new ArrayList<>();
		
		// Download source if it was requested
		Path sourceJar = null;
		if (source) {
			sourceJar = download(URI.create(baseUrl + "-sources.jar"), coordinates + "-sources.jar");
		}
		
		// Fetch the binary jar for symbols
		symbols.add(download(URI.create(baseUrl + ".jar"), coordinates + ".jar"));
		
		// ... and all compile-time dependencies for main source code, for symbols again
		URI pomUri = URI.create(baseUrl + ".pom");
		HttpResponse<String> response =  client.send(HttpRequest.newBuilder(pomUri)
				.GET().build(), BodyHandlers.ofString());
		if (response.statusCode() != 200) {
			throw new IOException("failed to GET " + pomUri + ": HTTP " + response.statusCode());
		}
		for (String dependency : getDependencies(response.body())) {
			try {
				ArtifactResults artifacts = downloadArtifacts(dependency, false);
				System.out.println("Fetching " + dependency);
				symbols.addAll(artifacts.symbols);
			} catch (ArtifactNotFoundException e) {
				// Failure to resolve a dependency is not necessarily critical
				// It will also happen quite often since our .pom parsing logic
				// is quite limited compared to actual build systems
				System.out.println(e.getMessage());
			}
		}
		
		return new ArtifactResults(sourceJar, symbols);
	}
}
