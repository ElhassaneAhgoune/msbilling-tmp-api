package com.moneysab.cardexis.repository;


import com.moneysab.cardexis.domain.entity.FileProcessingJob;
import com.moneysab.cardexis.domain.entity.Vss140SettlementRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for VSS-140 settlement records.
 *
 * @author S.AIT MOHAMMED
 * @version 1.0.0
 * @since 2025
 */

@Repository
public interface Vss140SettlementRecordRepository extends JpaRepository<Vss140SettlementRecord, UUID> {
    /**
     * Find the most recent VSS-140 record for a file processing job by line number.
     *
     * @param fileProcessingJob the file processing job
     * @return optional containing the most recent VSS-140 record
     */
    Optional<Vss140SettlementRecord> findTopByFileProcessingJobOrderByLineNumberDesc(FileProcessingJob fileProcessingJob);

    /**
     * Find distinct settlement dates in VSS-140 records for a job.
     *
     * @param fileProcessingJob the file processing job
     * @return list of distinct settlement dates
     */
    @Query("SELECT DISTINCT r.settlementDate FROM Vss140SettlementRecord r " +
            "WHERE r.fileProcessingJob = :job " +
            "AND r.settlementDate IS NOT NULL " +
            "ORDER BY r.settlementDate")
    List<LocalDate> findDistinctSettlementDates(@Param("job") FileProcessingJob fileProcessingJob);

    /**
     * Find distinct charge type codes in VSS-140 records for a job.
     *
     * @param fileProcessingJob the file processing job
     * @return list of distinct charge type codes
     */
    @Query("SELECT DISTINCT r.chargeTypeCode FROM Vss140SettlementRecord r " +
            "WHERE r.fileProcessingJob = :job " +
            "AND r.chargeTypeCode IS NOT NULL " +
            "AND r.chargeTypeCode <> '' " +
            "ORDER BY r.chargeTypeCode")
    List<String> findDistinctChargeTypeCodes(@Param("job") FileProcessingJob fileProcessingJob);

    @Modifying
    @Query("DELETE FROM Vss140SettlementRecord r WHERE r.fileProcessingJob = :job")
    int deleteByFileProcessingJob(@Param("job") FileProcessingJob fileProcessingJob);

    /**
     * Find the latest VSS-140 records by destination ID.
     *
     * @param destinationId the destination identifier
     * @param limit maximum number of records to return
     * @return list of latest VSS-140 settlement records
     */
    @Query("SELECT r FROM Vss140SettlementRecord r " +
            "WHERE r.destinationId = :destinationId " +
            "AND r.isValid = true " +
            "ORDER BY r.settlementDate DESC, r.createdAt DESC")
    List<Vss140SettlementRecord> findLatestByDestinationId(
            @Param("destinationId") String destinationId,
            Pageable pageable);

    /**
     * Find VSS-140 records with validation errors.
     *
     * @param fileProcessingJob the file processing job
     * @return list of VSS-140 records with errors
     */
    @Query("SELECT r FROM Vss140SettlementRecord r " +
            "WHERE r.fileProcessingJob = :job " +
            "AND r.isValid = false " +
            "AND r.validationErrors IS NOT NULL " +
            "ORDER BY r.lineNumber")
    List<Vss140SettlementRecord> findRecordsWithErrors(@Param("job") FileProcessingJob fileProcessingJob);

    /**
     * Count VSS-140 records by report ID number for a job.
     *
     * @param fileProcessingJob the file processing job
     * @param reportIdNumber the report ID number
     * @return count of records
     */
    long countByFileProcessingJobAndReportIdNumber(FileProcessingJob fileProcessingJob, String reportIdNumber);

    /**
     * Find distinct destination IDs in VSS-140 records for a job.
     *
     * @param fileProcessingJob the file processing job
     * @return list of distinct destination IDs
     */
    @Query("SELECT DISTINCT r.destinationId FROM Vss140SettlementRecord r " +
            "WHERE r.fileProcessingJob = :job " +
            "ORDER BY r.destinationId")
    List<String> findDistinctDestinationIds(@Param("job") FileProcessingJob fileProcessingJob);

    /**
     * Find distinct report ID numbers in VSS-140 records for a job.
     *
     * @param fileProcessingJob the file processing job
     * @return list of distinct report ID numbers
     */
    @Query("SELECT DISTINCT r.reportIdNumber FROM Vss140SettlementRecord r " +
            "WHERE r.fileProcessingJob = :job " +
            "ORDER BY r.reportIdNumber")
    List<String> findDistinctReportIdNumbers(@Param("job") FileProcessingJob fileProcessingJob);

    @Query("SELECT t0, t1 FROM Vss140SettlementRecord t0 JOIN Vss120Tcr1Record t1 ON t1.parentVssRecord = t0.id "+
            "WHERE t0.summaryLevel = '09'"+
            "AND ( t0.settlementDate >= COALESCE(:startDate, t0.settlementDate)) " +
            "AND ( t0.settlementDate <= COALESCE(:endDate, t0.settlementDate))  " +
            "AND (:currencyCode IS NULL OR t0.settlementCurrencyCode = :currencyCode)"+
            "AND (:binCode IS NULL OR t0.destinationId LIKE %:binCode%)")
    List<Object[]> findAllWithTcr1( @Param("currencyCode") String currencyCode,
                                    @Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate,
                                    @Param("binCode") String binCode);
}
