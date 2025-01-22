package com.msbillingauthapi.mapper;

import com.msbillingauthapi.dto.UserProfileDto;
import com.msbillingauthapi.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserProfileDto toUserProfileDto(final User user) {
        return new UserProfileDto(user.getEmail(), user.getUsername(), user.isEmailVerified());
    }

}