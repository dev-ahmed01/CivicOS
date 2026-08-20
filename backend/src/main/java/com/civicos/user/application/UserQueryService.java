package com.civicos.user.application;

import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.civicos.auth.application.AuthorizationService;
import com.civicos.auth.domain.PermissionCode;
import com.civicos.auth.domain.SystemRole;
import com.civicos.user.api.UserResponse;
import com.civicos.user.domain.User;
import com.civicos.user.repository.UserRepository;

@Service
public class UserQueryService {

	private final UserRepository userRepository;
	private final AuthorizationService authorizationService;

	public UserQueryService(UserRepository userRepository, AuthorizationService authorizationService) {
		this.userRepository = userRepository;
		this.authorizationService = authorizationService;
	}

	@Transactional(readOnly = true)
	public Page<UserResponse> list(Pageable pageable) {
		authorizationService.authorize(PermissionCode.USER_VIEW);
		if (!authorizationService.currentPrincipal().roles().contains(SystemRole.ADMIN.name())) {
			throw new AccessDeniedException("Only administrators can list all users.");
		}
		return userRepository.findAll(pageable).map(UserResponse::from);
	}

	@Transactional(readOnly = true)
	public UserResponse findVisibleUser(UUID userId) {
		authorizationService.authorize(PermissionCode.USER_VIEW);
		User user = userRepository.findWithAuthoritiesById(userId)
				.orElseThrow(() -> new NoSuchElementException("User not found."));
		UUID agencyId = user.getAgency() == null ? null : user.getAgency().getId();
		if (!authorizationService.canAccessAgency(agencyId)) {
			throw new AccessDeniedException("User is outside the actor's authorised agency scope.");
		}
		return UserResponse.from(user);
	}
}
