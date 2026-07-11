package com.lorafilm.movie.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI movieServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Movie Service API")
                        .description("API documentation for Movie Service in Lorafilm Project")
                        .version("1.0.0"));
    }
}
