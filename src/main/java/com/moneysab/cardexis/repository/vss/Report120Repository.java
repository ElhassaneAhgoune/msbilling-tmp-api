package com.moneysab.cardexis.repository.vss;

import com.moneysab.cardexis.domain.entity.vss.Report120Entity;
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
 * Repository interface for VSS-120 text report entities.
 * 
 * @author EL.AHGOUNE
 * @version 1.0.0
 * @since 2024
 */
@Repository
public interface Report120Repository extends JpaRepository<Report120Entity, UUID> {

    Page<Report120Entity> findBySettlementCurrency(String settlementCurrency, Pageable pageable);
    
    Page<Report120Entity> findByClearingCurrency(String clearingCurrency, Pageable pageable);
    
    Page<Report120Entity> findByProcessingDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);
    
    Page<Report120Entity> findBySourceFileName(String sourceFileName, Pageable pageable);
    
    Page<Report120Entity> findByTransactionType(String transactionType, Pageable pageable);

    @Query("SELECT DISTINCT r.settlementCurrency FROM Report120Entity r ORDER BY r.settlementCurrency")
    List<String> findDistinctSettlementCurrencies();

    @Query("SELECT DISTINCT r.clearingCurrency FROM Report120Entity r WHERE r.clearingCurrency IS NOT NULL ORDER BY r.clearingCurrency")
    List<String> findDistinctClearingCurrencies();

    @Query("SELECT DISTINCT r.transactionType FROM Report120Entity r WHERE r.transactionType IS NOT NULL ORDER BY r.transactionType")
    List<String> findDistinctTransactionTypes();

    @Query("""
        SELECT NEW map(
            r.settlementCurrency as settlementCurrency,
            r.clearingCurrency as clearingCurrency,
            COUNT(r) as recordCount,
            SUM(r.clearingAmount) as totalClearingAmount,
            SUM(r.interchangeCredits) as totalInterchangeCredits,
            SUM(r.interchangeDebits) as totalInterchangeDebits
        )
        FROM Report120Entity r
        WHERE r.settlementCurrency = :settlementCurrency
        AND (:clearingCurrency IS NULL OR r.clearingCurrency = :clearingCurrency)
        AND r.processingDate = :date
        GROUP BY r.settlementCurrency, r.clearingCurrency
        """)
    List<Object> getSummaryByCurrencyAndDate(
        @Param("settlementCurrency") String settlementCurrency, 
        @Param("clearingCurrency") String clearingCurrency,
        @Param("date") LocalDate processingDate);

    Page<Report120Entity> findByIsValidFalse(Pageable pageable);
    
    void deleteBySourceFileName(String sourceFileName);
}