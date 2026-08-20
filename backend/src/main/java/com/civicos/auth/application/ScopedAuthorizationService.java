package com.civicos.auth.application;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.civicos.auth.domain.PermissionCode;
import com.civicos.auth.domain.SystemRole;
import com.civicos.auth.security.CivicPrincipal;

@Service
public class ScopedAuthorizationService {

	private final AuthorizationService authorizationService;

	public ScopedAuthorizationService(AuthorizationService authorizationService) {
		this.authorizationService = authorizationService;
	}

	public CivicPrincipal authorize(
			PermissionCode permission,
			UUID agencyId,
			SystemRole... allowedRoles) {
		authorizationService.authorize(permission);
		CivicPrincipal principal = authorizationService.currentPrincipal();
		Set<String> roles = principal.roles();
		boolean allowedRole = Arrays.stream(allowedRoles)
				.map(SystemRole::name)
				.anyMatch(roles::contains);
		boolean explicitAdminOverride = roles.contains(SystemRole.ADMIN.name())
				&& principal.permissions().contains(PermissionCode.ADMIN_OVERRIDE.name());
		if (!allowedRole && !explicitAdminOverride) {
			throw new AccessDeniedException("The actor role cannot perform this action.");
		}
		if (agencyId != null && !authorizationService.canAccessAgency(agencyId)) {
			throw new AccessDeniedException("The resource is outside the actor's agency scope.");
		}
		return principal;
	}
}
