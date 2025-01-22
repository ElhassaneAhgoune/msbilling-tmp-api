package com.msbillingauthapi.dto;

import com.msbillingauthapi.entity.Role;
import jakarta.validation.constraints.*;

public record RegistrationRequestDto(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email address")
        @Size(max = 100, message = "Email must be less than 100 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 255, message = "Password must be between 6 and 255 characters") // Increased max size
        String password,

        @NotBlank(message = "First name is required")
        @Size(min = 2, max = 255, message = "First name must be between 2 and 255 characters") // Increased max size
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(min = 2, max = 255, message = "Last name must be between 2 and 255 characters") // Increased max size
        String lastName,

        @Size(max = 20, message = "Phone number must be less than 20 characters")
        String phoneNumber,

        @Size(max = 50, message = "Timezone must be less than 50 characters")
        String timezone,

        @Size(max = 10, message = "Locale must be less than 10 characters")
        String locale,

        @NotNull
        @Min(1)
        @Max(100)
        Integer roleId
) {
}