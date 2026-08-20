package com.civicos.agency.domain;

import com.civicos.common.persistence.AbstractAuditableEntity;
import com.civicos.common.validation.ValidationRules;

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

	public static Agency create(
			String code, String name, Type type, String jurisdiction,
			String contactEmail, String contactPhone) {
		Agency agency = new Agency();
		agency.code = ValidationRules.requiredText(code, "Agency code").toUpperCase();
		agency.applyAdministrativeUpdate(name, type, jurisdiction, contactEmail, contactPhone, true);
		return agency;
	}

	public void applyAdministrativeUpdate(
			String name, Type type, String jurisdiction,
			String contactEmail, String contactPhone, boolean active) {
		this.name = ValidationRules.requiredText(name, "Agency name");
		this.type = ValidationRules.required(type, "Agency type");
		this.jurisdiction = ValidationRules.optionalText(jurisdiction);
		this.contactEmail = ValidationRules.optionalText(contactEmail);
		this.contactPhone = ValidationRules.optionalText(contactPhone);
		this.active = active;
	}

	public String getCode() { return code; }
	public String getName() { return name; }
	public Type getType() { return type; }
	public String getJurisdiction() { return jurisdiction; }
	public String getContactEmail() { return contactEmail; }
	public String getContactPhone() { return contactPhone; }
	public boolean isActive() { return active; }
}
