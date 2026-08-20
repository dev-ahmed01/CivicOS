package com.civicos.sla.config;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Positive;

@Component
@Validated
@ConfigurationProperties(prefix = "civicos.sla")
public class SlaPolicyProperties {

	private ZoneId businessZone = ZoneId.of("Asia/Kolkata");
	private LocalTime businessDayStart = LocalTime.of(9, 0);
	private LocalTime businessDayEnd = LocalTime.of(18, 0);
	private List<java.time.LocalDate> holidays = new ArrayList<>();
	@Positive
	private int atRiskBeforeHours = 4;
	@Positive private int reviewWorkingHours = 9;
	@Positive private int coordinationMediumWorkingHours = 18;
	@Positive private int coordinationHighWorkingHours = 9;
	@Positive private int coordinationCriticalWorkingHours = 4;
	@Positive private int approvalWorkingHours = 9;
	@Positive private int preWorkWorkingHours = 9;
	@Positive private int executionWorkingHours = 9;
	@Positive private int evidenceWorkingHours = 9;
	@Positive private int verificationWorkingHours = 9;
	@Positive private int correctionWorkingHours = 18;
	@Positive private int closureWorkingHours = 9;

	public ZoneId getBusinessZone() { return businessZone; }
	public LocalTime getBusinessDayStart() { return businessDayStart; }
	public LocalTime getBusinessDayEnd() { return businessDayEnd; }
	public List<java.time.LocalDate> getHolidays() { return List.copyOf(holidays); }
	public int getAtRiskBeforeHours() { return atRiskBeforeHours; }
	public int getReviewWorkingHours() { return reviewWorkingHours; }
	public int getCoordinationMediumWorkingHours() { return coordinationMediumWorkingHours; }
	public int getCoordinationHighWorkingHours() { return coordinationHighWorkingHours; }
	public int getCoordinationCriticalWorkingHours() { return coordinationCriticalWorkingHours; }
	public int getApprovalWorkingHours() { return approvalWorkingHours; }
	public int getPreWorkWorkingHours() { return preWorkWorkingHours; }
	public int getExecutionWorkingHours() { return executionWorkingHours; }
	public int getEvidenceWorkingHours() { return evidenceWorkingHours; }
	public int getVerificationWorkingHours() { return verificationWorkingHours; }
	public int getCorrectionWorkingHours() { return correctionWorkingHours; }
	public int getClosureWorkingHours() { return closureWorkingHours; }

	public void setBusinessZone(ZoneId businessZone) { this.businessZone = businessZone; }
	public void setBusinessDayStart(LocalTime value) { this.businessDayStart = value; }
	public void setBusinessDayEnd(LocalTime value) { this.businessDayEnd = value; }
	public void setHolidays(List<java.time.LocalDate> values) {
		this.holidays = values == null ? new ArrayList<>() : new ArrayList<>(values);
	}
	public void setAtRiskBeforeHours(int value) { this.atRiskBeforeHours = value; }
	public void setReviewWorkingHours(int value) { this.reviewWorkingHours = value; }
	public void setCoordinationMediumWorkingHours(int value) { this.coordinationMediumWorkingHours = value; }
	public void setCoordinationHighWorkingHours(int value) { this.coordinationHighWorkingHours = value; }
	public void setCoordinationCriticalWorkingHours(int value) { this.coordinationCriticalWorkingHours = value; }
	public void setApprovalWorkingHours(int value) { this.approvalWorkingHours = value; }
	public void setPreWorkWorkingHours(int value) { this.preWorkWorkingHours = value; }
	public void setExecutionWorkingHours(int value) { this.executionWorkingHours = value; }
	public void setEvidenceWorkingHours(int value) { this.evidenceWorkingHours = value; }
	public void setVerificationWorkingHours(int value) { this.verificationWorkingHours = value; }
	public void setCorrectionWorkingHours(int value) { this.correctionWorkingHours = value; }
	public void setClosureWorkingHours(int value) { this.closureWorkingHours = value; }
}
