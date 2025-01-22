package com.msbillingauthapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private static final String ISSUER = "TestApp";
    private static final Duration JWT_TTL = Duration.ofMinutes(60);
    private static final String USERNAME = "testUser";
    private static final String ROLE = "ROLE_USER";
    private static final String TOKEN_VALUE = "mockedJwtToken";

    @Mock
    private JwtEncoder jwtEncoder;

    @Mock
    private Jwt jwt;

    @Test
    void generateToken_validUsername_returnsToken() {
        final var jwtService = new JwtService(ISSUER, JWT_TTL, jwtEncoder);

        final var claimsSet = JwtClaimsSet.builder()
                .subject(USERNAME)
                .issuer(ISSUER)
                .expiresAt(Instant.now().plus(JWT_TTL))
                .build();

        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);
        when(jwt.getTokenValue()).thenReturn(TOKEN_VALUE);

        final var token = jwtService.generateToken(USERNAME,ROLE);

        assertThat(token).isEqualTo(TOKEN_VALUE);
    }

}