package com.civicos.workflow.application;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.civicos.auth.application.AuthorizationService;
import com.civicos.auth.domain.PermissionCode;
import com.civicos.auth.domain.SystemRole;
import com.civicos.auth.security.CivicPrincipal;

@Component
class WorkflowAuthorizationPolicy {

	private final AuthorizationService authorizationService;

	WorkflowAuthorizationPolicy(AuthorizationService authorizationService) {
		this.authorizationService = authorizationService;
	}

	CivicPrincipal authorize(PermissionCode permission, UUID agencyId, SystemRole... allowedRoles) {
		authorizationService.authorize(permission);
		CivicPrincipal principal = authorizationService.currentPrincipal();
		Set<String> roles = principal.roles();
		boolean allowedRole = Arrays.stream(allowedRoles)
				.map(SystemRole::name)
				.anyMatch(roles::contains);
		boolean explicitAdminOverride = roles.contains(SystemRole.ADMIN.name())
				&& principal.permissions().contains(PermissionCode.ADMIN_OVERRIDE.name());
		if (!allowedRole && !explicitAdminOverride) {
			throw new AccessDeniedException("The actor role cannot perform this workflow action.");
		}
		if (agencyId != null && !authorizationService.canAccessAgency(agencyId)) {
			throw new AccessDeniedException("The resource is outside the actor's agency scope.");
		}
		return principal;
	}
}
