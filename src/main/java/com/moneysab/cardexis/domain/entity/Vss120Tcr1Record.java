package com.moneysab.cardexis.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entity representing a TCR1 record for VSS-120 (TC46, TCR1, Report Group V, Subgroup 4).
 *
 * TCR1 records contain the actual count and amount data that correspond to the VSS-120 TCR0 records.
 * Each TCR1 record provides up to 6 count/amount pairs mapped according to the report type.
 *
 * Complete TCR1 Record Format (168 characters fixed-width) for Report Subgroup 4:
 * - Contains Rate Table ID, multiple count/amount fields with signs
 * - Maps to different data types based on the Report ID Number (120, 130, 131, etc.)
 *
 * @author EL.AHGOUNE
 * @version 1.0.0
 * @since 2024
 */
@Entity
@Table(name = "vss120_tcr1_records", indexes = {
        @Index(name = "idx_vss120_tcr1_job_id", columnList = "job_id"),
        @Index(name = "idx_vss120_tcr1_transaction_code", columnList = "transaction_code"),
        @Index(name = "idx_vss120_tcr1_destination_id", columnList = "destination_id"),
        @Index(name = "idx_vss120_tcr1_rate_table_id", columnList = "rate_table_id"),
        @Index(name = "idx_vss120_tcr1_parent_record", columnList = "parent_vss120_record_id")
})
public class Vss120Tcr1Record extends BaseEntity {

    /**
     * Reference to the file processing job.
     */
    @NotNull(message = "File processing job cannot be null")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private FileProcessingJob fileProcessingJob;

    /**
     * Reference to the parent VSS- TCR0 record.
     */

    @Column(name = "parent_record_id", nullable = false)
    private UUID parentVssRecord;

    // === HEADER FIELDS (Positions 1-4) ===

    /**
     * Field 1: Transaction Code (positions 1-2).
     * Must be "46" for TCR1 records.
     */
    @NotBlank(message = "Transaction code cannot be blank")
    @Pattern(regexp = "^46$", message = "Transaction code must be '46'")
    @Column(name = "transaction_code", nullable = false, length = 2)
    private String transactionCode;

    /**
     * Field 2: Transaction Code Qualifier (position 3).
     * Always "0" for TCR1 records.
     */
    @Pattern(regexp = "^0$", message = "Transaction code qualifier must be '0'")
    @Column(name = "transaction_code_qualifier", length = 1)
    private String transactionCodeQualifier;

    /**
     * Field 3: Transaction Component Sequence Number (position 4).
     * Always "1" for TCR1 records (distinguishing from TCR0).
     */
    @NotBlank(message = "Transaction component sequence number cannot be blank")
    @Pattern(regexp = "^1$", message = "Transaction component sequence number must be '1' for TCR1")
    @Column(name = "transaction_component_seq_number", nullable = false, length = 1)
    private String transactionComponentSequenceNumber;

    // === IDENTIFICATION FIELDS (Positions 5-9) ===

    /**
     * Field 4: Destination Identifier (positions 5-10).
     * Must match the parent TCR0 record.
     */
    @NotBlank(message = "Destination ID cannot be blank")
    @Size(min = 6, max = 6, message = "Destination ID must be exactly 6 characters")
    @Column(name = "destination_id", nullable = false, length = 6)
    private String destinationId;

    /**
     * Field 5: Rate Table ID (positions 5-9).
     * Alpha and numeric values identifying the rate table.
     */
    @Size(max = 5, message = "Rate table ID cannot exceed 5 characters")
    @Column(name = "rate_table_id", length = 5)
    private String rateTableId;

    // === RESERVED FIELDS (Positions 10-11) ===

    /**
     * Field 6: Reserved (positions 10-11).
     * Always spaces.
     */
    @Size(max = 2, message = "Reserved field cannot exceed 2 characters")
    @Column(name = "reserved_field", length = 2)
    private String reservedField;

    // === COUNT AND AMOUNT FIELDS (Positions 12-143) ===

    /**
     * Field 7: First Count (positions 12-26).
     * 15-character count field.
     */
    @Column(name = "first_count")
    private Long firstCount;

