package com.msbillingauthapi.testdata;

import static java.time.Duration.ofDays;

import com.msbillingauthapi.entity.RefreshToken;
import java.time.Instant;
import java.util.UUID;

public class TestRefreshTokenBuilder {

    private final RefreshToken refreshToken;

    private TestRefreshTokenBuilder() {
        refreshToken = new RefreshToken();
        refreshToken.setExpiresAt(Instant.now().plus(ofDays(1)));
    }

    public static TestRefreshTokenBuilder refreshTokenBuilder() {
        return new TestRefreshTokenBuilder();
    }

    public TestRefreshTokenBuilder withRandomId() {
        refreshToken.setId(UUID.randomUUID());
        return this;
    }

    public TestRefreshTokenBuilder withTestUser() {
        refreshToken.setUser(TestUserBuilder.userBuilder().build());
        return this;
    }

    public RefreshToken build() {
        return refreshToken;
    }

}
