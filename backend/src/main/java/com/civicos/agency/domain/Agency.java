package com.civicos.agency.domain;

import com.civicos.common.persistence.AbstractAuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "agencies")
public class Agency extends AbstractAuditableEntity {

	public enum Type { GOVERNMENT, UTILITY, TELECOM, CONTRACTOR, OTHER }

	@Column(nullable = false, unique = true, length = 50)
	private String code;

	@Column(nullable = false, unique = true, length = 200)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "agency_type", nullable = false, length = 50)
	private Type type;

	@Column(length = 200)
	private String jurisdiction;

	@Column(name = "contact_email")
	private String contactEmail;

	@Column(name = "contact_phone", length = 30)
	private String contactPhone;

	@Column(nullable = false)
	private boolean active = true;

	public String getCode() { return code; }
	public String getName() { return name; }
	public Type getType() { return type; }
	public String getJurisdiction() { return jurisdiction; }
	public String getContactEmail() { return contactEmail; }
	public String getContactPhone() { return contactPhone; }
	public boolean isActive() { return active; }
}
