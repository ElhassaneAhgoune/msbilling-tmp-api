package com.moneysab.cardexis.parser;

import com.moneysab.cardexis.domain.entity.FileProcessingJob;
import com.moneysab.cardexis.domain.entity.Vss110SettlementRecord;
import com.moneysab.cardexis.exception.VssParsingException;
import com.moneysab.cardexis.exception.VssValidationException;
import com.moneysab.cardexis.util.VssDateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Enhanced parser for VSS-110 settlement record files supporting all 30 TCR0 fields.
 *
 * FIXED VERSION - Now properly sets all entity fields that correspond to database columns.
 *
 * @author EL.AHGOUNE
 * @version 2.1.0
 * @since 2024
 */
@Component
public class Vss110FileParser {

    private static final Logger log = LoggerFactory.getLogger(Vss110FileParser.class);

    // Record length constants
    private static final int EXPECTED_RECORD_LENGTH = 168;
    private static final int MIN_RECORD_LENGTH = 155;

    // All 30 Field position constants (1-based positions from specification)
    // Field 1: Transaction Code
    private static final int TRANSACTION_CODE_START = 1;
    private static final int TRANSACTION_CODE_END = 2;

    // Field 2: Transaction Code Qualifier
    private static final int TRANSACTION_CODE_QUALIFIER_START = 3;
    private static final int TRANSACTION_CODE_QUALIFIER_END = 3;

    // Field 3: Transaction Component Sequence Number
    private static final int TRANSACTION_COMPONENT_SEQ_START = 4;
    private static final int TRANSACTION_COMPONENT_SEQ_END = 4;

    // Field 4: Destination Identifier
    private static final int DESTINATION_ID_START = 5;
    private static final int DESTINATION_ID_END = 10;

    // Field 5: Source Identifier
    private static final int SOURCE_ID_START = 11;
    private static final int SOURCE_ID_END = 16;

    // Field 6: Reporting for SRE Identifier
    private static final int REPORTING_SRE_ID_START = 17;
    private static final int REPORTING_SRE_ID_END = 26;

    // Field 7: Rollup to SRE Identifier
    private static final int ROLLUP_SRE_ID_START = 27;
    private static final int ROLLUP_SRE_ID_END = 36;

    // Field 8: Funds Transfer SRE Identifier
    private static final int FUNDS_TRANSFER_SRE_ID_START = 37;
    private static final int FUNDS_TRANSFER_SRE_ID_END = 46;

    // Field 9: Settlement Service Identifier
    private static final int SETTLEMENT_SERVICE_START = 47;
    private static final int SETTLEMENT_SERVICE_END = 49;

    // Field 10: Settlement Currency Code
    private static final int CURRENCY_CODE_START = 50;
    private static final int CURRENCY_CODE_END = 52;

    // Field 11: No Data Indicator
    private static final int NO_DATA_INDICATOR_START = 53;
    private static final int NO_DATA_INDICATOR_END = 53;

    // Field 12: Reserved
    private static final int RESERVED1_START = 54;
    private static final int RESERVED1_END = 58;

    // Field 13: Report Group
    private static final int REPORT_GROUP_START = 59;
    private static final int REPORT_GROUP_END = 59;

    // Field 14: Report Subgroup
    private static final int REPORT_SUBGROUP_START = 60;
    private static final int REPORT_SUBGROUP_END = 60;

    // Field 15: Report Identification Number
    private static final int REPORT_ID_START = 61;
    private static final int REPORT_ID_END = 63;

    // Field 16: Report Identification Suffix
    private static final int REPORT_ID_SUFFIX_START = 64;
    private static final int REPORT_ID_SUFFIX_END = 65;

    // Field 17: Settlement Date
    private static final int SETTLEMENT_DATE_START = 66;
    private static final int SETTLEMENT_DATE_END = 72;

    // Field 18: Report Date
    private static final int REPORT_DATE_START = 73;
    private static final int REPORT_DATE_END = 79;

    // Field 19: From Date
    private static final int FROM_DATE_START = 80;
    private static final int FROM_DATE_END = 86;

    // Field 20: To Date
    private static final int TO_DATE_START = 87;
    private static final int TO_DATE_END = 93;

    // Field 21: Amount Type
    private static final int AMOUNT_TYPE_START = 94;
    private static final int AMOUNT_TYPE_END = 94;

    // Field 22: Business Mode
    private static final int BUSINESS_MODE_START = 95;
    private static final int BUSINESS_MODE_END = 95;

    // Field 23: Count
    private static final int TRANSACTION_COUNT_START = 96;
    private static final int TRANSACTION_COUNT_END = 110;

    // Field 24: Credit Amount
    private static final int CREDIT_AMOUNT_START = 111;
    private static final int CREDIT_AMOUNT_END = 125;

    // Field 25: Debit Amount
    private static final int DEBIT_AMOUNT_START = 126;
    private static final int DEBIT_AMOUNT_END = 140;

    // Field 26: Net Amount
    private static final int NET_AMOUNT_START = 141;
    private static final int NET_AMOUNT_END = 155;

    // Field 27: Net Amount Sign
    private static final int AMOUNT_SIGN_START = 156;
    private static final int AMOUNT_SIGN_END = 157;

