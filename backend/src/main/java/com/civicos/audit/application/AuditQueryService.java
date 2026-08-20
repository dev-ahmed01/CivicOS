package com.civicos.audit.application;

import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.civicos.audit.domain.AuditEvent;
import com.civicos.audit.repository.AuditEventRepository;
import com.civicos.auth.application.AuthorizationService;
import com.civicos.auth.domain.PermissionCode;
import com.civicos.auth.security.CivicPrincipal;
import com.civicos.common.validation.ValidationRules;

@Service
public class AuditQueryService {

	private final AuditEventRepository auditEventRepository;
	private final AuthorizationService authorizationService;
	private final AuditVisibilityPolicy visibilityPolicy;

	public AuditQueryService(
			AuditEventRepository auditEventRepository,
			AuthorizationService authorizationService,
			AuditVisibilityPolicy visibilityPolicy) {
		this.auditEventRepository = auditEventRepository;
		this.authorizationService = authorizationService;
		this.visibilityPolicy = visibilityPolicy;
	}

	@Transactional(readOnly = true)
	public AuditEventResult byEventId(UUID eventId) {
		CivicPrincipal principal = principal();
		AuditEvent event = auditEventRepository.findByEventId(eventId)
				.orElseThrow(() -> new NoSuchElementException("Audit event not found: " + eventId));
		visibilityPolicy.assertVisible(event, principal);
		return result(event);
	}

	@Transactional(readOnly = true)
	public List<AuditEventResult> entityTrail(String entityType, UUID entityId) {
		String normalizedType = ValidationRules.requiredText(entityType, "Audit entity type")
				.toUpperCase(Locale.ROOT);
		ValidationRules.required(entityId, "Audit entity id");
		CivicPrincipal principal = principal();
		List<AuditEvent> events = auditEventRepository
				.findByEntityTypeAndEntityIdOrderByOccurredAtAsc(normalizedType, entityId);
		if (events.isEmpty()) {
			return List.of();
		}
		events.forEach(event -> visibilityPolicy.assertVisible(event, principal));
		return events.stream().map(this::result).toList();
	}

	private CivicPrincipal principal() {
		authorizationService.authorize(PermissionCode.AUDIT_VIEW);
		return authorizationService.currentPrincipal();
	}

	private AuditEventResult result(AuditEvent event) {
		return new AuditEventResult(
				event.getEventId(), event.getActor() == null ? null : event.getActor().getId(),
				event.getAction(), event.getEntityType(), event.getEntityId(), event.getOccurredAt(),
				event.getBeforeState(), event.getAfterState(), event.getReason(), event.getRequestId(),
				event.getMetadata());
	}
}
