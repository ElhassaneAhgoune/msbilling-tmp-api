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
 * DTO for VSS-110 Settlement Summary Report data.
 * Maps to the field positions specified in ReadingReports.txt for Report 110.
 * 
 * Field mappings:
 * - SETTLEMENT CURRENCY: Position 24 read three from left to right
 * - COUNT: Position 52 read from right to left  
 * - CREDIT AMOUNT: Position 78 read from right to left
 * - DEBIT AMOUNT: Position 104 read from right to left
 * - TOTAL AMOUNT: Position 132 read from right to left
 * 
 * @author EL.AHGOUNE
 * @version 1.0.0
 * @since 2024
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report110Dto {

    /**
     * Settlement currency code (3 characters from position 24).
     */
    @NotNull
    @Size(max = 3, message = "Settlement currency must be exactly 3 characters")
    private String settlementCurrency;

    /**
     * Transaction count (from position 52, read right to left).
     */
    private Long count;

    /**
     * Credit amount (from position 78, read right to left).
     */
    private BigDecimal creditAmount;

    /**
     * Debit amount (from position 104, read right to left).  
     */
    private BigDecimal debitAmount;

    /**
     * Total amount (from position 132, read right to left).
     */
    private BigDecimal totalAmount;

    /**
     * Report section name (e.g., "INTERCHANGE VALUE", "REIMBURSEMENT FEES", etc.).
     */
    private String sectionName;

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