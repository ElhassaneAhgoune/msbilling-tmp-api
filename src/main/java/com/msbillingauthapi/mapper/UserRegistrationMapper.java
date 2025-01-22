package com.msbillingauthapi.mapper;

import com.msbillingauthapi.dto.RegistrationRequestDto;
import com.msbillingauthapi.dto.RegistrationResponseDto;
import com.msbillingauthapi.entity.Role;
import com.msbillingauthapi.entity.User;
import com.msbillingauthapi.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserRegistrationMapper {


    private final RoleRepository roleRepository;

    public User toEntity(final RegistrationRequestDto registrationRequestDto) {
        final var user = new User();

        user.setEmail(registrationRequestDto.email());
        user.setUsername(registrationRequestDto.username());
        user.setPassword(registrationRequestDto.password());
        user.setFirstName(registrationRequestDto.firstName());
        user.setLastName(registrationRequestDto.lastName());
        user.setPhoneNumber(registrationRequestDto.phoneNumber());
        user.setTimezone(registrationRequestDto.timezone());
        user.setLocale(registrationRequestDto.locale());
        // Fetch the Role from the database
        Role role = roleRepository.findById(registrationRequestDto.roleId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid role ID: " + registrationRequestDto.roleId()));

        // Set the Role
        user.setRole(role);


        return user;
    }

    public RegistrationResponseDto toResponseDto(final User user) {
        return new RegistrationResponseDto(user.getEmail(), user.getUsername());
    }

}