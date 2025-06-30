package com.moneysab.cardexis.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneysab.cardexis.exception.ProblemDetailBuilder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.moneysab.cardexis.exception.ErrorType.UNAUTHORIZED;


@Component
@RequiredArgsConstructor
@Slf4j
public final class BearerTokenAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public void commence(final HttpServletRequest request, final HttpServletResponse response, final AuthenticationException authException) throws IOException {
        final var status = HttpStatus.UNAUTHORIZED;

        log.info("{}: {}", status.getReasonPhrase(), authException.getMessage());

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ProblemDetailBuilder.forStatus(status).withErrorType(UNAUTHORIZED).build());
    }

}
