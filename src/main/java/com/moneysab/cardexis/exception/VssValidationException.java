package com.moneysab.cardexis.exception;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Exception thrown when VSS (Visa Settlement Service) record validation fails.
 * 
 * This exception is thrown when parsed VSS records fail business rule validation,
 * such as:
 * - Invalid business mode combinations
 * - Inconsistent amount calculations
 * - Missing required relationships
 * - Business rule violations
 * - Data integrity issues
 * 
 * @author EL.AHGOUNE
 * @version 1.0.0
 * @since 2024
 */
public class VssValidationException extends RuntimeException {
    
    private final int lineNumber;
    private final String recordType;
    private final List<String> validationErrors;
    private final String recordId;
    
    /**
     * Constructs a new VSS validation exception with the specified detail message.
     * 
     * @param message the detail message
     */
    public VssValidationException(String message) {
        super(message);
        this.lineNumber = -1;
        this.recordType = null;
        this.validationErrors = new ArrayList<>();
        this.recordId = null;
    }
    
    /**
     * Constructs a new VSS validation exception with the specified detail message and cause.
     * 
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public VssValidationException(String message, Throwable cause) {
        super(message, cause);
        this.lineNumber = -1;
        this.recordType = null;
        this.validationErrors = new ArrayList<>();
        this.recordId = null;
    }
    
    /**
     * Constructs a new VSS validation exception with detailed context information.
     * 
     * @param message the detail message
     * @param lineNumber the line number where the error occurred
     * @param recordType the type of record being validated (e.g., "VSS-110", "VSS-120")
     */
    public VssValidationException(String message, int lineNumber, String recordType) {
        super(formatMessage(message, lineNumber, recordType, null));
        this.lineNumber = lineNumber;
        this.recordType = recordType;
        this.validationErrors = new ArrayList<>();
        this.recordId = null;
    }
    
    /**
     * Constructs a new VSS validation exception with detailed context information and record ID.
     * 
     * @param message the detail message
     * @param lineNumber the line number where the error occurred
     * @param recordType the type of record being validated (e.g., "VSS-110", "VSS-120")
     * @param recordId the identifier of the record (e.g., destination ID)
     */
    public VssValidationException(String message, int lineNumber, String recordType, String recordId) {
        super(formatMessage(message, lineNumber, recordType, recordId));
        this.lineNumber = lineNumber;
        this.recordType = recordType;
        this.validationErrors = new ArrayList<>();
        this.recordId = recordId;
    }
    
    /**
     * Constructs a new VSS validation exception with multiple validation errors.
     * 
     * @param validationErrors the list of validation error messages
     * @param lineNumber the line number where the error occurred
     * @param recordType the type of record being validated
     */
    public VssValidationException(List<String> validationErrors, int lineNumber, String recordType) {
        super(formatMultipleErrorsMessage(validationErrors, lineNumber, recordType, null));
        this.lineNumber = lineNumber;
        this.recordType = recordType;
        this.validationErrors = new ArrayList<>(validationErrors);
        this.recordId = null;
    }
    
    /**
     * Constructs a new VSS validation exception with multiple validation errors and record ID.
     * 
     * @param validationErrors the list of validation error messages
     * @param lineNumber the line number where the error occurred
     * @param recordType the type of record being validated
     * @param recordId the identifier of the record
     */
    public VssValidationException(List<String> validationErrors, int lineNumber, String recordType, String recordId) {
        super(formatMultipleErrorsMessage(validationErrors, lineNumber, recordType, recordId));
        this.lineNumber = lineNumber;
        this.recordType = recordType;
        this.validationErrors = new ArrayList<>(validationErrors);
        this.recordId = recordId;
    }
    
    /**
     * Gets the line number where the validation error occurred.
     * 
     * @return the line number, or -1 if not available
     */
    public int getLineNumber() {
        return lineNumber;
    }
    
