package com.moneysab.cardexis.parser;

import com.moneysab.cardexis.domain.entity.FileProcessingJob;
import com.moneysab.cardexis.domain.entity.Vss120SettlementRecord;
import com.moneysab.cardexis.domain.entity.VssSubGroup4Record;
import com.moneysab.cardexis.exception.VssParsingException;
import com.moneysab.cardexis.exception.VssValidationException;
import com.moneysab.cardexis.util.VssDateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Parser for VSS-SubGroup4 settlement record files (TC46, TCR0, Report Group V, Subgroup 4).
 *
 * Handles all VSS-4 report types: 120, 130, 131, 135, 136, 140, 210, 215, 230, 640.
 *
 * @author S.AIT MOHAMMED
 * @version 2.0.0
 * @since 2025
 */
@Component
public class VssSubgroup4Parser {

    private static final Logger log = LoggerFactory.getLogger(VssSubgroup4Parser.class);

    // Record length constants
    private static final int EXPECTED_RECORD_LENGTH = 168;
    private static final int MIN_RECORD_LENGTH = 155;

    // Field position constants (1-based positions from specification)
    // Fields 1-18 (positions 1-65) are identical to VSS-110
    private static final int TRANSACTION_CODE_START = 1;
    private static final int TRANSACTION_CODE_END = 2;
    private static final int TRANSACTION_CODE_QUALIFIER_START = 3;
    private static final int TRANSACTION_CODE_QUALIFIER_END = 3;
    private static final int TRANSACTION_COMPONENT_SEQ_START = 4;
    private static final int TRANSACTION_COMPONENT_SEQ_END = 4;
    private static final int DESTINATION_ID_START = 5;
    private static final int DESTINATION_ID_END = 10;
    private static final int SOURCE_ID_START = 11;
    private static final int SOURCE_ID_END = 16;
    private static final int REPORTING_SRE_ID_START = 17;
    private static final int REPORTING_SRE_ID_END = 26;
    private static final int ROLLUP_SRE_ID_START = 27;
    private static final int ROLLUP_SRE_ID_END = 36;
    private static final int FUNDS_TRANSFER_SRE_ID_START = 37;
    private static final int FUNDS_TRANSFER_SRE_ID_END = 46;
    private static final int SETTLEMENT_SERVICE_START = 47;
    private static final int SETTLEMENT_SERVICE_END = 49;

    // VSS-SubGroup4 specific field positions
    private static final int SETTLEMENT_CURRENCY_CODE_START = 50;
    private static final int SETTLEMENT_CURRENCY_CODE_END = 52;
    private static final int CLEARING_CURRENCY_CODE_START = 53;
    private static final int CLEARING_CURRENCY_CODE_END = 55;
    private static final int BUSINESS_MODE_START = 56;
    private static final int BUSINESS_MODE_END = 56;
    private static final int NO_DATA_INDICATOR_START = 57;
    private static final int NO_DATA_INDICATOR_END = 57;
    private static final int RESERVED_FIELD_START = 58;
    private static final int RESERVED_FIELD_END = 58;

    // Common report identification fields
    private static final int REPORT_GROUP_START = 59;
    private static final int REPORT_GROUP_END = 59;
    private static final int REPORT_SUBGROUP_START = 60;
    private static final int REPORT_SUBGROUP_END = 60;
    private static final int REPORT_ID_START = 61;
    private static final int REPORT_ID_END = 63;
    private static final int REPORT_ID_SUFFIX_START = 64;
    private static final int REPORT_ID_SUFFIX_END = 65;

    // Date fields
    private static final int SETTLEMENT_DATE_START = 66;
    private static final int SETTLEMENT_DATE_END = 72;
    private static final int REPORT_DATE_START = 73;
    private static final int REPORT_DATE_END = 79;
    private static final int FROM_DATE_START = 80;
    private static final int FROM_DATE_END = 86;
    private static final int TO_DATE_START = 87;
    private static final int TO_DATE_END = 93;

