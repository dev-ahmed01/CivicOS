package com.civicos.sla.application;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;

import org.springframework.stereotype.Component;

import com.civicos.common.domain.DomainValidationException;
import com.civicos.sla.config.SlaPolicyProperties;
import com.civicos.sla.domain.Sla;

@Component
public class SlaDeadlineCalculator {

	private final SlaPolicyProperties policy;

	public SlaDeadlineCalculator(SlaPolicyProperties policy) {
		this.policy = policy;
	}

	public Instant calculate(Instant startAt, Sla.Type type, SlaUrgency urgency) {
		if (!policy.getBusinessDayEnd().isAfter(policy.getBusinessDayStart())) {
			throw new DomainValidationException("Business day end must be after business day start.");
		}
		return addWorkingHours(startAt, workingHours(type, urgency));
	}

	private int workingHours(Sla.Type type, SlaUrgency urgency) {
		if (type == Sla.Type.COORDINATION) {
			return switch (urgency == null ? SlaUrgency.MEDIUM : urgency) {
				case CRITICAL -> policy.getCoordinationCriticalWorkingHours();
				case HIGH -> policy.getCoordinationHighWorkingHours();
				case MEDIUM, STANDARD -> policy.getCoordinationMediumWorkingHours();
			};
		}
		return switch (type) {
			case REVIEW -> policy.getReviewWorkingHours();
			case APPROVAL -> policy.getApprovalWorkingHours();
			case PRE_WORK -> policy.getPreWorkWorkingHours();
			case EXECUTION -> policy.getExecutionWorkingHours();
			case EVIDENCE -> policy.getEvidenceWorkingHours();
			case VERIFICATION -> policy.getVerificationWorkingHours();
			case CORRECTION -> policy.getCorrectionWorkingHours();
			case CLOSURE -> policy.getClosureWorkingHours();
			case COORDINATION -> throw new IllegalStateException();
		};
	}

	private Instant addWorkingHours(Instant startAt, int hours) {
		ZonedDateTime cursor = nextWorkingInstant(startAt.atZone(policy.getBusinessZone()));
		Duration remaining = Duration.ofHours(hours);
		while (!remaining.isZero()) {
			ZonedDateTime endOfDay = cursor.toLocalDate()
					.atTime(policy.getBusinessDayEnd()).atZone(policy.getBusinessZone());
			Duration available = Duration.between(cursor, endOfDay);
			if (remaining.compareTo(available) <= 0) {
				return cursor.plus(remaining).toInstant();
			}
			remaining = remaining.minus(available);
			cursor = nextWorkingInstant(endOfDay.plusNanos(1));
		}
		return cursor.toInstant();
	}

	private ZonedDateTime nextWorkingInstant(ZonedDateTime value) {
		ZonedDateTime cursor = value;
		while (true) {
			LocalDate date = cursor.toLocalDate();
			if (!workingDay(date)) {
				cursor = date.plusDays(1).atTime(policy.getBusinessDayStart())
						.atZone(policy.getBusinessZone());
				continue;
			}
			ZonedDateTime start = date.atTime(policy.getBusinessDayStart())
					.atZone(policy.getBusinessZone());
			ZonedDateTime end = date.atTime(policy.getBusinessDayEnd())
					.atZone(policy.getBusinessZone());
			if (cursor.isBefore(start)) {
				return start;
			}
			if (!cursor.isBefore(end)) {
				cursor = date.plusDays(1).atTime(policy.getBusinessDayStart())
						.atZone(policy.getBusinessZone());
				continue;
			}
			return cursor;
		}
	}

	private boolean workingDay(LocalDate date) {
		return date.getDayOfWeek() != DayOfWeek.SATURDAY
				&& date.getDayOfWeek() != DayOfWeek.SUNDAY
				&& !policy.getHolidays().contains(date);
	}
}
