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
 * Entity for VSS-140 Visa Charges Report data.
 * 
 * @author EL.AHGOUNE
 * @version 1.0.0
 * @since 2024
 */
@Entity
@Table(name = "vss_report_140", 
       indexes = {
           @Index(name = "idx_vss_report_140_currency", columnList = "settlement_currency"),
           @Index(name = "idx_vss_report_140_proc_date", columnList = "processing_date"),
           @Index(name = "idx_vss_report_140_file", columnList = "source_file_name")
       })
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class Report140Entity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Size(max = 3)
    @Column(name = "settlement_currency", length = 3, nullable = true)
    private String settlementCurrency;

    @Column(name = "count")
    private Long count;

    @Column(name = "interchange_amount", precision = 15, scale = 2)
    private BigDecimal interchangeAmount;

    @Column(name = "visa_charges_credits", precision = 15, scale = 2)
    private BigDecimal visaChargesCredits;

    @Column(name = "visa_charges_debits", precision = 15, scale = 2)
    private BigDecimal visaChargesDebits;

    @Size(max = 100)
    @Column(name = "charge_type", length = 100)
    private String chargeType;

    @Size(max = 100)
    @Column(name = "transaction_type", length = 100)
    private String transactionType;

    @Size(max = 100)
    @Column(name = "transaction_detail", length = 100)
    private String transactionDetail;

    @Size(max = 100)
    @Column(name = "region", length = 100)
    private String region;

    @Column(name = "processing_date")
    private LocalDate processingDate;

    @Column(name = "report_date")
    private LocalDate reportDate;

    @Size(max = 255)
    @Column(name = "source_file_name", length = 255)
    private String sourceFileName;

    @Column(name = "line_number")
    private Integer lineNumber;

    @Column(name = "raw_line_content", columnDefinition = "TEXT")
    private String rawLineContent;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder.Default
    @Column(name = "is_valid")
    private Boolean isValid = true;

    @Column(name = "validation_errors", columnDefinition = "TEXT")
    private String validationErrors;
} 