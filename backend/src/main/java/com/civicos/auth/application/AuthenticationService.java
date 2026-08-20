package com.civicos.auth.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import com.civicos.auth.security.CivicPrincipal;

@Service
public class AuthenticationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationService.class);

	private final AuthenticationManager authenticationManager;
	private final TokenService tokenService;

	public AuthenticationService(AuthenticationManager authenticationManager, TokenService tokenService) {
		this.authenticationManager = authenticationManager;
		this.tokenService = tokenService;
	}

	public TokenPair login(String email, String password, String requestId) {
		try {
			Authentication authentication = authenticationManager.authenticate(
					UsernamePasswordAuthenticationToken.unauthenticated(email.strip(), password));
			CivicPrincipal principal = (CivicPrincipal) authentication.getPrincipal();
			return tokenService.issueForLogin(principal.userId(), requestId);
		} catch (AuthenticationException exception) {
			LOGGER.warn("Authentication failed requestId={}", requestId);
			throw exception;
		}
	}
}
