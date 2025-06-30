package com.moneysab.cardexis.exception;

/**
 * Exception thrown when VSS (Visa Settlement Service) file processing fails.
 * 
 * This exception is thrown when there are issues during the overall processing
 * of VSS files, such as:
 * - File I/O errors
 * - Database transaction failures
 * - Batch processing errors
 * - System resource issues
 * - Configuration problems
 * 
 * @author EL.AHGOUNE
 * @version 1.0.0
 * @since 2024
 */
public class VssProcessingException extends RuntimeException {
    
    private final String fileName;
    private final String processingStage;
    private final int recordsProcessed;
    private final int totalRecords;
    
    /**
     * Constructs a new VSS processing exception with the specified detail message.
     * 
     * @param message the detail message
     */
    public VssProcessingException(String message) {
        super(message);
        this.fileName = null;
        this.processingStage = null;
        this.recordsProcessed = -1;
        this.totalRecords = -1;
    }
    
    /**
     * Constructs a new VSS processing exception with the specified detail message and cause.
     * 
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public VssProcessingException(String message, Throwable cause) {
        super(message, cause);
        this.fileName = null;
        this.processingStage = null;
        this.recordsProcessed = -1;
        this.totalRecords = -1;
    }
    
    /**
     * Constructs a new VSS processing exception with file context.
     * 
     * @param message the detail message
     * @param fileName the name of the file being processed
     */
    public VssProcessingException(String message, String fileName) {
        super(formatMessage(message, fileName, null, -1, -1));
        this.fileName = fileName;
        this.processingStage = null;
        this.recordsProcessed = -1;
        this.totalRecords = -1;
    }
    
    /**
     * Constructs a new VSS processing exception with file context and cause.
     * 
     * @param message the detail message
     * @param fileName the name of the file being processed
     * @param cause the cause of the exception
     */
    public VssProcessingException(String message, String fileName, Throwable cause) {
        super(formatMessage(message, fileName, null, -1, -1), cause);
        this.fileName = fileName;
        this.processingStage = null;
        this.recordsProcessed = -1;
        this.totalRecords = -1;
    }
    
    /**
     * Constructs a new VSS processing exception with detailed processing context.
     * 
     * @param message the detail message
     * @param fileName the name of the file being processed
     * @param processingStage the stage of processing where the error occurred
     */
    public VssProcessingException(String message, String fileName, String processingStage) {
        super(formatMessage(message, fileName, processingStage, -1, -1));
        this.fileName = fileName;
        this.processingStage = processingStage;
        this.recordsProcessed = -1;
        this.totalRecords = -1;
    }
    
    /**
     * Constructs a new VSS processing exception with detailed processing context and cause.
     * 
     * @param message the detail message
     * @param fileName the name of the file being processed
     * @param processingStage the stage of processing where the error occurred
     * @param cause the cause of the exception
     */
    public VssProcessingException(String message, String fileName, String processingStage, Throwable cause) {
        super(formatMessage(message, fileName, processingStage, -1, -1), cause);
        this.fileName = fileName;
        this.processingStage = processingStage;
        this.recordsProcessed = -1;
        this.totalRecords = -1;
    }
    
    /**
     * Constructs a new VSS processing exception with progress information.
     * 
     * @param message the detail message
     * @param fileName the name of the file being processed
     * @param processingStage the stage of processing where the error occurred
     * @param recordsProcessed the number of records successfully processed
     * @param totalRecords the total number of records in the file
     */
    public VssProcessingException(String message, String fileName, String processingStage, int recordsProcessed, int totalRecords) {
        super(formatMessage(message, fileName, processingStage, recordsProcessed, totalRecords));
        this.fileName = fileName;
        this.processingStage = processingStage;
        this.recordsProcessed = recordsProcessed;
        this.totalRecords = totalRecords;
    }
    
    /**
     * Constructs a new VSS processing exception with progress information and cause.
     * 
     * @param message the detail message
     * @param fileName the name of the file being processed
     * @param processingStage the stage of processing where the error occurred
     * @param recordsProcessed the number of records successfully processed
     * @param totalRecords the total number of records in the file
     * @param cause the cause of the exception
     */
    public VssProcessingException(String message, String fileName, String processingStage, int recordsProcessed, int totalRecords, Throwable cause) {
        super(formatMessage(message, fileName, processingStage, recordsProcessed, totalRecords), cause);
        this.fileName = fileName;
        this.processingStage = processingStage;
        this.recordsProcessed = recordsProcessed;
        this.totalRecords = totalRecords;
    }
    
    /**
     * Gets the name of the file being processed when the error occurred.
     * 
     * @return the file name, or null if not available
     */
    public String getFileName() {
        return fileName;
    }
    
    /**
     * Gets the processing stage where the error occurred.
     * 
     * @return the processing stage, or null if not available
     */
    public String getProcessingStage() {
        return processingStage;
    }
    
    /**
     * Gets the number of records successfully processed before the error.
     * 
     * @return the number of records processed, or -1 if not available
     */
    public int getRecordsProcessed() {
        return recordsProcessed;
    }
    
    /**
     * Gets the total number of records in the file.
     * 
     * @return the total number of records, or -1 if not available
     */
    public int getTotalRecords() {
        return totalRecords;
    }
    
    /**
     * Checks if this exception has file name context.
     * 
     * @return true if file name is available
     */
    public boolean hasFileName() {
        return fileName != null && !fileName.trim().isEmpty();
    }
    
    /**
     * Checks if this exception has processing stage context.
     * 
     * @return true if processing stage is available
     */
    public boolean hasProcessingStage() {
        return processingStage != null && !processingStage.trim().isEmpty();
    }
    
