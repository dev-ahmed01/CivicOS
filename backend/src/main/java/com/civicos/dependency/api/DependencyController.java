package com.civicos.dependency.api;

import java.util.UUID;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.civicos.common.application.DomainMutationResult;
import com.civicos.common.web.ApiIdempotencyService;
import com.civicos.common.web.CorrelationIdFilter;
import com.civicos.dependency.application.CreateDependencyCommand;
import com.civicos.dependency.application.DependencyQueryService;
import com.civicos.dependency.application.DependencyManagementService;
import com.civicos.dependency.domain.Dependency;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Dependencies")
public class DependencyController {

	private final DependencyManagementService dependencyService;
	private final DependencyQueryService queryService;
	private final ApiIdempotencyService idempotencyService;

	public DependencyController(
			DependencyManagementService dependencyService,
			DependencyQueryService queryService,
			ApiIdempotencyService idempotencyService) {
		this.dependencyService = dependencyService;
		this.queryService = queryService;
		this.idempotencyService = idempotencyService;
	}

	@GetMapping("/interventions/{interventionId}/dependencies")
	public List<DependencyResponse> forIntervention(@PathVariable UUID interventionId) {
		return queryService.forIntervention(interventionId);
	}

	@PostMapping("/dependencies")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create an intervention dependency")
	public DomainMutationResult create(
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@Valid @RequestBody CreateRequest body,
			HttpServletRequest request) {
		return idempotencyService.execute(idempotencyKey, "DEPENDENCY_CREATE", body,
				DomainMutationResult.class, () -> dependencyService.create(body.toCommand(),
						CorrelationIdFilter.requestId(request)));
	}

	@PostMapping("/dependencies/{dependencyId}/cancel")
	@Operation(summary = "Cancel a dependency without deleting its audit history")
	public DomainMutationResult cancel(
			@PathVariable UUID dependencyId,
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@Valid @RequestBody CancelRequest body,
			HttpServletRequest request) {
		return idempotencyService.execute(idempotencyKey, "DEPENDENCY_CANCEL:" + dependencyId, body,
				DomainMutationResult.class, () -> dependencyService.cancel(dependencyId,
						body.expectedVersion(), body.reason(), CorrelationIdFilter.requestId(request)));
	}

	public record CreateRequest(
			@NotNull UUID sourceInterventionId,
			@NotNull UUID targetInterventionId,
			@NotNull Dependency.Type type,
			boolean required,
			@NotBlank String reason) {
		CreateDependencyCommand toCommand() {
			return new CreateDependencyCommand(
					sourceInterventionId, targetInterventionId, type, required, reason);
		}
	}

	public record CancelRequest(long expectedVersion, @NotBlank String reason) {
	}
}