    /**
     * Field 8: Second Count (positions 27-41).
     * 15-character count field.
     */
    @Column(name = "second_count")
    private Long secondCount;

    /**
     * Field 9: First Amount (positions 42-56).
     * 15-character amount with implied 2 decimal places.
     */
    @Column(name = "first_amount", precision = 15, scale = 2)
    private BigDecimal firstAmount;

    /**
     * Field 10: First Amount Sign (positions 57-58).
     * DB for debit or CR for credit.
     */
    @Size(max = 2, message = "First amount sign cannot exceed 2 characters")
    @Column(name = "first_amount_sign", length = 2)
    private String firstAmountSign;

    /**
     * Field 11: Second Amount (positions 59-73).
     * 15-character amount with implied 2 decimal places.
     */
    @Column(name = "second_amount", precision = 15, scale = 2)
    private BigDecimal secondAmount;

    /**
     * Field 12: Second Amount Sign (positions 74-75).
     * DB for debit or CR for credit.
     */
    @Size(max = 2, message = "Second amount sign cannot exceed 2 characters")
    @Column(name = "second_amount_sign", length = 2)
    private String secondAmountSign;

    /**
     * Field 13: Third Amount (positions 76-90).
     * 15-character amount with implied 2 decimal places.
     */
    @Column(name = "third_amount", precision = 15, scale = 2)
    private BigDecimal thirdAmount;

    /**
     * Field 14: Third Amount Sign (positions 91-92).
     * DB for debit or CR for credit.
     */
    @Size(max = 2, message = "Third amount sign cannot exceed 2 characters")
    @Column(name = "third_amount_sign", length = 2)
    private String thirdAmountSign;

    /**
     * Field 15: Fourth Amount (positions 93-107).
     * 15-character amount with implied 2 decimal places.
     */
    @Column(name = "fourth_amount", precision = 15, scale = 2)
    private BigDecimal fourthAmount;

    /**
     * Field 16: Fourth Amount Sign (positions 108-109).
     * DB for debit or CR for credit.
     */
    @Size(max = 2, message = "Fourth amount sign cannot exceed 2 characters")
    @Column(name = "fourth_amount_sign", length = 2)
    private String fourthAmountSign;

    /**
     * Field 17: Fifth Amount (positions 110-124).
     * 15-character amount with implied 2 decimal places.
     */
    @Column(name = "fifth_amount", precision = 15, scale = 2)
    private BigDecimal fifthAmount;

    /**
     * Field 18: Fifth Amount Sign (positions 125-126).
     * DB for debit or CR for credit.
     */
    @Size(max = 2, message = "Fifth amount sign cannot exceed 2 characters")
    @Column(name = "fifth_amount_sign", length = 2)
    private String fifthAmountSign;

    /**
     * Field 19: Sixth Amount (positions 127-141).
     * 15-character amount with implied 2 decimal places.
     */
    @Column(name = "sixth_amount", precision = 15, scale = 2)
    private BigDecimal sixthAmount;

    /**
     * Field 20: Sixth Amount Sign (positions 142-143).
     * DB for debit or CR for credit.
     */
    @Size(max = 2, message = "Sixth amount sign cannot exceed 2 characters")
    @Column(name = "sixth_amount_sign", length = 2)
    private String sixthAmountSign;

    // === RESERVED FIELD (Positions 144-168) ===

    /**
     * Field 21: Reserved (positions 144-168).
     * Always spaces - reserved for future use.
     */
    @Size(max = 25, message = "Reserved field 2 cannot exceed 25 characters")
    @Column(name = "reserved_field_2", length = 25)
    private String reservedField2;

    // === METADATA FIELDS ===

    /**
     * Complete raw record line as received (168 characters).
     * Preserved for audit trail and debugging.
     */
    @Column(name = "raw_record_line", columnDefinition = "TEXT")
    private String rawRecordLine;

    /**
     * Indicates if the record passed all validation checks.
     */
    @Column(name = "is_valid")
    private Boolean isValid = true;

    /**
     * Validation error messages if record validation failed.
     */
    @Column(name = "validation_errors", columnDefinition = "TEXT")
    private String validationErrors;

