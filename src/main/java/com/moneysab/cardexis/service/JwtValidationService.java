package com.moneysab.cardexis.service;

import com.moneysab.cardexis.dto.JwtValidationRequestDto;
import com.moneysab.cardexis.dto.JwtValidationResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Service for validating JWT tokens by calling API-1.
 */
@Service
public class JwtValidationService {

    private static final Logger logger = LoggerFactory.getLogger(JwtValidationService.class);

    private final WebClient webClient;
    private final String authServiceUrl;

    public JwtValidationService(
            WebClient.Builder webClientBuilder,
            @Value("${auth.service.url:http://localhost:8080}") String authServiceUrl) {
        this.authServiceUrl = authServiceUrl;
        this.webClient = webClientBuilder
                .baseUrl(authServiceUrl)
                .build();
    }

    /**
     * Validates a JWT token by calling API-1's validation endpoint.
     *
     * @param token the JWT token to validate
     * @return validation response containing user details if valid
     */
    public JwtValidationResponseDto validateToken(String token) {
        try {
            logger.debug("Validating JWT token with auth service at: {}", authServiceUrl);
            
            JwtValidationRequestDto request = new JwtValidationRequestDto(token);
            
            Mono<JwtValidationResponseDto> responseMono = webClient
                    .post()
                    .uri("/api/auth/validate-token")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(JwtValidationResponseDto.class)
                    .timeout(Duration.ofSeconds(5)); // 5 second timeout
            
            JwtValidationResponseDto response = responseMono.block();
            
            if (response != null && response.valid()) {
                logger.debug("Token validation successful for user: {}", response.username());
            } else {
                logger.warn("Token validation failed: {}", response != null ? response.errorMessage() : "No response");
            }
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error validating token with auth service: {}", e.getMessage());
            return new JwtValidationResponseDto(false, null, null, "Auth service unavailable: " + e.getMessage());
        }
    }

    /**
     * Extracts the token from the Authorization header.
     *
     * @param authorizationHeader the Authorization header value
     * @return the JWT token without the "Bearer " prefix, or null if invalid
     */
    public String extractTokenFromHeader(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }
}