    // Field 28: Funds-Transfer Date
    private static final int FUNDS_TRANSFER_DATE_START = 158;
    private static final int FUNDS_TRANSFER_DATE_END = 164;

    // Field 29: Reserved
    private static final int RESERVED2_START = 165;
    private static final int RESERVED2_END = 167;

    // Field 30: Reimbursement Attribute
    private static final int REIMBURSEMENT_ATTR_START = 168;
    private static final int REIMBURSEMENT_ATTR_END = 168;

    // Validation patterns
    private static final Pattern TRANSACTION_CODE_PATTERN = Pattern.compile("^46$");
    private static final Pattern TRANSACTION_CODE_QUALIFIER_PATTERN = Pattern.compile("^0$");
    private static final Pattern TRANSACTION_COMPONENT_SEQ_PATTERN = Pattern.compile("^0$");
    private static final Pattern DESTINATION_ID_PATTERN = Pattern.compile("^\\d{6}$");
    private static final Pattern REPORT_GROUP_PATTERN = Pattern.compile("^V$");
    private static final Pattern REPORT_SUBGROUP_PATTERN = Pattern.compile("^2$");
    private static final Pattern REPORT_ID_PATTERN = Pattern.compile("^(110|111)$");
    private static final Pattern AMOUNT_TYPE_PATTERN = Pattern.compile("^[IFCT ]$");
    private static final Pattern BUSINESS_MODE_PATTERN = Pattern.compile("^[1239 ]$");
    private static final Pattern NUMERIC_AMOUNT_PATTERN = Pattern.compile("^[0-9 ]{15}$");
    private static final Pattern NO_DATA_INDICATOR_PATTERN = Pattern.compile("^[VY ]$");

    // Configuration properties
    @Value("${moneysab.cardexis.processing.strict-validation:true}")
    private boolean strictValidation;

    @Value("${moneysab.cardexis.processing.skip-invalid-records:false}")
    private boolean skipInvalidRecords;

    @Value("${moneysab.cardexis.formats.vss110.expected-length:168}")
    private int expectedRecordLength;

    /**
     * Parses a single VSS-110 record line with all 30 TCR0 fields.
     */
    public Vss110SettlementRecord parseLine(String line, int lineNumber, FileProcessingJob fileProcessingJob) {
        if (line == null) {
            throw VssParsingException.missingRequiredField("record_line", lineNumber, "VSS-110");
        }

        log.debug("Parsing VSS-110 record at line {}: length={}", lineNumber, line.length());

        // Create new record instance
        Vss110SettlementRecord record = new Vss110SettlementRecord(fileProcessingJob);
        record.setLineNumber(lineNumber);
        record.setRawRecordLine(line);

        try {
            // Validate record length
            validateRecordLength(line, lineNumber);

            // Parse all 30 fields according to TCR0 specification
            parseField1_TransactionCode(record, line, lineNumber);
            parseField2_TransactionCodeQualifier(record, line, lineNumber);
            parseField3_TransactionComponentSequenceNumber(record, line, lineNumber);
            parseField4_DestinationIdentifier(record, line, lineNumber);
            parseField5_SourceIdentifier(record, line, lineNumber);
            parseField6_ReportingForSREIdentifier(record, line, lineNumber);
            parseField7_RollupToSREIdentifier(record, line, lineNumber);
            parseField8_FundsTransferSREIdentifier(record, line, lineNumber);
            parseField9_SettlementServiceIdentifier(record, line, lineNumber);
            parseField10_SettlementCurrencyCode(record, line, lineNumber);
            parseField11_NoDataIndicator(record, line, lineNumber);
            parseField12_Reserved1(record, line, lineNumber);
            parseField13_ReportGroup(record, line, lineNumber);
            parseField14_ReportSubgroup(record, line, lineNumber);
            parseField15_ReportIdentificationNumber(record, line, lineNumber);
            parseField16_ReportIdentificationSuffix(record, line, lineNumber);
            parseField17_SettlementDate(record, line, lineNumber);
            parseField18_ReportDate(record, line, lineNumber);
            parseField19_FromDate(record, line, lineNumber);
            parseField20_ToDate(record, line, lineNumber);
            parseField21_AmountType(record, line, lineNumber);
            parseField22_BusinessMode(record, line, lineNumber);
            parseField23_Count(record, line, lineNumber);
            parseField24_CreditAmount(record, line, lineNumber);
            parseField25_DebitAmount(record, line, lineNumber);
            parseField26_NetAmount(record, line, lineNumber);
            parseField27_NetAmountSign(record, line, lineNumber);
            parseField28_FundsTransferDate(record, line, lineNumber);
            parseField29_Reserved2(record, line, lineNumber);
            parseField30_ReimbursementAttribute(record, line, lineNumber);

            // Validate the parsed record
            validateRecord(record, lineNumber);

            // Don't recalculate net amount after parsing since it's already parsed from the file
            // The VSS-110 format provides the net amount and sign directly
            // record.calculateNetAmount(); // REMOVED - causes validation issues

            log.debug("Successfully parsed VSS-110 record: {}", record.getRecordDescription());

        } catch (VssParsingException | VssValidationException e) {
            record.setIsValid(false);
            record.setValidationErrors(e.getMessage());

            if (strictValidation && !skipInvalidRecords) {
                throw e;
            }

            log.warn("Invalid VSS-110 record at line {}: {}", lineNumber, e.getMessage());
        } catch (Exception e) {
            record.setIsValid(false);
            record.setValidationErrors("Unexpected parsing error: " + e.getMessage());

            VssParsingException parsingException = new VssParsingException(
                    "Unexpected error parsing VSS-110 record", lineNumber, "VSS-110", e);

            if (strictValidation && !skipInvalidRecords) {
                throw parsingException;
            }

            log.error("Unexpected error parsing VSS-110 record at line {}", lineNumber, e);
        }

        return record;
    }

