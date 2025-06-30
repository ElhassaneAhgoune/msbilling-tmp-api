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
 * Enhanced entity representing a VSS-110 settlement record from Visa EPIN files with all 30 TCR0 fields.
 *
 * VSS-110 records contain settlement summary data based on the official Visa Settlement Service specification.
 * Each record represents settlement activity for a specific date with detailed financial information
 * including interchange fees, processing fees, chargebacks, and totals.
 *
 * Complete TCR0 Record Format (168 characters fixed-width) - All 30 Fields:
 * Field 1: Transaction Code (positions 1-2): Must be "46"
 * Field 2: Transaction Code Qualifier (position 3): Always "0"
 * Field 3: Transaction Component Sequence Number (position 4): Always "0"
 * Field 4: Destination Identifier (positions 5-10): 6-digit destination identifier
 * Field 5: Source Identifier (positions 11-16): Sending entity ID, usually zero
 * Field 6: Reporting SRE ID (positions 17-26): Settlement Reporting Entity identifier
 * Field 7: Rollup SRE ID (positions 27-36): Parent SRE in hierarchy
 * Field 8: Funds Transfer SRE ID (positions 37-46): Funds transfer entity SRE
 * Field 9: Settlement Service ID (positions 47-49): Settlement Service Code
 * Field 10: Settlement Currency Code (positions 50-52): ISO numeric currency code
 * Field 11: No Data Indicator (position 53).
 * V = Data not available, Y = Data available, space = normal processing
 * Field 12: Reserved (positions 54-58): Always spaces
 * Field 13: Report Group (position 59): Always "V"
 * Field 14: Report Subgroup (position 60): Always "2"
 * Field 15: Report ID Number (positions 61-63): "110" or "111"
 * Field 16: Report ID Suffix (positions 64-65): Space or "M"
 * Field 17: Settlement Date (positions 66-72): Format: CCYYDDD
 * Field 18: Report Date (positions 73-79): Format: CCYYDDD
 * Field 19: From Date (positions 80-86): Format: CCYYDDD
 * Field 20: To Date (positions 87-93): Format: CCYYDDD
 * Field 21: Amount Type (position 94): I, F, C, T, space
 * Field 22: Business Mode (position 95): 1=Acquirer, 2=Issuer, 3=Other, 9=Total
 * Field 23: Count (positions 96-110): Transaction count (if I), else space
 * Field 24: Credit Amount (positions 111-125): Credit amount
 * Field 25: Debit Amount (positions 126-140): Debit amount
 * Field 26: Net Amount (positions 141-155): Credit - Debit
 * Field 27: Net Amount Sign (positions 156-157): "CR" or "DB"
 * Field 28: Funds-Transfer Date (positions 158-164): Format: CCYDDD
 * Field 29: Reserved (positions 165-167): Always spaces
 * Field 30: Reimbursement Attribute (position 168): Obsolete – always zero-filled
 *
 * Business Rules:
 * - VSS-110 records (V2110): Detailed fee breakdown by category
 * - VSS-111 records (V2111): Summary totals only
 * - Amount Type: I=Interchange, F=Reimbursement Fees, C=Visa Charges, T=Total
 * - Business Mode: 1=Acquirer, 2=Issuer, 3=Other, 9=Total
 * - TC46, TCR 0, Report Group V, Subgroup 2 - Complete Record Set Analysis
 *
 * @author EL.AHGOUNE
 * @version 3.0.0
 * @since 2024
 */
@Entity
@Table(name = "vss110_settlement_records", indexes = {
        @Index(name = "idx_vss110_settlement_date", columnList = "settlement_date"),
        @Index(name = "idx_vss110_job_id", columnList = "job_id"),
        @Index(name = "idx_vss110_transaction_code", columnList = "transaction_code"),
        @Index(name = "idx_vss110_destination_id", columnList = "destination_id"),
        @Index(name = "idx_vss110_report_id", columnList = "report_id_number"),
        @Index(name = "idx_vss110_amount_type", columnList = "amount_type"),
        @Index(name = "idx_vss110_business_mode", columnList = "business_mode"),
        @Index(name = "idx_vss110_source_id", columnList = "source_identifier"),
        @Index(name = "idx_vss110_from_date", columnList = "from_date"),
        @Index(name = "idx_vss110_to_date", columnList = "to_date")
})
public class Vss110SettlementRecord extends BaseEntity {

