package com.moneysab.cardexis.config;

import com.moneysab.cardexis.service.JwtService;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.*;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;

@Configuration
@Setter
@Getter
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    @Value("${jwt.private-key}")
    private RSAPrivateKey privateKey;

    @Value("${jwt.public-key}")
    private RSAPublicKey publicKey;

    private Duration accessTokenTtl;

    @Bean
    public JwtEncoder jwtEncoder() {
        final var jwk = new RSAKey.Builder(publicKey).privateKey(privateKey).build();

        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withPublicKey(publicKey).build();
    }

    @Bean
    public JwtService jwtService(@Value("${spring.application.name}") final String appName, final JwtEncoder jwtEncoder) {
        return new JwtService(appName, accessTokenTtl, jwtEncoder);
    }
    public String extractRefreshTokenIdFromJWT(String token) {
        // Décoder le token
        Jwt jwt = jwtDecoder().decode(token);

        // Extraire le claim personnalisé "refreshTokenId"
        return jwt.getClaim("refreshTokenId");
    }

}
