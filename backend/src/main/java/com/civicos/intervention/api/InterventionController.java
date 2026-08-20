package com.civicos.intervention.api;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.civicos.common.application.DomainMutationResult;
import com.civicos.common.domain.DomainValidationException;
import com.civicos.common.web.ApiIdempotencyService;
import com.civicos.common.web.CorrelationIdFilter;
import com.civicos.common.web.PageRequestFactory;
import com.civicos.common.web.PagedResponse;
import com.civicos.intervention.application.CreateInterventionCommand;
import com.civicos.intervention.application.InterventionManagementService;
import com.civicos.intervention.application.InterventionQueryService;
import com.civicos.intervention.application.UpdateDraftInterventionCommand;
import com.civicos.intervention.domain.Intervention;
import com.civicos.workflow.application.InterventionWorkflowService;
import com.civicos.workflow.application.WorkflowTransitionResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/v1/interventions")
@Tag(name = "Interventions")
public class InterventionController {

	private final InterventionQueryService queryService;
	private final InterventionManagementService managementService;
	private final InterventionWorkflowService workflowService;
	private final PageRequestFactory pageRequestFactory;
	private final ApiIdempotencyService idempotencyService;

	public InterventionController(
			InterventionQueryService queryService,
			InterventionManagementService managementService,
			InterventionWorkflowService workflowService,
			PageRequestFactory pageRequestFactory,
			ApiIdempotencyService idempotencyService) {
		this.queryService = queryService;
		this.managementService = managementService;
		this.workflowService = workflowService;
		this.pageRequestFactory = pageRequestFactory;
		this.idempotencyService = idempotencyService;
	}

