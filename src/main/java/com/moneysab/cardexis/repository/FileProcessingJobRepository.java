package com.moneysab.cardexis.repository;

import com.moneysab.cardexis.domain.entity.FileProcessingJob;
import com.moneysab.cardexis.domain.enums.FileType;
import com.moneysab.cardexis.domain.enums.ProcessingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for FileProcessingJob entity operations.
 * 
 * Provides data access methods for file processing job management,
 * including status tracking, client-specific queries, and performance
 * monitoring capabilities.
 * 
 * Key Features:
 * - Status-based job queries
 * - Client-specific filtering
 * - Performance and statistics queries
 * - Retry management support
 * - Audit and monitoring queries
 * 
 * @author EL.AHGOUNE
 * @version 1.0.0
 * @since 2024
 */
@Repository
public interface FileProcessingJobRepository extends JpaRepository<FileProcessingJob, UUID> {
    
    // Basic Status Queries
    
    /**
     * Finds all jobs with the specified processing status.
     * 
     * @param status the processing status to filter by
     * @return list of jobs with the specified status
     */
    List<FileProcessingJob> findByStatus(ProcessingStatus status);
    
    /**
     * Finds all jobs with the specified processing status, paginated.
     * 
     * @param status the processing status to filter by
     * @param pageable pagination information
     * @return page of jobs with the specified status
     */
    Page<FileProcessingJob> findByStatus(ProcessingStatus status, Pageable pageable);
    
    /**
     * Finds all jobs with status in the specified list.
     * 
     * @param statuses list of processing statuses to filter by
     * @return list of jobs with any of the specified statuses
     */
    List<FileProcessingJob> findByStatusIn(List<ProcessingStatus> statuses);
    
    /**
     * Finds all active jobs (UPLOADED, PROCESSING).
     * 
     * @return list of active jobs
     */
    @Query("SELECT j FROM FileProcessingJob j WHERE j.status IN ('UPLOADED', 'PROCESSING')")
    List<FileProcessingJob> findActiveJobs();
    
    /**
     * Finds all completed jobs (COMPLETED, FAILED, CANCELLED).
     * 
     * @return list of completed jobs
     */
    @Query("SELECT j FROM FileProcessingJob j WHERE j.status IN ('COMPLETED', 'FAILED', 'CANCELLED')")
    List<FileProcessingJob> findCompletedJobs();
    
    // Client-Specific Queries
    
    /**
     * Finds all jobs for a specific client.
     * 
     * @param clientId the client identifier
     * @return list of jobs for the specified client
     */
    List<FileProcessingJob> findByClientId(String clientId);
    
    /**
     * Finds all jobs for a specific client with pagination.
     * 
     * @param clientId the client identifier
     * @param pageable pagination information
     * @return page of jobs for the specified client
     */
    Page<FileProcessingJob> findByClientId(String clientId, Pageable pageable);
    
    /**
     * Finds jobs for a specific client with the specified status.
     * 
     * @param clientId the client identifier
     * @param status the processing status
     * @return list of jobs for the client with the specified status
     */
    List<FileProcessingJob> findByClientIdAndStatus(String clientId, ProcessingStatus status);
    
    /**
     * Finds the most recent job for a specific client.
     * 
     * @param clientId the client identifier
     * @return the most recent job for the client, if any
     */
    Optional<FileProcessingJob> findTopByClientIdOrderByCreatedAtDesc(String clientId);
    
    /**
     * Finds all jobs for a specific client ordered by creation date (newest first).
     *
     * @param clientId the client identifier
     * @return list of jobs for the client ordered by creation date descending
     */
    List<FileProcessingJob> findByClientIdOrderByCreatedAtDesc(String clientId);
    
    // File Type Queries
    
    /**
     * Finds all jobs for a specific file type.
     * 
     * @param fileType the file type to filter by
     * @return list of jobs for the specified file type
     */
    List<FileProcessingJob> findByFileType(FileType fileType);
    
    /**
     * Finds jobs for a specific file type and status.
     * 
     * @param fileType the file type
     * @param status the processing status
     * @return list of jobs matching both criteria
     */
    List<FileProcessingJob> findByFileTypeAndStatus(FileType fileType, ProcessingStatus status);
    
