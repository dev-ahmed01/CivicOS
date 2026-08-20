package com.civicos.admin.api;

import java.time.Instant;
import java.util.UUID;

import com.civicos.agency.domain.Agency;

public record AgencyAdminResponse(
		UUID id, String code, String name, Agency.Type type, String jurisdiction,
		String contactEmail, String contactPhone, boolean active,
		Instant createdAt, Instant updatedAt) {
	public static AgencyAdminResponse from(Agency agency) {
		return new AgencyAdminResponse(
				agency.getId(), agency.getCode(), agency.getName(), agency.getType(),
				agency.getJurisdiction(), agency.getContactEmail(), agency.getContactPhone(),
				agency.isActive(), agency.getCreatedAt(), agency.getUpdatedAt());
	}
}