    /**
     * Gets the type of record that was being validated when the error occurred.
     * 
     * @return the record type (e.g., "VSS-110", "VSS-120"), or null if not available
     */
    public String getRecordType() {
        return recordType;
    }
    
    /**
     * Gets the list of validation error messages.
     * 
     * @return an unmodifiable list of validation errors
     */
    public List<String> getValidationErrors() {
        return Collections.unmodifiableList(validationErrors);
    }
    
    /**
     * Gets the record identifier.
     * 
     * @return the record ID, or null if not available
     */
    public String getRecordId() {
        return recordId;
    }
    
    /**
     * Checks if this exception has line number context.
     * 
     * @return true if line number is available
     */
    public boolean hasLineNumber() {
        return lineNumber > 0;
    }
    
    /**
     * Checks if this exception has record type context.
     * 
     * @return true if record type is available
     */
    public boolean hasRecordType() {
        return recordType != null && !recordType.trim().isEmpty();
    }
    
    /**
     * Checks if this exception has multiple validation errors.
     * 
     * @return true if there are multiple validation errors
     */
    public boolean hasMultipleErrors() {
        return validationErrors.size() > 1;
    }
    
    /**
     * Checks if this exception has a record identifier.
     * 
     * @return true if record ID is available
     */
    public boolean hasRecordId() {
        return recordId != null && !recordId.trim().isEmpty();
    }
    
    /**
     * Gets the number of validation errors.
     * 
     * @return the count of validation errors
     */
    public int getErrorCount() {
        return validationErrors.size();
    }
    
    /**
     * Formats the exception message with context information.
     * 
     * @param message the base message
     * @param lineNumber the line number
     * @param recordType the record type
     * @param recordId the record identifier
     * @return the formatted message
     */
    private static String formatMessage(String message, int lineNumber, String recordType, String recordId) {
        StringBuilder sb = new StringBuilder();
        
        if (recordType != null && !recordType.trim().isEmpty()) {
            sb.append("[").append(recordType).append("] ");
        }
        
        if (lineNumber > 0) {
            sb.append("Line ").append(lineNumber).append(": ");
        }
        
        if (recordId != null && !recordId.trim().isEmpty()) {
            sb.append("Record '").append(recordId).append("' - ");
        }
        
        sb.append(message);
        
        return sb.toString();
    }
    
    /**
     * Formats the exception message for multiple validation errors.
     * 
     * @param errors the list of validation errors
     * @param lineNumber the line number
     * @param recordType the record type
     * @param recordId the record identifier
     * @return the formatted message
     */
    private static String formatMultipleErrorsMessage(List<String> errors, int lineNumber, String recordType, String recordId) {
        StringBuilder sb = new StringBuilder();
        
        if (recordType != null && !recordType.trim().isEmpty()) {
            sb.append("[").append(recordType).append("] ");
        }
        
        if (lineNumber > 0) {
            sb.append("Line ").append(lineNumber).append(": ");
        }
        
        if (recordId != null && !recordId.trim().isEmpty()) {
            sb.append("Record '").append(recordId).append("' - ");
        }
        
        sb.append("Multiple validation errors (").append(errors.size()).append("): ");
        
        for (int i = 0; i < errors.size(); i++) {
            if (i > 0) {
                sb.append("; ");
            }
            sb.append(errors.get(i));
        }
        
        return sb.toString();
    }
    
    /**
     * Creates a validation exception for invalid transaction code.
     * 
     * @param actualCode the actual transaction code
     * @param expectedCode the expected transaction code
     * @param lineNumber the line number
     * @param recordType the record type
     * @return the formatted exception
     */
    public static VssValidationException invalidTransactionCode(String actualCode, String expectedCode, int lineNumber, String recordType) {
        String message = String.format("Invalid transaction code '%s'. Expected '%s'", actualCode, expectedCode);
        return new VssValidationException(message, lineNumber, recordType);
    }
    
