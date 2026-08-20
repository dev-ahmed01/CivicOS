package com.civicos.ai.provider;

import com.civicos.ai.application.AiRequest;
import com.civicos.ai.application.AiResponse;
import com.civicos.ai.prompt.AiPrompt;

public interface AiProvider {
	String providerId();

	AiResponse execute(AiRequest request, AiPrompt prompt, String model);
}
