package com.civicos.admin.api;

import java.util.List;
import java.util.UUID;

import com.civicos.auth.domain.Role;

public record RoleAdminResponse(
		UUID id, String code, String name, String description, boolean systemRole,
		List<String> permissions) {
	public static RoleAdminResponse from(Role role) {
		return new RoleAdminResponse(
				role.getId(), role.getCode(), role.getName(), role.getDescription(), role.isSystemRole(),
				role.getPermissions().stream().map(item -> item.getCode()).sorted().toList());
	}
}
