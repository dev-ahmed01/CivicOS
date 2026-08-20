package com.civicos.common.persistence;

import java.time.Instant;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class AbstractAuditableEntity extends AbstractCreatedEntity {

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
