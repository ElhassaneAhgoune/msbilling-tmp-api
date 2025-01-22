package com.msbillingauthapi.service;

import java.util.UUID;

import com.msbillingauthapi.config.OtpConfig;
import com.msbillingauthapi.util.OtpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@RequiredArgsConstructor
public class OtpService {

    private final OtpConfig.OtpConfigProperties configProperties;

    private final RedisTemplate<String, String> redisTemplate;

    private final PasswordEncoder passwordEncoder;

    public String generateAndStoreOtp(final UUID id) {
        final var otp = OtpUtil.generateOtp(configProperties.length());
        final var cacheKey = getCacheKey(id);

        redisTemplate.opsForValue().set(cacheKey, passwordEncoder.encode(otp), configProperties.ttl());

        return otp;
    }

    public boolean isOtpValid(final UUID id, final String otp) {
        final var cacheKey = getCacheKey(id);

        return passwordEncoder.matches(otp, redisTemplate.opsForValue().get(cacheKey));
    }

    public void deleteOtp(final UUID id) {
        final var cacheKey = getCacheKey(id);

        redisTemplate.delete(cacheKey);
    }

    private String getCacheKey(UUID id) {
        return configProperties.cachePrefix().formatted(id);
    }

}
