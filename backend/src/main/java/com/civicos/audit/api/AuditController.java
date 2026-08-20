package com.civicos.audit.api;

import java.util.Set;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.civicos.audit.application.AuditEventResult;
import com.civicos.audit.application.AuditQueryService;
import com.civicos.common.web.CorrelationIdFilter;
import com.civicos.common.web.PageRequestFactory;
import com.civicos.common.web.PagedResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/audit-events")
@Tag(name = "Audit")
public class AuditController {

	private final AuditQueryService auditQueryService;
	private final PageRequestFactory pageRequestFactory;

	public AuditController(AuditQueryService auditQueryService, PageRequestFactory pageRequestFactory) {
		this.auditQueryService = auditQueryService;
		this.pageRequestFactory = pageRequestFactory;
	}

	@GetMapping("/{eventId}")
	public AuditEventResult byEventId(@PathVariable UUID eventId) {
		return auditQueryService.byEventId(eventId);
	}

	@GetMapping("/entities/{entityType}/{entityId}")
	public PagedResponse<AuditEventResult> entityTrail(
			@PathVariable String entityType,
			@PathVariable UUID entityId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "occurredAt,asc") String sort,
			HttpServletRequest request) {
		return PagedResponse.from(auditQueryService.entityTrail(entityType, entityId,
				pageRequestFactory.create(page, size, sort, Set.of("occurredAt", "action"))),
				CorrelationIdFilter.requestId(request));
	}
}
