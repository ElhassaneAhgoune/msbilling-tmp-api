package com.moneysab.cardexis.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;


@MappedSuperclass
public abstract class VssSubGroup4Record extends  BaseEntity{
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
     * Must be "46" for VSS-SubGroup4 records.
     */
    @NotBlank(message = "Transaction code cannot be blank")
    @Pattern(regexp = "^46$", message = "Transaction code must be '46'")
    @Column(name = "transaction_code", nullable = false, length = 2)
    private String transactionCode;

    /**
     * Field 2: Transaction Code Qualifier (position 3).
     * Always "0" for VSS-SubGroup4 records.
     */
    @Pattern(regexp = "^0$", message = "Transaction code qualifier must be '0'")
    @Column(name = "transaction_code_qualifier", length = 1)
    private String transactionCodeQualifier;

    /**
     * Field 3: Transaction Component Sequence Number (position 4).
     * Always "0" for VSS-SubGroup4 records.
     */
    @Pattern(regexp = "^0$", message = "Transaction component sequence number must be '0'")
    @Column(name = "transaction_component_seq_number", length = 1)
    private String transactionComponentSequenceNumber;

    /**
     * Field 4: Destination Identifier (positions 5-10).
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
     * Settlement Reporting Entity identifier.
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
     * Service identifier for settlement processing.
     */
    @Size(max = 20, message = "Settlement service cannot exceed 20 characters")
    @Column(name = "settlement_service", length = 20)
    private String settlementService;

    /**
     * Field 10: Settlement Currency Code (positions 50-52).
     * ISO 4217 numeric currency code.
     */
    @Size(max = 3, message = "Currency code cannot exceed 3 characters")
    @Column(name = "settlement_currency_code", length = 3)
    private String settlementCurrencyCode = "978"; // Default to EUR

    /**
     * Field 11: Clearing Currency Code (positions 53-55).
     * ISO 4217 numeric currency code for clearing.
     */
    @Size(max = 3, message = "Clearing currency code cannot exceed 3 characters")
    @Column(name = "clearing_currency_code", length = 3)
    private String clearingCurrencyCode;

    /**
     * Field 12: Business Mode (position 56).
     * 1=Acquirer, 2=Issuer, 3=Other, 9=Total
     */
    @Pattern(regexp = "^[1239 ]$", message = "Business mode must be 1, 2, 3, 9, or space")
    @Column(name = "business_mode", length = 1)
    private String businessMode;

    /**
     * Field 13: No Data Indicator (position 57).
     * Y = No data, or space.
     */
    @Pattern(regexp = "^[Y ]?$", message = "No data indicator must be 'Y' or space")
    @Column(name = "no_data_indicator", length = 1)
    private String noDataIndicator;

    /**
     * Field 14: Reserved (position 58).
     * Always space.
     */
    @Size(max = 1, message = "Reserved field cannot exceed 1 character")
    @Column(name = "reserved_field", length = 1)
    private String reservedField;

    // === REPORT IDENTIFICATION (Positions 59-65) ===

    /**
     * Field 15: Report Group (position 59).
     * Must be "V" for Visa reports.
     */
    @NotBlank(message = "Report group cannot be blank")
    @Pattern(regexp = "^V$", message = "Report group must be 'V'")
    @Column(name = "report_group", nullable = false, length = 1)
    private String reportGroup;

    /**
     * Field 16: Report Subgroup (position 60).
     * Must be "4" for VSS-SubGroup4 reports.
     */
    @NotBlank(message = "Report subgroup cannot be blank")
    @Pattern(regexp = "^4$", message = "Report subgroup must be '4'")
    @Column(name = "report_subgroup", nullable = false, length = 1)
    private String reportSubgroup;

    /**
     * Field 17: Report ID Number (positions 61-63).
     * "120", "130", "131", "135", "136", "140", "210", "215", "230", or "640".
     */
    @NotBlank(message = "Report ID number cannot be blank")
    @Pattern(regexp = "^(120|130|131|135|136|140|210|215|230|640)$",
            message = "Report ID number must be valid VSS-4 report type")
    @Column(name = "report_id_number", nullable = false, length = 3)
    private String reportIdNumber;

    /**
     * Field 18: Report Identification Suffix (positions 64-65).
     * Space or "M" - additional report qualifier.
     */
    @Size(max = 2, message = "Report ID suffix cannot exceed 2 characters")
    @Column(name = "report_id_suffix", length = 2)
    private String reportIdSuffix;

    // === DATE FIELDS (Positions 66-93) ===

