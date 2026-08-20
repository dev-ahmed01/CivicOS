package com.civicos.user.api;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import com.civicos.auth.api.CurrentUserResponse;
import com.civicos.auth.security.CivicPrincipal;
import com.civicos.user.application.UserQueryService;
import com.civicos.common.web.PageRequestFactory;
import com.civicos.common.web.PagedResponse;
import com.civicos.common.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserQueryService userQueryService;
	private final PageRequestFactory pageRequestFactory;

	public UserController(UserQueryService userQueryService, PageRequestFactory pageRequestFactory) {
		this.userQueryService = userQueryService;
		this.pageRequestFactory = pageRequestFactory;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('USER_VIEW')")
	public PagedResponse<UserResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "createdAt,desc") String sort,
			HttpServletRequest request) {
		return PagedResponse.from(userQueryService.list(pageRequestFactory.create(
				page, size, sort, Set.of("createdAt", "updatedAt", "fullName", "email", "status"))),
				CorrelationIdFilter.requestId(request));
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
