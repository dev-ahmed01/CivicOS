package com.civicos.evidence.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.civicos.evidence.config.FileStorageProperties;

@Service
public class LocalFileStorageService implements FileStorageService {

	private static final DateTimeFormatter DIRECTORY_FORMAT = DateTimeFormatter
			.ofPattern("uuuu/MM")
			.withZone(ZoneOffset.UTC);

	private final Path storageRoot;
	private final Clock clock;

	public LocalFileStorageService(FileStorageProperties properties, Clock clock) {
		this.storageRoot = properties.getPath().toAbsolutePath().normalize();
		this.clock = clock;
		try {
			Files.createDirectories(storageRoot);
		} catch (IOException exception) {
			throw new FileStorageException("Unable to initialize the evidence storage directory.", exception);
		}
	}

	@Override
	public StoredFile store(byte[] content) {
		String fileReference = DIRECTORY_FORMAT.format(clock.instant()) + "/" + UUID.randomUUID();
		Path destination = resolve(fileReference);
		try {
			Files.createDirectories(destination.getParent());
			Files.write(destination, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
			return new StoredFile(fileReference, content.length, sha256(content));
		} catch (IOException exception) {
			throw new FileStorageException("Unable to store evidence content.", exception);
		}
	}

	@Override
	public InputStream open(String fileReference) {
		try {
			return Files.newInputStream(resolve(fileReference), StandardOpenOption.READ);
		} catch (IOException exception) {
			throw new FileStorageException("Unable to read evidence content.", exception);
		}
	}

	@Override
	public void delete(String fileReference) {
		try {
			Files.deleteIfExists(resolve(fileReference));
		} catch (IOException exception) {
			throw new FileStorageException("Unable to remove uncommitted evidence content.", exception);
		}
	}

	private Path resolve(String fileReference) {
		if (fileReference == null || fileReference.isBlank()) {
			throw new IllegalArgumentException("File reference is required.");
		}
		Path resolved = storageRoot.resolve(fileReference).normalize();
		if (!resolved.startsWith(storageRoot)) {
			throw new IllegalArgumentException("File reference escapes the configured storage root.");
		}
		return resolved;
	}

	private String sha256(byte[] content) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable.", exception);
		}
	}
}