    /**
     * Reference to the file processing job.
     */
    @NotNull(message = "File processing job cannot be null")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private FileProcessingJob fileProcessingJob;

    // === HEADER FIELDS (Positions 1-58) ===

    /**
     * Field 1: Transaction Code (positions 1-2).
     * Must be "46" for VSS-110/111 records.
     */
    @NotBlank(message = "Transaction code cannot be blank")
    @Pattern(regexp = "^46$", message = "Transaction code must be '46'")
    @Column(name = "transaction_code", nullable = false, length = 2)
    private String transactionCode;

    /**
     * Field 2: Transaction Code Qualifier (position 3).
     * Always "0" for VSS-110/111 records.
     */
    @Pattern(regexp = "^0$", message = "Transaction code qualifier must be '0'")
    @Column(name = "transaction_code_qualifier", length = 1)
    private String transactionCodeQualifier;

    /**
     * Field 3: Transaction Component Sequence Number (position 4).
     * Always "0" for VSS-110/111 records.
     */
    @Pattern(regexp = "^0$", message = "Transaction component sequence number must be '0'")
    @Column(name = "transaction_component_seq_number", length = 1)
    private String transactionComponentSequenceNumber;

    /**
     * Field 4: Destination ID (positions 5-10).
     * 6-digit identifier for the settlement destination.
     */
    @NotBlank(message = "Destination ID cannot be blank")
    @Size(min = 6, max = 6, message = "Destination ID must be exactly 6 characters")
    @Column(name = "destination_id", nullable = false, length = 6)
    private String destinationId;

    /**
     * Field 5: Source Identifier (positions 11-16).
     * Sending entity ID, usually zero.
     */
    @Size(max = 6, message = "Source identifier cannot exceed 6 characters")
    @Column(name = "source_identifier", length = 6)
    private String sourceIdentifier;

    /**
     * Field 6: Reporting SRE ID (positions 17-26).
     * Settlement Reporting Entity identifier (up to 100 chars, increased to handle longer identifiers).
     */
    @Size(max = 100, message = "Reporting SRE ID cannot exceed 100 characters")
    @Column(name = "reporting_sre_id", length = 100)
    private String reportingSreId;

    /**
     * Field 7: Rollup SRE ID (positions 27-36).
     * Settlement Reporting Entity for rollup purposes.
     */
    @Size(max = 100, message = "Rollup SRE ID cannot exceed 100 characters")
    @Column(name = "rollup_sre_id", length = 100)
    private String rollupSreId;

    /**
     * Field 8: Funds Transfer SRE ID (positions 37-46).
     * Settlement Reporting Entity for funds transfer.
     */
    @Size(max = 100, message = "Funds Transfer SRE ID cannot exceed 100 characters")
    @Column(name = "funds_transfer_sre_id", length = 100)
    private String fundsTransferSreId;

    /**
     * Field 9: Settlement Service Code (positions 47-49).
     * Service identifier for settlement processing (increased length to handle various service identifiers).
     */
    @Size(max = 20, message = "Settlement service cannot exceed 20 characters")
    @Column(name = "settlement_service", length = 20)
    private String settlementService;

    /**
     * Field 10: Settlement Currency Code (positions 50-52).
     * ISO 4217 numeric currency code (e.g., 978 for EUR, 840 for USD).
     */
    @Size(max = 3, message = "Currency code cannot exceed 3 characters")
    @Column(name = "currency_code", length = 3)
    private String currencyCode = "978"; // Default to EUR as per VSS 110 spec

    /**
     * Field 11: No Data Indicator (position 53).
     * V = Data not available, Y = Data available, space = normal processing
     */
    @Pattern(regexp = "^[VY ]?$", message = "No data indicator must be 'V', 'Y', or space")
    @Column(name = "no_data_indicator", length = 1)
    private String noDataIndicator;

    /**
     * Field 12: Reserved (positions 54-58).
     * Always spaces - stored for audit purposes.
     */
    @Size(max = 5, message = "Reserved field 1 cannot exceed 5 characters")
    @Column(name = "reserved_field_1", length = 5)
    private String reservedField1;

    // === REPORT IDENTIFICATION (Positions 59-65) ===