    /**
     * Line number in the source file for error reporting.
     */
    @Column(name = "line_number")
    private Integer lineNumber;

    @NotBlank(message = "Report ID number cannot be blank")
    @Pattern(regexp = "^(120|130|131|135|136|140|210|215|230|640)$",
            message = "Report ID number must be valid VSS-4 report type")
    @Column(name = "parent_report_number", nullable = false, length = 3)
    private String parentReportNumber;

    // === CONSTANTS ===

    public static final String TRANSACTION_CODE_TCR1 = "46";
    public static final String TRANSACTION_CODE_QUALIFIER_DEFAULT = "0";
    public static final String TRANSACTION_COMPONENT_SEQ_TCR1 = "1";

    // === CONSTRUCTORS ===

    /**
     * Default constructor for JPA.
     */
    protected Vss120Tcr1Record() {
        super();
    }

    /**
     * Creates a new Vss120Tcr1Record with the specified job reference.
     *
     * @param fileProcessingJob the associated file processing job
     */
    public Vss120Tcr1Record(FileProcessingJob fileProcessingJob) {
        this();
        this.fileProcessingJob = fileProcessingJob;
    }

    /**
     * Creates a new Vss120Tcr1Record with job and parent record references.
     *
     * @param fileProcessingJob the associated file processing job
     * @param parentVssRecord the parent VSS-120 TCR0 record
     */
    public Vss120Tcr1Record(FileProcessingJob fileProcessingJob, VssSubGroup4Record  parentVssRecord) {
        this(fileProcessingJob);
        this.parentVssRecord = parentVssRecord.getId();
        if (parentVssRecord != null) {
            this.destinationId = parentVssRecord.getDestinationId();
            this.parentReportNumber=parentVssRecord.getReportIdNumber();
        }
    }

    // === GETTERS AND SETTERS ===

    public FileProcessingJob getFileProcessingJob() {
        return fileProcessingJob;
    }

    public void setFileProcessingJob(FileProcessingJob fileProcessingJob) {
        this.fileProcessingJob = fileProcessingJob;
    }

    public UUID  getParentVssRecord() {
        return parentVssRecord;
    }

    public void setParentVssRecord(UUID  parentVssRecord) {
        this.parentVssRecord = parentVssRecord;
    }

    public String getTransactionCode() {
        return transactionCode;
    }

    public void setTransactionCode(String transactionCode) {
        this.transactionCode = transactionCode;
    }

    public String getParentReportNumber() {
        return parentReportNumber;
    }

    public void setParentReportNumber(String parentReportNumber) {
        this.parentReportNumber = parentReportNumber;
    }

    public String getTransactionCodeQualifier() {
        return transactionCodeQualifier;
    }

    public void setTransactionCodeQualifier(String transactionCodeQualifier) {
        this.transactionCodeQualifier = transactionCodeQualifier;
    }

    public String getTransactionComponentSequenceNumber() {
        return transactionComponentSequenceNumber;
    }

    public void setTransactionComponentSequenceNumber(String transactionComponentSequenceNumber) {
        this.transactionComponentSequenceNumber = transactionComponentSequenceNumber;
    }

    public String getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(String destinationId) {
        this.destinationId = destinationId;
    }

    public String getRateTableId() {
        return rateTableId;
    }

    public void setRateTableId(String rateTableId) {
        this.rateTableId = rateTableId;
    }

    public String getReservedField() {
        return reservedField;
    }

    public void setReservedField(String reservedField) {
        this.reservedField = reservedField;
    }

    public Long getFirstCount() {
        return firstCount;
    }

    public void setFirstCount(Long firstCount) {
        this.firstCount = firstCount;
    }

    public Long getSecondCount() {
        return secondCount;
    }

    public void setSecondCount(Long secondCount) {
        this.secondCount = secondCount;
    }

    public BigDecimal getFirstAmount() {
        return firstAmount;
    }

    public void setFirstAmount(BigDecimal firstAmount) {
        this.firstAmount = firstAmount;
    }