    /**
     * Field 1: Transaction Code (positions 1-2) - Always "46"
     */
    private void parseField1_TransactionCode(Vss110SettlementRecord record, String line, int lineNumber) {
        String transactionCode = extractField(line, TRANSACTION_CODE_START, TRANSACTION_CODE_END, "transaction_code", lineNumber);

        if (!TRANSACTION_CODE_PATTERN.matcher(transactionCode).matches()) {
            throw VssParsingException.invalidFieldFormat("transaction_code", "46", transactionCode, lineNumber, "VSS-110");
        }

        record.setTransactionCode(transactionCode);
    }

    /**
     * Field 2: Transaction Code Qualifier (position 3) - Always "0"
     */
    private void parseField2_TransactionCodeQualifier(Vss110SettlementRecord record, String line, int lineNumber) {
        String qualifier = extractField(line, TRANSACTION_CODE_QUALIFIER_START, TRANSACTION_CODE_QUALIFIER_END, "transaction_code_qualifier", lineNumber);

        if (!TRANSACTION_CODE_QUALIFIER_PATTERN.matcher(qualifier).matches()) {
            if (strictValidation) {
                throw VssParsingException.invalidFieldFormat("transaction_code_qualifier", "0", qualifier, lineNumber, "VSS-110");
            } else {
                log.warn("Invalid transaction code qualifier at line {}: expected='0', actual='{}'", lineNumber, qualifier);
            }
        }

        // FIXED: Actually set the field in the entity
        record.setTransactionCodeQualifier(qualifier);
        log.debug("Transaction Code Qualifier at line {}: {}", lineNumber, qualifier);
    }

    /**
     * Field 3: Transaction Component Sequence Number (position 4) - Always "0"
     */
    private void parseField3_TransactionComponentSequenceNumber(Vss110SettlementRecord record, String line, int lineNumber) {
        String seqNumber = extractField(line, TRANSACTION_COMPONENT_SEQ_START, TRANSACTION_COMPONENT_SEQ_END, "transaction_component_sequence_number", lineNumber);

        if (!TRANSACTION_COMPONENT_SEQ_PATTERN.matcher(seqNumber).matches()) {
            if (strictValidation) {
                throw VssParsingException.invalidFieldFormat("transaction_component_sequence_number", "0", seqNumber, lineNumber, "VSS-110");
            } else {
                log.warn("Invalid transaction component sequence number at line {}: expected='0', actual='{}'", lineNumber, seqNumber);
            }
        }

        // FIXED: Actually set the field in the entity
        record.setTransactionComponentSequenceNumber(seqNumber);
        log.debug("Transaction Component Sequence Number at line {}: {}", lineNumber, seqNumber);
    }

    /**
     * Field 4: Destination Identifier (positions 5-10) - Receiving entity ID
     */
    private void parseField4_DestinationIdentifier(Vss110SettlementRecord record, String line, int lineNumber) {
        String destinationId = extractField(line, DESTINATION_ID_START, DESTINATION_ID_END, "destination_id", lineNumber);

        if (!DESTINATION_ID_PATTERN.matcher(destinationId).matches()) {
            throw VssParsingException.invalidFieldFormat("destination_id", "6 digits", destinationId, lineNumber, "VSS-110");
        }

        record.setDestinationId(destinationId);
    }

    /**
     * Field 5: Source Identifier (positions 11-16) - Sending entity ID, usually zero
     */
    private void parseField5_SourceIdentifier(Vss110SettlementRecord record, String line, int lineNumber) {
        String sourceId = extractField(line, SOURCE_ID_START, SOURCE_ID_END, "source_id", lineNumber);

        // FIXED: Actually set the field in the entity
        record.setSourceIdentifier(sourceId);
        log.debug("Source Identifier at line {}: {}", lineNumber, sourceId);
    }

    /**
     * Field 6: Reporting for SRE Identifier (positions 17-26) - SRE being reported
     */
    private void parseField6_ReportingForSREIdentifier(Vss110SettlementRecord record, String line, int lineNumber) {
        String reportingSreId = extractField(line, REPORTING_SRE_ID_START, REPORTING_SRE_ID_END, "reporting_sre_id", lineNumber);

        // Truncate to fit entity constraint if needed
        if (reportingSreId.length() > 100) {
            reportingSreId = reportingSreId.substring(0, 100);
            log.debug("Truncated reporting SRE ID to 100 characters at line {}", lineNumber);
        }

        record.setReportingSreId(reportingSreId);
    }