    /**
     * Field 13: Report Group (position 59).
     * Must be "V" for Visa reports.
     */
    @NotBlank(message = "Report group cannot be blank")
    @Pattern(regexp = "^V$", message = "Report group must be 'V'")
    @Column(name = "report_group", nullable = false, length = 1)
    private String reportGroup;

    /**
     * Field 14: Report Subgroup (position 60).
     * Must be "2" for VSS-110/111 reports.
     */
    @NotBlank(message = "Report subgroup cannot be blank")
    @Pattern(regexp = "^2$", message = "Report subgroup must be '2'")
    @Column(name = "report_subgroup", nullable = false, length = 1)
    private String reportSubgroup;

    /**
     * Field 15: Report ID Number (positions 61-63).
     * "110" for detailed records, "111" for summary records.
     */
    @NotBlank(message = "Report ID number cannot be blank")
    @Pattern(regexp = "^(110|111)$", message = "Report ID number must be '110' or '111'")
    @Column(name = "report_id_number", nullable = false, length = 3)
    private String reportIdNumber;

    /**
     * Field 16: Report Identification Suffix (positions 64-65).
     * Space or "M" - additional report qualifier.
     */
    @Size(max = 2, message = "Report ID suffix cannot exceed 2 characters")
    @Column(name = "report_id_suffix", length = 2)
    private String reportIdSuffix;

    // === DATE FIELDS (Positions 66-93) ===

    /**
     * Field 17: Settlement Date (positions 66-72).
     * Date in CCYYDDD format (century, year, day of year).
     */
    @NotNull(message = "Settlement date cannot be null")
    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    /**
     * Raw settlement date string as it appears in the file.
     * Preserved for audit and debugging purposes.
     * Can be 5-7 digits to accommodate both YYDDD and CCYYDDD formats.
     */
    @NotBlank(message = "Raw settlement date cannot be blank")
    @Size(min = 5, max = 7, message = "Raw settlement date must be 5-7 characters")
    @Pattern(regexp = "^[0-9]{5,7}$", message = "Raw settlement date must contain only digits")
    @Column(name = "raw_settlement_date", nullable = false, length = 7)
    private String rawSettlementDate;

    /**
     * Field 18: Report Date (positions 73-79).
     * Date when the report was generated in CCYYDDD format.
     */
    @Column(name = "report_date")
    private LocalDate reportDate;

    /**
     * Raw report date string as it appears in the file.
     */
    @Size(max = 7, message = "Raw report date cannot exceed 7 characters")
    @Column(name = "raw_report_date", length = 7)
    private String rawReportDate;

    /**
     * Field 19: From Date (positions 80-86).
     * Start date of the reporting period in CCYYDDD format.
     */
    @Column(name = "from_date")
    private LocalDate fromDate;

    /**
     * Raw from date string as it appears in the file.
     */
    @Size(max = 7, message = "Raw from date cannot exceed 7 characters")
    @Column(name = "raw_from_date", length = 7)
    private String rawFromDate;

    /**
     * Field 20: To Date (positions 87-93).
     * End date of the reporting period in CCYYDDD format.
     */
    @Column(name = "to_date")
    private LocalDate toDate;

    /**
     * Raw to date string as it appears in the file.
     */
    @Size(max = 7, message = "Raw to date cannot exceed 7 characters")
    @Column(name = "raw_to_date", length = 7)
    private String rawToDate;

    // === BUSINESS CLASSIFICATION (Positions 94-95) ===

    /**
     * Field 21: Amount Type (position 94).
     * For VSS-110: I=Interchange, F=Reimbursement Fees, C=Visa Charges, T=Total
     * For VSS-111: Space or T=Total only
     */
    @Pattern(regexp = "^[IFCT ]$", message = "Amount type must be I, F, C, T, or space")
    @Column(name = "amount_type", length = 1)
    private String amountType;

    /**
     * Field 22: Business Mode (position 95).
     * 1=Acquirer, 2=Issuer, 3=Other, 9=Total
     */
    @Pattern(regexp = "^[1239 ]$", message = "Business mode must be 1, 2, 3, 9, or space")
    @Column(name = "business_mode", length = 1)
    private String businessMode;

    // === FINANCIAL AMOUNTS (Positions 96-157) ===

