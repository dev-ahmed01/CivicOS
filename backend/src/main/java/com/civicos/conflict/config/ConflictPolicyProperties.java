package com.civicos.conflict.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

@Component
@Validated
@ConfigurationProperties(prefix = "civicos.conflict")
public class ConflictPolicyProperties {

	@DecimalMin("0.0")
	private double proximityMeters;

	@Positive
	private int repeatDiggingWindowDays = 30;

	@Min(2)
	private int multiAgencyMinimum = 3;

	public double getProximityMeters() { return proximityMeters; }
	public int getRepeatDiggingWindowDays() { return repeatDiggingWindowDays; }
	public int getMultiAgencyMinimum() { return multiAgencyMinimum; }

	public void setProximityMeters(double proximityMeters) {
		this.proximityMeters = proximityMeters;
	}

	public void setRepeatDiggingWindowDays(int repeatDiggingWindowDays) {
		this.repeatDiggingWindowDays = repeatDiggingWindowDays;
	}

	public void setMultiAgencyMinimum(int multiAgencyMinimum) {
		this.multiAgencyMinimum = multiAgencyMinimum;
	}
}
