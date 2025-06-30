package com.moneysab.cardexis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManagerFactory;

/**
 * Configuration class for transaction management in the Visa EPIN system.
 * 
 * This configuration ensures proper transaction handling for batch operations
 * and resolves issues with Hibernate batch processing and UUID generation.
 * 
 * @author EL.AHGOUNE
 * @version 1.0.0
 * @since 2024
 */
@Configuration
@EnableTransactionManagement
public class TransactionConfig {

    /**
     * Configures the JPA transaction manager with proper settings for batch processing.
     * 
     * @param entityManagerFactory the entity manager factory
     * @return configured transaction manager
     */
    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);
        
        // Enable proper transaction rollback on batch failures
        transactionManager.setRollbackOnCommitFailure(true);
        
        // Set default timeout for long-running batch operations (5 minutes)
        transactionManager.setDefaultTimeout(300);
        
        return transactionManager;
    }
}