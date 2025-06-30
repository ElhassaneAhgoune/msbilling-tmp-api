package com.moneysab.cardexis.domain.entity.vss;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity for VSS-110 Settlement Summary Report data.
 * Stores parsed data from text-based VSS reports using field mappings.
 * 
 * @author EL.AHGOUNE
 * @version 1.0.0
 * @since 2024
 */
@Entity
@Table(name = "vss_report_110", 
       indexes = {
           @Index(name = "idx_vss_report_110_currency", columnList = "settlement_currency"),
           @Index(name = "idx_vss_report_110_proc_date", columnList = "processing_date"),
           @Index(name = "idx_vss_report_110_file", columnList = "source_file_name"),
           @Index(name = "idx_vss_report_110_section", columnList = "section_name")
       })
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class Report110Entity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Settlement currency code (3 characters from position 24).
     */
    @Size(max = 3, message = "Settlement currency must be exactly 3 characters")
    @Column(name = "settlement_currency", length = 3, nullable = true)
    private String settlementCurrency;

    /**
     * Transaction count (from position 52, read right to left).
     */
    @Column(name = "count")
    private Long count;

    /**
     * Credit amount (from position 78, read right to left).
     */
    @Column(name = "credit_amount", precision = 15, scale = 2)
    private BigDecimal creditAmount;

    /**
     * Debit amount (from position 104, read right to left).
     */
    @Column(name = "debit_amount", precision = 15, scale = 2)
    private BigDecimal debitAmount;

    /**
     * Total amount (from position 132, read right to left).
     */
    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    /**
     * Report section name (e.g., "INTERCHANGE VALUE", "REIMBURSEMENT FEES", etc.).
     */
    @Size(max = 100, message = "Section name cannot exceed 100 characters")
    @Column(name = "section_name", length = 100)
    private String sectionName;

    /**
     * Report processing date.
     */
    @Column(name = "processing_date")
    private LocalDate processingDate;

    /**
     * Report date.
     */
    @Column(name = "report_date")
    private LocalDate reportDate;

    /**
     * Source file name.
     */
    @Size(max = 255, message = "Source file name cannot exceed 255 characters")
    @Column(name = "source_file_name", length = 255)
    private String sourceFileName;

    /**
     * Line number in the source file.
     */
    @Column(name = "line_number")
    private Integer lineNumber;

    /**
     * Raw line content for audit purposes.
     */
    @Column(name = "raw_line_content", columnDefinition = "TEXT")
    private String rawLineContent;

    /**
     * Record creation timestamp.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Record last modification timestamp.
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Processing status for tracking data quality.
     */
    @Builder.Default
    @Column(name = "is_valid")
    private Boolean isValid = true;

    /**
     * Validation error messages if any.
     */
    @Column(name = "validation_errors", columnDefinition = "TEXT")
    private String validationErrors;
} 