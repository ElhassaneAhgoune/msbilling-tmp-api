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
 * DTO for VSS-130 Reimbursement Fees Report data.
 * Maps to the field positions specified in ReadingReports.txt for Report 130.
 * 
 * Field mappings:
 * - SETTLEMENT CURRENCY: Position 24 read three from left to right
 * - COUNT: Position 62 read from right to left
 * - INTERCHANGE AMOUNT: Position 87 read from right to left
 * - REIMBURSEMENT FEE CREDITS: Position 110 read from right to left
 * - REIMBURSEMENT FEE DEBITS: Position 132 read from right to left
 * 
 * @author EL.AHGOUNE
 * @version 1.0.0
 * @since 2024
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report130Dto {

    /**
     * Settlement currency code (3 characters from position 24).
     */
    @NotNull
    @Size(max = 3, message = "Settlement currency must be exactly 3 characters")
    private String settlementCurrency;

    /**
     * Transaction count (from position 62, read right to left).
     */
    private Long count;

    /**
     * Interchange amount (from position 87, read right to left).
     */
    private BigDecimal interchangeAmount;

    /**
     * Reimbursement fee credits (from position 110, read right to left).
     */
    private BigDecimal reimbursementFeeCredits;

    /**
     * Reimbursement fee debits (from position 132, read right to left).
     */
    private BigDecimal reimbursementFeeDebits;

    /**
     * Transaction type (e.g., "PURCHASE", "MANUAL CASH", etc.).
     */
    private String transactionType;

    /**
     * Transaction detail (e.g., "ORIGINAL SALE", "ORIGINAL ADVANCE", etc.).
     */
    private String transactionDetail;

    /**
     * Fee category or geographical region.
     */
    private String feeCategory;

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