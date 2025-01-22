package com.msbillingauthapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.msbillingauthapi.dto.EmailVerificationRequestDto;
import com.msbillingauthapi.entity.User;
import com.msbillingauthapi.model.AuthTokens;
import com.msbillingauthapi.service.AuthenticationService;
import com.msbillingauthapi.service.EmailVerificationService;
import java.time.Duration;

import com.msbillingauthapi.testdata.TestUserBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(EmailVerificationController.class)
class EmailVerificationControllerTest extends ControllerTest {

    @MockBean
    private EmailVerificationService emailVerificationService;

    @MockBean
    private AuthenticationService authenticationService;

    @Test
    void resendEmailVerificationOtp_valid_returnsNoContent() throws Exception {
        doNothing().when(emailVerificationService).resendEmailVerificationOtp(anyString());

        mockMvc.perform(post("/api/auth/request-verification-email").param("email", "test@example.com"))
                .andExpect(status().isNoContent());

        verify(emailVerificationService).resendEmailVerificationOtp("test@example.com");
    }

    @Test
    void resendEmailVerificationOtp_NotFound_returnsNotFound() throws Exception {
        doThrow(new ResponseStatusException(NOT_FOUND, "Email not found"))
                .when(emailVerificationService).resendEmailVerificationOtp("nonexistent@example.com");

        mockMvc.perform(post("/api/auth/request-verification-email").param("email", "nonexistent@example.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Email not found"));
    }

    @Test
    void verifyOtp_validToken_returnsValidSession() throws Exception {
        final var authTokens = new AuthTokens("accessToken", "refreshToken", Duration.ofHours(1));

        when(emailVerificationService.verifyEmailOtp(anyString(), anyString())).thenReturn(TestUserBuilder.userBuilder().withVerifiedEmail().build());
        when(authenticationService.authenticate(any(User.class))).thenReturn(authTokens);

        final var requestDto = new EmailVerificationRequestDto("test@example.com", "123456");

        mockMvc.perform(post("/api/auth/verify-email").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(AuthTokens.REFRESH_TOKEN_COOKIE_NAME))
                .andExpect(jsonPath("$.accessToken").value(authTokens.accessToken()));

        verify(emailVerificationService).verifyEmailOtp(eq(requestDto.email()), eq(requestDto.otp()));
    }

    @Test
    void verifyOtp_invalidToken_returnsBadRequest() throws Exception {
        when(emailVerificationService.verifyEmailOtp(anyString(), anyString())).thenThrow(new ResponseStatusException(BAD_REQUEST, "Invalid token"));

        final var requestDto = new EmailVerificationRequestDto("test@example.com", "000000");

        mockMvc.perform(post("/api/auth/verify-email").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid token"));
    }

}