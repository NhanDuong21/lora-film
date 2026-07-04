package com.project.paymentservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI paymentOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("LoraFilm Payment Service API")
                        .description("Payment Core, idempotent operations, MOCK checkout")
                        .version("1.0.0"))
                .servers(List.of(
                        new Server().url("http://localhost:8084").description("Payment Service direct local testing")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Paste the raw JWT only. Swagger UI automatically adds the \"Bearer \" prefix. JWT must contain the numeric userId claim.")));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("payment-public")
                .pathsToMatch("/api/payments/**")
                .build();
    }
}