    /**
     * Creates a validation exception for invalid business mode.
     * 
     * @param businessMode the invalid business mode
     * @param validModes the list of valid business modes
     * @param lineNumber the line number
     * @param recordType the record type
     * @return the formatted exception
     */
    public static VssValidationException invalidBusinessMode(String businessMode, List<String> validModes, int lineNumber, String recordType) {
        String message = String.format("Invalid business mode '%s'. Valid modes: %s", businessMode, validModes);
        return new VssValidationException(message, lineNumber, recordType);
    }
    
    /**
     * Creates a validation exception for invalid amount type.
     * 
     * @param amountType the invalid amount type
     * @param reportId the report ID number
     * @param validTypes the list of valid amount types for this report ID
     * @param lineNumber the line number
     * @param recordType the record type
     * @return the formatted exception
     */
    public static VssValidationException invalidAmountType(String amountType, String reportId, List<String> validTypes, int lineNumber, String recordType) {
        String message = String.format("Invalid amount type '%s' for report ID '%s'. Valid types: %s", amountType, reportId, validTypes);
        return new VssValidationException(message, lineNumber, recordType);
    }
    
    /**
     * Creates a validation exception for inconsistent amounts.
     * 
     * @param creditAmount the credit amount
     * @param debitAmount the debit amount
     * @param netAmount the net amount
     * @param lineNumber the line number
     * @param recordType the record type
     * @return the formatted exception
     */
    public static VssValidationException inconsistentAmounts(String creditAmount, String debitAmount, String netAmount, int lineNumber, String recordType) {
        String message = String.format("Inconsistent amounts. Credit: %s, Debit: %s, Net: %s", creditAmount, debitAmount, netAmount);
        return new VssValidationException(message, lineNumber, recordType);
    }
    
    /**
     * Creates a validation exception for invalid destination ID format.
     * 
     * @param destinationId the invalid destination ID
     * @param lineNumber the line number
     * @param recordType the record type
     * @return the formatted exception
     */
    public static VssValidationException invalidDestinationId(String destinationId, int lineNumber, String recordType) {
        String message = String.format("Invalid destination ID format: '%s'. Must be exactly 6 digits", destinationId);
        return new VssValidationException(message, lineNumber, recordType);
    }
    
    /**
     * Creates a validation exception for invalid report identification.
     * 
     * @param reportGroup the report group
     * @param reportSubgroup the report subgroup
     * @param reportId the report ID
     * @param lineNumber the line number
     * @param recordType the record type
     * @return the formatted exception
     */
    public static VssValidationException invalidReportIdentification(String reportGroup, String reportSubgroup, String reportId, int lineNumber, String recordType) {
        String message = String.format("Invalid report identification. Group: '%s', Subgroup: '%s', ID: '%s'. Expected: 'V', '2', '110' or '111'", 
                                      reportGroup, reportSubgroup, reportId);
        return new VssValidationException(message, lineNumber, recordType);
    }
    
    /**
     * Creates a validation exception for missing required data.
     * 
     * @param fieldName the name of the missing field
     * @param lineNumber the line number
     * @param recordType the record type
     * @param recordId the record identifier
     * @return the formatted exception
     */
    public static VssValidationException missingRequiredData(String fieldName, int lineNumber, String recordType, String recordId) {
        String message = String.format("Required field '%s' is missing or empty", fieldName);
        return new VssValidationException(message, lineNumber, recordType, recordId);
    }
    
    /**
     * Creates a validation exception for date range validation.
     * 
     * @param settlementDate the settlement date
     * @param minDate the minimum allowed date
     * @param maxDate the maximum allowed date
     * @param lineNumber the line number
     * @param recordType the record type
     * @return the formatted exception
     */
    public static VssValidationException invalidDateRange(String settlementDate, String minDate, String maxDate, int lineNumber, String recordType) {
        String message = String.format("Settlement date '%s' is outside valid range [%s - %s]", settlementDate, minDate, maxDate);
        return new VssValidationException(message, lineNumber, recordType);
    }
}