package com.moneysab.cardexis.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Test controller to verify JWT authentication is working.
 */
@RestController
@RequestMapping("/api/v1/auth-test")
@Tag(name = "Authentication Test", description = "Endpoints for testing JWT authentication")
@SecurityRequirement(name = "bearerAuth")
public class AuthTestController {

    @GetMapping("/me")
    @Operation(summary = "Get current user information",
               description = "Returns information about the currently authenticated user from JWT token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User information retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token")
    })
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> response = new HashMap<>();
        response.put("authenticated", authentication != null && authentication.isAuthenticated());
        response.put("username", authentication != null ? authentication.getName() : null);
        response.put("authorities", authentication != null ? authentication.getAuthorities() : null);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/protected")
    @Operation(summary = "Test protected endpoint",
               description = "A simple protected endpoint to test JWT authentication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Access granted - JWT token is valid"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token")
    })
    public ResponseEntity<String> protectedEndpoint() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok("Hello " + authentication.getName() + "! This is a protected endpoint in API-2.");
    }
}