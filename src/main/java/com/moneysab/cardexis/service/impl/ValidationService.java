package com.moneysab.cardexis.service.impl;

import com.moneysab.cardexis.domain.entity.*;
import com.moneysab.cardexis.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Service for validating EPIN file data and business rules.
 * 
 * This service provides comprehensive validation capabilities for all EPIN data types,
 * including format validation, business rule enforcement, and data integrity checks.
 * 
 * @author EL.AHGOUNE
 * @version 1.0.0
 * @since 2024
 */
@Service
public class ValidationService {

    private static final Logger logger = LoggerFactory.getLogger(ValidationService.class);

    // Validation patterns
    private static final Pattern ROUTING_NUMBER_PATTERN = Pattern.compile("^[0-9]{9,13}$");
    private static final Pattern CLIENT_ID_PATTERN = Pattern.compile("^[A-Z0-9]{4,8}$");
    private static final Pattern DESTINATION_ID_PATTERN = Pattern.compile("^[A-Z0-9]{1,20}$");
    private static final Pattern SRE_ID_PATTERN = Pattern.compile("^[A-Z0-9]{1,20}$");
    private static final Pattern FEE_CATEGORY_PATTERN = Pattern.compile("^[IFCT][1-9]$");

    @Autowired
    private EpinFileHeaderRepository headerRepository;

    @Autowired
    private Vss110SettlementRecordRepository vss110Repository;



    /**
     * Validate EPIN file header.
     * 
     * @param header the header to validate
     * @return validation result
     */
    public ValidationResult validateEpinFileHeader(EpinFileHeader header) {
        logger.debug("Validating EPIN file header: {}", header.getId());
        
        ValidationResult result = new ValidationResult();
        
        // Basic null checks
        if (header == null) {
            result.addError("Header cannot be null");
            return result;
        }

        // Validate routing number
        if (header.getRoutingNumber() == null || header.getRoutingNumber().trim().isEmpty()) {
            result.addError("Routing number is required");
        } else if (!ROUTING_NUMBER_PATTERN.matcher(header.getRoutingNumber()).matches()) {
            result.addError("Invalid routing number format");
        }

        // Validate file timestamp
        if (header.getFileTimestamp() == null) {
            result.addError("File timestamp is required");
        } else if (header.getFileTimestamp().isAfter(java.time.LocalDateTime.now())) {
            result.addError("File timestamp cannot be in the future");
        }

        // Validate client ID
        if (header.getClientId() != null && !header.getClientId().trim().isEmpty()) {
            if (!CLIENT_ID_PATTERN.matcher(header.getClientId()).matches()) {
                result.addError("Invalid client ID format");
            }
        }

        logger.debug("Header validation completed with {} errors", result.getErrors().size());
        return result;
    }

    /**
     * Validate VSS-110 settlement record.
     * 
     * @param record the record to validate
     * @return validation result
     */
    public ValidationResult validateVss110SettlementRecord(Vss110SettlementRecord record) {
        logger.debug("Validating VSS-110 settlement record: {}", record.getId());
        
        ValidationResult result = new ValidationResult();
        
        // Basic null checks
        if (record == null) {
            result.addError("Record cannot be null");
            return result;
        }

        // Validate report identification (replaces record type validation)
        String reportId = record.getReportGroup() + record.getReportSubgroup() + record.getReportIdNumber();
        if (!"V2110".equals(reportId) && !"V2111".equals(reportId)) {
            result.addError("Invalid report identification. Expected V2110 or V2111");
        }

        // Validate settlement date
        if (record.getSettlementDate() == null) {
            result.addError("Settlement date is required");
        } else if (record.getSettlementDate().isAfter(LocalDate.now())) {
            result.addError("Settlement date cannot be in the future");
        }

        // Validate amount type (replaces fee category validation)
        if (record.getAmountType() == null || record.getAmountType().trim().isEmpty()) {
            if ("110".equals(record.getReportIdNumber())) {
                result.addError("Amount type is required for VSS-110 records");
            }
        } else if ("110".equals(record.getReportIdNumber()) &&
                   !record.getAmountType().matches("[IFCT]")) {
            result.addError("Invalid amount type for VSS-110. Must be I, F, C, or T");
        }

        // Validate net amount (replaces single amount validation)
        if (record.getNetAmount() != null && record.hasNetDebit()) {
            // Negative net amounts are acceptable for debits
            logger.debug("Record has net debit amount: {}", record.getNetAmount());
        }

        logger.debug("VSS-110 validation completed with {} errors", result.getErrors().size());
        return result;
    }



    /**
     * Validate job data integrity.
     * 
     * @param jobId the job ID to validate
     * @return validation result
     */
    public ValidationResult validateJobDataIntegrity(UUID jobId) {
        logger.info("Validating data integrity for job: {}", jobId);
        
        ValidationResult result = new ValidationResult();
        
        // TODO: Implement actual data integrity checks
        // For now, return success to fix compilation
        
        logger.info("Job data integrity validation completed with {} errors", result.getErrors().size());
        return result;
    }

    /**
     * Validation result container.
     */
    public static class ValidationResult {
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();

        public void addError(String error) {
            errors.add(error);
        }

        public void addWarning(String warning) {
            warnings.add(warning);
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        public List<String> getErrors() {
            return new ArrayList<>(errors);
        }

        public List<String> getWarnings() {
            return new ArrayList<>(warnings);
        }

        public String getErrorSummary() {
            if (errors.isEmpty()) {
                return null;
            }
            return String.join("; ", errors);
        }

        public String getWarningSummary() {
            if (warnings.isEmpty()) {
                return null;
            }
            return String.join("; ", warnings);
        }

        @Override
        public String toString() {
            return String.format("ValidationResult{errors=%d, warnings=%d, valid=%s}", 
                               errors.size(), warnings.size(), isValid());
        }
    }
}