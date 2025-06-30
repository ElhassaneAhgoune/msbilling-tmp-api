package com.moneysab.cardexis.repository;

import com.moneysab.cardexis.domain.entity.Vss110SettlementRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Vss110SettlementRecord entity operations.
 * 
 * Provides data access methods for VSS-110 settlement record management,
 * including fee category analysis, settlement tracking, currency handling,
 * and financial reporting capabilities.
 * 
 * Key Features:
 * - Fee category analysis (Interchange, Processing, Chargeback, Total)
 * - Settlement date-based queries
 * - Currency-specific filtering
 * - Credit/Debit indicator analysis
 * - Destination identifier tracking
 * - Financial aggregation and reporting
 * 
 * @author EL.AHGOUNE
 * @version 1.0.0
 * @since 2024
 */
@Repository
public interface Vss110SettlementRecordRepository extends JpaRepository<Vss110SettlementRecord, UUID> {
    
    // Job Association Queries
    
    /**
     * Finds all VSS-110 records for a specific processing job.
     * 
     * @param jobId the processing job ID
     * @return list of VSS-110 records for the job
     */
    List<Vss110SettlementRecord> findByFileProcessingJobId(UUID jobId);
    
    /**
     * Finds VSS-110 records for a job with pagination.
     *
     * @param jobId the processing job ID
     * @param pageable pagination information
     * @return page of VSS-110 records for the job
     */
    Page<Vss110SettlementRecord> findByFileProcessingJobId(UUID jobId, Pageable pageable);
    
    /**
     * Counts VSS-110 records for a specific job.
     *
     * @param jobId the processing job ID
     * @return count of VSS-110 records for the job
     */
    long countByFileProcessingJobId(UUID jobId);
    
    /**
     * Finds VSS-110 records for jobs belonging to a specific client.
     * 
     * @param clientId the client identifier
     * @return list of VSS-110 records for the client
     */
    @Query("SELECT r FROM Vss110SettlementRecord r JOIN r.fileProcessingJob j WHERE j.clientId = :clientId")
    List<Vss110SettlementRecord> findByClientId(@Param("clientId") String clientId);
    
    // Destination Identifier Queries
    
    /**
     * Finds records by destination identifier.
     * 
     * @param destinationIdentifierthe destination identifier
     * @return list of records for the destination
     */
    List<Vss110SettlementRecord> findByDestinationId(String destinationId);
    
    /**
     * Finds records by destination identifier with pagination.
     *
     * @param destinationId the destination identifier
     * @param pageable pagination information
     * @return page of records for the destination
     */
    Page<Vss110SettlementRecord> findByDestinationId(String destinationId, Pageable pageable);
    
    /**
     * Finds unique destination identifiers for a client.
     * 
     * @param clientId the client identifier
     * @return list of unique destination identifiers
     */
    @Query("SELECT DISTINCT r.destinationId FROM Vss110SettlementRecord r JOIN r.fileProcessingJob j " +
           "WHERE j.clientId = :clientId ORDER BY r.destinationId")
    List<String> findDistinctDestinationIdentifiersByClientId(@Param("clientId") String clientId);
    
    /**
     * Counts records by destination identifier.
     *
     * @param destinationId the destination identifier
     * @return count of records for the destination
     */
    long countByDestinationId(String destinationId);
    
    // Settlement Date Queries
    
    /**
     * Finds records by settlement date.
     * 
     * @param settlementDate the settlement date
     * @return list of records for the settlement date
     */
    List<Vss110SettlementRecord> findBySettlementDate(LocalDate settlementDate);
    
    /**
     * Finds records within a settlement date range.
     * 
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return list of records within the date range
     */
    List<Vss110SettlementRecord> findBySettlementDateBetween(LocalDate startDate, LocalDate endDate);
    
    /**
     * Finds records for a specific settlement date and destination.
     * 
     * @param settlementDate the settlement date
     * @param destinationIdentifierthe destination identifier
     * @return list of records matching both criteria
     */
    List<Vss110SettlementRecord> findBySettlementDateAndDestinationId(LocalDate settlementDate,
                                                                      String destinationId);
    
    /**
     * Finds the latest settlement date for a destination.
     *
     * @param destinationId the destination identifier
     * @return the latest settlement date, if any
     */
    @Query("SELECT MAX(r.settlementDate) FROM Vss110SettlementRecord r " +
           "WHERE r.destinationId = :destinationId")
    Optional<LocalDate> findLatestSettlementDateByDestination(@Param("destinationId") String destinationId);
    
