package com.msbillingauthapi.controller;

import static com.msbillingauthapi.util.CookieUtil.addCookie;
import static org.springframework.http.HttpHeaders.SET_COOKIE;

import com.msbillingauthapi.dto.AuthenticationResponseDto;
import com.msbillingauthapi.dto.EmailVerificationRequestDto;
import com.msbillingauthapi.service.AuthenticationService;
import com.msbillingauthapi.service.EmailVerificationService;
import com.msbillingauthapi.model.AuthTokens;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    private final AuthenticationService authenticationService;

    @PostMapping("/request-verification-email")
    public ResponseEntity<Void> resendVerificationOtp(@RequestParam final String email) {
        emailVerificationService.resendEmailVerificationOtp(email);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify-email")
    public ResponseEntity<AuthenticationResponseDto> verifyOtp(@Valid @RequestBody final EmailVerificationRequestDto requestDto) {
        final var verifiedUser = emailVerificationService.verifyEmailOtp(requestDto.email(), requestDto.otp());
        final var authTokens = authenticationService.authenticate(verifiedUser);

        return ResponseEntity.ok()
                .header(SET_COOKIE, addCookie(AuthTokens.REFRESH_TOKEN_COOKIE_NAME, authTokens.refreshToken(), authTokens.refreshTokenTtl()).toString())
                .body(new AuthenticationResponseDto(authTokens.accessToken()));
    }

}