	@GetMapping
	@Operation(summary = "List interventions", description = "Returns an agency-scoped, bounded collection.")
	public PagedResponse<InterventionResponse> list(
			@RequestParam(required = false) Set<Intervention.Status> status,
			@RequestParam(required = false) UUID agencyId,
			@RequestParam(required = false) UUID roadSegmentId,
			@RequestParam(required = false) Intervention.Priority priority,
			@RequestParam(required = false) Instant plannedFrom,
			@RequestParam(required = false) Instant plannedTo,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "createdAt,desc") String sort,
			HttpServletRequest request) {
		return PagedResponse.from(
				queryService.list(status, agencyId, roadSegmentId, priority, plannedFrom, plannedTo,
						pageRequestFactory.create(page, size, sort, Set.of(
								"createdAt", "updatedAt", "plannedStart", "plannedEnd",
								"priority", "status", "interventionNumber"))),
				CorrelationIdFilter.requestId(request));
	}

	@GetMapping("/{interventionId}")
	public InterventionResponse byId(@PathVariable UUID interventionId) {
		return queryService.byId(interventionId);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create a draft intervention")
	public DomainMutationResult create(
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@Valid @RequestBody InterventionWriteRequest body,
			HttpServletRequest request) {
		return idempotencyService.execute(idempotencyKey, "INTERVENTION_CREATE", body,
				DomainMutationResult.class,
				() -> managementService.create(body.toCreateCommand(), body.reason(),
						CorrelationIdFilter.requestId(request)));
	}

	@PatchMapping("/{interventionId}")
	@Operation(summary = "Update a draft intervention", description = "Status cannot be changed with this endpoint.")
	public DomainMutationResult updateDraft(
			@PathVariable UUID interventionId,
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@Valid @RequestBody InterventionWriteRequest body,
			HttpServletRequest request) {
		return idempotencyService.execute(idempotencyKey, "INTERVENTION_UPDATE:" + interventionId, body,
				DomainMutationResult.class,
				() -> managementService.updateDraft(interventionId, body.toUpdateCommand(), body.reason(),
						CorrelationIdFilter.requestId(request)));
	}

	@PostMapping("/{interventionId}/actions/{action}")
	@Operation(summary = "Execute an intervention workflow transition",
			description = "Supported actions follow the authoritative workflow graph; arbitrary status mutation is not allowed.")
	public WorkflowTransitionResult transition(
			@PathVariable UUID interventionId,
			@PathVariable String action,
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@Valid @RequestBody WorkflowRequest body,
			HttpServletRequest request) {
		Intervention.WorkflowAction workflowAction = workflowAction(action);
		return executeTransition(
				interventionId, workflowAction, idempotencyKey, body, request);
	}

	@PostMapping("/{interventionId}/submit")
	@Operation(summary = "Submit a draft intervention")
	public WorkflowTransitionResult submit(
			@PathVariable UUID interventionId,
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@Valid @RequestBody WorkflowRequest body,
			HttpServletRequest request) {
		return executeTransition(interventionId, Intervention.WorkflowAction.SUBMIT,
				idempotencyKey, body, request);
	}

	@PostMapping("/{interventionId}/start")
	@Operation(summary = "Start scheduled intervention work")
	public WorkflowTransitionResult start(
			@PathVariable UUID interventionId,
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@Valid @RequestBody WorkflowRequest body,
			HttpServletRequest request) {
		return executeTransition(interventionId, Intervention.WorkflowAction.START,
				idempotencyKey, body, request);
	}

	@PostMapping("/{interventionId}/complete")
	@Operation(summary = "Complete primary work and begin restoration")
	public WorkflowTransitionResult complete(
			@PathVariable UUID interventionId,
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@Valid @RequestBody WorkflowRequest body,
			HttpServletRequest request) {
		return executeTransition(interventionId, Intervention.WorkflowAction.COMPLETE,
				idempotencyKey, body, request);
	}

	@PostMapping("/{interventionId}/complete-restoration")
	@Operation(summary = "Complete restoration and begin evidence collection")
	public WorkflowTransitionResult completeRestoration(
			@PathVariable UUID interventionId,
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@Valid @RequestBody WorkflowRequest body,
			HttpServletRequest request) {
		return executeTransition(interventionId, Intervention.WorkflowAction.COMPLETE_RESTORATION,
				idempotencyKey, body, request);
	}

	@PostMapping("/{interventionId}/submit-evidence")
	@Operation(summary = "Submit accepted evidence and request verification")
	public WorkflowTransitionResult submitEvidence(
			@PathVariable UUID interventionId,
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@Valid @RequestBody WorkflowRequest body,
			HttpServletRequest request) {
		return executeTransition(interventionId, Intervention.WorkflowAction.SUBMIT_EVIDENCE,
				idempotencyKey, body, request);
	}

	private WorkflowTransitionResult executeTransition(
			UUID interventionId,
			Intervention.WorkflowAction workflowAction,
			String idempotencyKey,
			WorkflowRequest body,
			HttpServletRequest request) {
		return idempotencyService.execute(idempotencyKey,
				"INTERVENTION_WORKFLOW:" + interventionId + ":" + workflowAction, body,
				WorkflowTransitionResult.class,
				() -> workflowService.transition(interventionId, workflowAction, body.expectedVersion(),
						body.reason(), CorrelationIdFilter.requestId(request)));
	}

	private Intervention.WorkflowAction workflowAction(String action) {
		try {
			return Intervention.WorkflowAction.valueOf(action.replace('-', '_').toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new DomainValidationException("Unsupported intervention workflow action: " + action);
		}
	}

	public record InterventionWriteRequest(
			String interventionNumber,
			@NotNull UUID caseId,
			@NotNull UUID agencyId,
			@NotNull Intervention.Type type,
			@NotBlank String description,
			@NotNull UUID roadSegmentId,
			@NotBlank String geometryWkt,
			@NotNull Instant plannedStart,
			@NotNull Instant plannedEnd,
			@NotNull Intervention.Priority priority,
			long expectedVersion,
			@NotBlank String reason) {

		CreateInterventionCommand toCreateCommand() {
			return new CreateInterventionCommand(interventionNumber, caseId, agencyId, type, description,
					roadSegmentId, geometry(), plannedStart, plannedEnd, priority);
		}

		UpdateDraftInterventionCommand toUpdateCommand() {
			return new UpdateDraftInterventionCommand(caseId, agencyId, type, description,
					roadSegmentId, geometry(), plannedStart, plannedEnd, priority, expectedVersion);
		}

		private Geometry geometry() {
			try {
				Geometry geometry = new WKTReader().read(geometryWkt);
				geometry.setSRID(4326);
				return geometry;
			} catch (ParseException exception) {
				throw new DomainValidationException("Intervention geometryWkt is invalid.");
			}
		}
	}

	public record WorkflowRequest(long expectedVersion, @NotBlank String reason) {
	}
}
