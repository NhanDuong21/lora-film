package com.project.authservice.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
	/**
	 * Defines the OpenAPI metadata for the auth service.
	 *
	 * @return OpenAPI configuration
	 */
	@Bean
	public OpenAPI authServiceOpenAPI() {
		return new OpenAPI()
				.info(new Info().title("Auth Service API").version("v1").description("Authentication and registration endpoints"))
				.externalDocs(new ExternalDocumentation().description("Project Documentation").url("/README.md"));
	}
}