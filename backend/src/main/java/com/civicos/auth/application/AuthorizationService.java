package com.civicos.auth.application;

import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.civicos.auth.domain.PermissionCode;
import com.civicos.auth.domain.SystemRole;
import com.civicos.auth.security.CivicPrincipal;

@Service
public class AuthorizationService {

	public void authorize(PermissionCode permission) {
		if (!currentPrincipal().permissions().contains(permission.name())) {
			throw new AccessDeniedException("Required permission is absent: " + permission.name());
		}
	}

	public boolean canAccessAgency(UUID agencyId) {
		CivicPrincipal principal = currentPrincipal();
		return principal.roles().contains(SystemRole.ADMIN.name())
				|| principal.roles().contains(SystemRole.COORDINATOR.name())
				|| agencyId != null && agencyId.equals(principal.agencyId());
	}

	public CivicPrincipal currentPrincipal() {
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if (principal instanceof CivicPrincipal civicPrincipal) {
			return civicPrincipal;
		}
		throw new AccessDeniedException("Authenticated CivicOS principal is required.");
	}
}
