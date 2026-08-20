package com.civicos.casefile.api;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.civicos.casefile.application.CitizenObservationService;
import com.civicos.casefile.application.CitizenValidationDecision;
import com.civicos.casefile.application.CitizenValidationResult;
import com.civicos.casefile.application.CreateCitizenObservationCommand;
import com.civicos.common.web.ApiIdempotencyService;
import com.civicos.common.web.CorrelationIdFilter;
import com.civicos.common.web.PageRequestFactory;
import com.civicos.common.web.PagedResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@RestController
@Validated
@RequestMapping("/api/v1")
@Tag(name = "Citizen observations", description = "Citizen reporting is an input to the authoritative CivicOS coordination lifecycle.")
public class CitizenObservationController {

	private final CitizenObservationService observationService;
	private final ApiIdempotencyService idempotencyService;
	private final PageRequestFactory pageRequestFactory;

	public CitizenObservationController(
			CitizenObservationService observationService,
			ApiIdempotencyService idempotencyService,
			PageRequestFactory pageRequestFactory) {
		this.observationService = observationService;
		this.idempotencyService = idempotencyService;
		this.pageRequestFactory = pageRequestFactory;
	}

	@PostMapping("/observations")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Submit a citizen-controlled road observation")
	public CitizenObservationResponse create(
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@Valid @RequestBody CreateObservationRequest body,
			HttpServletRequest request) {
		return idempotencyService.execute(idempotencyKey, "CITIZEN_OBSERVATION_CREATE", body,
				CitizenObservationResponse.class, () -> observationService.create(
						new CreateCitizenObservationCommand(
								body.category(), body.description(), body.latitude(), body.longitude(),
								body.roadSegmentId()),
						CorrelationIdFilter.requestId(request)));
	}

	@GetMapping("/observations/{observationId}")
	public CitizenObservationResponse byId(@PathVariable UUID observationId) {
		return observationService.byId(observationId);
	}

	@GetMapping("/me/observations")
	public PagedResponse<CitizenObservationResponse> mine(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "submittedAt,desc") String sort,
			HttpServletRequest request) {
		return PagedResponse.from(observationService.mine(pageRequestFactory.create(
				page, size, sort, Set.of("submittedAt", "status", "category"))),
				CorrelationIdFilter.requestId(request));
	}

	@GetMapping("/observations/road-candidates")
	public List<RoadCandidateResponse> roadCandidates(
			@RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") double latitude,
			@RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") double longitude) {
		return observationService.roadCandidates(latitude, longitude);
	}

	@PostMapping("/observations/{observationId}/validation")
	@Operation(summary = "Record supporting citizen feedback without mutating official workflow")
	public CitizenValidationResult validateResolution(
			@PathVariable UUID observationId,
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@Valid @RequestBody CitizenValidationRequest body,
			HttpServletRequest request) {
		return idempotencyService.execute(
				idempotencyKey, "CITIZEN_VALIDATION:" + observationId, body,
				CitizenValidationResult.class, () -> observationService.validateResolution(
						observationId, body.decision(), body.reason(),
						CorrelationIdFilter.requestId(request)));
	}

	public record CreateObservationRequest(
			@NotBlank @Size(max = 60) String category,
			@NotBlank @Size(max = 4000) String description,
			@NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
			@NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
			UUID roadSegmentId) {
	}

	public record CitizenValidationRequest(
			@NotNull CitizenValidationDecision decision,
			@Size(max = 2000) String reason) {
	}
}