    public String getFirstAmountSign() {
        return firstAmountSign;
    }

    public void setFirstAmountSign(String firstAmountSign) {
        this.firstAmountSign = firstAmountSign;
    }

    public BigDecimal getSecondAmount() {
        return secondAmount;
    }

    public void setSecondAmount(BigDecimal secondAmount) {
        this.secondAmount = secondAmount;
    }

    public String getSecondAmountSign() {
        return secondAmountSign;
    }

    public void setSecondAmountSign(String secondAmountSign) {
        this.secondAmountSign = secondAmountSign;
    }

    public BigDecimal getThirdAmount() {
        return thirdAmount;
    }

    public void setThirdAmount(BigDecimal thirdAmount) {
        this.thirdAmount = thirdAmount;
    }

    public String getThirdAmountSign() {
        return thirdAmountSign;
    }

    public void setThirdAmountSign(String thirdAmountSign) {
        this.thirdAmountSign = thirdAmountSign;
    }

    public BigDecimal getFourthAmount() {
        return fourthAmount;
    }

    public void setFourthAmount(BigDecimal fourthAmount) {
        this.fourthAmount = fourthAmount;
    }

    public String getFourthAmountSign() {
        return fourthAmountSign;
    }

    public void setFourthAmountSign(String fourthAmountSign) {
        this.fourthAmountSign = fourthAmountSign;
    }

    public BigDecimal getFifthAmount() {
        return fifthAmount;
    }

    public void setFifthAmount(BigDecimal fifthAmount) {
        this.fifthAmount = fifthAmount;
    }

    public String getFifthAmountSign() {
        return fifthAmountSign;
    }

    public void setFifthAmountSign(String fifthAmountSign) {
        this.fifthAmountSign = fifthAmountSign;
    }

    public BigDecimal getSixthAmount() {
        return sixthAmount;
    }

    public void setSixthAmount(BigDecimal sixthAmount) {
        this.sixthAmount = sixthAmount;
    }

    public String getSixthAmountSign() {
        return sixthAmountSign;
    }

    public void setSixthAmountSign(String sixthAmountSign) {
        this.sixthAmountSign = sixthAmountSign;
    }

    public String getReservedField2() {
        return reservedField2;
    }

    public void setReservedField2(String reservedField2) {
        this.reservedField2 = reservedField2;
    }

    public String getRawRecordLine() {
        return rawRecordLine;
    }

    public void setRawRecordLine(String rawRecordLine) {
        this.rawRecordLine = rawRecordLine;
    }

    public Boolean getIsValid() {
        return isValid;
    }

    public void setIsValid(Boolean isValid) {
        this.isValid = isValid;
    }

    public String getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(String validationErrors) {
        this.validationErrors = validationErrors;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }

    // === BUSINESS LOGIC METHODS ===

    /**
     * Checks if the record is valid and ready for processing.
     *
     * @return true if valid, false otherwise
     */
    public boolean isValidForProcessing() {
        return isValid != null && isValid &&
                TRANSACTION_CODE_TCR1.equals(transactionCode) &&
                TRANSACTION_CODE_QUALIFIER_DEFAULT.equals(transactionCodeQualifier) &&
                TRANSACTION_COMPONENT_SEQ_TCR1.equals(transactionComponentSequenceNumber) &&
                destinationId != null && destinationId.length() == 6;
    }

    /**
     * Gets the signed first amount (applying the sign).
     *
     * @return the signed first amount
     */
    public BigDecimal getSignedFirstAmount() {
        if (firstAmount == null) return BigDecimal.ZERO;
        return "DB".equals(firstAmountSign) ? firstAmount.negate() : firstAmount;
    }

    /**
     * Gets the signed second amount (applying the sign).
     *
     * @return the signed second amount
     */
    public BigDecimal getSignedSecondAmount() {
        if (secondAmount == null) return BigDecimal.ZERO;
        return "DB".equals(secondAmountSign) ? secondAmount.negate() : secondAmount;
    }

