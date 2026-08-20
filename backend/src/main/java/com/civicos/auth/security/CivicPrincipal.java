package com.civicos.auth.security;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record CivicPrincipal(
		UUID userId,
		UUID agencyId,
		String fullName,
		String username,
		String password,
		boolean enabled,
		Set<String> roles,
		Set<String> permissions,
		Collection<? extends GrantedAuthority> authorities) implements UserDetails {

	@Override
	public String getUsername() { return username; }

	@Override
	public String getPassword() { return password; }

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }

	@Override
	public boolean isEnabled() { return enabled; }

	@Override
	public boolean isAccountNonExpired() { return true; }

	@Override
	public boolean isAccountNonLocked() { return true; }

	@Override
	public boolean isCredentialsNonExpired() { return true; }
}
