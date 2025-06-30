package com.moneysab.cardexis.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for the Cardexis Settlement Service.
 * 
 * This configuration sets up the OpenAPI documentation for the REST API,
 * providing comprehensive API documentation for EPIN file processing operations.
 * 
 * @author EL.AHGOUNE
 * @version 1.0.0
 * @since 2024
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.servlet.context-path:/}")
    private String contextPath;

    @Value("${server.port:8080}")
    private String serverPort;

    /**
     * Configures the OpenAPI specification for the application.
     * 
     * @return OpenAPI configuration
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .servers(List.of(
                    new Server()
                        .url("http://ec2-52-47-41-115.eu-west-3.compute.amazonaws.com:" + serverPort + contextPath)
                        .description("Development Server"),
                    new Server()
                        .url("https://api-2-staging.domain.com" + contextPath)
                        .description("Staging Server"),
                    new Server()
                        .url("https://api.cardexis.com" + contextPath)
                        .description("Production Server")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT token obtenu depuis API-1 (/api/auth/sign-in)")
                        )
                )
                .info(new Info()
                    .title("Cardexis Settlement Service API")
                    .description("""
                        REST API for processing Visa Electronic Payment Information Network (EPIN) settlement files.
                        
                        ## Supported File Formats
                        - **VSS-110**: Settlement summary data with fee categories and totals
                        - **VSS-120**: Enhanced settlement data with detailed financial information
                        
                        ## Key Features
                        - Asynchronous file processing with status tracking
                        - Comprehensive validation and error reporting
                        - Client-specific data isolation
                        - Processing job management and retry capabilities
                        - Real-time processing statistics and monitoring
                        
                        ## Authentication
                        This API uses JWT Bearer Token authentication.
                        
                        ### How to authenticate:
                        1. Obtain a JWT token from API-1 by calling `/api/auth/sign-in`
                        2. Click the "Authorize" button above
                        3. Enter your JWT token in the format: `Bearer your_jwt_token_here`
                        4. All API calls will automatically include the Authorization header
                        
                        ## Rate Limiting
                        API requests are rate-limited per client. Default limit is 100 requests per hour.
                        """)
                    .version("1.0.0")
                    .contact(new Contact()
                        .name("EL.AHGOUNE")
                        .email("support@cardexis.com")
                        .url("https://www.cardexis.com"))
                    .license(new License()
                        .name("Proprietary")
                        .url("https://www.cardexis.com/license")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
