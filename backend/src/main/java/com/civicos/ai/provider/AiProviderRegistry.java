package com.civicos.ai.provider;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class AiProviderRegistry {

	private final Map<String, AiProvider> providers;

	public AiProviderRegistry(List<AiProvider> providers) {
		Map<String, AiProvider> registered = new LinkedHashMap<>();
		for (AiProvider provider : providers) {
			String key = provider.providerId().strip().toLowerCase(Locale.ROOT);
			if (registered.putIfAbsent(key, provider) != null) {
				throw new IllegalStateException("Duplicate AI provider registration: " + key);
			}
		}
		this.providers = Map.copyOf(registered);
	}

	public Optional<AiProvider> find(String providerId) {
		if (providerId == null || providerId.isBlank()) {
			return Optional.empty();
		}
		return Optional.ofNullable(providers.get(providerId.strip().toLowerCase(Locale.ROOT)));
	}
}
