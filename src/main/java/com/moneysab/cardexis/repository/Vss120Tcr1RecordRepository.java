package com.moneysab.cardexis.repository;

import com.moneysab.cardexis.domain.entity.FileProcessingJob;
import com.moneysab.cardexis.domain.entity.Vss120SettlementRecord;
import com.moneysab.cardexis.domain.entity.Vss120Tcr1Record;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Repository for VSS-120 TCR1 records.
 *
 * @author EL.AHGOUNE
 * @version 1.0.0
 * @since 2024
 */
@Repository
public interface Vss120Tcr1RecordRepository extends JpaRepository<Vss120Tcr1Record, UUID> {

    /**
     * Find all TCR1 records for a specific file processing job.
     *
     * @param fileProcessingJob the file processing job
     * @return list of TCR1 records
     */
    List<Vss120Tcr1Record> findByFileProcessingJob(FileProcessingJob fileProcessingJob);

    /**
     * Find all TCR1 records for a specific file processing job with pagination.
     *
     * @param fileProcessingJob the file processing job
     * @param pageable pagination information
     * @return page of TCR1 records
     */
    Page<Vss120Tcr1Record> findByFileProcessingJob(FileProcessingJob fileProcessingJob, Pageable pageable);

    /**
     * Find TCR1 records by parent VSS-120 record.
     *
     * @param parentRecord the parent VSS-120 record
     * @return list of associated TCR1 records
     */
    List<Vss120Tcr1Record> findByParentVssRecord(Vss120SettlementRecord parentRecord);

    /**
     * Find TCR1 records by destination ID.
     *
     * @param destinationId the destination identifier
     * @return list of TCR1 records
     */
    List<Vss120Tcr1Record> findByDestinationId(String destinationId);

    /**
     * Find TCR1 records by rate table ID.
     *
     * @param rateTableId the rate table identifier
     * @return list of TCR1 records
     */
    List<Vss120Tcr1Record> findByRateTableId(String rateTableId);

    /**
     * Find valid TCR1 records for a specific file processing job.
     *
     * @param fileProcessingJob the file processing job
     * @return list of valid TCR1 records
     */
    List<Vss120Tcr1Record> findByFileProcessingJobAndIsValidTrue(FileProcessingJob fileProcessingJob);

    /**
     * Find invalid TCR1 records for a specific file processing job.
     *
     * @param fileProcessingJob the file processing job
     * @return list of invalid TCR1 records
     */
    List<Vss120Tcr1Record> findByFileProcessingJobAndIsValidFalse(FileProcessingJob fileProcessingJob);

    /**
     * Count TCR1 records by file processing job and validity.
     *
     * @param fileProcessingJob the file processing job
     * @param isValid validity flag
     * @return count of records
     */
    long countByFileProcessingJobAndIsValid(FileProcessingJob fileProcessingJob, Boolean isValid);

    /**
     * Find TCR1 records with non-zero first count.
     *
     * @param fileProcessingJob the file processing job
     * @return list of TCR1 records with first count > 0
     */
    @Query("SELECT r FROM Vss120Tcr1Record r " +
            "WHERE r.fileProcessingJob = :job " +
            "AND r.rateTableId IS NOT NULL " +
            "GROUP BY r.rateTableId " +
            "ORDER BY r.rateTableId")
    List<Object[]> getSummaryByRateTableId(@Param("job") FileProcessingJob fileProcessingJob);

    /**
     * Calculate total amounts by sign for a specific job.
     *
     * @param fileProcessingJob the file processing job
     * @return array of totals [firstAmount, secondAmount, thirdAmount, fourthAmount, fifthAmount, sixthAmount]
     */
    @Query("SELECT " +
            "SUM(CASE WHEN r.firstAmount IS NOT NULL THEN r.firstAmount ELSE 0 END), " +
            "SUM(CASE WHEN r.secondAmount IS NOT NULL THEN r.secondAmount ELSE 0 END), " +
            "SUM(CASE WHEN r.thirdAmount IS NOT NULL THEN r.thirdAmount ELSE 0 END), " +
            "SUM(CASE WHEN r.fourthAmount IS NOT NULL THEN r.fourthAmount ELSE 0 END), " +
            "SUM(CASE WHEN r.fifthAmount IS NOT NULL THEN r.fifthAmount ELSE 0 END), " +
            "SUM(CASE WHEN r.sixthAmount IS NOT NULL THEN r.sixthAmount ELSE 0 END) " +
            "FROM Vss120Tcr1Record r " +
            "WHERE r.fileProcessingJob = :job " +
            "AND r.isValid = true")
    Object[] calculateTotalAmounts(@Param("job") FileProcessingJob fileProcessingJob);