    /**
     * Field 23: Transaction Count (positions 96-110).
     * Number of transactions contributing to the amounts.
     */
    @Column(name = "transaction_count")
    private Integer transactionCount;

    /**
     * Field 24: Credit Amount (positions 111-125).
     * 15-character amount with implied 2 decimal places.
     */
    @Column(name = "credit_amount", precision = 15, scale = 2)
    private BigDecimal creditAmount;

    /**
     * Field 25: Debit Amount (positions 126-140).
     * 15-character amount with implied 2 decimal places.
     */
    @Column(name = "debit_amount", precision = 15, scale = 2)
    private BigDecimal debitAmount;

    /**
     * Field 26: Net Amount (positions 141-155).
     * 15-character amount with implied 2 decimal places.
     * Calculated as Credit Amount - Debit Amount.
     */
    @Column(name = "net_amount", precision = 15, scale = 2)
    private BigDecimal netAmount;

    /**
     * Field 27: Amount sign indicator (positions 156-157).
     * CR for Credit, DB for Debit.
     */
    @Size(max = 2, message = "Amount sign cannot exceed 2 characters")
    @Column(name = "amount_sign", length = 2)
    private String amountSign;

    // === ADDITIONAL FIELDS (Positions 158-168) ===

    /**
     * Field 28: Funds-Transfer Date (positions 158-164).
     * Date of funds transfer in CCYDDD format (6 digits).
     */
    @Column(name = "funds_transfer_date")
    private LocalDate fundsTransferDate;

    /**
     * Raw funds transfer date string as it appears in the file.
     */
    @Size(max = 7, message = "Raw funds transfer date cannot exceed 7 characters")
    @Column(name = "raw_funds_transfer_date", length = 7)
    private String rawFundsTransferDate;

    /**
     * Field 29: Reserved (positions 165-167).
     * Always spaces - stored for audit purposes.
     */
    @Size(max = 3, message = "Reserved field 2 cannot exceed 3 characters")
    @Column(name = "reserved_field_2", length = 3)
    private String reservedField2;

    /**
     * Field 30: Reimbursement Attribute (position 168).
     * Obsolete field – always zero-filled.
     */
    @Size(max = 1, message = "Reimbursement attribute cannot exceed 1 character")
    @Column(name = "reimbursement_attribute", length = 1)
    private String reimbursementAttribute;

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

    // === CONSTANTS ===

    public static final String TRANSACTION_CODE_VSS110 = "46";
    public static final String TRANSACTION_CODE_QUALIFIER_DEFAULT = "0";
    public static final String TRANSACTION_COMPONENT_SEQ_DEFAULT = "0";
    public static final String REPORT_GROUP_VISA = "V";
    public static final String REPORT_SUBGROUP_VSS = "2";
    public static final String REPORT_ID_DETAILED = "110";
    public static final String REPORT_ID_SUMMARY = "111";

    // Amount Type constants for VSS 110 (TC46, TCR 0, Report Group V, Subgroup 2)
    public static final String AMOUNT_TYPE_INTERCHANGE = "I";
    public static final String AMOUNT_TYPE_REIMBURSEMENT_FEES = "F";
    public static final String AMOUNT_TYPE_VISA_CHARGES = "C";
    public static final String AMOUNT_TYPE_TOTAL = "T";
    public static final String AMOUNT_TYPE_SPACE = " ";

    public static final List<String> VALID_AMOUNT_TYPES_110 =
            Arrays.asList(AMOUNT_TYPE_INTERCHANGE, AMOUNT_TYPE_REIMBURSEMENT_FEES,
                    AMOUNT_TYPE_VISA_CHARGES, AMOUNT_TYPE_TOTAL, AMOUNT_TYPE_SPACE);

    public static final List<String> VALID_AMOUNT_TYPES_111 =
            Arrays.asList(AMOUNT_TYPE_SPACE, AMOUNT_TYPE_TOTAL);

    // Business Mode constants
    public static final String BUSINESS_MODE_ACQUIRER = "1";
    public static final String BUSINESS_MODE_ISSUER = "2";
    public static final String BUSINESS_MODE_OTHER = "3";
    public static final String BUSINESS_MODE_TOTAL = "9";

    public static final List<String> VALID_BUSINESS_MODES =
            Arrays.asList(BUSINESS_MODE_ACQUIRER, BUSINESS_MODE_ISSUER,
                    BUSINESS_MODE_OTHER, BUSINESS_MODE_TOTAL, " ");