    /**
     * Field 7: Rollup to SRE Identifier (positions 27-36) - Parent SRE in hierarchy
     */
    private void parseField7_RollupToSREIdentifier(Vss110SettlementRecord record, String line, int lineNumber) {
        String rollupSreId = extractField(line, ROLLUP_SRE_ID_START, ROLLUP_SRE_ID_END, "rollup_sre_id", lineNumber);

        if (rollupSreId.length() > 100) {
            rollupSreId = rollupSreId.substring(0, 100);
            log.debug("Truncated rollup SRE ID to 100 characters at line {}", lineNumber);
        }

        record.setRollupSreId(rollupSreId);
    }

    /**
     * Field 8: Funds Transfer SRE Identifier (positions 37-46) - Funds transfer entity SRE
     */
    private void parseField8_FundsTransferSREIdentifier(Vss110SettlementRecord record, String line, int lineNumber) {
        String fundsTransferSreId = extractField(line, FUNDS_TRANSFER_SRE_ID_START, FUNDS_TRANSFER_SRE_ID_END, "funds_transfer_sre_id", lineNumber);

        if (fundsTransferSreId.length() > 100) {
            fundsTransferSreId = fundsTransferSreId.substring(0, 100);
            log.debug("Truncated funds transfer SRE ID to 100 characters at line {}", lineNumber);
        }

        record.setFundsTransferSreId(fundsTransferSreId);
    }

    /**
     * Field 9: Settlement Service Identifier (positions 47-49) - Settlement Service Code
     */
    private void parseField9_SettlementServiceIdentifier(Vss110SettlementRecord record, String line, int lineNumber) {
        String settlementService = extractField(line, SETTLEMENT_SERVICE_START, SETTLEMENT_SERVICE_END, "settlement_service", lineNumber);
        record.setSettlementService(settlementService);
    }

    /**
     * Field 10: Settlement Currency Code (positions 50-52) - ISO numeric currency code
     */
    private void parseField10_SettlementCurrencyCode(Vss110SettlementRecord record, String line, int lineNumber) {
        String currencyCode = extractField(line, CURRENCY_CODE_START, CURRENCY_CODE_END, "currency_code", lineNumber);
        record.setCurrencyCode(currencyCode.trim());
    }

    /**
     * Field 11: No Data Indicator (position 53) - V = No data, or space
     */
    private void parseField11_NoDataIndicator(Vss110SettlementRecord record, String line, int lineNumber) {
        String noDataIndicator = extractField(line, NO_DATA_INDICATOR_START, NO_DATA_INDICATOR_END, "no_data_indicator", lineNumber);

        // Accept any single character for no_data_indicator
        // Commonly: V (valid data), Y (yes), N (no), or space (not applicable)
        record.setNoDataIndicator(noDataIndicator);
        log.debug("No Data Indicator at line {}: '{}'", lineNumber, noDataIndicator);
    }

    /**
     * Field 12: Reserved (positions 54-58) - Always spaces
     */
    private void parseField12_Reserved1(Vss110SettlementRecord record, String line, int lineNumber) {
        String reserved = extractField(line, RESERVED1_START, RESERVED1_END, "reserved1", lineNumber);

        if (strictValidation && !reserved.trim().isEmpty()) {
            log.warn("Reserved field 1 contains non-space characters at line {}: '{}'", lineNumber, reserved);
        }

        // FIXED: Actually set the field in the entity
        record.setReservedField1(reserved);
        log.debug("Reserved field 1 at line {}: '{}'", lineNumber, reserved);
    }

    /**
     * Field 13: Report Group (position 59) - Always "V"
     */
    private void parseField13_ReportGroup(Vss110SettlementRecord record, String line, int lineNumber) {
        String reportGroup = extractField(line, REPORT_GROUP_START, REPORT_GROUP_END, "report_group", lineNumber);

        if (!REPORT_GROUP_PATTERN.matcher(reportGroup).matches()) {
            throw VssParsingException.invalidFieldFormat("report_group", "V", reportGroup, lineNumber, "VSS-110");
        }

        record.setReportGroup(reportGroup);
    }

    /**
     * Field 14: Report Subgroup (position 60) - Always "2"
     */
    private void parseField14_ReportSubgroup(Vss110SettlementRecord record, String line, int lineNumber) {
        String reportSubgroup = extractField(line, REPORT_SUBGROUP_START, REPORT_SUBGROUP_END, "report_subgroup", lineNumber);

        if (!REPORT_SUBGROUP_PATTERN.matcher(reportSubgroup).matches()) {
            throw VssParsingException.invalidFieldFormat("report_subgroup", "2", reportSubgroup, lineNumber, "VSS-110");
        }

        record.setReportSubgroup(reportSubgroup);
    }

    /**
     * Field 15: Report Identification Number (positions 61-63) - "110" or "111"
     */
    private void parseField15_ReportIdentificationNumber(Vss110SettlementRecord record, String line, int lineNumber) {
        String reportIdNumber = extractField(line, REPORT_ID_START, REPORT_ID_END, "report_id_number", lineNumber);

        if (!REPORT_ID_PATTERN.matcher(reportIdNumber).matches()) {
            throw VssParsingException.invalidFieldFormat("report_id_number", "110 or 111", reportIdNumber, lineNumber, "VSS-110");
        }

        record.setReportIdNumber(reportIdNumber);
    }