    // Filename and Duplicate Detection
    
    /**
     * Finds jobs by original filename.
     * 
     * @param originalFilename the original filename
     * @return list of jobs with the specified filename
     */
    List<FileProcessingJob> findByOriginalFilename(String originalFilename);
    
    /**
     * Finds jobs by original filename for a specific client.
     * 
     * @param originalFilename the original filename
     * @param clientId the client identifier
     * @return list of jobs matching both criteria
     */
    List<FileProcessingJob> findByOriginalFilenameAndClientId(String originalFilename, String clientId);
    
    /**
     * Checks if a file with the same name has been processed for a client.
     * 
     * @param originalFilename the original filename
     * @param clientId the client identifier
     * @param excludeJobId job ID to exclude from the search (for updates)
     * @return true if duplicate exists, false otherwise
     */
    @Query("SELECT COUNT(j) > 0 FROM FileProcessingJob j WHERE j.originalFilename = :filename " +
           "AND j.clientId = :clientId AND j.id != :excludeJobId")
    boolean existsDuplicateFile(@Param("filename") String originalFilename, 
                               @Param("clientId") String clientId, 
                               @Param("excludeJobId") UUID excludeJobId);
    
    // Time-Based Queries
    
    /**
     * Finds jobs created within a date range.
     * 
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return list of jobs created within the date range
     */
    List<FileProcessingJob> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Finds jobs that started processing within a date range.
     * 
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return list of jobs that started processing within the date range
     */
    List<FileProcessingJob> findByProcessingStartedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Finds jobs that completed within a date range.
     * 
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return list of jobs that completed within the date range
     */
    List<FileProcessingJob> findByProcessingCompletedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Finds jobs that have been processing for longer than the specified duration.
     * 
     * @param cutoffTime the cutoff time (jobs started before this time)
     * @return list of potentially stuck jobs
     */
    @Query("SELECT j FROM FileProcessingJob j WHERE j.status = 'PROCESSING' " +
           "AND j.processingStartedAt < :cutoffTime")
    List<FileProcessingJob> findStuckJobs(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    // Retry Management
    
    /**
     * Finds jobs that are eligible for retry.
     * 
     * @return list of jobs that can be retried
     */
    @Query("SELECT j FROM FileProcessingJob j WHERE j.status = 'FAILED' " +
           "AND j.retryCount < j.maxRetries")
    List<FileProcessingJob> findJobsEligibleForRetry();
    
    /**
     * Finds jobs that have exceeded their retry limit.
     * 
     * @return list of jobs that have exhausted retries
     */
    @Query("SELECT j FROM FileProcessingJob j WHERE j.status = 'FAILED' " +
           "AND j.retryCount >= j.maxRetries")
    List<FileProcessingJob> findJobsExhaustedRetries();
    
    // Statistics and Monitoring
    
    /**
     * Counts jobs by status.
     * 
     * @param status the processing status
     * @return count of jobs with the specified status
     */
    long countByStatus(ProcessingStatus status);
    
    /**
     * Counts jobs by client and status.
     * 
     * @param clientId the client identifier
     * @param status the processing status
     * @return count of jobs for the client with the specified status
     */
    long countByClientIdAndStatus(String clientId, ProcessingStatus status);
    
    /**
     * Gets processing statistics for a date range.
     * 
     * @param startDate the start date
     * @param endDate the end date
     * @return list of processing statistics
     */
    @Query("SELECT j.status, COUNT(j), AVG(j.totalRecords), AVG(j.processedRecords) " +
           "FROM FileProcessingJob j WHERE j.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY j.status")
    List<Object[]> getProcessingStatistics(@Param("startDate") LocalDateTime startDate, 
                                          @Param("endDate") LocalDateTime endDate);
    
    /**
     * Gets client processing statistics.
     * 
     * @param clientId the client identifier
     * @param startDate the start date
     * @param endDate the end date
     * @return list of client statistics
     */
    @Query("SELECT j.status, COUNT(j), SUM(j.totalRecords), SUM(j.processedRecords), SUM(j.failedRecords) " +
           "FROM FileProcessingJob j WHERE j.clientId = :clientId " +
           "AND j.createdAt BETWEEN :startDate AND :endDate GROUP BY j.status")
    List<Object[]> getClientStatistics(@Param("clientId") String clientId,
                                      @Param("startDate") LocalDateTime startDate, 
                                      @Param("endDate") LocalDateTime endDate);
    
    /**
     * Gets average processing time for completed jobs.
     * 
     * @param startDate the start date
     * @param endDate the end date
     * @return average processing time in seconds
     */
    @Query("SELECT AVG(EXTRACT(EPOCH FROM j.processingCompletedAt) - EXTRACT(EPOCH FROM j.processingStartedAt)) " +
           "FROM FileProcessingJob j WHERE j.status = 'COMPLETED' " +
           "AND j.processingCompletedAt BETWEEN :startDate AND :endDate " +
           "AND j.processingStartedAt IS NOT NULL AND j.processingCompletedAt IS NOT NULL")
    Double getAverageProcessingTimeSeconds(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);
    
    /**
     * Gets success rate for a date range.
     * 
     * @param startDate the start date
     * @param endDate the end date
     * @return success rate as a percentage (0.0 to 100.0)
     */
    @Query("SELECT (COUNT(CASE WHEN j.status = 'COMPLETED' THEN 1 END) * 100.0 / COUNT(j)) " +
           "FROM FileProcessingJob j WHERE j.processingCompletedAt BETWEEN :startDate AND :endDate")
    Double getSuccessRate(@Param("startDate") LocalDateTime startDate, 
                         @Param("endDate") LocalDateTime endDate);
    
    // File Size Analysis
    
    /**
     * Gets total file size processed for a client.
     * 
     * @param clientId the client identifier
     * @param startDate the start date
     * @param endDate the end date
     * @return total file size in bytes
     */
    @Query("SELECT SUM(j.fileSizeBytes) FROM FileProcessingJob j " +
           "WHERE j.clientId = :clientId AND j.status = 'COMPLETED' " +
           "AND j.processingCompletedAt BETWEEN :startDate AND :endDate")
    Long getTotalProcessedFileSize(@Param("clientId") String clientId,
                                  @Param("startDate") LocalDateTime startDate, 
                                  @Param("endDate") LocalDateTime endDate);
    
    /**
     * Gets average file size for a file type.
     * 
     * @param fileType the file type
     * @param startDate the start date
     * @param endDate the end date
     * @return average file size in bytes
     */
    @Query("SELECT AVG(j.fileSizeBytes) FROM FileProcessingJob j " +
           "WHERE j.fileType = :fileType " +
           "AND j.createdAt BETWEEN :startDate AND :endDate")
    Double getAverageFileSize(@Param("fileType") FileType fileType,
                             @Param("startDate") LocalDateTime startDate, 
                             @Param("endDate") LocalDateTime endDate);
    
    // Cleanup and Maintenance
    
    /**
     * Finds old completed jobs for cleanup.
     * 
     * @param cutoffDate jobs completed before this date
     * @return list of old completed jobs
     */
    @Query("SELECT j FROM FileProcessingJob j WHERE j.status IN ('COMPLETED', 'FAILED', 'CANCELLED') " +
           "AND j.processingCompletedAt < :cutoffDate")
    List<FileProcessingJob> findOldCompletedJobs(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Deletes old completed jobs for cleanup.
     * 
     * @param cutoffDate jobs completed before this date
     * @return number of deleted jobs
     */
    @Query("DELETE FROM FileProcessingJob j WHERE j.status IN ('COMPLETED', 'FAILED', 'CANCELLED') " +
           "AND j.processingCompletedAt < :cutoffDate")
    int deleteOldCompletedJobs(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Get recent jobs
     * @Authors= Samy
     * @param pageable jobs completed before this date
     * @return page of jobs
     */
    Page<FileProcessingJob> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<FileProcessingJob> findAll(Specification<FileProcessingJob> spec, Pageable pageable);
}
