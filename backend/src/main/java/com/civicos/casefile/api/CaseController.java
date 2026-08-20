package com.civicos.casefile.api;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.civicos.casefile.application.CaseQueryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/cases")
@Tag(name = "Cases", description = "A case is the traceable coordination/problem container for observations and one or more interventions.")
public class CaseController {

	private final CaseQueryService queryService;

	public CaseController(CaseQueryService queryService) {
		this.queryService = queryService;
	}

	@GetMapping("/{caseId}")
	@Operation(summary = "Get a case with its observation and intervention relationships")
	public CaseResponse byId(@PathVariable UUID caseId) {
		return queryService.byId(caseId);
	}
}
