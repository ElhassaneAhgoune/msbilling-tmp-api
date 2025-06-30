package com.moneysab.cardexis.repository;

import com.moneysab.cardexis.domain.entity.EpinFileHeader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for EpinFileHeader entity operations.
 * 
 * Provides data access methods for EPIN file header management,
 * including sequence validation, routing verification, and
 * client-specific header tracking.
 * 
 * Key Features:
 * - Header sequence validation
 * - Routing number verification
 * - Client-specific header queries
 * - Timestamp-based filtering
 * - Duplicate detection
 * 
 * @author EL.AHGOUNE
 * @version 1.0.0
 * @since 2024
 */
@Repository
public interface EpinFileHeaderRepository extends JpaRepository<EpinFileHeader, UUID> {
    
    // Job Association Queries
    
    /**
     * Finds the header associated with a specific processing job.
     * 
     * @param jobId the processing job ID
     * @return the header for the specified job, if any
     */
    Optional<EpinFileHeader> findByFileProcessingJobId(UUID jobId);
    
    /**
     * Finds all headers for jobs belonging to a specific client.
     * 
     * @param clientId the client identifier
     * @return list of headers for the specified client
     */
    @Query("SELECT h FROM EpinFileHeader h JOIN h.fileProcessingJob j WHERE j.clientId = :clientId")
    List<EpinFileHeader> findByClientId(@Param("clientId") String clientId);
    
    /**
     * Finds headers for a specific client with pagination.
     *
     * @param clientId the client identifier
     * @param pageable pagination information
     * @return page of headers for the specified client
     */
    @Query("SELECT h FROM EpinFileHeader h JOIN h.fileProcessingJob j WHERE j.clientId = :clientId")
    Page<EpinFileHeader> findByClientId(@Param("clientId") String clientId, Pageable pageable);
    
    // Routing Number Queries
    
    /**
     * Finds all headers with a specific routing number.
     * 
     * @param routingNumber the routing number
     * @return list of headers with the specified routing number
     */
    List<EpinFileHeader> findByRoutingNumber(String routingNumber);
    
    /**
     * Finds headers with a specific routing number for a client.
     * 
     * @param routingNumber the routing number
     * @param clientId the client identifier
     * @return list of headers matching both criteria
     */
    @Query("SELECT h FROM EpinFileHeader h JOIN h.fileProcessingJob j " +
           "WHERE h.routingNumber = :routingNumber AND j.clientId = :clientId")
    List<EpinFileHeader> findByRoutingNumberAndClientId(@Param("routingNumber") String routingNumber,
                                                       @Param("clientId") String clientId);
    
    /**
     * Finds the most recent header for a specific routing number.
     * 
     * @param routingNumber the routing number
     * @return the most recent header for the routing number, if any
     */
    Optional<EpinFileHeader> findTopByRoutingNumberOrderByFileTimestampDesc(String routingNumber);
    
    /**
     * Checks if a routing number is valid (exists in processed files).
     * 
     * @param routingNumber the routing number to validate
     * @return true if the routing number exists, false otherwise
     */
    boolean existsByRoutingNumber(String routingNumber);
    
    // Sequence Validation
    
    /**
     * Finds headers with a specific sequence number.
     * 
     * @param sequenceNumber the sequence number
     * @return list of headers with the specified sequence number
     */
    List<EpinFileHeader> findBySequenceNumber(String sequenceNumber);
    
    /**
     * Finds the next expected sequence number for a routing number.
     * 
     * @param routingNumber the routing number
     * @return the highest sequence number for the routing number
     */
    @Query("SELECT MAX(h.sequenceNumber) FROM EpinFileHeader h " +
           "WHERE h.routingNumber = :routingNumber")
    Optional<String> findMaxSequenceByRoutingNumber(@Param("routingNumber") String routingNumber);
    
    /**
     * Finds headers with sequence gaps for a routing number.
     * 
     * @param routingNumber the routing number
     * @return list of sequence numbers with gaps
     */
    @Query("SELECT h1.sequenceNumber FROM EpinFileHeader h1 " +
           "WHERE h1.routingNumber = :routingNumber " +
           "AND NOT EXISTS (SELECT h2 FROM EpinFileHeader h2 " +
           "WHERE h2.routingNumber = :routingNumber " +
           "AND CAST(h2.sequenceNumber AS INTEGER) = CAST(h1.sequenceNumber AS INTEGER) + 1)")
    List<String> findSequenceGaps(@Param("routingNumber") String routingNumber);
    