    // VSS-SubGroup4 specific business fields
    private static final int CHARGE_TYPE_CODE_START = 94;
    private static final int CHARGE_TYPE_CODE_END = 96;
    private static final int BUSINESS_TRANSACTION_TYPE_START = 97;
    private static final int BUSINESS_TRANSACTION_TYPE_END = 99;
    private static final int BUSINESS_TRANSACTION_CYCLE_START = 100;
    private static final int BUSINESS_TRANSACTION_CYCLE_END = 100;
    private static final int REVERSAL_INDICATOR_START = 101;
    private static final int REVERSAL_INDICATOR_END = 101;
    private static final int RETURN_INDICATOR_START = 102;
    private static final int RETURN_INDICATOR_END = 102;
    private static final int JURISDICTION_CODE_START = 103;
    private static final int JURISDICTION_CODE_END = 104;
    private static final int INTERREGIONAL_ROUTING_INDICATOR_START = 105;
    private static final int INTERREGIONAL_ROUTING_INDICATOR_END = 105;
    private static final int SOURCE_COUNTRY_CODE_START = 106;
    private static final int SOURCE_COUNTRY_CODE_END = 108;
    private static final int DESTINATION_COUNTRY_CODE_START = 109;
    private static final int DESTINATION_COUNTRY_CODE_END = 111;
    private static final int SOURCE_REGION_CODE_START = 112;
    private static final int SOURCE_REGION_CODE_END = 113;
    private static final int DESTINATION_REGION_CODE_START = 114;
    private static final int DESTINATION_REGION_CODE_END = 115;
    private static final int FEE_LEVEL_DESCRIPTOR_START = 116;
    private static final int FEE_LEVEL_DESCRIPTOR_END = 131;
    private static final int CR_DB_NET_INDICATOR_START = 132;
    private static final int CR_DB_NET_INDICATOR_END = 132;
    private static final int SUMMARY_LEVEL_START = 133;
    private static final int SUMMARY_LEVEL_END = 134;
    private static final int RESERVED_FIELD_2_START = 135;
    private static final int RESERVED_FIELD_2_END = 136;
    private static final int RESERVED_FIELD_3_START = 137;
    private static final int RESERVED_FIELD_3_END = 167;
    private static final int REIMBURSEMENT_ATTR_START = 168;
    private static final int REIMBURSEMENT_ATTR_END = 168;

    // Validation patterns
    private static final Pattern TRANSACTION_CODE_PATTERN = Pattern.compile("^46$");
    private static final Pattern TRANSACTION_CODE_QUALIFIER_PATTERN = Pattern.compile("^0$");
    private static final Pattern TRANSACTION_COMPONENT_SEQ_PATTERN = Pattern.compile("^0$");
    private static final Pattern DESTINATION_ID_PATTERN = Pattern.compile("^\\d{6}$");
    private static final Pattern REPORT_GROUP_PATTERN = Pattern.compile("^V$");
    private static final Pattern REPORT_SUBGROUP_PATTERN = Pattern.compile("^4$");
    private static final Pattern REPORT_ID_PATTERN = Pattern.compile("^(120|130|131|135|136|140|210|215|230|640)$");
    private static final Pattern BUSINESS_MODE_PATTERN = Pattern.compile("^[1239 ]$");
    private static final Pattern NO_DATA_INDICATOR_PATTERN = Pattern.compile("^[Y ]$");
    private static final Pattern REVERSAL_INDICATOR_PATTERN = Pattern.compile("^[YN ]$");
    private static final Pattern RETURN_INDICATOR_PATTERN = Pattern.compile("^[YN ]$");
    private static final Pattern INTERREGIONAL_ROUTING_PATTERN = Pattern.compile("^[YN ]$");
    private static final Pattern CR_DB_NET_INDICATOR_PATTERN = Pattern.compile("^[CDN ]$");

    // Configuration properties
    @Value("${moneysab.cardexis.processing.strict-validation:true}")
    private boolean strictValidation;

    @Value("${moneysab.cardexis.processing.skip-invalid-records:false}")
    private boolean skipInvalidRecords;

