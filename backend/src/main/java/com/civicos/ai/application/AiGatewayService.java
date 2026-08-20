package com.civicos.ai.application;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.stereotype.Service;

import com.civicos.ai.config.AiProperties;
import com.civicos.ai.domain.AiTask;
import com.civicos.ai.prompt.AiPrompt;
import com.civicos.ai.prompt.AiPromptService;
import com.civicos.ai.provider.AiProvider;
import com.civicos.ai.provider.AiProviderException;
import com.civicos.ai.provider.AiProviderRegistry;
import com.civicos.auth.application.AuthorizationService;
import com.civicos.auth.security.CivicPrincipal;

@Service
public class AiGatewayService implements AiGateway {

	private final AiProperties properties;
	private final AuthorizationService authorizationService;
	private final AiRequestPolicy requestPolicy;
	private final AiPromptService promptService;
	private final AiProviderRegistry providerRegistry;
	private final AiResponseValidator responseValidator;
	private final AiResourceAccessPolicy resourceAccessPolicy;
	private final AiRunLifecycleService lifecycleService;
	private final AiMetrics metrics;
	private final ExecutorService aiProviderExecutor;

	public AiGatewayService(
			AiProperties properties,
			AuthorizationService authorizationService,
			AiRequestPolicy requestPolicy,
			AiPromptService promptService,
			AiProviderRegistry providerRegistry,
			AiResponseValidator responseValidator,
			AiResourceAccessPolicy resourceAccessPolicy,
			AiRunLifecycleService lifecycleService,
			AiMetrics metrics,
			ExecutorService aiProviderExecutor) {
		this.properties = properties;
		this.authorizationService = authorizationService;
		this.requestPolicy = requestPolicy;
		this.promptService = promptService;
		this.providerRegistry = providerRegistry;
		this.responseValidator = responseValidator;
		this.resourceAccessPolicy = resourceAccessPolicy;
		this.lifecycleService = lifecycleService;
		this.metrics = metrics;
		this.aiProviderExecutor = aiProviderExecutor;
	}

	@Override
	public AiExecutionResult execute(AiRequest request) {
		AiRequestPolicy.NormalizedRequest normalized = requestPolicy.validate(request);
		AiRequest safeRequest = normalized.request();
		authorizationService.authorize(safeRequest.task().permission());
		CivicPrincipal principal = authorizationService.currentPrincipal();
		resourceAccessPolicy.assertCanAnalyze(safeRequest, principal);
		if (!properties.isEnabled()) {
			return AiExecutionResult.disabled();
		}

		AiProperties.Selection configuredSelection;
		try {
			configuredSelection = properties.selection(safeRequest.task());
		} catch (RuntimeException exception) {
			return AiExecutionResult.unavailable("AI configuration is unavailable; use the manual path.");
		}
		AiProperties.Selection actualSelection = selectAvailableProvider(configuredSelection);
		AiPrompt prompt = promptService.load(safeRequest.task());
		Map<String, Object> configuration = Map.of(
				"timeoutMs", properties.getTimeout().toMillis(),
				"maxAttempts", properties.getMaxAttempts(),
				"temperatureMode", "provider-default",
				"advisoryOnly", true);
		AiRunStart start = startRun(
				safeRequest, principal.userId(), actualSelection, prompt,
				normalized.inputHash(), configuration);
		if (start.failure() != null) {
			return start.failure();
		}

		long startedNanos = System.nanoTime();
		try {
			AiResponse response = executeWithRetry(
					start.provider(), safeRequest, prompt, actualSelection.model());
			responseValidator.validate(safeRequest.task(), response);
			boolean reviewRequired = safeRequest.task() == AiTask.RECOMMENDATION_GENERATION
					|| response.confidence().compareTo(properties.getHumanReviewThreshold()) < 0;
			metrics.record(
					safeRequest.task(), actualSelection.provider(), actualSelection.model(),
					response, elapsedMillis(startedNanos), false);
			return lifecycleService.complete(
					start.runId(), response, reviewRequired, elapsedMillis(startedNanos), safeRequest.requestId());
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			metrics.record(
					safeRequest.task(), actualSelection.provider(), actualSelection.model(),
					null, elapsedMillis(startedNanos), true);
			return lifecycleService.fail(
					start.runId(), "AI execution was interrupted.", elapsedMillis(startedNanos), safeRequest.requestId());
		} catch (RuntimeException exception) {
			metrics.record(
					safeRequest.task(), actualSelection.provider(), actualSelection.model(),
					null, elapsedMillis(startedNanos), true);
			return lifecycleService.fail(
					start.runId(), safeFailureMessage(exception), elapsedMillis(startedNanos), safeRequest.requestId());
		}
	}

