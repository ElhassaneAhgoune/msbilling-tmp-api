package com.moneysab.cardexis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Cardexis Settlement Service Application
 * 
 * Main Spring Boot application for processing Visa Electronic Payment Information Network (EPIN) files.
 * Supports VSS-110 (settlement summary) and VSS-120 (enhanced settlement data) formats.
 * 
 * Features:
 * - Asynchronous file processing with queue management
 * - Clean architecture with service layers
 * - PostgreSQL database with MyBatis ORM
 * - RESTful API endpoints for file upload and data retrieval
 * - Comprehensive audit logging and monitoring
 * 
 * @author EL.AHGOUNE
 * @version 1.0.0
 * @since 2024
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
@EnableTransactionManagement
public class CardexisSettlementServiceApplication {

    /**
     * Main entry point for the Cardexis Settlement Service application.
     * 
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(CardexisSettlementServiceApplication.class, args);
    }
}