    @Value("${moneysab.cardexis.formats.vss120.expected-length:168}")
    private int expectedRecordLength;

    /**
     * Parses a single VSS-SubGroup4 record line.
     */
    public <T extends VssSubGroup4Record> T parseLine(String line, int lineNumber, FileProcessingJob fileProcessingJob,Class<T> clazz) {
        if (line == null) {
            throw VssParsingException.missingRequiredField("record_line", lineNumber, clazz.getSimpleName());        }

        log.debug("Parsing {} record at line {}: length={}", clazz.getSimpleName(), lineNumber, line.length());

        // Create new record instance

        T record =null;

        try {
            record = clazz.getDeclaredConstructor().newInstance();
            log.debug("Instantiated record of type {}", record.getClass().getSimpleName());
            record.setFileProcessingJob(fileProcessingJob);
            record.setLineNumber(lineNumber);
            record.setRawRecordLine(line);
            // Validate record length
            validateRecordLength(line, lineNumber);

            // Parse all fields according to VSS-SubGroup4 specification
            parseCommonHeaderFields(record, line, lineNumber);
            parseVssV4SpecificFields(record, line, lineNumber);
            parseReportIdentificationFields(record, line, lineNumber);
            parseDateFields(record, line, lineNumber);
            parseBusinessFields(record, line, lineNumber);
            parseReservedFields(record, line, lineNumber);

            // Validate the parsed record
            validateRecord(record, lineNumber);

            log.debug("Successfully parsed VSS-SubGroup4 record: {}", record.getRecordDescription());

        } catch (VssParsingException | VssValidationException e) {
            if (record != null) {
                record.setIsValid(false);
                record.setValidationErrors(e.getMessage());
            }

            if (strictValidation && !skipInvalidRecords) {
                throw e;
            }

            log.warn("Invalid VSS-SubGroup4 record at line {}: {}", lineNumber, e.getMessage());
        } catch (Exception e) {
            if (record != null) {
                record.setIsValid(false);
                record.setValidationErrors(e.getMessage());
            }

            VssParsingException parsingException = new VssParsingException(
                    "Unexpected error parsing VSS-SubGroup4 record", lineNumber, "VSS-SubGroup4", e);

            if (strictValidation && !skipInvalidRecords) {
                throw parsingException;
            }

            log.error("Unexpected error parsing VSS-SubGroup4 record at line {}", lineNumber, e);
        }

        return record;
    }

