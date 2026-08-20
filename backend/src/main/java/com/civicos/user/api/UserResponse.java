package com.civicos.user.api;

import java.util.UUID;
import java.util.List;
import java.time.Instant;

import com.civicos.user.domain.User;

public record UserResponse(
		UUID id,
		UUID agencyId,
		String agencyName,
		String fullName,
		String email,
		String phone,
		User.Status status,
		List<String> roles,
		Instant createdAt,
		Instant updatedAt,
		Instant lastLoginAt) {

	public static UserResponse from(User user) {
		return new UserResponse(
				user.getId(),
				user.getAgency() == null ? null : user.getAgency().getId(),
				user.getAgency() == null ? null : user.getAgency().getName(),
				user.getFullName(),
				user.getEmail(),
				user.getPhone(),
				user.getStatus(),
				user.getRoles().stream().map(role -> role.getCode()).sorted().toList(),
				user.getCreatedAt(), user.getUpdatedAt(), user.getLastLoginAt());
	}
}
