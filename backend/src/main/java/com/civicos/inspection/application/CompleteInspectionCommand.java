package com.civicos.inspection.application;

import com.civicos.inspection.domain.Inspection;

public record CompleteInspectionCommand(
		Inspection.Result result,
		String notes,
		long expectedVersion) {
}
