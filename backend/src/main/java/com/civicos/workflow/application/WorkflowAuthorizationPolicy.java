package com.civicos.workflow.application;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.civicos.auth.application.ScopedAuthorizationService;
import com.civicos.auth.domain.PermissionCode;
import com.civicos.auth.domain.SystemRole;
import com.civicos.auth.security.CivicPrincipal;

@Component
class WorkflowAuthorizationPolicy {

	private final ScopedAuthorizationService scopedAuthorizationService;

	WorkflowAuthorizationPolicy(ScopedAuthorizationService scopedAuthorizationService) {
		this.scopedAuthorizationService = scopedAuthorizationService;
	}

	CivicPrincipal authorize(PermissionCode permission, UUID agencyId, SystemRole... allowedRoles) {
		return scopedAuthorizationService.authorize(permission, agencyId, allowedRoles);
	}
}
