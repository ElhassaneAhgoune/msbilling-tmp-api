package com.msbillingauthapi.service;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import com.msbillingauthapi.exception.RestErrorResponseException;
import com.msbillingauthapi.repository.UserRepository;
import com.msbillingauthapi.exception.ErrorType;
import com.msbillingauthapi.exception.ProblemDetailBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JpaUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(final String username) {
        return userRepository.findByUsername(username).map(user -> {
            if (!user.isEmailVerified()) {
                throw new RestErrorResponseException(ProblemDetailBuilder.forStatusAndDetail(UNAUTHORIZED, "Email verification required")
                        .withProperty("email", user.getEmail())
                        .withErrorType(ErrorType.EMAIL_VERIFICATION_REQUIRED)
                        .build()
                );
            }
            return User.builder()
                    .username(username)
                    .password(user.getPassword())
                    .build();
        }).orElseThrow(() -> new UsernameNotFoundException("User with username [%s] not found".formatted(username)));
    }

}