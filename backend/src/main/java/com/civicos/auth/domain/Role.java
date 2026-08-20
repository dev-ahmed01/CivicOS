package com.civicos.auth.domain;

import java.util.LinkedHashSet;
import java.util.Set;

import com.civicos.common.persistence.AbstractCreatedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "roles")
public class Role extends AbstractCreatedEntity {

	@Column(nullable = false, unique = true, length = 50)
	private String code;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(columnDefinition = "text")
	private String description;

	@Column(name = "system_role", nullable = false)
	private boolean systemRole;

	@ManyToMany
	@JoinTable(
			name = "role_permissions",
			joinColumns = @JoinColumn(name = "role_id"),
			inverseJoinColumns = @JoinColumn(name = "permission_id"))
	private Set<Permission> permissions = new LinkedHashSet<>();

	public String getCode() { return code; }
	public String getName() { return name; }
	public String getDescription() { return description; }
	public boolean isSystemRole() { return systemRole; }
	public Set<Permission> getPermissions() { return Set.copyOf(permissions); }
}