    /**
     * Checks if this exception has progress information.
     * 
     * @return true if progress information is available
     */
    public boolean hasProgressInfo() {
        return recordsProcessed >= 0 && totalRecords >= 0;
    }
    
    /**
     * Gets the processing progress as a percentage.
     * 
     * @return the progress percentage (0-100), or -1 if not available
     */
    public double getProgressPercentage() {
        if (!hasProgressInfo() || totalRecords == 0) {
            return -1.0;
        }
        return (double) recordsProcessed / totalRecords * 100.0;
    }
    
    /**
     * Gets the number of remaining records to process.
     * 
     * @return the number of remaining records, or -1 if not available
     */
    public int getRemainingRecords() {
        if (!hasProgressInfo()) {
            return -1;
        }
        return Math.max(0, totalRecords - recordsProcessed);
    }
    
    /**
     * Formats the exception message with context information.
     * 
     * @param message the base message
     * @param fileName the file name
     * @param processingStage the processing stage
     * @param recordsProcessed the number of records processed
     * @param totalRecords the total number of records
     * @return the formatted message
     */
    private static String formatMessage(String message, String fileName, String processingStage, int recordsProcessed, int totalRecords) {
        StringBuilder sb = new StringBuilder();
        
        if (fileName != null && !fileName.trim().isEmpty()) {
            sb.append("File '").append(fileName).append("': ");
        }
        
        if (processingStage != null && !processingStage.trim().isEmpty()) {
            sb.append("[").append(processingStage).append("] ");
        }
        
        sb.append(message);
        
        if (recordsProcessed >= 0 && totalRecords >= 0) {
            sb.append(String.format(" (Progress: %d/%d records, %.1f%%)", 
                                  recordsProcessed, totalRecords, 
                                  totalRecords > 0 ? (double) recordsProcessed / totalRecords * 100.0 : 0.0));
        }
        
        return sb.toString();
    }
    
    /**
     * Creates a processing exception for file I/O errors.
     * 
     * @param fileName the name of the file
     * @param operation the I/O operation that failed
     * @param cause the underlying I/O exception
     * @return the formatted exception
     */
    public static VssProcessingException fileIoError(String fileName, String operation, Throwable cause) {
        String message = String.format("File I/O error during %s", operation);
        return new VssProcessingException(message, fileName, "FILE_IO", cause);
    }
    
    /**
     * Creates a processing exception for database errors.
     * 
     * @param fileName the name of the file being processed
     * @param operation the database operation that failed
     * @param recordsProcessed the number of records processed
     * @param totalRecords the total number of records
     * @param cause the underlying database exception
     * @return the formatted exception
     */
    public static VssProcessingException databaseError(String fileName, String operation, int recordsProcessed, int totalRecords, Throwable cause) {
        String message = String.format("Database error during %s", operation);
        return new VssProcessingException(message, fileName, "DATABASE", recordsProcessed, totalRecords, cause);
    }
    
    /**
     * Creates a processing exception for batch processing errors.
     * 
     * @param fileName the name of the file being processed
     * @param batchSize the size of the batch that failed
     * @param recordsProcessed the number of records processed
     * @param totalRecords the total number of records
     * @param cause the underlying exception
     * @return the formatted exception
     */
    public static VssProcessingException batchProcessingError(String fileName, int batchSize, int recordsProcessed, int totalRecords, Throwable cause) {
        String message = String.format("Batch processing error (batch size: %d)", batchSize);
        return new VssProcessingException(message, fileName, "BATCH_PROCESSING", recordsProcessed, totalRecords, cause);
    }
    
    /**
     * Creates a processing exception for configuration errors.
     * 
     * @param configProperty the configuration property that is invalid
     * @param value the invalid value
     * @return the formatted exception
     */
    public static VssProcessingException configurationError(String configProperty, String value) {
        String message = String.format("Invalid configuration: %s = '%s'", configProperty, value);
        return new VssProcessingException(message, null, "CONFIGURATION");
    }
    
    /**
     * Creates a processing exception for resource exhaustion.
     * 
     * @param resourceType the type of resource that was exhausted
     * @param fileName the name of the file being processed
     * @param processingStage the processing stage
     * @return the formatted exception
     */
    public static VssProcessingException resourceExhausted(String resourceType, String fileName, String processingStage) {
        String message = String.format("Resource exhausted: %s", resourceType);
        return new VssProcessingException(message, fileName, processingStage);
    }
    
    /**
     * Creates a processing exception for timeout errors.
     * 
     * @param operation the operation that timed out
     * @param timeoutSeconds the timeout duration in seconds
     * @param fileName the name of the file being processed
     * @param recordsProcessed the number of records processed
     * @param totalRecords the total number of records
     * @return the formatted exception
     */
    public static VssProcessingException timeoutError(String operation, long timeoutSeconds, String fileName, int recordsProcessed, int totalRecords) {
        String message = String.format("Operation '%s' timed out after %d seconds", operation, timeoutSeconds);
        return new VssProcessingException(message, fileName, "TIMEOUT", recordsProcessed, totalRecords);
    }
    
    /**
     * Creates a processing exception for unsupported file format.
     * 
     * @param fileName the name of the file
     * @param detectedFormat the detected file format
     * @param supportedFormats the list of supported formats
     * @return the formatted exception
     */
    public static VssProcessingException unsupportedFormat(String fileName, String detectedFormat, String supportedFormats) {
        String message = String.format("Unsupported file format '%s'. Supported formats: %s", detectedFormat, supportedFormats);
        return new VssProcessingException(message, fileName, "FORMAT_DETECTION");
    }
}