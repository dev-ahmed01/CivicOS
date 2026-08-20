package com.civicos.evidence.storage;

public record StoredFile(String fileReference, long sizeBytes, String sha256Checksum) {
}