    // === CONSTRUCTORS ===

    /**
     * Default constructor for JPA.
     */
    protected Vss110SettlementRecord() {
        super();
    }

    /**
     * Creates a new Vss110SettlementRecord with the specified job reference.
     *
     * @param fileProcessingJob the associated file processing job
     */
    public Vss110SettlementRecord(FileProcessingJob fileProcessingJob) {
        this();
        this.fileProcessingJob = fileProcessingJob;
    }

    // === GETTERS AND SETTERS ===

    public FileProcessingJob getFileProcessingJob() {
        return fileProcessingJob;
    }

    public void setFileProcessingJob(FileProcessingJob fileProcessingJob) {
        this.fileProcessingJob = fileProcessingJob;
    }

    public String getTransactionCode() {
        return transactionCode;
    }

    public void setTransactionCode(String transactionCode) {
        this.transactionCode = transactionCode;
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

    public String getSourceIdentifier() {
        return sourceIdentifier;
    }

    public void setSourceIdentifier(String sourceIdentifier) {
        this.sourceIdentifier = sourceIdentifier;
    }

    public String getReportingSreId() {
        return reportingSreId;
    }

    public void setReportingSreId(String reportingSreId) {
        this.reportingSreId = reportingSreId;
    }

    public String getRollupSreId() {
        return rollupSreId;
    }

    public void setRollupSreId(String rollupSreId) {
        this.rollupSreId = rollupSreId;
    }

    public String getFundsTransferSreId() {
        return fundsTransferSreId;
    }

    public void setFundsTransferSreId(String fundsTransferSreId) {
        this.fundsTransferSreId = fundsTransferSreId;
    }

    public String getSettlementService() {
        return settlementService;
    }

    public void setSettlementService(String settlementService) {
        this.settlementService = settlementService;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getNoDataIndicator() {
        return noDataIndicator;
    }

    public void setNoDataIndicator(String noDataIndicator) {
        this.noDataIndicator = noDataIndicator;
    }

    public String getReservedField1() {
        return reservedField1;
    }

    public void setReservedField1(String reservedField1) {
        this.reservedField1 = reservedField1;
    }

    public String getReportGroup() {
        return reportGroup;
    }

    public void setReportGroup(String reportGroup) {
        this.reportGroup = reportGroup;
    }

    public String getReportSubgroup() {
        return reportSubgroup;
    }

    public void setReportSubgroup(String reportSubgroup) {
        this.reportSubgroup = reportSubgroup;
    }

    public String getReportIdNumber() {
        return reportIdNumber;
    }

    public void setReportIdNumber(String reportIdNumber) {
        this.reportIdNumber = reportIdNumber;
    }

    public String getReportIdSuffix() {
        return reportIdSuffix;
    }

    public void setReportIdSuffix(String reportIdSuffix) {
        this.reportIdSuffix = reportIdSuffix;
    }

    public LocalDate getSettlementDate() {
        return settlementDate;
    }

    public void setSettlementDate(LocalDate settlementDate) {
        this.settlementDate = settlementDate;
    }

    public String getRawSettlementDate() {
        return rawSettlementDate;
    }

    public void setRawSettlementDate(String rawSettlementDate) {
        this.rawSettlementDate = rawSettlementDate;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    public String getRawReportDate() {
        return rawReportDate;
    }

    public void setRawReportDate(String rawReportDate) {
        this.rawReportDate = rawReportDate;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    public String getRawFromDate() {
        return rawFromDate;
    }

    public void setRawFromDate(String rawFromDate) {
        this.rawFromDate = rawFromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }

    public String getRawToDate() {
        return rawToDate;
    }

    public void setRawToDate(String rawToDate) {
        this.rawToDate = rawToDate;
    }

    public String getAmountType() {
        return amountType;
    }

    public void setAmountType(String amountType) {
        this.amountType = amountType;
    }

    public String getBusinessMode() {
        return businessMode;
    }

    public void setBusinessMode(String businessMode) {
        this.businessMode = businessMode;
    }

    public Integer getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(Integer transactionCount) {
        this.transactionCount = transactionCount;
    }

    public BigDecimal getCreditAmount() {
        return creditAmount;
    }

    public void setCreditAmount(BigDecimal creditAmount) {
        this.creditAmount = creditAmount;
    }

    public BigDecimal getDebitAmount() {
        return debitAmount;
    }

    public void setDebitAmount(BigDecimal debitAmount) {
        this.debitAmount = debitAmount;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
    }

    public String getAmountSign() {
        return amountSign;
    }

    public void setAmountSign(String amountSign) {
        this.amountSign = amountSign;
    }

    public LocalDate getFundsTransferDate() {
        return fundsTransferDate;
    }

    public void setFundsTransferDate(LocalDate fundsTransferDate) {
        this.fundsTransferDate = fundsTransferDate;
    }

    public String getRawFundsTransferDate() {
        return rawFundsTransferDate;
    }

    public void setRawFundsTransferDate(String rawFundsTransferDate) {
        this.rawFundsTransferDate = rawFundsTransferDate;
    }

    public String getReservedField2() {
        return reservedField2;
    }

    public void setReservedField2(String reservedField2) {
        this.reservedField2 = reservedField2;
    }

    public String getReimbursementAttribute() {
        return reimbursementAttribute;
    }

    public void setReimbursementAttribute(String reimbursementAttribute) {
        this.reimbursementAttribute = reimbursementAttribute;
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
     * Checks if this is an interchange record (VSS-110 with amount type I).
     *
     * @return true if this is an interchange record
     */
    public boolean isInterchangeRecord() {
        return REPORT_ID_DETAILED.equals(reportIdNumber) &&
                AMOUNT_TYPE_INTERCHANGE.equals(amountType);
    }

    /**
     * Checks if this is a reimbursement fee record (VSS-110 with amount type F).
     *
     * @return true if this is a reimbursement fee record
     */
    public boolean isReimbursementFeesRecord() {
        return REPORT_ID_DETAILED.equals(reportIdNumber) &&
                AMOUNT_TYPE_REIMBURSEMENT_FEES.equals(amountType);
    }

    /**
     * Checks if this is a Visa charges record (VSS-110 with amount type C).
     *
     * @return true if this is a Visa charges record
     */
    public boolean isVisaChargesRecord() {
        return REPORT_ID_DETAILED.equals(reportIdNumber) &&
                AMOUNT_TYPE_VISA_CHARGES.equals(amountType);
    }

    /**
     * Checks if this is a total record (amount type T or VSS-111).
     *
     * @return true if this is a total record
     */
    public boolean isTotalRecord() {
        return AMOUNT_TYPE_TOTAL.equals(amountType) ||
                REPORT_ID_SUMMARY.equals(reportIdNumber);
    }

    /**
     * Checks if this is a detailed VSS-110 record.
     *
     * @return true if this is a VSS-110 record
     */
    public boolean isDetailedRecord() {
        return REPORT_ID_DETAILED.equals(reportIdNumber);
    }

    /**
     * Checks if this is a summary VSS-111 record.
     *
     * @return true if this is a VSS-111 record
     */
    public boolean isSummaryRecord() {
        return REPORT_ID_SUMMARY.equals(reportIdNumber);
    }

    /**
     * Checks if this record represents a reporting period (has from/to dates).
     *
     * @return true if this record has reporting period dates
     */
    public boolean hasReportingPeriod() {
        return fromDate != null && toDate != null;
    }

    /**
     * Gets the reporting period length in days.
     *
     * @return number of days in the reporting period, or 0 if not available
     */
    public long getReportingPeriodDays() {
        if (fromDate != null && toDate != null) {
            return java.time.temporal.ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        }
        return 0;
    }

    /**
     * Checks if funds transfer date is different from settlement date.
     *
     * @return true if funds transfer date differs from settlement date
     */
    public boolean hasDifferentFundsTransferDate() {
        return fundsTransferDate != null && settlementDate != null &&
                !fundsTransferDate.equals(settlementDate);
    }

    /**
     * Gets the business mode description.
     *
     * @return human-readable business mode description
     */
    public String getBusinessModeDescription() {
        if (businessMode == null) return "Unknown";

        return switch (businessMode) {
            case BUSINESS_MODE_ACQUIRER -> "Acquirer";
            case BUSINESS_MODE_ISSUER -> "Issuer";
            case BUSINESS_MODE_OTHER -> "Other";
            case BUSINESS_MODE_TOTAL -> "Total";
            default -> "Unknown";
        };
    }

    /**
     * Gets the amount type description.
     *
     * @return human-readable amount type description
     */
    public String getAmountTypeDescription() {
        if (amountType == null) return "Unknown";

        return switch (amountType) {
            case AMOUNT_TYPE_INTERCHANGE -> "Interchange";
            case AMOUNT_TYPE_REIMBURSEMENT_FEES -> "Reimbursement Fees";
            case AMOUNT_TYPE_VISA_CHARGES -> "Visa Charges";
            case AMOUNT_TYPE_TOTAL -> "Total";
            case AMOUNT_TYPE_SPACE -> "Summary";
            default -> "Unknown";
        };
    }

    /**
     * Checks if the record is valid and ready for processing.
     *
     * @return true if valid, false otherwise
     */
    public boolean isValidForProcessing() {
        return isValid != null && isValid &&
                TRANSACTION_CODE_VSS110.equals(transactionCode) &&
                destinationId != null && destinationId.length() == 6 &&
                settlementDate != null &&
                REPORT_GROUP_VISA.equals(reportGroup) &&
                REPORT_SUBGROUP_VSS.equals(reportSubgroup) &&
                (REPORT_ID_DETAILED.equals(reportIdNumber) || REPORT_ID_SUMMARY.equals(reportIdNumber)) &&
                VALID_BUSINESS_MODES.contains(businessMode);
    }

    /**
     * Gets the absolute net amount value.
     *
     * @return the absolute net amount
     */
    public BigDecimal getAbsoluteNetAmount() {
        return netAmount != null ? netAmount.abs() : BigDecimal.ZERO;
    }

    /**
     * Checks if this record has a net credit balance.
     *
     * @return true if net amount is positive
     */
    public boolean hasNetCredit() {
        return netAmount != null && netAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Checks if this record has a net debit balance.
     *
     * @return true if net amount is negative
     */
    public boolean hasNetDebit() {
        return netAmount != null && netAmount.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Checks if the no data indicator shows no data is available.
     *
     * @return true if no data indicator is "V"
     */
    public boolean hasNoData() {
        return "V".equals(noDataIndicator);
    }

    /**
     * Gets a human-readable description of the record.
     *
     * @return formatted record description
     */
    public String getRecordDescription() {
        StringBuilder description = new StringBuilder();
        description.append(String.format("VSS-%s [Dest: %s", reportIdNumber, destinationId));

        if (sourceIdentifier != null && !sourceIdentifier.trim().isEmpty() &&
                !sourceIdentifier.equals("000000")) {
            description.append(String.format(", Src: %s", sourceIdentifier));
        }

        description.append(String.format(", Date: %s",
                settlementDate != null ? settlementDate.toString() : "N/A"));

        if (hasReportingPeriod()) {
            description.append(String.format(", Period: %s to %s (%d days)",
                    fromDate, toDate, getReportingPeriodDays()));
        }

        description.append(String.format(", Mode: %s (%s)",
                businessMode, getBusinessModeDescription()));

        description.append(String.format(", Type: %s (%s)",
                amountType != null ? amountType : "N/A", getAmountTypeDescription()));

        description.append(String.format(", Net: %s %s",
                netAmount != null ? netAmount.toString() : "0.00",
                currencyCode != null ? currencyCode : "USD"));

        if (hasDifferentFundsTransferDate()) {
            description.append(String.format(", FT Date: %s", fundsTransferDate));
        }

        if (hasNoData()) {
            description.append(", [NO DATA]");
        }

        description.append("]");
        return description.toString();
    }

    /**
     * Validates amount type against report ID number.
     *
     * @return true if amount type is valid for the report ID
     */
    public boolean isAmountTypeValidForReportId() {
        if (reportIdNumber == null || amountType == null) {
            return false;
        }

        if (REPORT_ID_DETAILED.equals(reportIdNumber)) {
            return VALID_AMOUNT_TYPES_110.contains(amountType);
        } else if (REPORT_ID_SUMMARY.equals(reportIdNumber)) {
            return VALID_AMOUNT_TYPES_111.contains(amountType);
        }

        return false;
    }

    /**
     * Validates that required TCR0 header fields are set correctly.
     *
     * @return true if all required header fields are valid
     */
    public boolean isHeaderValid() {
        return TRANSACTION_CODE_VSS110.equals(transactionCode) &&
                TRANSACTION_CODE_QUALIFIER_DEFAULT.equals(transactionCodeQualifier) &&
                TRANSACTION_COMPONENT_SEQ_DEFAULT.equals(transactionComponentSequenceNumber) &&
                destinationId != null && destinationId.matches("^\\d{6}$") &&
                REPORT_GROUP_VISA.equals(reportGroup) &&
                REPORT_SUBGROUP_VSS.equals(reportSubgroup) &&
                (REPORT_ID_DETAILED.equals(reportIdNumber) || REPORT_ID_SUMMARY.equals(reportIdNumber));
    }

    /**
     * Calculates and validates the net amount based on credit and debit amounts.
     * Updates the net amount field if both credit and debit amounts are present.
     */
    public void calculateNetAmount() {
        if (creditAmount != null && debitAmount != null) {
            this.netAmount = creditAmount.subtract(debitAmount);
        } else if (creditAmount != null && debitAmount == null) {
            this.netAmount = creditAmount;
        } else if (creditAmount == null && debitAmount != null) {
            this.netAmount = debitAmount.negate();
        }

        // Update amount sign based on calculated net amount
        if (netAmount != null) {
            if (netAmount.compareTo(BigDecimal.ZERO) >= 0) {
                this.amountSign = "CR";
            } else {
                this.amountSign = "DB";
            }
        }
    }

    /**
     * Gets a summary of all date fields in the record.
     *
     * @return formatted string showing all dates
     */
    public String getDateSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Dates: Settlement=").append(settlementDate);

        if (reportDate != null) {
            summary.append(", Report=").append(reportDate);
        }

        if (hasReportingPeriod()) {
            summary.append(", Period=").append(fromDate).append(" to ").append(toDate);
        }

        if (fundsTransferDate != null) {
            summary.append(", FundsTransfer=").append(fundsTransferDate);
        }

        return summary.toString();
    }

    /**
     * Gets a summary of all identifier fields in the record.
     *
     * @return formatted string showing all identifiers
     */
    public String getIdentifierSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("IDs: Dest=").append(destinationId);

        if (sourceIdentifier != null && !sourceIdentifier.trim().isEmpty() &&
                !sourceIdentifier.equals("000000")) {
            summary.append(", Src=").append(sourceIdentifier);
        }

        if (reportingSreId != null && !reportingSreId.trim().isEmpty()) {
            summary.append(", ReportSRE=").append(reportingSreId.trim());
        }

        if (rollupSreId != null && !rollupSreId.trim().isEmpty()) {
            summary.append(", RollupSRE=").append(rollupSreId.trim());
        }

        if (fundsTransferSreId != null && !fundsTransferSreId.trim().isEmpty()) {
            summary.append(", FTSRE=").append(fundsTransferSreId.trim());
        }

        return summary.toString();
    }

    /**
     * Checks if all reserved fields contain expected values (spaces/zeros).
     *
     * @return true if reserved fields are properly formatted
     */
    public boolean areReservedFieldsValid() {
        boolean valid = true;

        if (reservedField1 != null && !reservedField1.matches("^\\s*$")) {
            valid = false;
        }

        if (reservedField2 != null && !reservedField2.matches("^\\s*$")) {
            valid = false;
        }

        if (reimbursementAttribute != null &&
                !reimbursementAttribute.equals("0") &&
                !reimbursementAttribute.equals(" ")) {
            valid = false;
        }

        return valid;
    }

    @Override
    public String toString() {
        return "Vss110SettlementRecord{" +
                "id=" + getId() +
                ", lineNumber=" + lineNumber +
                ", transactionCode='" + transactionCode + '\'' +
                ", destinationId='" + destinationId + '\'' +
                ", sourceIdentifier='" + sourceIdentifier + '\'' +
                ", settlementDate=" + settlementDate +
                ", reportIdNumber='" + reportIdNumber + '\'' +
                ", amountType='" + amountType + '\'' +
                ", businessMode='" + businessMode + '\'' +
                ", netAmount=" + netAmount +
                ", amountSign='" + amountSign + '\'' +
                ", isValid=" + isValid +
                '}';
    }
}