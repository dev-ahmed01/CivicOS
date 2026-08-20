package com.civicos.auth.api;

import java.util.Set;
import java.util.UUID;

import com.civicos.auth.security.CivicPrincipal;

public record CurrentUserResponse(
		UUID id,
		UUID agencyId,
		String fullName,
		String email,
		Set<String> roles,
		Set<String> permissions) {

	public static CurrentUserResponse from(CivicPrincipal principal) {
		return new CurrentUserResponse(
				principal.userId(),
				principal.agencyId(),
				principal.fullName(),
				principal.username(),
				principal.roles(),
				principal.permissions());
	}
}
