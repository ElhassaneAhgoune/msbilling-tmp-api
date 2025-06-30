package com.moneysab.cardexis.domain.entity;

import com.moneysab.cardexis.domain.enums.FileType;
import com.moneysab.cardexis.domain.enums.ProcessingStatus;
import com.moneysab.cardexis.domain.enums.VisaReportFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity representing a file processing job in the Visa EPIN system.
 * 
 * This entity tracks the complete lifecycle of file processing operations,
 * from initial upload through completion or failure. It maintains metadata
 * about the file, processing statistics, and state transitions.
 * 
 * Key Features:
 * - Complete job lifecycle management
 * - Metadata storage for format-specific information
 * - Processing statistics and timing
 * - State transition validation
 * - Error tracking and retry management
 * 
 * @author EL.AHGOUNE
 * @version 1.0.0
 * @since 2024
 */
@Entity
@Table(name = "file_processing_jobs", indexes = {
    @Index(name = "idx_file_processing_jobs_status", columnList = "status"),
    @Index(name = "idx_file_processing_jobs_client_id", columnList = "client_id"),
    @Index(name = "idx_file_processing_jobs_created_at", columnList = "created_at"),
    @Index(name = "idx_file_processing_jobs_filename", columnList = "original_filename")
})
public class FileProcessingJob extends BaseEntity {
    
    /**
     * Original filename as uploaded by the client.
     * Used for tracking and audit purposes.
     */
    @NotBlank(message = "Original filename cannot be blank")
    @Size(max = 255, message = "Filename cannot exceed 255 characters")
    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;
    
    /**
     * Type of file being processed (EPIN, VSS, etc.).
     * Determines the processing strategy and validation rules.
     */
    @NotNull(message = "File type cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 20)
    private FileType fileType;
    
    /**
     * Current processing status of the job.
     * Tracks the job through its lifecycle states.
     */
    @NotNull(message = "Processing status cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProcessingStatus status;
    
    /**
     * Detected Visa report format (VSS-110, VSS-120, etc.).
     * Determined during file analysis and used for format-specific processing.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "report_format", length = 20)
    private VisaReportFormat reportFormat;
    
    /**
     * Client identifier from the file header.
     * Used for client-specific processing and reporting.
     */
    @Size(max = 50, message = "Client ID cannot exceed 50 characters")
    @Column(name = "client_id", length = 50)
    private String clientId;
    
    /**
     * Size of the uploaded file in bytes.
     * Used for processing statistics and validation.
     */
    @Positive(message = "File size must be positive")
    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;
    
    /**
     * Total number of records found in the file.
     * Includes header, data, and trailer records.
     */
    @Column(name = "total_records")
    private Integer totalRecords;
    
    /**
     * Number of records successfully processed.
     * Used to track processing progress and calculate success rate.
     */
    @Column(name = "processed_records")
    private Integer processedRecords;
    
    /**
     * Number of records that failed processing.
     * Used for error reporting and quality metrics.
     */
    @Column(name = "failed_records")
    private Integer failedRecords;
    
    /**
     * Timestamp when processing started.
     * Used to calculate processing duration and detect stuck jobs.
     */
    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;
    
    /**
     * Timestamp when processing completed (successfully or with failure).
     * Used to calculate total processing time.
     */
    @Column(name = "processing_completed_at")
    private LocalDateTime processingCompletedAt;
    
    /**
     * Error message if processing failed.
     * Contains detailed information about the failure for troubleshooting.
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    /**
     * Number of retry attempts made for this job.
     * Used to implement retry limits and exponential backoff.
     */
    @Column(name = "retry_count")
    private Integer retryCount = 0;
    
    /**
     * Maximum number of retry attempts allowed.
     * Configurable per job type or client.
     */
    @Column(name = "max_retries")
    private Integer maxRetries = 3;
    
    /**
     * Additional metadata stored as key-value pairs.
     * Used for format-specific information and processing context.
     */
    @ElementCollection
    @CollectionTable(name = "file_processing_job_metadata", 
                    joinColumns = @JoinColumn(name = "job_id"))
    @MapKeyColumn(name = "metadata_key")
    @Column(name = "metadata_value")
    private Map<String, String> metadata = new HashMap<>();
    
    /**
     * Default constructor for JPA.
     */
    protected FileProcessingJob() {
        super();
    }
    
    /**
     * Creates a new FileProcessingJob with the specified parameters.
     * 
     * @param originalFilename the original filename
     * @param fileType the type of file
     * @param fileSizeBytes the size of the file in bytes
     */
    public FileProcessingJob(String originalFilename, FileType fileType, Long fileSizeBytes) {
        this();
        this.originalFilename = originalFilename;
        this.fileType = fileType;
        this.fileSizeBytes = fileSizeBytes;
        this.status = ProcessingStatus.UPLOADED;
    }
    
    // Getters and Setters
    
    public String getOriginalFilename() {
        return originalFilename;
    }
    
    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }
    
    public FileType getFileType() {
        return fileType;
    }
    
    public void setFileType(FileType fileType) {
        this.fileType = fileType;
    }
    
    public ProcessingStatus getStatus() {
        return status;
    }
    
    public void setStatus(ProcessingStatus status) {
        if (this.status != null) {
            this.status.validateTransition(status);
        }
        this.status = status;
    }
    
    public VisaReportFormat getReportFormat() {
        return reportFormat;
    }
    
    public void setReportFormat(VisaReportFormat reportFormat) {
        this.reportFormat = reportFormat;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    
    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }
    
    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }
    