    /**
     * Finds settlement dates for a client within a range.
     * 
     * @param clientId the client identifier
     * @param startDate the start date
     * @param endDate the end date
     * @return list of distinct settlement dates
     */
    @Query("SELECT DISTINCT r.settlementDate FROM Vss110SettlementRecord r JOIN r.fileProcessingJob j " +
           "WHERE j.clientId = :clientId " +
           "AND r.settlementDate BETWEEN :startDate AND :endDate " +
           "ORDER BY r.settlementDate")
    List<LocalDate> findSettlementDatesByClientId(@Param("clientId") String clientId,
                                                  @Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);
    
    // Fee Category Queries
    
    /**
     * Finds records by fee category.
     * 
     * @param feeCategory the fee category (I=Interchange, P=Processing, C=Chargeback, T=Total)
     * @return list of records for the fee category
     */
    List<Vss110SettlementRecord> findByAmountType(String amountType);
    
    /**
     * Finds interchange fee records.
     *
     * @return list of interchange fee records
     */
    @Query("SELECT r FROM Vss110SettlementRecord r WHERE r.amountType = 'I'")
    List<Vss110SettlementRecord> findInterchangeFeeRecords();
    
    /**
     * Finds processing fee records.
     *
     * @return list of processing fee records
     */
    @Query("SELECT r FROM Vss110SettlementRecord r WHERE r.amountType = 'F'")
    List<Vss110SettlementRecord> findProcessingFeeRecords();
    
    /**
     * Finds chargeback fee records.
     *
     * @return list of chargeback fee records
     */
    @Query("SELECT r FROM Vss110SettlementRecord r WHERE r.amountType = 'C'")
    List<Vss110SettlementRecord> findChargebackFeeRecords();
    
    /**
     * Finds total fee records.
     *
     * @return list of total fee records
     */
    @Query("SELECT r FROM Vss110SettlementRecord r WHERE r.amountType = 'T'")
    List<Vss110SettlementRecord> findTotalFeeRecords();
    
    /**
     * Finds fee category breakdown for a settlement date.
     * 
     * @param settlementDate the settlement date
     * @param destinationIdentifierthe destination identifier
     * @return list of records grouped by fee category
     */
    @Query("SELECT r FROM Vss110SettlementRecord r " +
           "WHERE r.settlementDate = :settlementDate " +
           "AND r.destinationId = :destinationId " +
           "ORDER BY r.amountType")
    List<Vss110SettlementRecord> findFeeCategoryBreakdown(@Param("settlementDate") LocalDate settlementDate,
                                                          @Param("destinationId") String destinationId);
    
    // Currency Code Queries
    
    /**
     * Finds records by currency code.
     * 
     * @param currencyCode the currency code (e.g., USD, EUR)
     * @return list of records for the currency
     */
    List<Vss110SettlementRecord> findByCurrencyCode(String currencyCode);
    
    /**
     * Finds unique currency codes for a client.
     * 
     * @param clientId the client identifier
     * @return list of unique currency codes
     */
    @Query("SELECT DISTINCT r.currencyCode FROM Vss110SettlementRecord r JOIN r.fileProcessingJob j " +
           "WHERE j.clientId = :clientId ORDER BY r.currencyCode")
    List<String> findDistinctCurrencyCodesByClientId(@Param("clientId") String clientId);
    
    /**
     * Finds records by currency and settlement date.
     * 
     * @param currencyCode the currency code
     * @param settlementDate the settlement date
     * @return list of records matching both criteria
     */
    List<Vss110SettlementRecord> findByCurrencyCodeAndSettlementDate(String currencyCode, 
                                                                     LocalDate settlementDate);
    
    // Credit/Debit Indicator Queries
    
    /**
     * Finds records by credit/debit indicator.
     * 
     * @param creditDebitIndicator the indicator (C=Credit, D=Debit)
     * @return list of records for the indicator
     */
    /**
     * Finds credit records (positive net amounts).
     *
     * @return list of credit records
     */
    @Query("SELECT r FROM Vss110SettlementRecord r WHERE r.netAmount > 0")
    List<Vss110SettlementRecord> findCreditRecords();
    
    /**
     * Finds debit records (negative net amounts).
     *
     * @return list of debit records
     */
    @Query("SELECT r FROM Vss110SettlementRecord r WHERE r.netAmount < 0")
    List<Vss110SettlementRecord> findDebitRecords();
    
    /**
     * Finds credit/debit breakdown for a settlement date and destination.
     * 
     * @param settlementDate the settlement date
     * @param destinationIdentifierthe destination identifier
     * @return list of records grouped by credit/debit indicator
     */
    @Query("SELECT r FROM Vss110SettlementRecord r " +
           "WHERE r.settlementDate = :settlementDate " +
           "AND r.destinationId = :destinationId " +
           "ORDER BY r.netAmount, r.amountType")
    List<Vss110SettlementRecord> findCreditDebitBreakdown(@Param("settlementDate") LocalDate settlementDate,
                                                          @Param("destinationId") String destinationId);
    