    /**
     * Validates sequence continuity for a routing number.
     * 
     * @param routingNumber the routing number
     * @param sequenceNumber the sequence number to validate
     * @return true if the sequence is valid (next in order), false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(h) = 0 THEN true " +
           "WHEN MAX(CAST(h.sequenceNumber AS INTEGER)) + 1 = CAST(:sequenceNumber AS INTEGER) THEN true " +
           "ELSE false END " +
           "FROM EpinFileHeader h WHERE h.routingNumber = :routingNumber")
    boolean isValidSequence(@Param("routingNumber") String routingNumber, 
                           @Param("sequenceNumber") String sequenceNumber);
    
    /**
     * Checks for duplicate sequence numbers within a routing number.
     * 
     * @param routingNumber the routing number
     * @param sequenceNumber the sequence number
     * @param excludeHeaderId header ID to exclude from the check (for updates)
     * @return true if duplicate exists, false otherwise
     */
    @Query("SELECT COUNT(h) > 0 FROM EpinFileHeader h " +
           "WHERE h.routingNumber = :routingNumber " +
           "AND h.sequenceNumber = :sequenceNumber " +
           "AND h.id != :excludeHeaderId")
    boolean existsDuplicateSequence(@Param("routingNumber") String routingNumber,
                                   @Param("sequenceNumber") String sequenceNumber,
                                   @Param("excludeHeaderId") UUID excludeHeaderId);
    
    // File Sequence Queries
    
    /**
     * Finds headers with a specific file sequence.
     * 
     * @param fileSequence the file sequence
     * @return list of headers with the specified file sequence
     */
    List<EpinFileHeader> findByFileSequence(String fileSequence);
    
    /**
     * Finds the maximum file sequence for a client.
     * 
     * @param clientId the client identifier
     * @return the highest file sequence for the client
     */
    @Query("SELECT MAX(h.fileSequence) FROM EpinFileHeader h JOIN h.fileProcessingJob j " +
           "WHERE j.clientId = :clientId")
    Optional<String> findMaxFileSequenceByClientId(@Param("clientId") String clientId);
    
    /**
     * Validates file sequence for a client.
     *
     * @param clientId the client identifier
     * @param fileSequence the file sequence to validate
     * @return true if the file sequence is valid, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(h) = 0 THEN true " +
           "WHEN MAX(CAST(h.fileSequence AS INTEGER)) + 1 = CAST(:fileSequence AS INTEGER) THEN true " +
           "ELSE false END " +
           "FROM EpinFileHeader h JOIN h.fileProcessingJob j WHERE j.clientId = :clientId")
    boolean isValidFileSequence(@Param("clientId") String clientId,
                               @Param("fileSequence") String fileSequence);
    
    // Timestamp-Based Queries
    
    /**
     * Finds headers within a timestamp range.
     * 
     * @param startTime the start timestamp
     * @param endTime the end timestamp
     * @return list of headers within the timestamp range
     */
    List<EpinFileHeader> findByFileTimestampBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * Finds headers for a specific date.
     * 
     * @param date the date to filter by
     * @return list of headers for the specified date
     */
    @Query("SELECT h FROM EpinFileHeader h " +
           "WHERE DATE(h.fileTimestamp) = DATE(:date)")
    List<EpinFileHeader> findByFileDate(@Param("date") LocalDateTime date);
    
    /**
     * Finds the most recent header for a client.
     * 
     * @param clientId the client identifier
     * @return the most recent header for the client, if any
     */
    @Query("SELECT h FROM EpinFileHeader h JOIN h.fileProcessingJob j " +
           "WHERE j.clientId = :clientId " +
           "ORDER BY h.fileTimestamp DESC LIMIT 1")
    Optional<EpinFileHeader> findMostRecentByClientId(@Param("clientId") String clientId);
    
    /**
     * Finds headers older than a specified timestamp.
     * 
     * @param cutoffTime the cutoff timestamp
     * @return list of headers older than the cutoff time
     */
    List<EpinFileHeader> findByFileTimestampBefore(LocalDateTime cutoffTime);
    
    // Validation Status Queries
    
    /**
     * Finds all headers with validation errors.
     * 
     * @return list of headers that failed validation
     */
    List<EpinFileHeader> findByIsValidFalse();
    
    /**
     * Finds headers with validation errors for a specific client.
     * 
     * @param clientId the client identifier
     * @return list of invalid headers for the client
     */
    @Query("SELECT h FROM EpinFileHeader h JOIN h.fileProcessingJob j " +
           "WHERE h.isValid = false AND j.clientId = :clientId")
    List<EpinFileHeader> findInvalidByClientId(@Param("clientId") String clientId);
    
    /**
     * Finds headers with specific validation errors.
     * 
     * @param errorPattern the error pattern to search for
     * @return list of headers with matching validation errors
     */
    @Query("SELECT h FROM EpinFileHeader h " +
           "WHERE h.validationErrors LIKE %:errorPattern%")
    List<EpinFileHeader> findByValidationErrorsContaining(@Param("errorPattern") String errorPattern);
    
    /**
     * Counts headers with validation errors.
     * 
     * @return count of headers with validation errors
     */
    long countByIsValidFalse();
    
