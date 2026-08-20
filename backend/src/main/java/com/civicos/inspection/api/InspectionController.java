package com.civicos.inspection.api;

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
import com.civicos.inspection.application.CompleteInspectionCommand;
import com.civicos.inspection.application.InspectionResult;
import com.civicos.inspection.application.InspectionService;
import com.civicos.inspection.domain.Inspection;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Inspections")
public class InspectionController {

	private final InspectionService inspectionService;
	private final ApiIdempotencyService idempotencyService;

	public InspectionController(InspectionService inspectionService, ApiIdempotencyService idempotencyService) {
		this.inspectionService = inspectionService;
		this.idempotencyService = idempotencyService;
	}

	@PostMapping("/interventions/{interventionId}/inspections")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Schedule an inspection")
	public InspectionResult schedule(
			@PathVariable UUID interventionId,
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@Valid @RequestBody ReasonRequest body,
			HttpServletRequest request) {
		return execute(idempotencyKey, "INSPECTION_SCHEDULE:" + interventionId, body,
				() -> inspectionService.schedule(interventionId, body.reason(),
						CorrelationIdFilter.requestId(request)));
	}

	@PostMapping("/interventions/{interventionId}/reinspections")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Request a reinspection")
	public InspectionResult reinspection(
			@PathVariable UUID interventionId,
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@Valid @RequestBody ReasonRequest body,
			HttpServletRequest request) {
		return execute(idempotencyKey, "REINSPECTION_REQUEST:" + interventionId, body,
				() -> inspectionService.requestReinspection(interventionId, body.reason(),
						CorrelationIdFilter.requestId(request)));
	}

	@PostMapping("/inspections/{inspectionId}/start")
	public InspectionResult start(
			@PathVariable UUID inspectionId,
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@Valid @RequestBody VersionRequest body,
			HttpServletRequest request) {
		return execute(idempotencyKey, "INSPECTION_START:" + inspectionId, body,
				() -> inspectionService.start(inspectionId, body.expectedVersion(),
						CorrelationIdFilter.requestId(request)));
	}

	@PostMapping("/inspections/{inspectionId}/complete")
	public InspectionResult complete(
			@PathVariable UUID inspectionId,
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@Valid @RequestBody CompleteRequest body,
			HttpServletRequest request) {
		return execute(idempotencyKey, "INSPECTION_COMPLETE:" + inspectionId, body,
				() -> inspectionService.complete(inspectionId,
						new CompleteInspectionCommand(body.result(), body.notes(), body.expectedVersion()),
						CorrelationIdFilter.requestId(request)));
	}

	private InspectionResult execute(
			String key, String operation, Object body, java.util.function.Supplier<InspectionResult> action) {
		return idempotencyService.execute(key, operation, body, InspectionResult.class, action);
	}

	public record ReasonRequest(@NotBlank String reason) {
	}

	public record VersionRequest(long expectedVersion) {
	}

	public record CompleteRequest(
			@NotNull Inspection.Result result,
			String notes,
			long expectedVersion) {
	}
}