	private AiRunStart startRun(
			AiRequest request,
			UUID requesterId,
			AiProperties.Selection selection,
			AiPrompt prompt,
			String inputHash,
			Map<String, Object> configuration) {
		AiProvider provider = providerRegistry.find(selection.provider()).orElse(null);
		var run = lifecycleService.start(
				request, requesterId, selection, prompt, inputHash, configuration);
		if (provider == null) {
			return new AiRunStart(
					run.getId(), null,
					lifecycleService.fail(
							run.getId(), "Configured AI provider is unavailable.", 0, request.requestId()));
		}
		return new AiRunStart(run.getId(), provider, null);
	}

	private AiProperties.Selection selectAvailableProvider(AiProperties.Selection primary) {
		if (providerRegistry.find(primary.provider()).isPresent()) {
			return primary;
		}
		String fallback = properties.getFallbackProvider();
		if (fallback != null && !fallback.isBlank() && providerRegistry.find(fallback).isPresent()) {
			return new AiProperties.Selection(fallback.strip(), primary.model());
		}
		return primary;
	}

	private AiResponse executeWithRetry(
			AiProvider provider,
			AiRequest request,
			AiPrompt prompt,
			String model) throws InterruptedException {
		RuntimeException lastFailure = null;
		for (int attempt = 1; attempt <= properties.getMaxAttempts(); attempt++) {
			try {
				return executeOnce(provider, request, prompt, model);
			} catch (AiProviderException exception) {
				lastFailure = exception;
				if (!exception.isRetryable() || attempt == properties.getMaxAttempts()) {
					throw exception;
				}
				waitBeforeRetry(properties.getRetryDelay());
			}
		}
		throw lastFailure == null
				? new AiProviderException("AI provider did not return a response.", false)
				: lastFailure;
	}

	private AiResponse executeOnce(
			AiProvider provider,
			AiRequest request,
			AiPrompt prompt,
			String model) throws InterruptedException {
		Future<AiResponse> future = aiProviderExecutor.submit(() -> provider.execute(request, prompt, model));
		try {
			return future.get(properties.getTimeout().toMillis(), TimeUnit.MILLISECONDS);
		} catch (TimeoutException exception) {
			future.cancel(true);
			throw new AiProviderException("AI provider timed out.", true, exception);
		} catch (ExecutionException exception) {
			Throwable cause = exception.getCause();
			if (cause instanceof AiProviderException providerException) {
				throw providerException;
			}
			throw new AiProviderException("AI provider failed.", false, cause);
		}
	}

	private void waitBeforeRetry(Duration delay) throws InterruptedException {
		if (!delay.isZero()) {
			Thread.sleep(delay.toMillis());
		}
	}

	private long elapsedMillis(long startedNanos) {
		return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos);
	}

	private String safeFailureMessage(RuntimeException exception) {
		if (exception instanceof AiInvalidResponseException) {
			return "AI response failed schema, semantic, or policy validation.";
		}
		if (exception instanceof AiProviderException providerException) {
			return providerException.isRetryable()
					? "AI provider remained unavailable after bounded retries."
					: "AI provider rejected or failed the advisory request.";
		}
		return "AI execution failed; use the deterministic/manual path.";
	}

	private record AiRunStart(UUID runId, AiProvider provider, AiExecutionResult failure) {
	}
}
