package com.civicos.evidence.config;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Component
@Validated
@ConfigurationProperties(prefix = "civicos.file-storage")
public class FileStorageProperties {

	@NotNull
	private Path path = Path.of("./data/uploads");

	@NotNull
	private DataSize maxUploadSize = DataSize.ofMegabytes(20);

	@NotEmpty
	private Set<String> allowedContentTypes = new LinkedHashSet<>(Set.of(
			"image/jpeg",
			"image/png",
			"image/webp",
			"application/pdf",
			"video/mp4"));

	public Path getPath() { return path; }
	public DataSize getMaxUploadSize() { return maxUploadSize; }
	public Set<String> getAllowedContentTypes() { return Set.copyOf(allowedContentTypes); }

	public void setPath(Path path) {
		this.path = path;
	}

	public void setMaxUploadSize(DataSize maxUploadSize) {
		this.maxUploadSize = maxUploadSize;
	}

	public void setAllowedContentTypes(Set<String> allowedContentTypes) {
		this.allowedContentTypes = new LinkedHashSet<>(allowedContentTypes);
	}
}