    /**
     * Gets the signed third amount (applying the sign).
     *
     * @return the signed third amount
     */
    public BigDecimal getSignedThirdAmount() {
        if (thirdAmount == null) return BigDecimal.ZERO;
        return "DB".equals(thirdAmountSign) ? thirdAmount.negate() : thirdAmount;
    }

    /**
     * Gets the signed fourth amount (applying the sign).
     *
     * @return the signed fourth amount
     */
    public BigDecimal getSignedFourthAmount() {
        if (fourthAmount == null) return BigDecimal.ZERO;
        return "DB".equals(fourthAmountSign) ? fourthAmount.negate() : fourthAmount;
    }

    /**
     * Gets the signed fifth amount (applying the sign).
     *
     * @return the signed fifth amount
     */
    public BigDecimal getSignedFifthAmount() {
        if (fifthAmount == null) return BigDecimal.ZERO;
        return "DB".equals(fifthAmountSign) ? fifthAmount.negate() : fifthAmount;
    }

    /**
     * Gets the signed sixth amount (applying the sign).
     *
     * @return the signed sixth amount
     */
    public BigDecimal getSignedSixthAmount() {
        if (sixthAmount == null) return BigDecimal.ZERO;
        return "DB".equals(sixthAmountSign) ? sixthAmount.negate() : sixthAmount;
    }

    /**
     * Gets the total of all signed amounts.
     *
     * @return the sum of all signed amounts
     */
    public BigDecimal getTotalSignedAmount() {
        return getSignedFirstAmount()
                .add(getSignedSecondAmount())
                .add(getSignedThirdAmount())
                .add(getSignedFourthAmount())
                .add(getSignedFifthAmount())
                .add(getSignedSixthAmount());
    }

    /**
     * Gets the total of all counts.
     *
     * @return the sum of all counts
     */
    public Long getTotalCount() {
        long total = 0;
        if (firstCount != null) total += firstCount;
        if (secondCount != null) total += secondCount;
        return total;
    }

    /**
     * Gets a human-readable description of the record.
     *
     * @return formatted record description
     */
    public String getRecordDescription() {
        StringBuilder description = new StringBuilder();
        description.append(String.format("TCR1 [Dest: %s", destinationId));

        if (rateTableId != null && !rateTableId.trim().isEmpty()) {
            description.append(String.format(", Rate: %s", rateTableId.trim()));
        }

        description.append(String.format(", Counts: %d/%d",
                firstCount != null ? firstCount : 0,
                secondCount != null ? secondCount : 0));

        description.append(String.format(", Total Amount: %s", getTotalSignedAmount()));

        if (parentVssRecord != null) {
            description.append(String.format(", Parent: VSS-%s", parentReportNumber));
        }

        description.append("]");
        return description.toString();
    }

    /**
     * Validates that required header fields are set correctly for TCR1.
     *
     * @return true if all required header fields are valid
     */
    public boolean isHeaderValid() {
        return TRANSACTION_CODE_TCR1.equals(transactionCode) &&
                TRANSACTION_CODE_QUALIFIER_DEFAULT.equals(transactionCodeQualifier) &&
                TRANSACTION_COMPONENT_SEQ_TCR1.equals(transactionComponentSequenceNumber) &&
                destinationId != null && destinationId.matches("^\\d{6}$");
    }

    /**
     * Checks if all reserved fields contain expected values (spaces).
     *
     * @return true if reserved fields are properly formatted
     */
    public boolean areReservedFieldsValid() {
        boolean valid = true;

        if (reservedField != null && !reservedField.matches("^\\s*$")) {
            valid = false;
        }

        if (reservedField2 != null && !reservedField2.matches("^\\s*$")) {
            valid = false;
        }

        return valid;
    }

    @Override
    public String toString() {
        return "Vss120Tcr1Record{" +
                "id=" + getId() +
                ", lineNumber=" + lineNumber +
                ", transactionCode='" + transactionCode + '\'' +
                ", destinationId='" + destinationId + '\'' +
                ", rateTableId='" + rateTableId + '\'' +
                ", firstCount=" + firstCount +
                ", secondCount=" + secondCount +
                ", totalAmount=" + getTotalSignedAmount() +
                ", isValid=" + isValid +
                '}';
    }
}