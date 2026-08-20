package com.civicos.evidence.storage;

import java.io.InputStream;

public interface FileStorageService {

	StoredFile store(byte[] content);

	InputStream open(String fileReference);

	void delete(String fileReference);
}
