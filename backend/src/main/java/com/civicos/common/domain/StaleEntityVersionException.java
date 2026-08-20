package com.civicos.common.domain;

public class StaleEntityVersionException extends RuntimeException {

	public StaleEntityVersionException(String entityType, long expectedVersion, long actualVersion) {
		super("%s was modified concurrently (expected version %d, actual version %d)."
				.formatted(entityType, expectedVersion, actualVersion));
	}
}