    /**
     * Field 19: Settlement Date (positions 66-72).
     * Date in CCYYDDD format.
     */
    @NotNull(message = "Settlement date cannot be null")
    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    /**
     * Raw settlement date string as it appears in the file.
     */
    @NotBlank(message = "Raw settlement date cannot be blank")
    @Size(min = 5, max = 7, message = "Raw settlement date must be 5-7 characters")
    @Pattern(regexp = "^[0-9]{5,7}$", message = "Raw settlement date must contain only digits")
    @Column(name = "raw_settlement_date", nullable = false, length = 7)
    private String rawSettlementDate;

    /**
     * Field 20: Report Date (positions 73-79).
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
     * Field 21: From Date (positions 80-86).
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
     * Field 22: To Date (positions 87-93).
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

    // === VSS-SubGroup4 SPECIFIC FIELDS (Positions 94-168) ===

    /**
     * Field 23: Charge Type Code (positions 94-96).
     * Type of charge being reported.
     */
    @Size(max = 3, message = "Charge type code cannot exceed 3 characters")
    @Column(name = "charge_type_code", length = 3)
    private String chargeTypeCode;

    /**
     * Field 24: Business Transaction Type (positions 97-99).
     * Type of business transaction.
     */
    @Size(max = 3, message = "Business transaction type cannot exceed 3 characters")
    @Column(name = "business_transaction_type", length = 3)
    private String businessTransactionType;

    /**
     * Field 25: Business Transaction Cycle (position 100).
     * Transaction cycle indicator.
     */
    @Size(max = 1, message = "Business transaction cycle cannot exceed 1 character")
    @Column(name = "business_transaction_cycle", length = 1)
    private String businessTransactionCycle;

    /**
     * Field 26: Reversal Indicator (position 101).
     * Y or N for reversal transactions.
     */
    @Pattern(regexp = "^[YN ]?$", message = "Reversal indicator must be 'Y', 'N', or space")
    @Column(name = "reversal_indicator", length = 1)
    private String reversalIndicator;

    /**
     * Field 27: Return Indicator (position 102).
     * Y or N for return transactions.
     */
    @Pattern(regexp = "^[YN ]?$", message = "Return indicator must be 'Y', 'N', or space")
    @Column(name = "return_indicator", length = 1)
    private String returnIndicator;

    /**
     * Field 28: Jurisdiction Code (positions 103-104).
     * Jurisdiction where the transaction occurred.
     */
    @Size(max = 2, message = "Jurisdiction code cannot exceed 2 characters")
    @Column(name = "jurisdiction_code", length = 2)
    private String jurisdictionCode;

    /**
     * Field 29: Interregional Routing Indicator (position 105).
     * Y or N for interregional routing.
     */
    @Pattern(regexp = "^[YN ]?$", message = "Interregional routing indicator must be 'Y', 'N', or space")
    @Column(name = "interregional_routing_indicator", length = 1)
    private String interregionalRoutingIndicator;

    /**
     * Field 30: Source Country Code (positions 106-108).
     * ISO country code for source.
     */
    @Size(max = 3, message = "Source country code cannot exceed 3 characters")
    @Column(name = "source_country_code", length = 3)
    private String sourceCountryCode;

    /**
     * Field 31: Destination Country Code (positions 109-111).
     * ISO country code for destination.
     */
    @Size(max = 3, message = "Destination country code cannot exceed 3 characters")
    @Column(name = "destination_country_code", length = 3)
    private String destinationCountryCode;

    /**
     * Field 32: Source Region Code (positions 112-113).
     * Region code for source.
     */
    @Size(max = 2, message = "Source region code cannot exceed 2 characters")
    @Column(name = "source_region_code", length = 2)
    private String sourceRegionCode;

    /**
     * Field 33: Destination Region Code (positions 114-115).
     * Region code for destination.
     */
    @Size(max = 2, message = "Destination region code cannot exceed 2 characters")
    @Column(name = "destination_region_code", length = 2)
    private String destinationRegionCode;

    /**
     * Field 34: Fee Level Descriptor (positions 116-131).
     * Description of the fee level.
     */
    @Size(max = 16, message = "Fee level descriptor cannot exceed 16 characters")
    @Column(name = "fee_level_descriptor", length = 16)
    private String feeLevelDescriptor;

    /**
     * Field 35: CR/DB/NET Indicator (position 132).
     * C=Credit, D=Debit, N=Net.
     */
    @Pattern(regexp = "^[CDN ]?$", message = "CR/DB/NET indicator must be 'C', 'D', 'N', or space")
    @Column(name = "cr_db_net_indicator", length = 1)
    private String crDbNetIndicator;

    /**
     * Field 36: Summary Level (positions 133-134).
     * Level of summarization.
     */
    @Size(max = 2, message = "Summary level cannot exceed 2 characters")
    @Column(name = "summary_level", length = 2)
    private String summaryLevel;