    /**
     * Parse common header fields (positions 1-49).
     */
    private void parseCommonHeaderFields(VssSubGroup4Record  record, String line, int lineNumber) {
        // Fields 1-3: Transaction code info
        String transactionCode = extractField(line, TRANSACTION_CODE_START, TRANSACTION_CODE_END, "transaction_code", lineNumber);
        if (!TRANSACTION_CODE_PATTERN.matcher(transactionCode).matches()) {
            throw VssParsingException.invalidFieldFormat("transaction_code", "46", transactionCode, lineNumber, "VSS-SubGroup4");
        }
        record.setTransactionCode(transactionCode);

        String qualifier = extractField(line, TRANSACTION_CODE_QUALIFIER_START, TRANSACTION_CODE_QUALIFIER_END, "transaction_code_qualifier", lineNumber);
        if (!TRANSACTION_CODE_QUALIFIER_PATTERN.matcher(qualifier).matches()) {
            if (strictValidation) {
                throw VssParsingException.invalidFieldFormat("transaction_code_qualifier", "0", qualifier, lineNumber, "VSS-SubGroup4");
            } else {
                log.warn("Invalid transaction code qualifier at line {}: expected='0', actual='{}'", lineNumber, qualifier);
            }
        }
        record.setTransactionCodeQualifier(qualifier);

        String seqNumber = extractField(line, TRANSACTION_COMPONENT_SEQ_START, TRANSACTION_COMPONENT_SEQ_END, "transaction_component_sequence_number", lineNumber);
        if (!TRANSACTION_COMPONENT_SEQ_PATTERN.matcher(seqNumber).matches()) {
            if (strictValidation) {
                throw VssParsingException.invalidFieldFormat("transaction_component_sequence_number", "0", seqNumber, lineNumber, "VSS-SubGroup4");
            } else {
                log.warn("Invalid transaction component sequence number at line {}: expected='0', actual='{}'", lineNumber, seqNumber);
            }
        }
        record.setTransactionComponentSequenceNumber(seqNumber);

        // Fields 4-5: Identifiers
        String destinationId = extractField(line, DESTINATION_ID_START, DESTINATION_ID_END, "destination_id", lineNumber);
        if (!DESTINATION_ID_PATTERN.matcher(destinationId).matches()) {
            throw VssParsingException.invalidFieldFormat("destination_id", "6 digits", destinationId, lineNumber, "VSS-SubGroup4");
        }
        record.setDestinationId(destinationId);

        String sourceId = extractField(line, SOURCE_ID_START, SOURCE_ID_END, "source_id", lineNumber);
        record.setSourceIdentifier(sourceId);

        // Fields 6-8: SRE Identifiers
        String reportingSreId = extractField(line, REPORTING_SRE_ID_START, REPORTING_SRE_ID_END, "reporting_sre_id", lineNumber);
        if (reportingSreId.length() > 100) {
            reportingSreId = reportingSreId.substring(0, 100);
            log.debug("Truncated reporting SRE ID to 100 characters at line {}", lineNumber);
        }
        record.setReportingSreId(reportingSreId);

        String rollupSreId = extractField(line, ROLLUP_SRE_ID_START, ROLLUP_SRE_ID_END, "rollup_sre_id", lineNumber);
        if (rollupSreId.length() > 100) {
            rollupSreId = rollupSreId.substring(0, 100);
            log.debug("Truncated rollup SRE ID to 100 characters at line {}", lineNumber);
        }
        record.setRollupSreId(rollupSreId);

        String fundsTransferSreId = extractField(line, FUNDS_TRANSFER_SRE_ID_START, FUNDS_TRANSFER_SRE_ID_END, "funds_transfer_sre_id", lineNumber);
        if (fundsTransferSreId.length() > 100) {
            fundsTransferSreId = fundsTransferSreId.substring(0, 100);
            log.debug("Truncated funds transfer SRE ID to 100 characters at line {}", lineNumber);
        }
        record.setFundsTransferSreId(fundsTransferSreId);

        // Field 9: Settlement Service
        String settlementService = extractField(line, SETTLEMENT_SERVICE_START, SETTLEMENT_SERVICE_END, "settlement_service", lineNumber);
        record.setSettlementService(settlementService);
    }

    /**
     * Parse VSS-SubGroup4 specific fields (positions 50-58).
     */
    private void parseVssV4SpecificFields(VssSubGroup4Record  record, String line, int lineNumber) {
        // Field 10: Settlement Currency Code
        String settlementCurrencyCode = extractField(line, SETTLEMENT_CURRENCY_CODE_START, SETTLEMENT_CURRENCY_CODE_END, "settlement_currency_code", lineNumber);
        record.setSettlementCurrencyCode(settlementCurrencyCode.trim());

        // Field 11: Clearing Currency Code
        String clearingCurrencyCode = extractField(line, CLEARING_CURRENCY_CODE_START, CLEARING_CURRENCY_CODE_END, "clearing_currency_code", lineNumber);
        record.setClearingCurrencyCode(clearingCurrencyCode.trim());

        // Field 12: Business Mode - Accept all values
        String businessMode = extractField(line, BUSINESS_MODE_START, BUSINESS_MODE_END, "business_mode", lineNumber);
        // Accept any business mode value (including spaces and other characters)
        record.setBusinessMode(businessMode);
        log.debug("Business mode at line {}: '{}'", lineNumber, businessMode);

        // Field 13: No Data Indicator
        String noDataIndicator = extractField(line, NO_DATA_INDICATOR_START, NO_DATA_INDICATOR_END, "no_data_indicator", lineNumber);
        if (!NO_DATA_INDICATOR_PATTERN.matcher(noDataIndicator).matches()) {
            if (strictValidation) {
                throw VssParsingException.invalidFieldFormat("no_data_indicator", "Y or space", noDataIndicator, lineNumber, "VSS-SubGroup4");
            } else {
                log.warn("Invalid no data indicator at line {}: expected='Y or space', actual='{}'", lineNumber, noDataIndicator);
            }
        }
        record.setNoDataIndicator(noDataIndicator);

        // Field 14: Reserved
        String reserved = extractField(line, RESERVED_FIELD_START, RESERVED_FIELD_END, "reserved", lineNumber);
        if (strictValidation && !reserved.trim().isEmpty()) {
            log.warn("Reserved field contains non-space characters at line {}: '{}'", lineNumber, reserved);
        }
        record.setReservedField(reserved);
    }

