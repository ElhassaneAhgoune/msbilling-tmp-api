package com.moneysab.cardexis.repository.vss;

import com.moneysab.cardexis.domain.entity.FileProcessingJob;
import com.moneysab.cardexis.domain.entity.vss.Report130Entity;
import org.springframework.data.domain.Page;
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
 * Repository interface for VSS-130 text report entities.
 * 
 * @author EL.AHGOUNE
 * @version 1.0.0
 * @since 2024
 */
@Repository
public interface Report130Repository extends JpaRepository<Report130Entity, UUID> {

    Page<Report130Entity> findBySettlementCurrency(String settlementCurrency, Pageable pageable);
    
    Page<Report130Entity> findByProcessingDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);
    
    Page<Report130Entity> findBySourceFileName(String sourceFileName, Pageable pageable);
    
    Page<Report130Entity> findByTransactionType(String transactionType, Pageable pageable);
    
    Page<Report130Entity> findByFeeCategory(String feeCategory, Pageable pageable);

    @Query("SELECT DISTINCT r.settlementCurrency FROM Report130Entity r ORDER BY r.settlementCurrency")
    List<String> findDistinctSettlementCurrencies();

    @Query("SELECT DISTINCT r.transactionType FROM Report130Entity r WHERE r.transactionType IS NOT NULL ORDER BY r.transactionType")
    List<String> findDistinctTransactionTypes();

    @Query("SELECT DISTINCT r.feeCategory FROM Report130Entity r WHERE r.feeCategory IS NOT NULL ORDER BY r.feeCategory")
    List<String> findDistinctFeeCategories();

    @Query("""
        SELECT NEW map(
            r.settlementCurrency as currency,
            COUNT(r) as recordCount,
            SUM(r.interchangeAmount) as totalInterchangeAmount,
            SUM(r.reimbursementFeeCredits) as totalFeeCredits,
            SUM(r.reimbursementFeeDebits) as totalFeeDebits
        )
        FROM Report130Entity r
        WHERE r.settlementCurrency = :currency
        AND r.processingDate = :date
        """)
    List<Object> getSummaryByCurrencyAndDate(@Param("currency") String settlementCurrency, @Param("date") LocalDate processingDate);

    Page<Report130Entity> findByIsValidFalse(Pageable pageable);
    
    void deleteBySourceFileName(String sourceFileName);


} 