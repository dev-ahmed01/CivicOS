package com.civicos.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Validated
@ConfigurationProperties(prefix = "civicos.security")
public record SecurityProperties(
		@NotBlank @Size(min = 32) String jwtSecret,
		@Min(5) @Max(1440) long jwtExpirationMinutes,
		@Min(1) @Max(90) long refreshExpirationDays,
		@NotBlank String issuer) {
}