    /**
     * Parse report identification fields (positions 59-65).
     */
    private void parseReportIdentificationFields(VssSubGroup4Record  record, String line, int lineNumber) {
        // Field 15: Report Group
        String reportGroup = extractField(line, REPORT_GROUP_START, REPORT_GROUP_END, "report_group", lineNumber);
        if (!REPORT_GROUP_PATTERN.matcher(reportGroup).matches()) {
            throw VssParsingException.invalidFieldFormat("report_group", "V", reportGroup, lineNumber, "VSS-SubGroup4");
        }
        record.setReportGroup(reportGroup);

        // Field 16: Report Subgroup
        String reportSubgroup = extractField(line, REPORT_SUBGROUP_START, REPORT_SUBGROUP_END, "report_subgroup", lineNumber);
        if (!REPORT_SUBGROUP_PATTERN.matcher(reportSubgroup).matches()) {
            throw VssParsingException.invalidFieldFormat("report_subgroup", "4", reportSubgroup, lineNumber, "VSS-SubGroup4");
        }
        record.setReportSubgroup(reportSubgroup);

        // Field 17: Report ID Number
        String reportIdNumber = extractField(line, REPORT_ID_START, REPORT_ID_END, "report_id_number", lineNumber);
        if (!REPORT_ID_PATTERN.matcher(reportIdNumber).matches()) {
            throw VssParsingException.invalidFieldFormat("report_id_number", "120, 130, 131, 135, 136, 140, 210, 215, 230, or 640", reportIdNumber, lineNumber, "VSS-SubGroup4");
        }
        record.setReportIdNumber(reportIdNumber);

        // Field 18: Report ID Suffix
        String reportIdSuffix = extractField(line, REPORT_ID_SUFFIX_START, REPORT_ID_SUFFIX_END, "report_id_suffix", lineNumber);
        record.setReportIdSuffix(reportIdSuffix);
    }

