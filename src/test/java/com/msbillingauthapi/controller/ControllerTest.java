package com.msbillingauthapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msbillingauthapi.config.JwtConfig;
import com.msbillingauthapi.config.SecurityConfig;
import com.msbillingauthapi.config.security.BearerTokenAccessDeniedHandler;
import com.msbillingauthapi.config.security.BearerTokenAuthenticationEntryPoint;
import com.msbillingauthapi.service.JpaUserDetailsService;
import com.msbillingauthapi.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@Import({
        SecurityConfig.class,
        JwtConfig.class,
        BearerTokenAuthenticationEntryPoint.class,
        BearerTokenAccessDeniedHandler.class
})
public abstract class ControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JpaUserDetailsService userDetailsService;

}