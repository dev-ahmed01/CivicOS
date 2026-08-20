package com.civicos.ai.config;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.civicos.ai.domain.AiTask;
import com.civicos.common.domain.DomainValidationException;

@ConfigurationProperties(prefix = "civicos.ai")
public class AiProperties {

	private boolean enabled;
	private String provider = "mock";
	private String fallbackProvider;
	private String model;
	private Duration timeout = Duration.ofSeconds(20);
	private int maxAttempts = 2;
	private Duration retryDelay = Duration.ofMillis(100);
	private BigDecimal humanReviewThreshold = new BigDecimal("0.70");
	private final Map<AiTask, TaskSelection> tasks = new EnumMap<>(AiTask.class);

	public boolean isEnabled() { return enabled; }
	public void setEnabled(boolean enabled) { this.enabled = enabled; }
	public String getProvider() { return provider; }
	public void setProvider(String provider) { this.provider = provider; }
	public String getFallbackProvider() { return fallbackProvider; }
	public void setFallbackProvider(String fallbackProvider) { this.fallbackProvider = fallbackProvider; }
	public String getModel() { return model; }
	public void setModel(String model) { this.model = model; }
	public Duration getTimeout() { return timeout; }
	public void setTimeout(Duration timeout) { this.timeout = timeout; }
	public int getMaxAttempts() { return maxAttempts; }
	public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
	public Duration getRetryDelay() { return retryDelay; }
	public void setRetryDelay(Duration retryDelay) { this.retryDelay = retryDelay; }
	public BigDecimal getHumanReviewThreshold() { return humanReviewThreshold; }
	public void setHumanReviewThreshold(BigDecimal humanReviewThreshold) {
		this.humanReviewThreshold = humanReviewThreshold;
	}
	public Map<AiTask, TaskSelection> getTasks() { return tasks; }

	public Selection selection(AiTask task) {
		validateRuntimeConfiguration();
		TaskSelection taskSelection = tasks.get(task);
		String selectedProvider = valueOrDefault(
				taskSelection == null ? null : taskSelection.getProvider(), provider);
		String selectedModel = valueOrDefault(
				taskSelection == null ? null : taskSelection.getModel(), model);
		if (selectedProvider == null || selectedModel == null) {
			throw new DomainValidationException("AI provider and model must be configured when AI is enabled.");
		}
		return new Selection(selectedProvider, selectedModel);
	}

	private void validateRuntimeConfiguration() {
		if (timeout == null || timeout.isZero() || timeout.isNegative()
				|| timeout.compareTo(Duration.ofSeconds(60)) > 0) {
			throw new DomainValidationException("AI timeout must be between 1 millisecond and 60 seconds.");
		}
		if (maxAttempts < 1 || maxAttempts > 3) {
			throw new DomainValidationException("AI max attempts must be between 1 and 3.");
		}
		if (retryDelay == null || retryDelay.isNegative()
				|| retryDelay.compareTo(Duration.ofSeconds(5)) > 0) {
			throw new DomainValidationException("AI retry delay must be between 0 and 5 seconds.");
		}
		if (humanReviewThreshold == null
				|| humanReviewThreshold.compareTo(BigDecimal.ZERO) < 0
				|| humanReviewThreshold.compareTo(BigDecimal.ONE) > 0) {
			throw new DomainValidationException("AI human review threshold must be between 0 and 1.");
		}
	}

	private String valueOrDefault(String value, String defaultValue) {
		if (value != null && !value.isBlank()) {
			return value.strip();
		}
		return defaultValue == null || defaultValue.isBlank() ? null : defaultValue.strip();
	}

	public record Selection(String provider, String model) {
	}

	public static class TaskSelection {
		private String provider;
		private String model;

		public String getProvider() { return provider; }
		public void setProvider(String provider) { this.provider = provider; }
		public String getModel() { return model; }
		public void setModel(String model) { this.model = model; }
	}
}
