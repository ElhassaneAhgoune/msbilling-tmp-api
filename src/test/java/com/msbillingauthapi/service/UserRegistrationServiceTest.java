package com.msbillingauthapi.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.msbillingauthapi.entity.User;
import com.msbillingauthapi.exception.RestErrorResponseException;
import com.msbillingauthapi.repository.UserRepository;
import com.msbillingauthapi.testdata.TestUserBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserRegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserRegistrationService userRegistrationService;

    @Test
    void registerUser_validRequest_savesUser() {
        final var user = TestUserBuilder.userBuilder().build();
        when(userRepository.existsByEmail(user.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(user.getUsername())).thenReturn(false);
        when(passwordEncoder.encode(user.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        userRegistrationService.registerUser(user);

        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_emailExists_throwsException() {
        final var user = TestUserBuilder.userBuilder().build();
        when(userRepository.existsByEmail(user.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> userRegistrationService.registerUser(user))
                .isInstanceOf(RestErrorResponseException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_usernameExists_throwsException() {
        final var user = TestUserBuilder.userBuilder().build();
        when(userRepository.existsByUsername(user.getUsername())).thenReturn(true);

        assertThatThrownBy(() -> userRegistrationService.registerUser(user))
                .isInstanceOf(RestErrorResponseException.class);

        verify(userRepository, never()).save(any(User.class));
    }

}