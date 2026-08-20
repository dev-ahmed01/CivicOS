package com.civicos.admin.application;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.civicos.ai.config.AiProperties;
import com.civicos.auth.application.AuthorizationService;
import com.civicos.auth.domain.PermissionCode;
import com.civicos.auth.domain.SystemRole;
import com.civicos.conflict.config.ConflictPolicyProperties;
import com.civicos.sla.config.SlaPolicyProperties;
import com.civicos.verification.config.VerificationPolicyProperties;

@Service
public class AdminConfigurationService {

	private final AuthorizationService authorizationService;
	private final AiProperties ai;
	private final ConflictPolicyProperties conflict;
	private final SlaPolicyProperties sla;
	private final VerificationPolicyProperties verification;

	public AdminConfigurationService(
			AuthorizationService authorizationService,
			AiProperties ai,
			ConflictPolicyProperties conflict,
			SlaPolicyProperties sla,
			VerificationPolicyProperties verification) {
		this.authorizationService = authorizationService;
		this.ai = ai; this.conflict = conflict; this.sla = sla; this.verification = verification;
	}

	public Map<String, Object> safeSnapshot() {
		authorizationService.authorize(PermissionCode.POLICY_VIEW);
		if (!authorizationService.currentPrincipal().roles().contains(SystemRole.ADMIN.name())) {
			throw new AccessDeniedException("Only administrators can view system configuration.");
		}
		Map<String, Object> snapshot = new LinkedHashMap<>();
		snapshot.put("configurationSource", "ENVIRONMENT");
		snapshot.put("runtimeMutationSupported", false);
		snapshot.put("ai", Map.of(
				"enabled", ai.isEnabled(), "provider", ai.getProvider(),
				"timeoutSeconds", ai.getTimeout().toSeconds(),
				"humanReviewThreshold", ai.getHumanReviewThreshold()));
		snapshot.put("conflict", Map.of(
				"proximityMeters", conflict.getProximityMeters(),
				"repeatDiggingWindowDays", conflict.getRepeatDiggingWindowDays(),
				"multiAgencyMinimum", conflict.getMultiAgencyMinimum()));
		snapshot.put("sla", Map.of(
				"businessZone", sla.getBusinessZone().toString(),
				"businessDayStart", sla.getBusinessDayStart().toString(),
				"businessDayEnd", sla.getBusinessDayEnd().toString(),
				"atRiskBeforeHours", sla.getAtRiskBeforeHours()));
		snapshot.put("verification", Map.of(
				"requiredEvidenceTypes", verification.getRequiredEvidenceTypes().stream()
						.map(Enum::name).sorted().toList()));
		return snapshot;
	}
}
