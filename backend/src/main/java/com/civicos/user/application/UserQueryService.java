package com.civicos.user.application;

import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.civicos.auth.application.AuthorizationService;
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
	public UserResponse findVisibleUser(UUID userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new NoSuchElementException("User not found."));
		UUID agencyId = user.getAgency() == null ? null : user.getAgency().getId();
		if (!authorizationService.canAccessAgency(agencyId)) {
			throw new AccessDeniedException("User is outside the actor's authorised agency scope.");
		}
		return UserResponse.from(user);
	}
}
