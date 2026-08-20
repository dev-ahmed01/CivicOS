package com.civicos.common.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OpenApiCustomizer;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfiguration {

	@Bean
	OpenAPI civicOsOpenApi() {
		String scheme = "bearerAuth";
		return new OpenAPI()
				.info(new Info()
						.title("CivicOS API")
						.version("v1")
						.description("Authoritative cross-agency road-work coordination API. A case is the "
								+ "coordination/problem container for observations and interventions. AI responses "
								+ "are advisory and workflow commands remain deterministic. External MARCS-style "
								+ "demo records are simulated unless a real integration is explicitly configured."))
				.components(new Components().addSecuritySchemes(
						scheme,
						new SecurityScheme()
								.name(scheme)
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")))
				.addSecurityItem(new SecurityRequirement().addList(scheme));
	}

	@Bean
	OpenApiCustomizer standardErrorResponses() {
		return openApi -> {
			ModelConverters.getInstance().read(ApiError.class)
					.forEach(openApi.getComponents()::addSchemas);
			openApi.getPaths().values().forEach(path -> path.readOperations().forEach(operation -> {
				addError(operation, "400", "Invalid request");
				addError(operation, "401", "Authentication required or invalid");
				addError(operation, "403", "Authenticated actor is not authorised");
				addError(operation, "404", "Resource not found");
				addError(operation, "409", "Domain, workflow, idempotency, or version conflict");
				addError(operation, "500", "Unexpected server error");
			}));
		};
	}

	private void addError(io.swagger.v3.oas.models.Operation operation, String status, String description) {
		if (operation.getResponses().containsKey(status)) {
			return;
		}
		Schema<?> schema = new Schema<>().$ref("#/components/schemas/ApiError");
		operation.getResponses().addApiResponse(status, new ApiResponse()
				.description(description)
				.content(new Content().addMediaType(
						org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
						new MediaType().schema(schema))));
	}
}
