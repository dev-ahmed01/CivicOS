package com.civicos.user.api;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.civicos.auth.api.CurrentUserResponse;
import com.civicos.auth.security.CivicPrincipal;
import com.civicos.user.application.UserQueryService;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserQueryService userQueryService;

	public UserController(UserQueryService userQueryService) {
		this.userQueryService = userQueryService;
	}

	@GetMapping("/me")
	public CurrentUserResponse me(@AuthenticationPrincipal CivicPrincipal principal) {
		return CurrentUserResponse.from(principal);
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('USER_VIEW')")
	public UserResponse findById(@PathVariable UUID id) {
		return userQueryService.findVisibleUser(id);
	}
}
