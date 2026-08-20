package com.civicos.user.domain;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Locale;

import com.civicos.agency.domain.Agency;
import com.civicos.auth.domain.Role;
import com.civicos.common.persistence.AbstractAuditableEntity;
import com.civicos.common.validation.ValidationRules;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User extends AbstractAuditableEntity {

	public enum Status { ACTIVE, INVITED, SUSPENDED, DEACTIVATED, DISABLED }

	@ManyToOne
	@JoinColumn(name = "agency_id")
	private Agency agency;

	@Column(name = "full_name", nullable = false, length = 150)
	private String fullName;

	@Column(nullable = false)
	private String email;

	@Column(length = 30)
	private String phone;

	@Column(name = "password_hash")
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private Status status = Status.ACTIVE;

	@Column(name = "external_reference", length = 100)
	private String externalReference;

	@Column(name = "last_login_at")
	private Instant lastLoginAt;

	@ManyToMany
	@JoinTable(
			name = "user_roles",
			joinColumns = @JoinColumn(name = "user_id"),
			inverseJoinColumns = @JoinColumn(name = "role_id"))
	private Set<Role> roles = new LinkedHashSet<>();

	public static User invite(
			Agency agency, String fullName, String email, String phone,
			String externalReference, Set<Role> roles) {
		User user = new User();
		user.applyAdministrativeProfile(agency, fullName, email, phone, externalReference, Status.INVITED, roles);
		return user;
	}

	public void applyAdministrativeProfile(
			Agency agency, String fullName, String email, String phone,
			String externalReference, Status status, Set<Role> roles) {
		this.agency = agency;
		this.fullName = ValidationRules.requiredText(fullName, "User full name");
		this.email = ValidationRules.requiredText(email, "User email").toLowerCase(Locale.ROOT);
		this.phone = ValidationRules.optionalText(phone);
		this.externalReference = ValidationRules.optionalText(externalReference);
		this.status = ValidationRules.required(status, "User status");
		this.roles.clear();
		this.roles.addAll(ValidationRules.required(roles, "User roles"));
	}

	public Agency getAgency() { return agency; }
	public String getFullName() { return fullName; }
	public String getEmail() { return email; }
	public String getPhone() { return phone; }
	public String getPasswordHash() { return passwordHash; }
	public Status getStatus() { return status; }
	public String getExternalReference() { return externalReference; }
	public Instant getLastLoginAt() { return lastLoginAt; }
	public Set<Role> getRoles() { return Set.copyOf(roles); }
}
