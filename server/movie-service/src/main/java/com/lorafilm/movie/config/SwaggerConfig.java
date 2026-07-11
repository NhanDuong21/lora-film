package com.lorafilm.movie.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI movieServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Movie Service API")
                        .description("API documentation for Movie Service in Lorafilm Project")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .addSecurityItem(new SecurityRequirement().addList("Internal Token"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication", createAPIKeyScheme())
                        .addSecuritySchemes("Internal Token", createInternalTokenScheme()));
    }

    private SecurityScheme createAPIKeyScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .bearerFormat("JWT")
                .scheme("bearer");
    }

    private SecurityScheme createInternalTokenScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("X-Internal-Token");
    }
}
