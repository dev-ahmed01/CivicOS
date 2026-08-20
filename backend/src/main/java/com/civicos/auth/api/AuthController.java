package com.civicos.auth.api;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.civicos.auth.application.AuthenticationService;
import com.civicos.auth.application.TokenPair;
import com.civicos.auth.application.TokenService;
import com.civicos.auth.security.CivicPrincipal;
import com.civicos.common.web.CorrelationIdFilter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthenticationService authenticationService;
	private final TokenService tokenService;

	public AuthController(AuthenticationService authenticationService, TokenService tokenService) {
		this.authenticationService = authenticationService;
		this.tokenService = tokenService;
	}

	@PostMapping("/login")
	public ResponseEntity<TokenPair> login(
			@Valid @RequestBody LoginRequest request,
			HttpServletRequest servletRequest) {
		return tokenResponse(authenticationService.login(
				request.email(),
				request.password(),
				CorrelationIdFilter.requestId(servletRequest)));
	}

	@PostMapping("/refresh")
	public ResponseEntity<TokenPair> refresh(
			@Valid @RequestBody RefreshTokenRequest request,
			HttpServletRequest servletRequest) {
		return tokenResponse(tokenService.refresh(
				request.refreshToken(),
				CorrelationIdFilter.requestId(servletRequest)));
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(
			@Valid @RequestBody RefreshTokenRequest request,
			HttpServletRequest servletRequest) {
		tokenService.logout(request.refreshToken(), CorrelationIdFilter.requestId(servletRequest));
		return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
	}

	@GetMapping("/me")
	public CurrentUserResponse me(@AuthenticationPrincipal CivicPrincipal principal) {
		return CurrentUserResponse.from(principal);
	}

	private ResponseEntity<TokenPair> tokenResponse(TokenPair pair) {
		return ResponseEntity.ok()
				.cacheControl(CacheControl.noStore())
				.body(pair);
	}
}
