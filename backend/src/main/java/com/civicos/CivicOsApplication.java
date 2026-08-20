package com.civicos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CivicOsApplication {

	public static void main(String[] args) {
		SpringApplication.run(CivicOsApplication.class, args);
	}

}
