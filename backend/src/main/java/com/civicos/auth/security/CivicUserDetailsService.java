package com.civicos.auth.security;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.civicos.auth.domain.PermissionCode;
import com.civicos.auth.domain.SystemRole;
import com.civicos.user.domain.User;
import com.civicos.user.repository.UserRepository;

@Service
public class CivicUserDetailsService implements UserDetailsService {

	private static final Set<String> VALID_ROLES = Arrays.stream(SystemRole.values())
			.map(Enum::name)
			.collect(Collectors.toUnmodifiableSet());
	private static final Set<String> VALID_PERMISSIONS = Arrays.stream(PermissionCode.values())
			.map(Enum::name)
			.collect(Collectors.toUnmodifiableSet());

	private final UserRepository userRepository;

	public CivicUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String email) {
		return userRepository.findByEmailIgnoreCase(email.strip())
				.map(this::toPrincipal)
				.orElseThrow(() -> new UsernameNotFoundException("Invalid email or password."));
	}

	@Transactional(readOnly = true)
	public CivicPrincipal loadUserById(UUID userId) {
		CivicPrincipal principal = userRepository.findWithAuthoritiesById(userId)
				.map(this::toPrincipal)
				.orElseThrow(() -> new UsernameNotFoundException("Authenticated user no longer exists."));
		if (!principal.enabled()) {
			throw new DisabledException("User account is not active.");
		}
		return principal;
	}

	private CivicPrincipal toPrincipal(User user) {
		Set<String> roles = user.getRoles().stream()
				.map(role -> role.getCode().toUpperCase(Locale.ROOT))
				.filter(VALID_ROLES::contains)
				.collect(Collectors.toUnmodifiableSet());
		Set<String> permissions = user.getRoles().stream()
				.flatMap(role -> role.getPermissions().stream())
				.map(permission -> permission.getCode().toUpperCase(Locale.ROOT))
				.filter(VALID_PERMISSIONS::contains)
				.collect(Collectors.toUnmodifiableSet());
		Set<SimpleGrantedAuthority> authorities = new LinkedHashSet<>();
		roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
		permissions.forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));

		return new CivicPrincipal(
				user.getId(),
				user.getAgency() == null ? null : user.getAgency().getId(),
				user.getFullName(),
				user.getEmail(),
				user.getPasswordHash() == null ? "" : user.getPasswordHash(),
				user.getStatus() == User.Status.ACTIVE,
				roles,
				permissions,
				Set.copyOf(authorities));
	}
}
