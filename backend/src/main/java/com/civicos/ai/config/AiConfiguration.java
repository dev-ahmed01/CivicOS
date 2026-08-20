package com.civicos.ai.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfiguration {

	@Bean(destroyMethod = "close")
	ExecutorService aiProviderExecutor() {
		return Executors.newVirtualThreadPerTaskExecutor();
	}
}
