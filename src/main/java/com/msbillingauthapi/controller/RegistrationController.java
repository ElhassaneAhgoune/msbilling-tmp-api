package com.msbillingauthapi.controller;

import com.msbillingauthapi.dto.RegistrationRequestDto;
import com.msbillingauthapi.dto.RegistrationResponseDto;
import com.msbillingauthapi.mapper.UserRegistrationMapper;
import com.msbillingauthapi.service.EmailVerificationService;
import com.msbillingauthapi.service.UserRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class RegistrationController {

    private final UserRegistrationService userRegistrationService;

    private final EmailVerificationService emailVerificationService;

    private final UserRegistrationMapper userRegistrationMapper;

    @PostMapping("/sign-up")
    public ResponseEntity<RegistrationResponseDto> registerUser(@Valid @RequestBody final RegistrationRequestDto registrationDTO) {
        final var registeredUser = userRegistrationService.registerUser(userRegistrationMapper.toEntity(registrationDTO));

        emailVerificationService.sendEmailVerificationOtp(registeredUser.getId(), registeredUser.getEmail());

        return ResponseEntity.ok(userRegistrationMapper.toResponseDto(registeredUser));
    }

}