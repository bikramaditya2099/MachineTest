package com.blueocean.auth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI 3 configuration.
 * Provides interactive API documentation at /swagger-ui.html.
 * Includes JWT Bearer authentication scheme for testing secured endpoints.
 */
@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    /**
     * Configures the OpenAPI spec with:
     * - API metadata (title, version, description, contact)
     * - JWT Bearer authentication scheme
     * - Global security requirement (all endpoints require auth by default in docs)
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("JWT Authentication System API")
                        .description("""
                                RESTful API for JWT-based User Authentication System.
                                
                                **Authentication Flow:**
                                1. Register at `POST /api/auth/register`
                                2. Verify OTP at `POST /api/auth/verify-otp`
                                3. Login at `POST /api/auth/login` to receive JWT token
                                4. Click 'Authorize' and enter: `Bearer <your-token>`
                                5. Access protected endpoints
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Blue Ocean Team")
                                .email("support@blueocean.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT"))
                )
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT token obtained from /api/auth/login")
                        )
                );
    }
}
