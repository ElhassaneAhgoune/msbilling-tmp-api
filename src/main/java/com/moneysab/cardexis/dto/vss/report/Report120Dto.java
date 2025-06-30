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
 * DTO for VSS-120 Interchange Value Report data.
 * Maps to the field positions specified in ReadingReports.txt for Report 120.
 * 
 * Field mappings:
 * - SETTLEMENT CURRENCY: Position 24 read three from left to right
 * - CLEARING CURRENCY: Position 24 read three from left to right
 * - TABLE ID: Position 52 read from right to left
 * - COUNT: Position 67 read from right to left
 * - CLEARING AMOUNT: Position 90 read from right to left
 * - INTERCHANGE CREDITS: Position 104 read from right to left
 * - INTERCHANGE DEBITS: Position 130 read from right to left
 * 
 * @author EL.AHGOUNE
 * @version 1.0.0
 * @since 2024
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report120Dto {

    /**
     * Settlement currency code (3 characters from position 24).
     */
    @NotNull
    @Size(max = 3, message = "Settlement currency must be exactly 3 characters")
    private String settlementCurrency;

    /**
     * Clearing currency code (3 characters from position 24).
     */
    @Size(max = 3, message = "Clearing currency must be exactly 3 characters")
    private String clearingCurrency;

    /**
     * Rate table ID (from position 52, read right to left).
     */
    private String tableId;

    /**
     * Transaction count (from position 67, read right to left).
     */
    private Long count;

    /**
     * Clearing amount (from position 90, read right to left).
     */
    private BigDecimal clearingAmount;

    /**
     * Interchange credit value (from position 104, read right to left).
     */
    private BigDecimal interchangeCredits;

    /**
     * Interchange debit value (from position 130, read right to left).
     */
    private BigDecimal interchangeDebits;

    /**
     * Transaction type (e.g., "PURCHASE", "MANUAL CASH", etc.).
     */
    private String transactionType;

    /**
     * Transaction detail (e.g., "ORIGINAL SALE", "ORIGINAL ADVANCE", etc.).
     */
    private String transactionDetail;

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