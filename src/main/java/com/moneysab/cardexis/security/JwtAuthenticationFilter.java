package com.moneysab.cardexis.security;

import com.moneysab.cardexis.service.LocalJwtValidationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT Authentication Filter for API-2.
 * This filter validates JWT tokens locally using the same keys as API-1.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final LocalJwtValidationService localJwtValidationService;

    public JwtAuthenticationFilter(LocalJwtValidationService localJwtValidationService) {
        this.localJwtValidationService = localJwtValidationService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = localJwtValidationService.extractTokenFromHeader(authorizationHeader);

            if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                Jwt jwt = localJwtValidationService.validateToken(token);

                if (jwt != null) {
                    String username = localJwtValidationService.extractUsername(jwt);
                    String role = localJwtValidationService.extractRole(jwt);

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    username,
                                    null,
                                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + (role != null ? role : "USER")))
                            );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    logger.debug("Authenticated user: {}", username);
                } else {
                    logger.warn("JWT validation failed: token invalid or expired.");
                }
            }

        } catch (Exception e) {
            logger.error("JWT filter error: {}", e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // Exclude Swagger & actuator endpoints from the filter
        return path.startsWith("/swagger-ui/") ||
                path.startsWith("/v3/api-docs/") ||
                path.startsWith("/swagger-resources/") ||
                path.startsWith("/webjars/") ||
                path.startsWith("/actuator/") ||
                path.equals("/") ||
                path.startsWith("/cardexis-settlement/swagger-ui/") ||
                path.startsWith("/cardexis-settlement/v3/api-docs/") ||
                path.startsWith("/cardexis-settlement/actuator/");
    }
}
