package com.civicos.user.api;

import java.util.UUID;

import com.civicos.user.domain.User;

public record UserResponse(
		UUID id,
		UUID agencyId,
		String fullName,
		String email,
		String phone,
		User.Status status) {

	public static UserResponse from(User user) {
		return new UserResponse(
				user.getId(),
				user.getAgency() == null ? null : user.getAgency().getId(),
				user.getFullName(),
				user.getEmail(),
				user.getPhone(),
				user.getStatus());
	}
}
