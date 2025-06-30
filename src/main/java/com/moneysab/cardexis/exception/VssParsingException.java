package com.moneysab.cardexis.exception;

/**
 * Exception thrown when parsing VSS (Visa Settlement Service) records fails.
 * 
 * This exception is thrown when there are issues parsing the fixed-width
 * VSS-110 or VSS-120 record formats, such as:
 * - Invalid record length
 * - Missing required fields
 * - Invalid field formats
 * - Malformed data
 * 
 * @author EL.AHGOUNE
 * @version 1.0.0
 * @since 2024
 */
public class VssParsingException extends RuntimeException {
    
    private final int lineNumber;
    private final String recordType;
    private final String fieldName;
    
    /**
     * Constructs a new VSS parsing exception with the specified detail message.
     * 
     * @param message the detail message
     */
    public VssParsingException(String message) {
        super(message);
        this.lineNumber = -1;
        this.recordType = null;
        this.fieldName = null;
    }
    
    /**
     * Constructs a new VSS parsing exception with the specified detail message and cause.
     * 
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public VssParsingException(String message, Throwable cause) {
        super(message, cause);
        this.lineNumber = -1;
        this.recordType = null;
        this.fieldName = null;
    }
    
    /**
     * Constructs a new VSS parsing exception with detailed context information.
     * 
     * @param message the detail message
     * @param lineNumber the line number where the error occurred
     * @param recordType the type of record being parsed (e.g., "VSS-110", "VSS-120")
     */
    public VssParsingException(String message, int lineNumber, String recordType) {
        super(formatMessage(message, lineNumber, recordType, null));
        this.lineNumber = lineNumber;
        this.recordType = recordType;
        this.fieldName = null;
    }
    
    /**
     * Constructs a new VSS parsing exception with detailed context information and cause.
     * 
     * @param message the detail message
     * @param lineNumber the line number where the error occurred
     * @param recordType the type of record being parsed (e.g., "VSS-110", "VSS-120")
     * @param cause the cause of the exception
     */
    public VssParsingException(String message, int lineNumber, String recordType, Throwable cause) {
        super(formatMessage(message, lineNumber, recordType, null), cause);
        this.lineNumber = lineNumber;
        this.recordType = recordType;
        this.fieldName = null;
    }
    
    /**
     * Constructs a new VSS parsing exception with field-level context information.
     * 
     * @param message the detail message
     * @param lineNumber the line number where the error occurred
     * @param recordType the type of record being parsed (e.g., "VSS-110", "VSS-120")
     * @param fieldName the name of the field that failed to parse
     */
    public VssParsingException(String message, int lineNumber, String recordType, String fieldName) {
        super(formatMessage(message, lineNumber, recordType, fieldName));
        this.lineNumber = lineNumber;
        this.recordType = recordType;
        this.fieldName = fieldName;
    }
    
    /**
     * Constructs a new VSS parsing exception with field-level context information and cause.
     * 
     * @param message the detail message
     * @param lineNumber the line number where the error occurred
     * @param recordType the type of record being parsed (e.g., "VSS-110", "VSS-120")
     * @param fieldName the name of the field that failed to parse
     * @param cause the cause of the exception
     */
    public VssParsingException(String message, int lineNumber, String recordType, String fieldName, Throwable cause) {
        super(formatMessage(message, lineNumber, recordType, fieldName), cause);
        this.lineNumber = lineNumber;
        this.recordType = recordType;
        this.fieldName = fieldName;
    }
    
    /**
     * Gets the line number where the parsing error occurred.
     * 
     * @return the line number, or -1 if not available
     */
    public int getLineNumber() {
        return lineNumber;
    }
    
    /**
     * Gets the type of record that was being parsed when the error occurred.
     * 
     * @return the record type (e.g., "VSS-110", "VSS-120"), or null if not available
     */
    public String getRecordType() {
        return recordType;
    }
    
    /**
     * Gets the name of the field that failed to parse.
     * 
     * @return the field name, or null if not available
     */
    public String getFieldName() {
        return fieldName;
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
     * Checks if this exception has field name context.
     * 
     * @return true if field name is available
     */
    public boolean hasFieldName() {
        return fieldName != null && !fieldName.trim().isEmpty();
    }
    
    /**
     * Formats the exception message with context information.
     * 
     * @param message the base message
     * @param lineNumber the line number
     * @param recordType the record type
     * @param fieldName the field name
     * @return the formatted message
     */
    private static String formatMessage(String message, int lineNumber, String recordType, String fieldName) {
        StringBuilder sb = new StringBuilder();
        
        if (recordType != null && !recordType.trim().isEmpty()) {
            sb.append("[").append(recordType).append("] ");
        }
        
        if (lineNumber > 0) {
            sb.append("Line ").append(lineNumber).append(": ");
        }
        
        if (fieldName != null && !fieldName.trim().isEmpty()) {
            sb.append("Field '").append(fieldName).append("' - ");
        }
        
        sb.append(message);
        
        return sb.toString();
    }
    
    /**
     * Creates a parsing exception for invalid record length.
     * 
     * @param expectedLength the expected record length
     * @param actualLength the actual record length
     * @param lineNumber the line number
     * @param recordType the record type
     * @return the formatted exception
     */
    public static VssParsingException invalidRecordLength(int expectedLength, int actualLength, int lineNumber, String recordType) {
        String message = String.format("Invalid record length. Expected %d characters, got %d", expectedLength, actualLength);
        return new VssParsingException(message, lineNumber, recordType);
    }
    
    /**
     * Creates a parsing exception for missing required field.
     * 
     * @param fieldName the name of the missing field
     * @param lineNumber the line number
     * @param recordType the record type
     * @return the formatted exception
     */
    public static VssParsingException missingRequiredField(String fieldName, int lineNumber, String recordType) {
        String message = String.format("Required field is missing or empty");
        return new VssParsingException(message, lineNumber, recordType, fieldName);
    }
    
    /**
     * Creates a parsing exception for invalid field format.
     * 
     * @param fieldName the name of the field
     * @param expectedFormat the expected format
     * @param actualValue the actual value
     * @param lineNumber the line number
     * @param recordType the record type
     * @return the formatted exception
     */
    public static VssParsingException invalidFieldFormat(String fieldName, String expectedFormat, String actualValue, int lineNumber, String recordType) {
        String message = String.format("Invalid format. Expected '%s', got '%s'", expectedFormat, actualValue);
        return new VssParsingException(message, lineNumber, recordType, fieldName);
    }
    
    /**
     * Creates a parsing exception for invalid numeric value.
     * 
     * @param fieldName the name of the field
     * @param value the invalid value
     * @param lineNumber the line number
     * @param recordType the record type
     * @param cause the underlying cause
     * @return the formatted exception
     */
    public static VssParsingException invalidNumericValue(String fieldName, String value, int lineNumber, String recordType, Throwable cause) {
        String message = String.format("Invalid numeric value: '%s'", value);
        return new VssParsingException(message, lineNumber, recordType, fieldName, cause);
    }
    
    /**
     * Creates a parsing exception for invalid date value.
     * 
     * @param fieldName the name of the field
     * @param value the invalid date value
     * @param expectedFormat the expected date format
     * @param lineNumber the line number
     * @param recordType the record type
     * @param cause the underlying cause
     * @return the formatted exception
     */
    public static VssParsingException invalidDateValue(String fieldName, String value, String expectedFormat, int lineNumber, String recordType, Throwable cause) {
        String message = String.format("Invalid date value: '%s'. Expected format: %s", value, expectedFormat);
        return new VssParsingException(message, lineNumber, recordType, fieldName, cause);
    }
}