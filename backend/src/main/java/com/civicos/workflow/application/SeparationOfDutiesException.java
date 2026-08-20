package com.civicos.workflow.application;

public class SeparationOfDutiesException extends RuntimeException {

	public SeparationOfDutiesException() {
		super("The current actor cannot perform this decision because of a separation-of-duties rule.");
	}
}