    // Amount-Based Queries
    
    /**
     * Finds records with amounts greater than a threshold.
     * 
     * @param threshold the amount threshold
     * @return list of records with amounts above the threshold
     */
    List<Vss110SettlementRecord> findByNetAmountGreaterThan(BigDecimal threshold);
    
    /**
     * Finds records with amounts within a range.
     *
     * @param minAmount the minimum amount (inclusive)
     * @param maxAmount the maximum amount (inclusive)
     * @return list of records within the amount range
     */
    List<Vss110SettlementRecord> findByNetAmountBetween(BigDecimal minAmount, BigDecimal maxAmount);
    
    /**
     * Finds records with zero amounts.
     *
     * @return list of records with zero amounts
     */
    @Query("SELECT r FROM Vss110SettlementRecord r WHERE r.netAmount = 0")
    List<Vss110SettlementRecord> findZeroAmountRecords();
    
    /**
     * Finds the largest amount for a settlement date.
     *
     * @param settlementDate the settlement date
     * @return the largest amount, if any
     */
    @Query("SELECT MAX(r.netAmount) FROM Vss110SettlementRecord r " +
           "WHERE r.settlementDate = :settlementDate")
    Optional<BigDecimal> findMaxAmountBySettlementDate(@Param("settlementDate") LocalDate settlementDate);
    
    // Transaction Count Queries
    
    /**
     * Finds records with transaction counts greater than a threshold.
     * 
     * @param threshold the transaction count threshold
     * @return list of records with transaction counts above the threshold
     */
    List<Vss110SettlementRecord> findByTransactionCountGreaterThan(Long threshold);
    
    /**
     * Finds the total transaction count for a settlement date.
     * 
     * @param settlementDate the settlement date
     * @return the total transaction count
     */
    @Query("SELECT SUM(r.transactionCount) FROM Vss110SettlementRecord r " +
           "WHERE r.settlementDate = :settlementDate")
    Optional<Long> findTotalTransactionCountBySettlementDate(@Param("settlementDate") LocalDate settlementDate);
    
    /**
     * Finds records with zero transaction counts.
     * 
     * @return list of records with zero transaction counts
     */
    @Query("SELECT r FROM Vss110SettlementRecord r WHERE r.transactionCount = 0")
    List<Vss110SettlementRecord> findZeroTransactionCountRecords();
    
    // Validation and Data Quality
    
    /**
     * Finds records with validation errors.
     * 
     * @return list of records that failed validation
     */
    List<Vss110SettlementRecord> findByIsValidFalse();
    
    /**
     * Finds records with validation errors for a specific job.
     * 
     * @param jobId the processing job ID
     * @return list of invalid records for the job
     */
    List<Vss110SettlementRecord> findByFileProcessingJobIdAndIsValidFalse(UUID jobId);
    
    /**
     * Finds records with specific validation errors.
     * 
     * @param errorPattern the error pattern to search for
     * @return list of records with matching validation errors
     */
    @Query("SELECT r FROM Vss110SettlementRecord r " +
           "WHERE r.validationErrors LIKE %:errorPattern%")
    List<Vss110SettlementRecord> findByValidationErrorsContaining(@Param("errorPattern") String errorPattern);
    
    /**
     * Counts records with validation errors.
     * 
     * @return count of records with validation errors
     */
    long countByIsValidFalse();
    
    /**
     * Gets validation success rate for a job.
     * 
     * @param jobId the processing job ID
     * @return validation success rate as a percentage
     */
    @Query("SELECT (COUNT(CASE WHEN r.isValid = true THEN 1 END) * 100.0 / COUNT(r)) " +
           "FROM Vss110SettlementRecord r WHERE r.fileProcessingJob.id = :jobId")
    Double getValidationSuccessRateByJob(@Param("jobId") UUID jobId);
    
    // Financial Aggregation and Reporting
    
    /**
     * Calculates total settlement amount by fee category for a date range.
     * 
     * @param startDate the start date
     * @param endDate the end date
     * @param destinationIdentifier the destination identifier
     * @return list of fee category totals
     */
    @Query("SELECT r.amountType, r.currencyCode, " +
           "SUM(r.netAmount), " +
           "SUM(r.transactionCount) " +
           "FROM Vss110SettlementRecord r " +
           "WHERE r.settlementDate BETWEEN :startDate AND :endDate " +
           "AND r.destinationId = :destinationId " +
           "GROUP BY r.amountType, r.currencyCode " +
           "ORDER BY r.amountType, r.currencyCode")
    List<Object[]> calculateFeeCategoryTotals(@Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate,
                                             @Param("destinationId") String destinationId);
    
