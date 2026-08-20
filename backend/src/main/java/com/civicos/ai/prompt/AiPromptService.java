package com.civicos.ai.prompt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.civicos.ai.domain.AiTask;

@Service
public class AiPromptService {

	private static final String VERSION = "v1";

	public AiPrompt load(AiTask task) {
		String directory = task.resourceDirectory();
		ClassPathResource prompt = new ClassPathResource(
				"ai/prompts/" + directory + "/" + VERSION + ".txt");
		ClassPathResource schema = new ClassPathResource(
				"ai/schemas/" + directory + "/" + VERSION + ".json");
		if (!prompt.exists() || !schema.exists()) {
			throw new IllegalStateException("Versioned AI prompt or schema is missing for " + task + ".");
		}
		try {
			return new AiPrompt(
					VERSION,
					VERSION,
					prompt.getContentAsString(StandardCharsets.UTF_8));
		} catch (IOException exception) {
			throw new IllegalStateException("Versioned AI prompt cannot be loaded for " + task + ".", exception);
		}
	}
}
