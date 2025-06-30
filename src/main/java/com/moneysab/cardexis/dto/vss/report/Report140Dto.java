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
 * DTO for VSS-140 Visa Charges Report data.
 * Maps to the field positions specified in ReadingReports.txt for Report 140.
 * 
 * Field mappings:
 * - SETTLEMENT CURRENCY: Position 24 read three from left to right
 * - COUNT: Position 67 read from right to left
 * - INTERCHANGE AMOUNT: Position 90 read from right to left
 * - VISA CHARGES CREDITS: Position 111 read from right to left
 * - VISA CHARGES DEBITS: Position 132 read from right to left
 * 
 * @author EL.AHGOUNE
 * @version 1.0.0
 * @since 2024
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report140Dto {

    /**
     * Settlement currency code (3 characters from position 24).
     */
    @NotNull
    @Size(max = 3, message = "Settlement currency must be exactly 3 characters")
    private String settlementCurrency;

    /**
     * Transaction count (from position 67, read right to left).
     */
    private Long count;

    /**
     * Interchange amount (from position 90, read right to left).
     */
    private BigDecimal interchangeAmount;

    /**
     * Visa charges credits (from position 111, read right to left).
     */
    private BigDecimal visaChargesCredits;

    /**
     * Visa charges debits (from position 132, read right to left).
     */
    private BigDecimal visaChargesDebits;

    /**
     * Charge type (e.g., "IAF CHARGE BASE CASH", etc.).
     */
    private String chargeType;

    /**
     * Transaction type (e.g., "MANUAL CASH", etc.).
     */
    private String transactionType;

    /**
     * Transaction detail (e.g., "ORIGINAL ADVANCE", etc.).
     */
    private String transactionDetail;

    /**
     * Geographical region or category.
     */
    private String region;

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