    /**
     * Field 16: Report Identification Suffix (positions 64-65) - Space or "M"
     */
    private void parseField16_ReportIdentificationSuffix(Vss110SettlementRecord record, String line, int lineNumber) {
        String reportIdSuffix = extractField(line, REPORT_ID_SUFFIX_START, REPORT_ID_SUFFIX_END, "report_id_suffix", lineNumber);

        // FIXED: Actually set the field in the entity
        record.setReportIdSuffix(reportIdSuffix);
        log.debug("Report ID Suffix at line {}: '{}'", lineNumber, reportIdSuffix);
    }

    /**
     * Field 17: Settlement Date (positions 66-72) - Format: CCYYDDD
     */
    private void parseField17_SettlementDate(Vss110SettlementRecord record, String line, int lineNumber) {
        String rawDate = extractField(line, SETTLEMENT_DATE_START, SETTLEMENT_DATE_END, "settlement_date", lineNumber);

        String trimmedRawDate = rawDate.trim();
        record.setRawSettlementDate(trimmedRawDate);

        try {
            LocalDate settlementDate = VssDateUtil.parseCCYYDDD(trimmedRawDate, lineNumber, "VSS-110");
            record.setSettlementDate(settlementDate);
        } catch (VssParsingException e) {
            if (strictValidation) {
                throw e;
            } else {
                LocalDate defaultDate = VssDateUtil.parseCCYYDDDLenient(trimmedRawDate);
                record.setSettlementDate(defaultDate);
                log.warn("Used default date for invalid settlement date at line {}: {}", lineNumber, trimmedRawDate);
            }
        }
    }

    /**
     * Field 18: Report Date (positions 73-79) - Format: CCYYDDD
     */
    private void parseField18_ReportDate(Vss110SettlementRecord record, String line, int lineNumber) {
        String rawReportDate = extractField(line, REPORT_DATE_START, REPORT_DATE_END, "report_date", lineNumber);

        // FIXED: Set raw report date
        record.setRawReportDate(rawReportDate);

        String trimmedRawReportDate = rawReportDate.trim();
        if (!trimmedRawReportDate.isEmpty() && !trimmedRawReportDate.matches("^\\s*$")) {
            try {
                LocalDate reportDate = VssDateUtil.parseCCYYDDD(trimmedRawReportDate, lineNumber, "VSS-110");
                record.setReportDate(reportDate);
            } catch (VssParsingException e) {
                if (strictValidation) {
                    throw e;
                } else {
                    LocalDate defaultDate = VssDateUtil.parseCCYYDDDLenient(trimmedRawReportDate);
                    record.setReportDate(defaultDate);
                    log.warn("Used default date for invalid report date at line {}: {}", lineNumber, trimmedRawReportDate);
                }
            }
        } else {
            record.setReportDate(null);
            log.debug("No report date data provided at line {} (spaces only), setting parsed date to null", lineNumber);
        }
    }

    /**
     * Field 19: From Date (positions 80-86) - Format: CCYYDDD
     */
    private void parseField19_FromDate(Vss110SettlementRecord record, String line, int lineNumber) {
        String fromDateRaw = extractField(line, FROM_DATE_START, FROM_DATE_END, "from_date", lineNumber);

        // FIXED: Set raw from date
        record.setRawFromDate(fromDateRaw);

        String trimmedFromDate = fromDateRaw.trim();
        if (!trimmedFromDate.isEmpty() && !trimmedFromDate.matches("^\\s*$")) {
            try {
                LocalDate fromDate = VssDateUtil.parseCCYYDDD(trimmedFromDate, lineNumber, "VSS-110");
                // FIXED: Set parsed from date
                record.setFromDate(fromDate);
                log.debug("From Date at line {}: {}", lineNumber, fromDate);
            } catch (VssParsingException e) {
                if (strictValidation) {
                    throw e;
                } else {
                    log.warn("Invalid from date at line {}: {}", lineNumber, trimmedFromDate);
                }
            }
        } else {
            record.setFromDate(null);
            log.debug("No from date data provided at line {} (spaces only)", lineNumber);
        }
    }

    /**
     * Field 20: To Date (positions 87-93) - Format: CCYYDDD
     */
    private void parseField20_ToDate(Vss110SettlementRecord record, String line, int lineNumber) {
        String toDateRaw = extractField(line, TO_DATE_START, TO_DATE_END, "to_date", lineNumber);

        // FIXED: Set raw to date
        record.setRawToDate(toDateRaw);

        String trimmedToDate = toDateRaw.trim();
        if (!trimmedToDate.isEmpty() && !trimmedToDate.matches("^\\s*$")) {
            try {
                LocalDate toDate = VssDateUtil.parseCCYYDDD(trimmedToDate, lineNumber, "VSS-110");
                // FIXED: Set parsed to date
                record.setToDate(toDate);
                log.debug("To Date at line {}: {}", lineNumber, toDate);
            } catch (VssParsingException e) {
                if (strictValidation) {
                    throw e;
                } else {
                    log.warn("Invalid to date at line {}: {}", lineNumber, trimmedToDate);
                }
            }
        } else {
            record.setToDate(null);
            log.debug("No to date data provided at line {} (spaces only)", lineNumber);
        }
    }

