package com.backend.NoBrokerApp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI noBrokerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("NoBroker Platform API")
                        .description("REST API for NoBroker — a broker-free property rental and buying platform. "
                                + "Supports email+OTP registration, Google OAuth, JWT authentication, "
                                + "property listings, admin approval, and booking management.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("NoBroker Team")
                                .email("team@nobroker.com")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer JWT"))
                .components(new Components()
                        .addSecuritySchemes("Bearer JWT",
                                new SecurityScheme()
                                        .name("Authorization")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Paste your JWT token here (without 'Bearer ' prefix)")));
    }
}
