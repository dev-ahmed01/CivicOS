package com.civicos.auth.domain;

import com.civicos.common.persistence.AbstractCreatedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "permissions")
public class Permission extends AbstractCreatedEntity {

	@Column(nullable = false, unique = true, length = 100)
	private String code;

	@Column(columnDefinition = "text")
	private String description;

	public String getCode() { return code; }
	public String getDescription() { return description; }
}