    /**
     * Field 21: Amount Type (position 94) - I, F, C, T, space
     */
    private void parseField21_AmountType(Vss110SettlementRecord record, String line, int lineNumber) {
        String amountType = extractField(line, AMOUNT_TYPE_START, AMOUNT_TYPE_END, "amount_type", lineNumber);

        if (!AMOUNT_TYPE_PATTERN.matcher(amountType).matches()) {
            throw VssParsingException.invalidFieldFormat("amount_type", "I, F, C, T, or space", amountType, lineNumber, "VSS-110");
        }

        record.setAmountType(amountType);
    }

    /**
     * Field 22: Business Mode (position 95) - 1=Acquirer, 2=Issuer, 3=Other, 9=Total
     */
    private void parseField22_BusinessMode(Vss110SettlementRecord record, String line, int lineNumber) {
        String businessMode = extractField(line, BUSINESS_MODE_START, BUSINESS_MODE_END, "business_mode", lineNumber);

        // Accept any single character for business_mode
        // Commonly: 1=Acquirer, 2=Issuer, 3=Other, 9=Total, or space
        record.setBusinessMode(businessMode);
        log.debug("Business mode at line {}: '{}'", lineNumber, businessMode);
    }

    /**
     * Field 23: Count (positions 96-110) - Transaction count (if I), else space
     */
    private void parseField23_Count(Vss110SettlementRecord record, String line, int lineNumber) {
        String transactionCountStr = extractField(line, TRANSACTION_COUNT_START, TRANSACTION_COUNT_END, "transaction_count", lineNumber);

        if (transactionCountStr != null && !transactionCountStr.trim().isEmpty() && !transactionCountStr.trim().equals("000000000000000")) {
            try {
                String cleanCount = transactionCountStr.trim().replaceFirst("^0+", "");
                if (!cleanCount.isEmpty()) {
                    int transactionCount = Integer.parseInt(cleanCount);
                    record.setTransactionCount(transactionCount);
                }
            } catch (NumberFormatException e) {
                if (strictValidation) {
                    throw VssParsingException.invalidNumericValue("transaction_count", transactionCountStr, lineNumber, "VSS-110", e);
                } else {
                    log.warn("Invalid transaction count at line {}: {}", lineNumber, transactionCountStr);
                    record.setTransactionCount(0);
                }
            }
        }
    }

    /**
     * Field 24: Credit Amount (positions 111-125) - Credit amount
     */
    private void parseField24_CreditAmount(Vss110SettlementRecord record, String line, int lineNumber) {
        String creditAmountStr = extractField(line, CREDIT_AMOUNT_START, CREDIT_AMOUNT_END, "credit_amount", lineNumber);
        BigDecimal creditAmount = parseAmount(creditAmountStr, "credit_amount", lineNumber);
        record.setCreditAmount(creditAmount);
    }

    /**
     * Field 25: Debit Amount (positions 126-140) - Debit amount
     */
    private void parseField25_DebitAmount(Vss110SettlementRecord record, String line, int lineNumber) {
        String debitAmountStr = extractField(line, DEBIT_AMOUNT_START, DEBIT_AMOUNT_END, "debit_amount", lineNumber);
        BigDecimal debitAmount = parseAmount(debitAmountStr, "debit_amount", lineNumber);
        record.setDebitAmount(debitAmount);
    }

    /**
     * Field 26: Net Amount (positions 141-155) - Credit - Debit
     */
    private void parseField26_NetAmount(Vss110SettlementRecord record, String line, int lineNumber) {
        String netAmountStr = extractField(line, NET_AMOUNT_START, NET_AMOUNT_END, "net_amount", lineNumber);
        BigDecimal netAmount = parseAmount(netAmountStr, "net_amount", lineNumber);
        record.setNetAmount(netAmount);
    }

    /**
     * Field 27: Net Amount Sign (positions 156-157) - "CR" or "DB"
     * FIXED: Better handling of empty/space signs for zero amounts
     */
    private void parseField27_NetAmountSign(Vss110SettlementRecord record, String line, int lineNumber) {
        if (line.length() >= AMOUNT_SIGN_END) {
            String amountSign = extractField(line, AMOUNT_SIGN_START, AMOUNT_SIGN_END, "amount_sign", lineNumber);
            if (amountSign != null) {
                String cleanSign = amountSign.trim();

                // For VSS-110, accept CR, DB, or empty (for zero amounts)
                if ("CR".equals(cleanSign) || "DB".equals(cleanSign) || cleanSign.isEmpty()) {
                    record.setAmountSign(amountSign); // Store the raw value (including spaces)
                } else if (strictValidation) {
                    throw VssParsingException.invalidFieldFormat("amount_sign", "CR, DB, or spaces", amountSign, lineNumber, "VSS-110");
                } else {
                    record.setAmountSign(amountSign);
                    log.warn("Unusual amount sign at line {}: '{}'", lineNumber, amountSign);
                }
            }
        }
    }

