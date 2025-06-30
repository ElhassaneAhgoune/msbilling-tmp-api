package com.moneysab.cardexis.repository.vss;

import com.moneysab.cardexis.domain.entity.vss.Report900Entity;
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
 * Repository interface for VSS-900 text report entities.
 * 
 * @author EL.AHGOUNE
 * @version 1.0.0
 * @since 2024
 */
@Repository
public interface Report900Repository extends JpaRepository<Report900Entity, UUID> {

    Page<Report900Entity> findByClearingCurrency(String clearingCurrency, Pageable pageable);
    
    Page<Report900Entity> findByProcessingDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);
    
    Page<Report900Entity> findBySourceFileName(String sourceFileName, Pageable pageable);
    
    Page<Report900Entity> findByTransactionCategory(String transactionCategory, Pageable pageable);
    
    Page<Report900Entity> findByTransactionDirection(String transactionDirection, Pageable pageable);

    @Query("SELECT DISTINCT r.clearingCurrency FROM Report900Entity r ORDER BY r.clearingCurrency")
    List<String> findDistinctClearingCurrencies();

    @Query("SELECT DISTINCT r.transactionCategory FROM Report900Entity r WHERE r.transactionCategory IS NOT NULL ORDER BY r.transactionCategory")
    List<String> findDistinctTransactionCategories();

    @Query("SELECT DISTINCT r.transactionDirection FROM Report900Entity r WHERE r.transactionDirection IS NOT NULL ORDER BY r.transactionDirection")
    List<String> findDistinctTransactionDirections();

    @Query("""
        SELECT NEW map(
            r.clearingCurrency as currency,
            COUNT(r) as recordCount,
            SUM(r.count) as totalCount,
            SUM(r.clearingAmount) as totalClearingAmount,
            SUM(r.totalCount) as grandTotalCount,
            SUM(r.totalClearingAmount) as grandTotalAmount
        )
        FROM Report900Entity r
        WHERE r.clearingCurrency = :currency
        AND r.processingDate = :date
        """)
    List<Object> getSummaryByCurrencyAndDate(@Param("currency") String clearingCurrency, @Param("date") LocalDate processingDate);

    Page<Report900Entity> findByIsValidFalse(Pageable pageable);
    
    void deleteBySourceFileName(String sourceFileName);
} 