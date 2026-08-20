package com.civicos.auth.security;

import java.util.UUID;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class CivicJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

	private final CivicUserDetailsService userDetailsService;

	public CivicJwtAuthenticationConverter(CivicUserDetailsService userDetailsService) {
		this.userDetailsService = userDetailsService;
	}

	@Override
	public AbstractAuthenticationToken convert(Jwt jwt) {
		try {
			CivicPrincipal principal = userDetailsService.loadUserById(UUID.fromString(jwt.getSubject()));
			return UsernamePasswordAuthenticationToken.authenticated(
					principal,
					jwt.getTokenValue(),
					principal.getAuthorities());
		} catch (IllegalArgumentException exception) {
			throw new BadCredentialsException("Invalid access token subject.", exception);
		}
	}
}