    /**
     * Field 28: Funds-Transfer Date (positions 158-164) - Format: CCYDDD
     */
    private void parseField28_FundsTransferDate(Vss110SettlementRecord record, String line, int lineNumber) {
        String fundsTransferDateRaw = extractField(line, FUNDS_TRANSFER_DATE_START, FUNDS_TRANSFER_DATE_END, "funds_transfer_date", lineNumber);

        // FIXED: Set raw funds transfer date
        record.setRawFundsTransferDate(fundsTransferDateRaw);

        String trimmedFundsTransferDate = fundsTransferDateRaw.trim();
        if (!trimmedFundsTransferDate.isEmpty() && !trimmedFundsTransferDate.matches("^\\s*$")) {
            try {
                // Note: This field uses CCYDDD format (6 digits) instead of CCYYDDD (7 digits)
                LocalDate fundsTransferDate = VssDateUtil.parseCCYDDD(trimmedFundsTransferDate, lineNumber, "VSS-110");
                // FIXED: Set parsed funds transfer date
                record.setFundsTransferDate(fundsTransferDate);
                log.debug("Funds Transfer Date at line {}: {}", lineNumber, fundsTransferDate);
            } catch (VssParsingException e) {
                if (strictValidation) {
                    throw e;
                } else {
                    log.warn("Invalid funds transfer date at line {}: {}", lineNumber, trimmedFundsTransferDate);
                }
            }
        } else {
            record.setFundsTransferDate(null);
            log.debug("No funds transfer date data provided at line {} (spaces only)", lineNumber);
        }
    }

    /**
     * Field 29: Reserved (positions 165-167) - Always spaces
     */
    private void parseField29_Reserved2(Vss110SettlementRecord record, String line, int lineNumber) {
        String reserved = extractField(line, RESERVED2_START, RESERVED2_END, "reserved2", lineNumber);

        if (strictValidation && !reserved.trim().isEmpty()) {
            log.warn("Reserved field 2 contains non-space characters at line {}: '{}'", lineNumber, reserved);
        }

        // FIXED: Actually set the field in the entity
        record.setReservedField2(reserved);
        log.debug("Reserved field 2 at line {}: '{}'", lineNumber, reserved);
    }

    /**
     * Field 30: Reimbursement Attribute (position 168) - Obsolete – always zero-filled
     */
    private void parseField30_ReimbursementAttribute(Vss110SettlementRecord record, String line, int lineNumber) {
        if (line.length() >= REIMBURSEMENT_ATTR_END) {
            String reimbursementAttr = extractField(line, REIMBURSEMENT_ATTR_START, REIMBURSEMENT_ATTR_END, "reimbursement_attribute", lineNumber);

            if (strictValidation && !"0".equals(reimbursementAttr.trim()) && !" ".equals(reimbursementAttr)) {
                log.warn("Reimbursement attribute should be zero-filled at line {}: '{}'", lineNumber, reimbursementAttr);
            }

            // FIXED: Actually set the field in the entity
            record.setReimbursementAttribute(reimbursementAttr);
            log.debug("Reimbursement Attribute at line {}: '{}'", lineNumber, reimbursementAttr);
        }
    }

    /**
     * Parses multiple VSS-110 record lines.
     *
     * @param lines the list of record lines to parse
     * @param fileProcessingJob the associated file processing job
     * @return the list of parsed Vss110SettlementRecord objects
     */
    public List<Vss110SettlementRecord> parseFile(List<String> lines, FileProcessingJob fileProcessingJob) {
        if (lines == null || lines.isEmpty()) {
            log.warn("No lines provided for VSS-110 parsing");
            return new ArrayList<>();
        }

        log.info("Parsing VSS-110 file with {} lines", lines.size());

        List<Vss110SettlementRecord> records = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            int lineNumber = i + 1;
            String line = lines.get(i);

            try {
                Vss110SettlementRecord record = parseLine(line, lineNumber, fileProcessingJob);
                records.add(record);

            } catch (VssParsingException | VssValidationException e) {
                errors.add(String.format("Line %d: %s", lineNumber, e.getMessage()));

                if (!skipInvalidRecords) {
                    log.error("Stopping file parsing due to error at line {}: {}", lineNumber, e.getMessage());
                    break;
                }
            }
        }

        if (!errors.isEmpty()) {
            log.warn("VSS-110 file parsing completed with {} errors", errors.size());
            if (strictValidation && !skipInvalidRecords && !errors.isEmpty()) {
                throw new VssParsingException(
                        String.format("File parsing failed with %d errors: %s",
                                errors.size(), String.join("; ", errors.subList(0, Math.min(3, errors.size())))));
            }
        }

