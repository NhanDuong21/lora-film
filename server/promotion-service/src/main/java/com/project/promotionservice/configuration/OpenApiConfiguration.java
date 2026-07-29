package com.project.promotionservice.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI customOpenAPI() {
        final String bearerSchemeName = "bearerAuth";
        final String internalTokenSchemeName = "internalTokenAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Promotion Service API")
                        .description("Coupon, voucher and compensation APIs. New checkout integrations use "
                                + "validate -> reserve -> confirm/rollback.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("LoraFilm Architecture Team")
                                .email("support@lorafilm.com")
                                .url("https://lorafilm.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .addSecurityItem(new SecurityRequirement().addList(bearerSchemeName))
                .addSecurityItem(new SecurityRequirement().addList(internalTokenSchemeName))
                .components(new Components()
                        .addSecuritySchemes(bearerSchemeName,
                                new SecurityScheme()
                                        .name(bearerSchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT Token xác thực từ Auth Service (Authorization: Bearer <token>)"))
                        .addSecuritySchemes(internalTokenSchemeName,
                                new SecurityScheme()
                                        .name("X-Internal-Token")
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .description("Internal Token xác thực giữa các Microservices (Header: X-Internal-Token)")));
    }

    @Bean
    public OperationCustomizer internalServiceIdentityHeader() {
        return (operation, handlerMethod) -> {
            if (handlerMethod.getBeanType().getSimpleName().startsWith("Internal")) {
                operation.addParametersItem(new Parameter()
                        .in("header")
                        .name("X-Service-Name")
                        .required(true)
                        .description("BOOKING_SERVICE or PAYMENT_SERVICE, matching the configured token"));
            }
            return operation;
        };
    }
}
