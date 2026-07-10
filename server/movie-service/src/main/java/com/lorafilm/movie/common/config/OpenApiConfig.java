package com.lorafilm.movie.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI movieServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("LoraFilm Movie Service API")
                        .version("1.0.0")
                        .description("Hệ thống tài liệu và kiểm thử API dành cho Movie Service."));
    }
}
