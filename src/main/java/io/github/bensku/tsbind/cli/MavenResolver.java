package io.github.bensku.tsbind.cli;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

public class MavenResolver {

	private HttpClient client;
	private String repo;
	
	public MavenResolver(String repo) {
		this.client = HttpClient.newHttpClient();
		this.repo = repo;
	}
	
	private String getSnapshotMetadata(String group, String artifact, String version) throws InterruptedException, IOException {
		URI uri = URI.create(repo + "/" + group.replace('.', '/')
				+ "/" + artifact + "/" + version + "/maven-metadata.xml");
		HttpResponse<String> response = client.send(HttpRequest.newBuilder(uri).GET().build(), BodyHandlers.ofString());
		if (response.statusCode() == 200) {
			return response.body(); // Got a response, let's hope it is valid XML
		}
		throw new IllegalArgumentException("cannot find artifact " + group + ":" + artifact + ":" + version);
	}
	
	private URI getSnapshotSources(String metadataStr) {
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
		return URI.create(repo + "/" + group.replace('.', '/')
				+ "/" + artifact + "/" + version
				+ "/" + artifact + "-" + snapshotVersion + "-sources.jar");
	}
	
	public Path downloadSources(String coordinates) throws InterruptedException, IOException {
		String[] parts = coordinates.split(":");
		String group = parts[0];
		String artifact = parts[1];
		String version = parts[2];
		
		URI sourceUri;
		if (version.contains("SNAPSHOT")) {
			String metadata = getSnapshotMetadata(group, artifact, version);
			sourceUri = getSnapshotSources(metadata);
		} else {
			throw new UnsupportedOperationException("non-snapshot resolution is not yet supported");
		}
		
		// Download source jar to temporary file
		Path tempFile = Files.createTempFile("tsbind", "-sources.jar");
		client.send(HttpRequest.newBuilder(sourceUri).GET().build(), BodyHandlers.ofFile(tempFile));
		return tempFile;
	}
}