        log.info("Successfully parsed {} VSS-110 records from {} lines", records.size(), lines.size());
        return records;
    }

    /**
     * Validates the record length.
     */
    private void validateRecordLength(String line, int lineNumber) {
        if (line.length() < MIN_RECORD_LENGTH) {
            throw VssParsingException.invalidRecordLength(expectedRecordLength, line.length(), lineNumber, "VSS-110");
        }

        if (strictValidation && line.length() != expectedRecordLength) {
            log.warn("VSS-110 record at line {} has unexpected length: expected={}, actual={}",
                    lineNumber, expectedRecordLength, line.length());
        }
    }

    /**
     * Extracts a field from the record line based on positions.
     */
    private String extractField(String line, int startPos, int endPos, String fieldName, int lineNumber) {
        try {
            // Convert from 1-based to 0-based indexing
            int start = startPos - 1;
            int end = Math.min(endPos, line.length());

            if (start >= line.length()) {
                throw VssParsingException.missingRequiredField(fieldName, lineNumber, "VSS-110");
            }

            return line.substring(start, end);

        } catch (StringIndexOutOfBoundsException e) {
            throw VssParsingException.invalidFieldFormat(fieldName,
                    String.format("positions %d-%d", startPos, endPos),
                    "field extends beyond record length", lineNumber, "VSS-110");
        }
    }

    /**
     * Parses an amount field with implied 2 decimal places.
     */
    private BigDecimal parseAmount(String amountStr, String fieldName, int lineNumber) {
        if (amountStr == null || amountStr.trim().isEmpty() || amountStr.trim().equals("000000000000000")) {
            return BigDecimal.ZERO;
        }

        String trimmedAmount = amountStr.trim();

        // Validate format
        if (!NUMERIC_AMOUNT_PATTERN.matcher(amountStr).matches()) {
            throw VssParsingException.invalidFieldFormat(fieldName, "15-digit numeric", amountStr, lineNumber, "VSS-110");
        }

        try {
            // Remove leading zeros and spaces
            String cleanAmount = trimmedAmount.replaceFirst("^0+", "");
            if (cleanAmount.isEmpty()) {
                return BigDecimal.ZERO;
            }

            // Parse as long and convert to BigDecimal with 2 decimal places
            long amountCents = Long.parseLong(cleanAmount);
            return BigDecimal.valueOf(amountCents, 2);

        } catch (NumberFormatException e) {
            throw VssParsingException.invalidNumericValue(fieldName, amountStr, lineNumber, "VSS-110", e);
        }
    }

    /**
     * Validates the parsed record according to business rules.
     * FIXED: Updated to handle VSS-110 amount sign convention correctly, including zero-amount records.
     */
    private void validateRecord(Vss110SettlementRecord record, int lineNumber) {
        List<String> errors = new ArrayList<>();

        // Validate amount type against report ID
        if (!record.isAmountTypeValidForReportId()) {
            errors.add(String.format("Invalid amount type '%s' for report ID '%s'",
                    record.getAmountType(), record.getReportIdNumber()));
        }

        // FIXED: Validate amount consistency using VSS-110 logic
        // In VSS-110 format:
        // - Net amount field contains the ABSOLUTE value
        // - Amount sign field indicates direction: CR (Credit) or DB (Debit)
        if (record.getCreditAmount() != null && record.getDebitAmount() != null && record.getNetAmount() != null) {

            BigDecimal calculatedNet = record.getCreditAmount().subtract(record.getDebitAmount());
            BigDecimal expectedAbsoluteNet = calculatedNet.abs();

            // Check if stored net amount matches calculated absolute net
            boolean amountMatches = expectedAbsoluteNet.compareTo(record.getNetAmount()) == 0;

            // FIXED: Handle zero amounts - when net is zero, amount sign can be empty/spaces
            boolean signMatches = true; // Default to true for zero amounts

            if (calculatedNet.compareTo(BigDecimal.ZERO) != 0) {
                // Only validate sign for non-zero amounts
                String expectedSign = calculatedNet.compareTo(BigDecimal.ZERO) >= 0 ? "CR" : "DB";
                String actualSign = record.getAmountSign() != null ? record.getAmountSign().trim() : "";
                signMatches = expectedSign.equals(actualSign);
            } else {
                // For zero amounts, accept empty/space signs or valid CR/DB signs
                String actualSign = record.getAmountSign() != null ? record.getAmountSign().trim() : "";
                signMatches = actualSign.isEmpty() || "CR".equals(actualSign) || "DB".equals(actualSign);
            }

            if (!amountMatches || !signMatches) {
                String expectedSign = calculatedNet.compareTo(BigDecimal.ZERO) == 0 ? "(empty/CR/DB)" :
                        (calculatedNet.compareTo(BigDecimal.ZERO) >= 0 ? "CR" : "DB");
                String actualSign = record.getAmountSign() != null ?
                        ("'" + record.getAmountSign() + "'") : "null";

                errors.add(String.format("Amount inconsistency: Credit(%s) - Debit(%s) = %s, expected Net=%s %s, actual Net=%s %s",
                        record.getCreditAmount(),
                        record.getDebitAmount(),
                        calculatedNet,
                        expectedAbsoluteNet,
                        expectedSign,
                        record.getNetAmount(),
                        actualSign));
            }
        }

        // Validate settlement date range (basic check)
        if (record.getSettlementDate() != null) {
            LocalDate minDate = LocalDate.of(2000, 1, 1);
            LocalDate maxDate = LocalDate.now().plusYears(1);

            if (!VssDateUtil.isDateInRange(record.getSettlementDate(), minDate, maxDate)) {
                errors.add(String.format("Settlement date %s is outside valid range [%s - %s]",
                        record.getSettlementDate(), minDate, maxDate));
            }
        }

        if (!errors.isEmpty()) {
            throw new VssValidationException(errors, lineNumber, "VSS-110", record.getDestinationId());
        }
    }

    /**
     * Gets the expected record length for VSS-110 files.
     *
     * @return the expected record length
     */
    public int getExpectedRecordLength() {
        return expectedRecordLength;
    }

    /**
     * Checks if strict validation is enabled.
     *
     * @return true if strict validation is enabled
     */
    public boolean isStrictValidation() {
        return strictValidation;
    }

    /**
     * Checks if invalid records should be skipped.
     *
     * @return true if invalid records should be skipped
     */
    public boolean isSkipInvalidRecords() {
        return skipInvalidRecords;
    }
}