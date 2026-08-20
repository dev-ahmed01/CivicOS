package com.civicos.sla.api;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.civicos.common.web.ApiIdempotencyService;
import com.civicos.common.web.CorrelationIdFilter;
import com.civicos.sla.application.CreateSlaCommand;
import com.civicos.sla.application.SlaResult;
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
	private final ApiIdempotencyService idempotencyService;

	public SlaController(SlaService slaService, ApiIdempotencyService idempotencyService) {
		this.slaService = slaService;
		this.idempotencyService = idempotencyService;
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
