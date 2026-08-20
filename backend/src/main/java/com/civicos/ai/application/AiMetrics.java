package com.civicos.ai.application;

import java.time.Duration;

import org.springframework.stereotype.Component;

import com.civicos.ai.domain.AiRecommendation;
import com.civicos.ai.domain.AiTask;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
public class AiMetrics {

	private final MeterRegistry registry;

	public AiMetrics(MeterRegistry registry) {
		this.registry = registry;
	}

	public void record(
			AiTask task,
			String provider,
			String model,
			AiResponse response,
			long latencyMs,
			boolean failed) {
		String operation = task.name();
		Counter.builder("ai_requests_total")
				.tags("operation", operation, "provider", provider, "model", model)
				.register(registry).increment();
		Timer.builder("ai_latency")
				.tags("operation", operation, "provider", provider, "model", model)
				.register(registry).record(Duration.ofMillis(latencyMs));
		if (failed) {
			Counter.builder("ai_requests_failed")
					.tags("operation", operation, "provider", provider, "model", model)
					.register(registry).increment();
			return;
		}
		DistributionSummary.builder("ai_tokens_input")
				.tags("operation", operation, "provider", provider, "model", model)
				.register(registry).record(response.inputTokens());
		DistributionSummary.builder("ai_tokens_output")
				.tags("operation", operation, "provider", provider, "model", model)
				.register(registry).record(response.outputTokens());
		DistributionSummary.builder("ai_cost_estimate")
				.tags("operation", operation, "provider", provider, "model", model)
				.register(registry).record(response.estimatedCost().doubleValue());
		DistributionSummary.builder("ai_confidence_distribution")
				.tags("operation", operation, "provider", provider, "model", model)
				.register(registry).record(response.confidence().doubleValue());
	}

	public void recordReview(AiRecommendation.Status status) {
		String metric = status == AiRecommendation.Status.ACCEPTED
				? "ai_human_acceptance_rate"
				: "ai_human_rejection_rate";
		Counter.builder(metric).register(registry).increment();
	}
}
