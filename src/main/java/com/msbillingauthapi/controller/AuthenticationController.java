package com.msbillingauthapi.controller;

import static com.msbillingauthapi.util.CookieUtil.addCookie;
import static com.msbillingauthapi.util.CookieUtil.removeCookie;
import static org.springframework.http.HttpHeaders.SET_COOKIE;

import com.msbillingauthapi.dto.AuthenticationRequestDto;
import com.msbillingauthapi.dto.AuthenticationResponseDto;
import com.msbillingauthapi.service.AuthenticationService;
import com.msbillingauthapi.model.AuthTokens;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/sign-in")
    public ResponseEntity<AuthenticationResponseDto> authenticate(@Valid @RequestBody final AuthenticationRequestDto requestDto) {
        final var authTokens = authenticationService.authenticate(requestDto.username(), requestDto.password());

        return ResponseEntity.ok()
                .header(SET_COOKIE, addCookie(AuthTokens.REFRESH_TOKEN_COOKIE_NAME, authTokens.refreshToken(), authTokens.refreshTokenTtl()).toString())
                .body(new AuthenticationResponseDto(authTokens.accessToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponseDto> refreshToken(@CookieValue(AuthTokens.REFRESH_TOKEN_COOKIE_NAME) final String refreshToken) {
        final var authTokens = authenticationService.refreshToken(refreshToken);

        return ResponseEntity.ok(new AuthenticationResponseDto(authTokens.accessToken()));
    }

    @PostMapping("/sign-out")
    public ResponseEntity<Void> revokeToken(@CookieValue(AuthTokens.REFRESH_TOKEN_COOKIE_NAME) final String refreshToken) {
        authenticationService.revokeRefreshToken(refreshToken);

        return ResponseEntity.noContent()
                .header(SET_COOKIE, removeCookie(AuthTokens.REFRESH_TOKEN_COOKIE_NAME).toString())
                .build();
    }

}