package com.moneysab.cardexis.dto.vss.report;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for VSS-900 Summary Reconciliation Report data.
 * Maps to the field positions specified in ReadingReports.txt for Report 900.
 * 
 * Field mappings:
 * - CLEARING CURRENCY: Position 22 read three from left to right
 * - COUNT: Position 67 read from right to left
 * - CLEARING AMOUNT: Position 89 read from right to left
 * - TOTAL COUNT: Position 106 read from right to left
 * - TOTAL CLEARING AMOUNT: Position 131 read from right to left
 * 
 * @author EL.AHGOUNE
 * @version 1.0.0
 * @since 2024
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report900Dto {

    /**
     * Clearing currency code (3 characters from position 22).
     */
    @NotNull
    @Size(max = 3, message = "Clearing currency must be exactly 3 characters")
    private String clearingCurrency;

    /**
     * Transaction count (from position 67, read right to left).
     */
    private Long count;

    /**
     * Clearing amount (from position 89, read right to left).
     */
    private BigDecimal clearingAmount;

    /**
     * Total count (from position 106, read right to left).
     */
    private Long totalCount;

    /**
     * Total clearing amount (from position 131, read right to left).
     */
    private BigDecimal totalClearingAmount;

    /**
     * Transaction category (e.g., "FINANCIAL TRANSACTIONS", "NON-FINANCIAL TRANSACTIONS").
     */
    private String transactionCategory;

    /**
     * Transaction direction (e.g., "SENT TO VISA", "RECEIVED FROM VISA").
     */
    private String transactionDirection;

    /**
     * Report processing date.
     */
    private LocalDate processingDate;

    /**
     * Report date.
     */
    private LocalDate reportDate;

    /**
     * Source file name.
     */
    private String sourceFileName;

    /**
     * Line number in the source file.
     */
    private Integer lineNumber;

    /**
     * Raw line content for audit purposes.
     */
    private String rawLineContent;
} 