    /**
     * Parse date fields (positions 66-93).
     */
    private void parseDateFields(VssSubGroup4Record  record, String line, int lineNumber) {
        // Field 19: Settlement Date
        String rawDate = extractField(line, SETTLEMENT_DATE_START, SETTLEMENT_DATE_END, "settlement_date", lineNumber);
        String trimmedRawDate = rawDate.trim();
        record.setRawSettlementDate(trimmedRawDate);

        try {
            LocalDate settlementDate = VssDateUtil.parseCCYYDDD(trimmedRawDate, lineNumber, "VSS-SubGroup4");
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

        // Field 20: Report Date
        String rawReportDate = extractField(line, REPORT_DATE_START, REPORT_DATE_END, "report_date", lineNumber);
        record.setRawReportDate(rawReportDate);

        String trimmedRawReportDate = rawReportDate.trim();
        if (!trimmedRawReportDate.isEmpty() && !trimmedRawReportDate.matches("^\\s*$")) {
            try {
                LocalDate reportDate = VssDateUtil.parseCCYYDDD(trimmedRawReportDate, lineNumber, "VSS-SubGroup4");
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

        // Field 21: From Date
        String fromDateRaw = extractField(line, FROM_DATE_START, FROM_DATE_END, "from_date", lineNumber);
        record.setRawFromDate(fromDateRaw);

        String trimmedFromDate = fromDateRaw.trim();
        if (!trimmedFromDate.isEmpty() && !trimmedFromDate.matches("^\\s*$")) {
            try {
                LocalDate fromDate = VssDateUtil.parseCCYYDDD(trimmedFromDate, lineNumber, "VSS-SubGroup4");
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

        // Field 22: To Date
        String toDateRaw = extractField(line, TO_DATE_START, TO_DATE_END, "to_date", lineNumber);
        record.setRawToDate(toDateRaw);

        String trimmedToDate = toDateRaw.trim();
        if (!trimmedToDate.isEmpty() && !trimmedToDate.matches("^\\s*$")) {
            try {
                LocalDate toDate = VssDateUtil.parseCCYYDDD(trimmedToDate, lineNumber, "VSS-SubGroup4");
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
     * Parse business fields (positions 94-134).
     */
    private void parseBusinessFields(VssSubGroup4Record  record, String line, int lineNumber) {
        // Field 23: Charge Type Code
        String chargeTypeCode = extractField(line, CHARGE_TYPE_CODE_START, CHARGE_TYPE_CODE_END, "charge_type_code", lineNumber);
        record.setChargeTypeCode(chargeTypeCode.trim());

        // Field 24: Business Transaction Type
        String businessTransactionType = extractField(line, BUSINESS_TRANSACTION_TYPE_START, BUSINESS_TRANSACTION_TYPE_END, "business_transaction_type", lineNumber);
        record.setBusinessTransactionType(businessTransactionType.trim());

        // Field 25: Business Transaction Cycle
        String businessTransactionCycle = extractField(line, BUSINESS_TRANSACTION_CYCLE_START, BUSINESS_TRANSACTION_CYCLE_END, "business_transaction_cycle", lineNumber);
        record.setBusinessTransactionCycle(businessTransactionCycle);

        // Field 26: Reversal Indicator
        String reversalIndicator = extractField(line, REVERSAL_INDICATOR_START, REVERSAL_INDICATOR_END, "reversal_indicator", lineNumber);
        if (!REVERSAL_INDICATOR_PATTERN.matcher(reversalIndicator).matches()) {
            if (strictValidation) {
                throw VssParsingException.invalidFieldFormat("reversal_indicator", "Y, N, or space", reversalIndicator, lineNumber, "VSS-SubGroup4");
            } else {
                log.warn("Invalid reversal indicator at line {}: expected='Y, N, or space', actual='{}'", lineNumber, reversalIndicator);
            }
        }
        record.setReversalIndicator(reversalIndicator);

        // Field 27: Return Indicator
        String returnIndicator = extractField(line, RETURN_INDICATOR_START, RETURN_INDICATOR_END, "return_indicator", lineNumber);
        if (!RETURN_INDICATOR_PATTERN.matcher(returnIndicator).matches()) {
            if (strictValidation) {
                throw VssParsingException.invalidFieldFormat("return_indicator", "Y, N, or space", returnIndicator, lineNumber, "VSS-SubGroup4");
            } else {
                log.warn("Invalid return indicator at line {}: expected='Y, N, or space', actual='{}'", lineNumber, returnIndicator);
            }
        }
        record.setReturnIndicator(returnIndicator);

        // Field 28: Jurisdiction Code
        String jurisdictionCode = extractField(line, JURISDICTION_CODE_START, JURISDICTION_CODE_END, "jurisdiction_code", lineNumber);
        record.setJurisdictionCode(jurisdictionCode.trim());

        // Field 29: Interregional Routing Indicator
        String interregionalRoutingIndicator = extractField(line, INTERREGIONAL_ROUTING_INDICATOR_START, INTERREGIONAL_ROUTING_INDICATOR_END, "interregional_routing_indicator", lineNumber);
        if (!INTERREGIONAL_ROUTING_PATTERN.matcher(interregionalRoutingIndicator).matches()) {
            if (strictValidation) {
                throw VssParsingException.invalidFieldFormat("interregional_routing_indicator", "Y, N, or space", interregionalRoutingIndicator, lineNumber, "VSS-SubGroup4");
            } else {
                log.warn("Invalid interregional routing indicator at line {}: expected='Y, N, or space', actual='{}'", lineNumber, interregionalRoutingIndicator);
            }
        }
        record.setInterregionalRoutingIndicator(interregionalRoutingIndicator);

        // Field 30: Source Country Code
        String sourceCountryCode = extractField(line, SOURCE_COUNTRY_CODE_START, SOURCE_COUNTRY_CODE_END, "source_country_code", lineNumber);
        record.setSourceCountryCode(sourceCountryCode.trim());

        // Field 31: Destination Country Code
        String destinationCountryCode = extractField(line, DESTINATION_COUNTRY_CODE_START, DESTINATION_COUNTRY_CODE_END, "destination_country_code", lineNumber);
        record.setDestinationCountryCode(destinationCountryCode.trim());

        // Field 32: Source Region Code
        String sourceRegionCode = extractField(line, SOURCE_REGION_CODE_START, SOURCE_REGION_CODE_END, "source_region_code", lineNumber);
        record.setSourceRegionCode(sourceRegionCode.trim());

        // Field 33: Destination Region Code
        String destinationRegionCode = extractField(line, DESTINATION_REGION_CODE_START, DESTINATION_REGION_CODE_END, "destination_region_code", lineNumber);
        record.setDestinationRegionCode(destinationRegionCode.trim());

        // Field 34: Fee Level Descriptor
        String feeLevelDescriptor = extractField(line, FEE_LEVEL_DESCRIPTOR_START, FEE_LEVEL_DESCRIPTOR_END, "fee_level_descriptor", lineNumber);
        record.setFeeLevelDescriptor(feeLevelDescriptor.trim());

        // Field 35: CR/DB/NET Indicator
        String crDbNetIndicator = extractField(line, CR_DB_NET_INDICATOR_START, CR_DB_NET_INDICATOR_END, "cr_db_net_indicator", lineNumber);
        if (!CR_DB_NET_INDICATOR_PATTERN.matcher(crDbNetIndicator).matches()) {
            if (strictValidation) {
                throw VssParsingException.invalidFieldFormat("cr_db_net_indicator", "C, D, N, or space", crDbNetIndicator, lineNumber, "VSS-SubGroup4");
            } else {
                log.warn("Invalid CR/DB/NET indicator at line {}: expected='C, D, N, or space', actual='{}'", lineNumber, crDbNetIndicator);
            }
        }
        record.setCrDbNetIndicator(crDbNetIndicator);

        // Field 36: Summary Level
        String summaryLevel = extractField(line, SUMMARY_LEVEL_START, SUMMARY_LEVEL_END, "summary_level", lineNumber);
        record.setSummaryLevel(summaryLevel.trim());
    }

    /**
     * Parse reserved fields (positions 135-168).
     */
    private void parseReservedFields(VssSubGroup4Record  record, String line, int lineNumber) {
        // Field 37: Reserved field 2
        String reserved2 = extractField(line, RESERVED_FIELD_2_START, RESERVED_FIELD_2_END, "reserved2", lineNumber);
        if (strictValidation && !reserved2.trim().isEmpty()) {
            log.warn("Reserved field 2 contains non-space characters at line {}: '{}'", lineNumber, reserved2);
        }
        record.setReservedField2(reserved2);

        // Field 38: Reserved field 3
        String reserved3 = extractField(line, RESERVED_FIELD_3_START, RESERVED_FIELD_3_END, "reserved3", lineNumber);
        if (strictValidation && !reserved3.trim().isEmpty()) {
            log.warn("Reserved field 3 contains non-space characters at line {}: '{}'", lineNumber, reserved3);
        }
        record.setReservedField3(reserved3);

        // Field 39: Reimbursement Attribute
        if (line.length() >= REIMBURSEMENT_ATTR_END) {
            String reimbursementAttr = extractField(line, REIMBURSEMENT_ATTR_START, REIMBURSEMENT_ATTR_END, "reimbursement_attribute", lineNumber);

            if (strictValidation && !"0".equals(reimbursementAttr.trim()) && !" ".equals(reimbursementAttr)) {
                log.warn("Reimbursement attribute should be zero-filled at line {}: '{}'", lineNumber, reimbursementAttr);
            }

            record.setReimbursementAttribute(reimbursementAttr);
            log.debug("Reimbursement Attribute at line {}: '{}'", lineNumber, reimbursementAttr);
        }
    }

    /**
     * Parses multiple VSS-SubGroup4 record lines.
     *
     * @param lines the list of record lines to parse
     * @param fileProcessingJob the associated file processing job
     * @return the list of parsed VssSubGroup4Record  objects
     */
    public <T extends VssSubGroup4Record> List<T> parseFile(List<String> lines, FileProcessingJob fileProcessingJob,Class<T> clazz) {
        if (lines == null || lines.isEmpty()) {
            log.warn("No lines provided for VSS-SubGroup4 parsing");
            return new ArrayList<>();
        }

        log.info("Parsing VSS-SubGroup4 file with {} lines", lines.size());

        List<T> records = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            int lineNumber = i + 1;
            String line = lines.get(i);

            try {
                T record = parseLine(line, lineNumber, fileProcessingJob, clazz);
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
            log.warn("VSS-SubGroup4 file parsing completed with {} errors", errors.size());
            if (strictValidation && !skipInvalidRecords && !errors.isEmpty()) {
                throw new VssParsingException(
                        String.format("File parsing failed with %d errors: %s",
                                errors.size(), String.join("; ", errors.subList(0, Math.min(3, errors.size())))));
            }
        }

        log.info("Successfully parsed {} VSS-SubGroup4 records from {} lines", records.size(), lines.size());
        return records;
    }

    /**
     * Validates the record length.
     */
    private void validateRecordLength(String line, int lineNumber) {
        if (line.length() < MIN_RECORD_LENGTH) {
            throw VssParsingException.invalidRecordLength(expectedRecordLength, line.length(), lineNumber, "VSS-SubGroup4");
        }

        if (strictValidation && line.length() != expectedRecordLength) {
            log.warn("VSS-SubGroup4 record at line {} has unexpected length: expected={}, actual={}",
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
                throw VssParsingException.missingRequiredField(fieldName, lineNumber, "VSS-SubGroup4");
            }

            return line.substring(start, end);

        } catch (StringIndexOutOfBoundsException e) {
            throw VssParsingException.invalidFieldFormat(fieldName,
                    String.format("positions %d-%d", startPos, endPos),
                    "field extends beyond record length", lineNumber, "VSS-SubGroup4");
        }
    }

    /**
     * Validates the parsed record according to business rules.
     */
    private void validateRecord(VssSubGroup4Record record, int lineNumber) {
        List<String> errors = new ArrayList<>();

        // Validate report ID against valid VSS-4 report types
        if (!VssSubGroup4Record.VALID_REPORT_IDS.contains(record.getReportIdNumber())) {
            errors.add(String.format("Invalid report ID '%s' for VSS-SubGroup4 format", record.getReportIdNumber()));
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

        // Validate business mode
        if (!VssSubGroup4Record.VALID_BUSINESS_MODES.contains(record.getBusinessMode())) {
            errors.add(String.format("Invalid business mode '%s'", record.getBusinessMode()));
        }

        // Additional VSS-SubGroup4 specific validations
        if ("Y".equals(record.getNoDataIndicator()) &&
                (record.getChargeTypeCode() != null && !record.getChargeTypeCode().trim().isEmpty())) {
            log.warn("Record at line {} has no data indicator 'Y' but contains charge type code", lineNumber);
        }

        if (!errors.isEmpty()) {
            throw new VssValidationException(errors, lineNumber, "VSS-SubGroup4", record.getDestinationId());
        }
    }

    /**
     * Gets the expected record length for VSS-SubGroup4 files.
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