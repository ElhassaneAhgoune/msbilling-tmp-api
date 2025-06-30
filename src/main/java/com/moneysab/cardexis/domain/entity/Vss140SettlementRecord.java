package com.moneysab.cardexis.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * Entity representing a VSS-140 settlement record from Visa EPIN files (TC46, TCR0, Report Group V, Subgroup 4).
 *
 * VSS-140 records contain client settlement data based on the official Visa Settlement Service specification.
 * Each record represents settlement activity for a specific date with detailed transaction information
 * including counts, amounts, and various fee types.
 *
 * Complete TCR0 Record Format (168 characters fixed-width) - Client Settlement Data:
 * This format differs from VSS-110 and includes additional fields for client settlement reporting.
 *
 * @author S.AIT MOHAMMED
 * @version 1.0.0
 * @since 2024
 */
@Entity
@Table(name = "vss140_settlement_records", indexes = {
        @Index(name = "idx_vss140_settlement_date", columnList = "settlement_date"),
        @Index(name = "idx_vss140_job_id", columnList = "job_id"),
        @Index(name = "idx_vss140_transaction_code", columnList = "transaction_code"),
        @Index(name = "idx_vss140_destination_id", columnList = "destination_id"),
        @Index(name = "idx_vss140_report_id", columnList = "report_id_number"),
        @Index(name = "idx_vss140_business_mode", columnList = "business_mode"),
        @Index(name = "idx_vss140_source_id", columnList = "source_identifier"),
        @Index(name = "idx_vss140_from_date", columnList = "from_date"),
        @Index(name = "idx_vss140_to_date", columnList = "to_date")
})
public class Vss140SettlementRecord extends VssSubGroup4Record {

    // === CONSTRUCTORS ===

    /**
     * Default constructor for JPA.
     */
    public Vss140SettlementRecord() {
        super();
    }

    /**
     * Creates a new Vss120SettlementRecord with the specified job reference.
     *
     * @param fileProcessingJob the associated file processing job
     */
    public Vss140SettlementRecord(FileProcessingJob fileProcessingJob) {
        super(fileProcessingJob);
    }


    @Override
    public String toString() {
        return "Vss140SettlementRecord{" +
                "id=" + getId() +
                ", lineNumber=" + getLineNumber() +
                ", transactionCode='" + getTransactionCode() + '\'' +
                ", destinationId='" + getDestinationId()  + '\'' +
                ", sourceIdentifier='" + getSourceIdentifier()  + '\'' +
                ", settlementDate=" + getSettlementDate()  +
                ", reportIdNumber='" + getReportIdNumber()  + '\'' +
                ", businessMode='" + getBusinessMode() + '\'' +
                ", chargeTypeCode='" + getChargeTypeCode() + '\'' +
                ", businessTransactionType='" + getBusinessTransactionType()  + '\'' +
                ", isValid=" + getIsValid()  +
                '}';
    }
}