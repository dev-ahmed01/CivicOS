package com.civicos.admin.api;

import java.util.UUID;

import com.civicos.auth.domain.Permission;

public record PermissionAdminResponse(UUID id, String code, String description) {
	public static PermissionAdminResponse from(Permission permission) {
		return new PermissionAdminResponse(permission.getId(), permission.getCode(), permission.getDescription());
	}
}
