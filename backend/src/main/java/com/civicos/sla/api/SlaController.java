package com.civicos.sla.api;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.civicos.common.web.ApiIdempotencyService;
import com.civicos.common.web.CorrelationIdFilter;
import com.civicos.common.web.PageRequestFactory;
import com.civicos.common.web.PagedResponse;
import com.civicos.sla.application.CreateSlaCommand;
import com.civicos.sla.application.SlaResult;
import com.civicos.sla.application.SlaQueryService;
import com.civicos.sla.application.SlaService;
import com.civicos.sla.application.SlaUrgency;
import com.civicos.sla.domain.Sla;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/v1/slas")
@Tag(name = "SLAs")
public class SlaController {

	private final SlaService slaService;
	private final SlaQueryService queryService;
	private final ApiIdempotencyService idempotencyService;
	private final PageRequestFactory pageRequestFactory;

	public SlaController(
			SlaService slaService,
			SlaQueryService queryService,
			ApiIdempotencyService idempotencyService,
			PageRequestFactory pageRequestFactory) {
		this.slaService = slaService;
		this.queryService = queryService;
		this.idempotencyService = idempotencyService;
		this.pageRequestFactory = pageRequestFactory;
	}

	@GetMapping
	public PagedResponse<SlaResponse> list(
			@org.springframework.web.bind.annotation.RequestParam(required = false) Set<Sla.Status> status,
			@org.springframework.web.bind.annotation.RequestParam(required = false) Sla.Type slaType,
			@org.springframework.web.bind.annotation.RequestParam(required = false) String targetType,
			@org.springframework.web.bind.annotation.RequestParam(required = false) Set<UUID> targetId,
			@org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
			@org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") int size,
			@org.springframework.web.bind.annotation.RequestParam(defaultValue = "deadline,asc") String sort,
			HttpServletRequest request) {
		return PagedResponse.from(queryService.list(status, slaType, targetType, targetId,
				pageRequestFactory.create(page, size, sort,
						Set.of("deadline", "status", "slaType", "startAt"))),
				CorrelationIdFilter.requestId(request));
	}

	@GetMapping("/{slaId}")
	public SlaResponse byId(@PathVariable UUID slaId) {
		return queryService.byId(slaId);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create an SLA instance")
	public SlaResult create(
			@RequestHeader("Idempotency-Key") String key,
			@Valid @RequestBody CreateRequest body,
			HttpServletRequest request) {
		return idempotencyService.execute(key, "SLA_CREATE", body, SlaResult.class,
				() -> slaService.create(new CreateSlaCommand(
						body.targetType(), body.targetId(), body.slaType(), body.urgency(), body.startAt()),
						body.reason(), CorrelationIdFilter.requestId(request)));
	}

	@PostMapping("/{slaId}/{action}")
	@Operation(summary = "Pause, resume, or complete an SLA")
	public SlaResult mutate(
			@PathVariable UUID slaId,
			@PathVariable String action,
			@RequestHeader("Idempotency-Key") String key,
			@Valid @RequestBody MutationRequest body,
			HttpServletRequest request) {
		String requestId = CorrelationIdFilter.requestId(request);
		return idempotencyService.execute(key,
				"SLA_" + action.toUpperCase(Locale.ROOT) + ":" + slaId, body,
				SlaResult.class, () -> switch (action.toLowerCase(Locale.ROOT)) {
					case "pause" -> slaService.pause(slaId, body.expectedVersion(), body.reason(), requestId);
					case "resume" -> slaService.resume(slaId, body.expectedVersion(), body.reason(), requestId);
					case "complete" -> slaService.complete(slaId, body.expectedVersion(), body.reason(), requestId);
					default -> throw new com.civicos.common.domain.DomainValidationException(
							"Unsupported SLA action: " + action);
				});
	}

	public record CreateRequest(
			@NotBlank String targetType,
			@NotNull UUID targetId,
			@NotNull Sla.Type slaType,
			@NotNull SlaUrgency urgency,
			Instant startAt,
			@NotBlank String reason) {
	}

	public record MutationRequest(long expectedVersion, @NotBlank String reason) {
	}
}