    public Integer getTotalRecords() {
        return totalRecords;
    }
    
    public void setTotalRecords(Integer totalRecords) {
        this.totalRecords = totalRecords;
    }
    
    public Integer getProcessedRecords() {
        return processedRecords;
    }
    
    public void setProcessedRecords(Integer processedRecords) {
        this.processedRecords = processedRecords;
    }
    
    public Integer getFailedRecords() {
        return failedRecords;
    }
    
    public void setFailedRecords(Integer failedRecords) {
        this.failedRecords = failedRecords;
    }
    
    public LocalDateTime getProcessingStartedAt() {
        return processingStartedAt;
    }
    
    public void setProcessingStartedAt(LocalDateTime processingStartedAt) {
        this.processingStartedAt = processingStartedAt;
    }
    
    public LocalDateTime getProcessingCompletedAt() {
        return processingCompletedAt;
    }
    
    public void setProcessingCompletedAt(LocalDateTime processingCompletedAt) {
        this.processingCompletedAt = processingCompletedAt;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public Integer getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }
    
    public Integer getMaxRetries() {
        return maxRetries;
    }
    
    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }
    
    public Map<String, String> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }
    
    // Business Logic Methods
    
    /**
     * Starts processing by transitioning to PROCESSING status and recording start time.
     */
    public void startProcessing() {
        setStatus(ProcessingStatus.PROCESSING);
        this.processingStartedAt = LocalDateTime.now();
    }
    
    /**
     * Completes processing successfully by transitioning to COMPLETED status.
     */
    public void completeProcessing() {
        setStatus(ProcessingStatus.COMPLETED);
        this.processingCompletedAt = LocalDateTime.now();
    }
    
    /**
     * Marks processing as failed with an error message.
     * 
     * @param errorMessage the error message describing the failure
     */
    public void failProcessing(String errorMessage) {
        setStatus(ProcessingStatus.FAILED);
        this.processingCompletedAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
    }
    
    /**
     * Cancels processing by transitioning to CANCELLED status.
     */
    public void cancelProcessing() {
        setStatus(ProcessingStatus.CANCELLED);
        this.processingCompletedAt = LocalDateTime.now();
    }
    
    /**
     * Increments the retry count and checks if max retries exceeded.
     * 
     * @return true if retry is allowed, false if max retries exceeded
     */
    public boolean incrementRetryCount() {
        this.retryCount++;
        return this.retryCount <= this.maxRetries;
    }
    
    /**
     * Calculates the processing duration if processing has started.
     * 
     * @return the processing duration, or null if not started
     */
    public Duration getProcessingDuration() {
        if (processingStartedAt == null) {
            return null;
        }
        
        LocalDateTime endTime = processingCompletedAt != null ? 
            processingCompletedAt : LocalDateTime.now();
        return Duration.between(processingStartedAt, endTime);
    }
    
    /**
     * Calculates the success rate of record processing.
     * 
     * @return the success rate as a percentage (0.0 to 100.0)
     */
    public double getSuccessRate() {
        if (totalRecords == null || totalRecords == 0) {
            return 0.0;
        }
        
        int successful = processedRecords != null ? processedRecords : 0;
        return (double) successful / totalRecords * 100.0;
    }
    
    /**
     * Adds metadata key-value pair.
     * 
     * @param key the metadata key
     * @param value the metadata value
     */
    public void addMetadata(String key, String value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }
    
    /**
     * Gets metadata value by key.
     * 
     * @param key the metadata key
     * @return the metadata value, or null if not found
     */
    public String getMetadata(String key) {
        return this.metadata != null ? this.metadata.get(key) : null;
    }
    
    /**
     * Checks if the job can be retried based on current status and retry count.
     * 
     * @return true if retry is allowed, false otherwise
     */
    public boolean canRetry() {
        return status.canRetry() && retryCount < maxRetries;
    }
    
    /**
     * Checks if the job is currently in an active processing state.
     * 
     * @return true if the job is active, false otherwise
     */
    public boolean isActive() {
        return status.isActive();
    }
    
    /**
     * Checks if the job has completed (successfully or with failure).
     * 
     * @return true if the job is in a terminal state, false otherwise
     */
    public boolean isCompleted() {
        return status.isTerminal();
    }
}