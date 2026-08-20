package com.civicos.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

@Component
@Validated
@ConfigurationProperties(prefix = "civicos.notification")
public class NotificationProperties {

	private boolean dispatcherEnabled = true;

	@Positive
	private long dispatcherIntervalMs = 5_000;

	@Min(1)
	@Max(500)
	private int batchSize = 50;

	@Min(1)
	@Max(20)
	private int maxAttempts = 5;

	@Positive
	private long retryDelaySeconds = 30;

	public boolean isDispatcherEnabled() { return dispatcherEnabled; }
	public long getDispatcherIntervalMs() { return dispatcherIntervalMs; }
	public int getBatchSize() { return batchSize; }
	public int getMaxAttempts() { return maxAttempts; }
	public long getRetryDelaySeconds() { return retryDelaySeconds; }

	public void setDispatcherEnabled(boolean dispatcherEnabled) {
		this.dispatcherEnabled = dispatcherEnabled;
	}

	public void setDispatcherIntervalMs(long dispatcherIntervalMs) {
		this.dispatcherIntervalMs = dispatcherIntervalMs;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	public void setMaxAttempts(int maxAttempts) {
		this.maxAttempts = maxAttempts;
	}

	public void setRetryDelaySeconds(long retryDelaySeconds) {
		this.retryDelaySeconds = retryDelaySeconds;
	}
}