    /**
     * Field 37: Reserved (positions 135-136).
     * Reserved for future use.
     */
    @Size(max = 2, message = "Reserved field 2 cannot exceed 2 characters")
    @Column(name = "reserved_field_2", length = 2)
    private String reservedField2;

    /**
     * Field 38: Reserved (positions 137-167).
     * Reserved for future use.
     */
    @Size(max = 31, message = "Reserved field 3 cannot exceed 31 characters")
    @Column(name = "reserved_field_3", length = 31)
    private String reservedField3;

    /**
     * Field 39: Reimbursement Attribute (position 168).
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

    public static final String TRANSACTION_CODE_VSSSubGroup4 = "46";
    public static final String TRANSACTION_CODE_QUALIFIER_DEFAULT = "0";
    public static final String TRANSACTION_COMPONENT_SEQ_DEFAULT = "0";
    public static final String REPORT_GROUP_VISA = "V";
    public static final String REPORT_SUBGROUP_VSS4 = "4";

    // Valid Report ID Numbers for VSS-4 subgroup
    public static final List<String> VALID_REPORT_IDS = Arrays.asList(
            "120", "130", "131", "135", "136", "140", "210", "215", "230", "640"
    );

    // Business Mode constants
    public static final String BUSINESS_MODE_ACQUIRER = "1";
    public static final String BUSINESS_MODE_ISSUER = "2";
    public static final String BUSINESS_MODE_OTHER = "3";
    public static final String BUSINESS_MODE_TOTAL = "9";

    public static final List<String> VALID_BUSINESS_MODES = Arrays.asList(
            BUSINESS_MODE_ACQUIRER, BUSINESS_MODE_ISSUER, BUSINESS_MODE_OTHER, BUSINESS_MODE_TOTAL, " "
    );

    protected VssSubGroup4Record() {
        super();
    }
    /**
     * Creates a new VSSSubGroup4SettlementRecord with the specified job reference.
     *
     * @param fileProcessingJob the associated file processing job
     */
    protected VssSubGroup4Record(FileProcessingJob fileProcessingJob) {

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

    public String getSettlementCurrencyCode() {
        return settlementCurrencyCode;
    }

    public void setSettlementCurrencyCode(String settlementCurrencyCode) {
        this.settlementCurrencyCode = settlementCurrencyCode;
    }

    public String getClearingCurrencyCode() {
        return clearingCurrencyCode;
    }

    public void setClearingCurrencyCode(String clearingCurrencyCode) {
        this.clearingCurrencyCode = clearingCurrencyCode;
    }

    public String getBusinessMode() {
        return businessMode;
    }

    public void setBusinessMode(String businessMode) {
        this.businessMode = businessMode;
    }

    public String getNoDataIndicator() {
        return noDataIndicator;
    }

    public void setNoDataIndicator(String noDataIndicator) {
        this.noDataIndicator = noDataIndicator;
    }

    public String getReservedField() {
        return reservedField;
    }

    public void setReservedField(String reservedField) {
        this.reservedField = reservedField;
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

    public String getChargeTypeCode() {
        return chargeTypeCode;
    }

    public void setChargeTypeCode(String chargeTypeCode) {
        this.chargeTypeCode = chargeTypeCode;
    }

    public String getBusinessTransactionType() {
        return businessTransactionType;
    }

    public void setBusinessTransactionType(String businessTransactionType) {
        this.businessTransactionType = businessTransactionType;
    }

    public String getBusinessTransactionCycle() {
        return businessTransactionCycle;
    }

    public void setBusinessTransactionCycle(String businessTransactionCycle) {
        this.businessTransactionCycle = businessTransactionCycle;
    }

    public String getReversalIndicator() {
        return reversalIndicator;
    }

    public void setReversalIndicator(String reversalIndicator) {
        this.reversalIndicator = reversalIndicator;
    }

    public String getReturnIndicator() {
        return returnIndicator;
    }

    public void setReturnIndicator(String returnIndicator) {
        this.returnIndicator = returnIndicator;
    }

    public String getJurisdictionCode() {
        return jurisdictionCode;
    }

    public void setJurisdictionCode(String jurisdictionCode) {
        this.jurisdictionCode = jurisdictionCode;
    }

    public String getInterregionalRoutingIndicator() {
        return interregionalRoutingIndicator;
    }

    public void setInterregionalRoutingIndicator(String interregionalRoutingIndicator) {
        this.interregionalRoutingIndicator = interregionalRoutingIndicator;
    }

    public String getSourceCountryCode() {
        return sourceCountryCode;
    }

    public void setSourceCountryCode(String sourceCountryCode) {
        this.sourceCountryCode = sourceCountryCode;
    }

    public String getDestinationCountryCode() {
        return destinationCountryCode;
    }

    public void setDestinationCountryCode(String destinationCountryCode) {
        this.destinationCountryCode = destinationCountryCode;
    }

    public String getSourceRegionCode() {
        return sourceRegionCode;
    }

    public void setSourceRegionCode(String sourceRegionCode) {
        this.sourceRegionCode = sourceRegionCode;
    }

    public String getDestinationRegionCode() {
        return destinationRegionCode;
    }

    public void setDestinationRegionCode(String destinationRegionCode) {
        this.destinationRegionCode = destinationRegionCode;
    }

    public String getFeeLevelDescriptor() {
        return feeLevelDescriptor;
    }

    public void setFeeLevelDescriptor(String feeLevelDescriptor) {
        this.feeLevelDescriptor = feeLevelDescriptor;
    }

    public String getCrDbNetIndicator() {
        return crDbNetIndicator;
    }

    public void setCrDbNetIndicator(String crDbNetIndicator) {
        this.crDbNetIndicator = crDbNetIndicator;
    }

    public String getSummaryLevel() {
        return summaryLevel;
    }

    public void setSummaryLevel(String summaryLevel) {
        this.summaryLevel = summaryLevel;
    }

    public String getReservedField2() {
        return reservedField2;
    }

    public void setReservedField2(String reservedField2) {
        this.reservedField2 = reservedField2;
    }

    public String getReservedField3() {
        return reservedField3;
    }

    public void setReservedField3(String reservedField3) {
        this.reservedField3 = reservedField3;
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
     * Checks if the record is valid and ready for processing.
     *
     * @return true if valid, false otherwise
     */
    public boolean isValidForProcessing() {
        return isValid != null && isValid &&
                TRANSACTION_CODE_VSSSubGroup4.equals(transactionCode) &&
                destinationId != null && destinationId.length() == 6 &&
                settlementDate != null &&
                REPORT_GROUP_VISA.equals(reportGroup) &&
                REPORT_SUBGROUP_VSS4.equals(reportSubgroup) &&
                VALID_REPORT_IDS.contains(reportIdNumber) &&
                VALID_BUSINESS_MODES.contains(businessMode);
    }

    /**
     * Checks if the no data indicator shows no data is available.
     *
     * @return true if no data indicator is "Y"
     */
    public boolean hasNoData() {
        return "Y".equals(noDataIndicator);
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

        if (chargeTypeCode != null && !chargeTypeCode.trim().isEmpty()) {
            description.append(String.format(", Charge: %s", chargeTypeCode));
        }

        if (businessTransactionType != null && !businessTransactionType.trim().isEmpty()) {
            description.append(String.format(", BizType: %s", businessTransactionType));
        }

        if (hasNoData()) {
            description.append(", [NO DATA]");
        }

        description.append("]");
        return description.toString();
    }

    /**
     * Validates that required header fields are set correctly.
     *
     * @return true if all required header fields are valid
     */
    public boolean isHeaderValid() {
        return TRANSACTION_CODE_VSSSubGroup4.equals(transactionCode) &&
                TRANSACTION_CODE_QUALIFIER_DEFAULT.equals(transactionCodeQualifier) &&
                TRANSACTION_COMPONENT_SEQ_DEFAULT.equals(transactionComponentSequenceNumber) &&
                destinationId != null && destinationId.matches("^\\d{6}$") &&
                REPORT_GROUP_VISA.equals(reportGroup) &&
                REPORT_SUBGROUP_VSS4.equals(reportSubgroup) &&
                VALID_REPORT_IDS.contains(reportIdNumber);
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

        if (reservedField != null && !reservedField.matches("^\\s*$")) {
            valid = false;
        }

        if (reservedField2 != null && !reservedField2.matches("^\\s*$")) {
            valid = false;
        }

        if (reservedField3 != null && !reservedField3.matches("^\\s*$")) {
            valid = false;
        }

        if (reimbursementAttribute != null &&
                !reimbursementAttribute.equals("0") &&
                !reimbursementAttribute.equals(" ")) {
            valid = false;
        }

        return valid;
    }

    /**
     * Checks if this is a reversal transaction.
     *
     * @return true if reversal indicator is "Y"
     */
    public boolean isReversal() {
        return "Y".equals(reversalIndicator);
    }

    /**
     * Checks if this is a return transaction.
     *
     * @return true if return indicator is "Y"
     */
    public boolean isReturn() {
        return "Y".equals(returnIndicator);
    }

    /**
     * Checks if this transaction involves interregional routing.
     *
     * @return true if interregional routing indicator is "Y"
     */
    public boolean hasInterregionalRouting() {
        return "Y".equals(interregionalRoutingIndicator);
    }
}
