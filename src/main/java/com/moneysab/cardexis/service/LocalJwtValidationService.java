package com.moneysab.cardexis.service;

import com.moneysab.cardexis.config.JwtConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Service for local JWT token validation using the same keys as API-1.
 * This provides better performance than calling API-1 for each validation.
 */
@Service
public class LocalJwtValidationService {

    private static final Logger logger = LoggerFactory.getLogger(LocalJwtValidationService.class);

    private final JwtDecoder jwtDecoder;
    private final JwtConfig jwtConfig;

    public LocalJwtValidationService(JwtDecoder jwtDecoder, JwtConfig jwtConfig) {
        this.jwtDecoder = jwtDecoder;
        this.jwtConfig = jwtConfig;
    }

    /**
     * Validates a JWT token locally using the shared public key.
     *
     * @param token the JWT token to validate
     * @return JWT object if valid, null if invalid
     */
    public Jwt validateToken(String token) {
        try {
            logger.debug("Validating JWT token locally");
            
            Jwt jwt = jwtDecoder.decode(token);
            
            // Check if token is expired
            if (jwt.getExpiresAt() != null && jwt.getExpiresAt().isBefore(Instant.now())) {
                logger.warn("JWT token is expired");
                return null;
            }
            
            // Additional validation can be added here
            String username = jwt.getSubject();
            String role = jwt.getClaim("role");
            
            if (username == null || username.trim().isEmpty()) {
                logger.warn("JWT token has no valid subject");
                return null;
            }
            
            logger.debug("JWT token validation successful for user: {}", username);
            return jwt;
            
        } catch (JwtException e) {
            logger.warn("JWT token validation failed: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            logger.error("Unexpected error during JWT validation: {}", e.getMessage());
            return null;
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

    /**
     * Extracts username from JWT token.
     *
     * @param jwt the JWT token
     * @return username or null if not found
     */
    public String extractUsername(Jwt jwt) {
        return jwt != null ? jwt.getSubject() : null;
    }

    /**
     * Extracts role from JWT token.
     *
     * @param jwt the JWT token
     * @return role or null if not found
     */
    public String extractRole(Jwt jwt) {
        return jwt != null ? jwt.getClaim("role") : null;
    }

    /**
     * Extracts refresh token ID from JWT token.
     *
     * @param jwt the JWT token
     * @return refresh token ID or null if not found
     */
    public String extractRefreshTokenId(Jwt jwt) {
        return jwt != null ? jwt.getClaim("refreshTokenId") : null;
    }
}