    /**
     * Counts valid headers for a client.
     * 
     * @param clientId the client identifier
     * @return count of valid headers for the client
     */
    @Query("SELECT COUNT(h) FROM EpinFileHeader h JOIN h.fileProcessingJob j " +
           "WHERE h.isValid = true AND j.clientId = :clientId")
    long countValidByClientId(@Param("clientId") String clientId);
    
    // Raw Header Data Queries
    
    /**
     * Finds headers by raw header data pattern.
     * 
     * @param headerPattern the header pattern to search for
     * @return list of headers with matching raw data
     */
    @Query("SELECT h FROM EpinFileHeader h " +
           "WHERE h.rawHeaderLine LIKE %:headerPattern%")
    List<EpinFileHeader> findByRawHeaderDataContaining(@Param("headerPattern") String headerPattern);
    
    /**
     * Finds headers with specific raw header data.
     *
     * @param rawHeaderData the exact raw header data
     * @return list of headers with the specified raw data
     */
    List<EpinFileHeader> findByRawHeaderLine(String rawHeaderData);
    
    /**
     * Checks for duplicate raw header data.
     *
     * @param rawHeaderData the raw header data
     * @param excludeHeaderId header ID to exclude from the check
     * @return true if duplicate exists, false otherwise
     */
    @Query("SELECT COUNT(h) > 0 FROM EpinFileHeader h " +
           "WHERE h.rawHeaderLine = :rawHeaderData " +
           "AND h.id != :excludeHeaderId")
    boolean existsDuplicateRawHeader(@Param("rawHeaderData") String rawHeaderData,
                                    @Param("excludeHeaderId") UUID excludeHeaderId);
    
    // Statistics and Analysis
    
    /**
     * Gets header statistics for a date range.
     * 
     * @param startDate the start date
     * @param endDate the end date
     * @return list of header statistics
     */
    @Query("SELECT h.routingNumber, COUNT(h), " +
           "SUM(CASE WHEN h.isValid = true THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN h.isValid = false THEN 1 ELSE 0 END) " +
           "FROM EpinFileHeader h " +
           "WHERE h.fileTimestamp BETWEEN :startDate AND :endDate " +
           "GROUP BY h.routingNumber")
    List<Object[]> getHeaderStatistics(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);
    
    /**
     * Gets client header statistics.
     * 
     * @param clientId the client identifier
     * @param startDate the start date
     * @param endDate the end date
     * @return list of client header statistics
     */
    @Query("SELECT h.routingNumber, COUNT(h), " +
           "MIN(h.fileTimestamp), MAX(h.fileTimestamp), " +
           "SUM(CASE WHEN h.isValid = true THEN 1 ELSE 0 END) " +
           "FROM EpinFileHeader h JOIN h.fileProcessingJob j " +
           "WHERE j.clientId = :clientId " +
           "AND h.fileTimestamp BETWEEN :startDate AND :endDate " +
           "GROUP BY h.routingNumber")
    List<Object[]> getClientHeaderStatistics(@Param("clientId") String clientId,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);
    
    /**
     * Gets validation success rate.
     * 
     * @param startDate the start date
     * @param endDate the end date
     * @return validation success rate as a percentage
     */
    @Query("SELECT (COUNT(CASE WHEN h.isValid = true THEN 1 END) * 100.0 / COUNT(h)) " +
           "FROM EpinFileHeader h " +
           "WHERE h.fileTimestamp BETWEEN :startDate AND :endDate")
    Double getValidationSuccessRate(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);
    
    // Cleanup Operations
    
    /**
     * Finds old headers for cleanup.
     * 
     * @param cutoffDate headers older than this date
     * @return list of old headers
     */
    @Query("SELECT h FROM EpinFileHeader h JOIN h.fileProcessingJob j " +
           "WHERE j.status IN ('COMPLETED', 'FAILED', 'CANCELLED') " +
           "AND h.fileTimestamp < :cutoffDate")
    List<EpinFileHeader> findOldHeaders(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Deletes old headers for cleanup.
     *
     * @param cutoffDate headers older than this date
     * @return number of deleted headers
     */
    @Query("DELETE FROM EpinFileHeader h WHERE h.id IN " +
           "(SELECT h2.id FROM EpinFileHeader h2 JOIN h2.fileProcessingJob j " +
           "WHERE j.status IN ('COMPLETED', 'FAILED', 'CANCELLED') " +
           "AND h2.fileTimestamp < :cutoffDate)")
    int deleteOldHeaders(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Deletes all headers associated with a specific file processing job.
     *
     * @param fileProcessingJob the file processing job
     * @return number of deleted headers
     */
    int deleteByFileProcessingJob(com.moneysab.cardexis.domain.entity.FileProcessingJob fileProcessingJob);
}