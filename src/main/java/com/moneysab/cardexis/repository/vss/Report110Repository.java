package com.moneysab.cardexis.repository.vss;

import com.moneysab.cardexis.domain.entity.vss.Report110Entity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for VSS-110 text report entities.
 * Provides data access methods for VSS-110 settlement summary report management.
 * 
 * @author EL.AHGOUNE
 * @version 1.0.0
 * @since 2024
 */
@Repository
public interface Report110Repository extends JpaRepository<Report110Entity, UUID> {

    /**
     * Find reports by settlement currency.
     *
     * @param settlementCurrency the settlement currency code
     * @param pageable pagination information
     * @return page of reports
     */
    Page<Report110Entity> findBySettlementCurrency(String settlementCurrency, Pageable pageable);

    /**
     * Find reports by processing date range.
     *
     * @param startDate start date (inclusive)
     * @param endDate end date (inclusive)
     * @param pageable pagination information
     * @return page of reports
     */
    Page<Report110Entity> findByProcessingDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);

    /**
     * Find reports by source file name.
     *
     * @param sourceFileName the source file name
     * @param pageable pagination information
     * @return page of reports
     */
    Page<Report110Entity> findBySourceFileName(String sourceFileName, Pageable pageable);

    /**
     * Find reports by section name.
     *
     * @param sectionName the section name
     * @param pageable pagination information
     * @return page of reports
     */
    Page<Report110Entity> findBySectionName(String sectionName, Pageable pageable);

    /**
     * Find all distinct settlement currencies.
     *
     * @return list of settlement currencies
     */
    @Query("SELECT DISTINCT r.settlementCurrency FROM Report110Entity r ORDER BY r.settlementCurrency")
    List<String> findDistinctSettlementCurrencies();

    /**
     * Find all distinct section names.
     *
     * @return list of section names
     */
    @Query("SELECT DISTINCT r.sectionName FROM Report110Entity r WHERE r.sectionName IS NOT NULL ORDER BY r.sectionName")
    List<String> findDistinctSectionNames();

    /**
     * Get summary by settlement currency and processing date.
     *
     * @param settlementCurrency the settlement currency
     * @param processingDate the processing date
     * @return summary data
     */
    @Query("""
        SELECT NEW map(
            r.settlementCurrency as currency,
            COUNT(r) as recordCount,
            SUM(r.creditAmount) as totalCredits,
            SUM(r.debitAmount) as totalDebits,
            SUM(r.totalAmount) as netTotal
        )
        FROM Report110Entity r
        WHERE r.settlementCurrency = :currency
        AND r.processingDate = :date
        """)
    List<Object> getSummaryByCurrencyAndDate(@Param("currency") String settlementCurrency, @Param("date") LocalDate processingDate);

    /**
     * Find reports with validation errors.
     *
     * @param pageable pagination information
     * @return page of reports with errors
     */
    Page<Report110Entity> findByIsValidFalse(Pageable pageable);

    /**
     * Delete reports by source file name.
     *
     * @param sourceFileName the source file name
     */
    void deleteBySourceFileName(String sourceFileName);
} 