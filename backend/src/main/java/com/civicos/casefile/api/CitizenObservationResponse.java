package com.civicos.casefile.api;

import java.time.Instant;
import java.util.UUID;

import com.civicos.casefile.domain.CitizenObservation;

public record CitizenObservationResponse(
		UUID observationId,
		UUID caseId,
		String trackingId,
		String category,
		String description,
		double latitude,
		double longitude,
		RoadCandidateResponse detectedRoad,
		Instant submittedAt,
		CitizenObservation.Status status,
		String caseStatus,
		String publicStatus,
		String publicMessage,
		String suggestedCategory) {

	public static CitizenObservationResponse from(CitizenObservation observation) {
		return new CitizenObservationResponse(
				observation.getId(),
				observation.getCivicCase().getId(),
				observation.getCivicCase().getCaseNumber(),
				observation.getCategory(),
				observation.getDescription(),
				observation.getLocation().getY(),
				observation.getLocation().getX(),
				RoadCandidateResponse.from(observation.getRoadSegment()),
				observation.getSubmittedAt(),
				observation.getStatus(),
				observation.getCivicCase().getStatus().name(),
				publicStatus(observation),
				publicMessage(observation),
				observation.getAiSuggestedCategory());
	}

	private static String publicStatus(CitizenObservation observation) {
		if (observation.getStatus() == CitizenObservation.Status.RESOLVED) {
			return "COMPLETED";
		}
		return switch (observation.getCivicCase().getStatus()) {
			case PENDING_VERIFICATION, VERIFIED -> "VERIFICATION";
			case IN_PROGRESS -> "WORK_IN_PROGRESS";
			case OPEN, UNDER_REVIEW -> switch (observation.getStatus()) {
			case SUBMITTED -> "REPORT_RECEIVED";
			case TRIAGED, FLAGGED -> "UNDER_REVIEW";
			case MATCHED -> "MATCHED_TO_WORK";
			case FORWARDED -> "ACTION_ASSIGNED";
			case RESOLVED -> "COMPLETED";
			case DISMISSED, DUPLICATE -> "CLOSED";
			};
			case CLOSED -> "CLOSED";
		};
	}

	private static String publicMessage(CitizenObservation observation) {
		return switch (publicStatus(observation)) {
			case "REPORT_RECEIVED" -> "Your report has been received and is waiting for review.";
			case "UNDER_REVIEW" -> "The report is being reviewed and routed.";
			case "MATCHED_TO_WORK" -> "The report has been linked to relevant road work.";
			case "ACTION_ASSIGNED" -> "Action has been assigned to the responsible team.";
			case "WORK_IN_PROGRESS" -> "Work associated with this report is in progress.";
			case "VERIFICATION" -> "The completed work is being verified.";
			case "COMPLETED" -> "The reported issue is ready for your resolution feedback.";
			case "CLOSED" -> observation.getStatus() == CitizenObservation.Status.DUPLICATE
					? "This report is covered by another active report."
					: "The review is complete and no further action is planned.";
			default -> "Your report has a new public status update.";
		};
	}
}
