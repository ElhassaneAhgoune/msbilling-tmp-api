package com.moneysab.cardexis.repository.vss;

import com.moneysab.cardexis.domain.entity.FileProcessingJob;
import com.moneysab.cardexis.domain.entity.vss.Report140Entity;
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
 * Repository interface for VSS-140 text report entities.
 * 
 * @author EL.AHGOUNE
 * @version 1.0.0
 * @since 2024
 */
@Repository
public interface Report140Repository extends JpaRepository<Report140Entity, UUID> {

    Page<Report140Entity> findBySettlementCurrency(String settlementCurrency, Pageable pageable);
    
    Page<Report140Entity> findByProcessingDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);
    
    Page<Report140Entity> findBySourceFileName(String sourceFileName, Pageable pageable);
    
    Page<Report140Entity> findByChargeType(String chargeType, Pageable pageable);
    
    Page<Report140Entity> findByTransactionType(String transactionType, Pageable pageable);

    @Query("SELECT DISTINCT r.settlementCurrency FROM Report140Entity r ORDER BY r.settlementCurrency")
    List<String> findDistinctSettlementCurrencies();

    @Query("SELECT DISTINCT r.chargeType FROM Report140Entity r WHERE r.chargeType IS NOT NULL ORDER BY r.chargeType")
    List<String> findDistinctChargeTypes();

    @Query("SELECT DISTINCT r.transactionType FROM Report140Entity r WHERE r.transactionType IS NOT NULL ORDER BY r.transactionType")
    List<String> findDistinctTransactionTypes();

    @Query("""
        SELECT NEW map(
            r.settlementCurrency as currency,
            COUNT(r) as recordCount,
            SUM(r.interchangeAmount) as totalInterchangeAmount,
            SUM(r.visaChargesCredits) as totalChargeCredits,
            SUM(r.visaChargesDebits) as totalChargeDebits
        )
        FROM Report140Entity r
        WHERE r.settlementCurrency = :currency
        AND r.processingDate = :date
        """)
    List<Object> getSummaryByCurrencyAndDate(@Param("currency") String settlementCurrency, @Param("date") LocalDate processingDate);

    Page<Report140Entity> findByIsValidFalse(Pageable pageable);
    
    void deleteBySourceFileName(String sourceFileName);


} 