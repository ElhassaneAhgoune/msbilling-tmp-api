package com.moneysab.cardexis.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA configuration for the Visa EPIN system.
 *
 * This configuration ensures proper repository scanning.
 * JPA auditing and transaction management are enabled in the main application class.
 *
 * @author EL.AHGOUNE
 * @version 1.0.0
 * @since 2024
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.moneysab.cardexis.repository")
public class JpaConfig {
    // Configuration is handled through annotations and application.yml
}