    /**
     * Calculate total counts for a specific job.
     *
     * @param fileProcessingJob the file processing job
     * @return array of totals [firstCount, secondCount]
     */
    @Query("SELECT " +
            "SUM(CASE WHEN r.firstCount IS NOT NULL THEN r.firstCount ELSE 0 END), " +
            "SUM(CASE WHEN r.secondCount IS NOT NULL THEN r.secondCount ELSE 0 END) " +
            "FROM Vss120Tcr1Record r " +
            "WHERE r.fileProcessingJob = :job " +
            "AND r.isValid = true")
    Object[] calculateTotalCounts(@Param("job") FileProcessingJob fileProcessingJob);

    /**
     * Find TCR1 records by parent record and rate table ID.
     *
     * @param parentRecord the parent VSS-120 record
     * @param rateTableId the rate table identifier
     * @return list of TCR1 records
     */
    List<Vss120Tcr1Record> findByParentVssRecordAndRateTableId(
            Vss120SettlementRecord parentRecord, String rateTableId);

    /**
     * Find TCR1 records with specific amount ranges.
     *
     * @param fileProcessingJob the file processing job
     * @param minAmount minimum amount threshold
     * @param maxAmount maximum amount threshold
     * @return list of TCR1 records within amount range
     */
    @Query("SELECT r FROM Vss120Tcr1Record r " +
            "WHERE r.fileProcessingJob = :job " +
            "AND r.isValid = true " +
            "AND (r.firstAmount BETWEEN :minAmount AND :maxAmount " +
            "OR r.secondAmount BETWEEN :minAmount AND :maxAmount " +
            "OR r.thirdAmount BETWEEN :minAmount AND :maxAmount " +
            "OR r.fourthAmount BETWEEN :minAmount AND :maxAmount " +
            "OR r.fifthAmount BETWEEN :minAmount AND :maxAmount " +
            "OR r.sixthAmount BETWEEN :minAmount AND :maxAmount) " +
            "ORDER BY r.destinationId, r.lineNumber")
    List<Vss120Tcr1Record> findRecordsWithAmountInRange(
            @Param("job") FileProcessingJob fileProcessingJob,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount);

    /**
     * Find TCR1 records with credit amounts (CR sign).
     *
     * @param fileProcessingJob the file processing job
     * @return list of TCR1 records with credit amounts
     */
    @Query("SELECT r FROM Vss120Tcr1Record r " +
            "WHERE r.fileProcessingJob = :job " +
            "AND r.isValid = true " +
            "AND (r.firstAmountSign = 'CR' " +
            "OR r.secondAmountSign = 'CR' " +
            "OR r.thirdAmountSign = 'CR' " +
            "OR r.fourthAmountSign = 'CR' " +
            "OR r.fifthAmountSign = 'CR' " +
            "OR r.sixthAmountSign = 'CR') " +
            "ORDER BY r.destinationId, r.lineNumber")
    List<Vss120Tcr1Record> findRecordsWithCreditAmounts(@Param("job") FileProcessingJob fileProcessingJob);

    /**
     * Find TCR1 records with debit amounts (DB sign).
     *
     * @param fileProcessingJob the file processing job
     * @return list of TCR1 records with debit amounts
     */
    @Query("SELECT r FROM Vss120Tcr1Record r " +
            "WHERE r.fileProcessingJob = :job " +
            "AND r.isValid = true " +
            "AND (r.firstAmountSign = 'DB' " +
            "OR r.secondAmountSign = 'DB' " +
            "OR r.thirdAmountSign = 'DB' " +
            "OR r.fourthAmountSign = 'DB' " +
            "OR r.fifthAmountSign = 'DB' " +
            "OR r.sixthAmountSign = 'DB') " +
            "ORDER BY r.destinationId, r.lineNumber")
    List<Vss120Tcr1Record> findRecordsWithDebitAmounts(@Param("job") FileProcessingJob fileProcessingJob);

    /**
     * Find orphaned TCR1 records (without parent VSS-120 record).
     *
     * @param fileProcessingJob the file processing job
     * @return list of orphaned TCR1 records
     */
    List<Vss120Tcr1Record> findByFileProcessingJobAndParentVssRecordIsNull(FileProcessingJob fileProcessingJob);

