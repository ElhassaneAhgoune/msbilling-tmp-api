package com.msbillingauthapi.service;

import static org.springframework.http.HttpStatus.GONE;

import com.msbillingauthapi.entity.User;
import com.msbillingauthapi.exception.RestErrorResponseException;
import com.msbillingauthapi.repository.UserRepository;
import com.msbillingauthapi.exception.ErrorType;
import com.msbillingauthapi.exception.ProblemDetailBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User getUserByUsername(final String username) {
        return userRepository.findByUsername(username).orElseThrow(() ->
                new RestErrorResponseException(ProblemDetailBuilder.forStatusAndDetail(GONE, "The user account has been deleted or inactivated")
                        .withErrorType(ErrorType.ACCOUNT_UNAVAILABLE)
                        .build()
                )
        );
    }
}