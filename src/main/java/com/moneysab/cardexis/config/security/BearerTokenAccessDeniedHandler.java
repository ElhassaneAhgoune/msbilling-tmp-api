//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.moneysab.cardexis.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.moneysab.cardexis.exception.ErrorType.FORBIDDEN;
import static com.moneysab.cardexis.exception.ProblemDetailBuilder.forStatus;


@Component
@RequiredArgsConstructor
@Slf4j
public final class BearerTokenAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public void handle(final HttpServletRequest request, final HttpServletResponse response, final AccessDeniedException accessDeniedException) throws IOException {
        final var status = HttpStatus.FORBIDDEN;

        log.info("{}: {}", status.getReasonPhrase(), accessDeniedException.getMessage());

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), forStatus(status).withErrorType(FORBIDDEN).build());
    }

}