    /**
     * Find TCR1 records with validation errors.
     *
     * @param fileProcessingJob the file processing job
     * @return list of TCR1 records with errors
     */
    @Query("SELECT r FROM Vss120Tcr1Record r " +
            "WHERE r.fileProcessingJob = :job " +
            "AND r.isValid = false " +
            "AND r.validationErrors IS NOT NULL " +
            "ORDER BY r.lineNumber")
    List<Vss120Tcr1Record> findRecordsWithErrors(@Param("job") FileProcessingJob fileProcessingJob);

    /**
     * Find distinct rate table IDs in TCR1 records for a job.
     *
     * @param fileProcessingJob the file processing job
     * @return list of distinct rate table IDs
     */
    @Query("SELECT DISTINCT r.rateTableId FROM Vss120Tcr1Record r " +
            "WHERE r.fileProcessingJob = :job " +
            "AND r.rateTableId IS NOT NULL " +
            "AND r.rateTableId <> '' " +
            "ORDER BY r.rateTableId")
    List<String> findDistinctRateTableIds(@Param("job") FileProcessingJob fileProcessingJob);

    /**
     * Find distinct destination IDs in TCR1 records for a job.
     *
     * @param fileProcessingJob the file processing job
     * @return list of distinct destination IDs
     */
    @Query("SELECT DISTINCT r.destinationId FROM Vss120Tcr1Record r " +
            "WHERE r.fileProcessingJob = :job " +
            "ORDER BY r.destinationId")
    List<String> findDistinctDestinationIds(@Param("job") FileProcessingJob fileProcessingJob);

    /**
     * Count TCR1 records by destination ID for a job.
     *
     * @param fileProcessingJob the file processing job
     * @param destinationId the destination identifier
     * @return count of records
     */
    long countByFileProcessingJobAndDestinationId(FileProcessingJob fileProcessingJob, String destinationId);

    /**
     * Count TCR1 records by rate table ID for a job.
     *
     * @param fileProcessingJob the file processing job
     * @param rateTableId the rate table identifier
     * @return count of records
     */
    long countByFileProcessingJobAndRateTableId(FileProcessingJob fileProcessingJob, String rateTableId);

    /**
     * Find the latest TCR1 records by destination ID.
     *
     * @param destinationId the destination identifier
     * @param pageable pagination parameters
     * @return list of latest TCR1 records
     */
    @Query("SELECT r FROM Vss120Tcr1Record r " +
            "WHERE r.destinationId = :destinationId " +
            "AND r.isValid = true " +
            "ORDER BY r.createdAt DESC")
    List<Vss120Tcr1Record> findLatestByDestinationId(
            @Param("destinationId") String destinationId,
            Pageable pageable);

    /**
     * Delete all TCR1 records for a specific file processing job.
     *
     * @param fileProcessingJob the file processing job
     * @return number of deleted records
     */
    @Modifying
    @Query("DELETE FROM Vss120Tcr1Record r WHERE r.fileProcessingJob = :job")
    int deleteByFileProcessingJob(@Param("job") FileProcessingJob fileProcessingJob);

    /**
     * Delete all TCR1 records for a specific parent VSS-120 record.
     *
     * @param parentRecord the parent VSS-120 record
     * @return number of deleted records
     */
    @Modifying
    @Query("DELETE FROM Vss120Tcr1Record r WHERE r.parentVssRecord = :parent")
    int deleteByparentVssRecord(@Param("parent") Vss120SettlementRecord parentRecord);

    /**
     * Find TCR1 records that need to be linked to parent records.
     * This query finds TCR1 records that don't have a parent but could be matched by destination ID.
     *
     * @param fileProcessingJob the file processing job
     * @return list of TCR1 records that could be linked
     */
    @Query("SELECT t FROM Vss120Tcr1Record t " +
            "WHERE t.fileProcessingJob = :job " +
            "AND t.parentVssRecord IS NULL " +
            "AND EXISTS (SELECT v FROM Vss120SettlementRecord v " +
            "           WHERE v.fileProcessingJob = :job " +
            "           AND v.destinationId = t.destinationId " +
            "           AND v.isValid = true) " +
            "ORDER BY t.destinationId, t.lineNumber")
    List<Vss120Tcr1Record> findUnlinkedRecordsWithPotentialParents(@Param("job") FileProcessingJob fileProcessingJob);

    /**
     * Get aggregated amounts by destination ID and rate table ID.
     *
     * @param fileProcessingJob the file processing job
     * @return aggregated data [destinationId, rateTableId, totalFirstAmount, totalSecondAmount, etc.]
     */
}