    /**
     * Calculates net settlement amount for a destination and date range.
     * 
     * @param destinationIdentifier the destination identifier
     * @param startDate the start date
     * @param endDate the end date
     * @param currencyCode the currency code
     * @return net settlement amount (credits minus debits)
     */
    @Query("SELECT SUM(r.netAmount) " +
           "FROM Vss110SettlementRecord r " +
           "WHERE r.destinationId = :destinationId " +
           "AND r.settlementDate BETWEEN :startDate AND :endDate " +
           "AND r.currencyCode = :currencyCode")
    Optional<BigDecimal> calculateNetSettlementAmount(@Param("destinationId") String destinationId,
                                                     @Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate,
                                                     @Param("currencyCode") String currencyCode);
    
    /**
     * Gets daily settlement summary for a destination.
     * 
     * @param destinationIdentifier the destination identifier
     * @param startDate the start date
     * @param endDate the end date
     * @return list of daily settlement summaries
     */
    @Query("SELECT r.settlementDate, r.currencyCode, " +
           "SUM(r.creditAmount), " +
           "SUM(r.debitAmount), " +
           "SUM(r.netAmount), " +
           "SUM(r.transactionCount) " +
           "FROM Vss110SettlementRecord r " +
           "WHERE r.destinationId = :destinationId " +
           "AND r.settlementDate BETWEEN :startDate AND :endDate " +
           "GROUP BY r.settlementDate, r.currencyCode " +
           "ORDER BY r.settlementDate, r.currencyCode")
    List<Object[]> getDailySettlementSummary(@Param("destinationId") String destinationId,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);
    
    /**
     * Gets client settlement statistics.
     * 
     * @param clientId the client identifier
     * @param startDate the start date
     * @param endDate the end date
     * @return list of client settlement statistics
     */
    @Query("SELECT r.destinationId, r.currencyCode, r.amountType, " +
           "COUNT(r), SUM(r.netAmount), SUM(r.transactionCount), " +
           "AVG(r.netAmount), MIN(r.settlementDate), MAX(r.settlementDate) " +
           "FROM Vss110SettlementRecord r JOIN r.fileProcessingJob j " +
           "WHERE j.clientId = :clientId " +
           "AND r.settlementDate BETWEEN :startDate AND :endDate " +
           "GROUP BY r.destinationId, r.currencyCode, r.amountType " +
           "ORDER BY r.destinationId, r.currencyCode, r.amountType")
    List<Object[]> getClientSettlementStatistics(@Param("clientId") String clientId,
                                                @Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);
    
    // Raw Record Data Queries
    
    /**
     * Finds records by raw record line pattern.
     * 
     * @param recordPattern the record pattern to search for
     * @return list of records with matching raw data
     */
    @Query("SELECT r FROM Vss110SettlementRecord r " +
           "WHERE r.rawRecordLine LIKE %:recordPattern%")
    List<Vss110SettlementRecord> findByRawRecordLineContaining(@Param("recordPattern") String recordPattern);
    
    /**
     * Finds records with specific raw record data.
     * 
     * @param rawRecordLine the exact raw record line
     * @return list of records with the specified raw data
     */
    List<Vss110SettlementRecord> findByRawRecordLine(String rawRecordLine);
    
    // Cleanup Operations
    
    /**
     * Finds old records for cleanup.
     * 
     * @param cutoffDate records older than this date
     * @return list of old records
     */
    @Query("SELECT r FROM Vss110SettlementRecord r JOIN r.fileProcessingJob j " +
           "WHERE j.status IN ('COMPLETED', 'FAILED', 'CANCELLED') " +
           "AND r.settlementDate < :cutoffDate")
    List<Vss110SettlementRecord> findOldRecords(@Param("cutoffDate") LocalDate cutoffDate);
    
    /**
     * Deletes old records for cleanup.
     * 
     * @param cutoffDate records older than this date
     * @return number of deleted records
     */
    @Query("DELETE FROM Vss110SettlementRecord r WHERE r.id IN " +
           "(SELECT r2.id FROM Vss110SettlementRecord r2 JOIN r2.fileProcessingJob j " +
           "WHERE j.status IN ('COMPLETED', 'FAILED', 'CANCELLED') " +
           "AND r2.settlementDate < :cutoffDate)")
    int deleteOldRecords(@Param("cutoffDate") LocalDate cutoffDate);
    
    /**
     * Deletes all VSS-110 records associated with a specific file processing job.
     *
     * @param fileProcessingJob the file processing job
     * @return number of deleted records
     */
    int deleteByFileProcessingJob(com.moneysab.cardexis.domain.entity.FileProcessingJob fileProcessingJob);

    /**
     * Get Kpis stats
     * @Auhtors =Samy
     * @param spec  start date endDate currencyCode for report
     *
     */
    List<Vss110SettlementRecord> findAll(Specification spec);
}