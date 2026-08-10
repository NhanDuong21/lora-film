package com.project.scoreservice.config;
 
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
 
@Configuration
public class OpenApiConfig {
 
    private static final String BEARER_AUTH = "bearerAuth";
    private static final String INTERNAL_AUTH = "internalAuth";
 
    @Bean
    public OpenAPI scoreServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Score Service API")
                        .description("Movie Booking System - Score Service APIs")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .addSecurityItem(new SecurityRequirement().addList(INTERNAL_AUTH))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"))
                        .addSecuritySchemes(INTERNAL_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Internal-Token")));
    }
}
