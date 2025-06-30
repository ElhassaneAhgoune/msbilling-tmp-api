package com.moneysab.cardexis.parser;

import com.moneysab.cardexis.domain.entity.FileProcessingJob;
import com.moneysab.cardexis.domain.entity.Vss120SettlementRecord;
import com.moneysab.cardexis.domain.entity.Vss120Tcr1Record;
import com.moneysab.cardexis.domain.entity.VssSubGroup4Record;
import com.moneysab.cardexis.exception.VssParsingException;
import com.moneysab.cardexis.exception.VssValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Parser for VSS-120 TCR1 records (TC46, TCR1, Report Group V, Subgroup 4).
 *
 * TCR1 records contain count and amount data that correspond to VSS-120 TCR0 records.
 *
 * @author EL.AHGOUNE
 * @version 1.0.0
 * @since 2024
 */
@Component
public class Vss120Tcr1Parser {

    private static final Logger log = LoggerFactory.getLogger(Vss120Tcr1Parser.class);

    // Record length constants
    private static final int EXPECTED_RECORD_LENGTH = 168;
    private static final int MIN_RECORD_LENGTH = 143;

    // Field position constants (1-based positions from specification)
    // CORRECTED based on actual file format analysis
    private static final int TRANSACTION_CODE_START = 1;
    private static final int TRANSACTION_CODE_END = 2;
    private static final int TRANSACTION_CODE_QUALIFIER_START = 3;
    private static final int TRANSACTION_CODE_QUALIFIER_END = 3;
    private static final int TRANSACTION_COMPONENT_SEQ_START = 4;
    private static final int TRANSACTION_COMPONENT_SEQ_END = 4;

    // In TCR1 records, positions 5-11 contain Rate Table ID and reserved fields
    // There's no separate destination ID field - it's linked through the parent record
    private static final int RATE_TABLE_ID_START = 5;
    private static final int RATE_TABLE_ID_END = 9;
    private static final int RESERVED_FIELD_START = 10;
    private static final int RESERVED_FIELD_END = 11;

    // Count and amount field positions
    private static final int FIRST_COUNT_START = 12;
    private static final int FIRST_COUNT_END = 26;
    private static final int SECOND_COUNT_START = 27;
    private static final int SECOND_COUNT_END = 41;
    private static final int FIRST_AMOUNT_START = 42;
    private static final int FIRST_AMOUNT_END = 56;
    private static final int FIRST_AMOUNT_SIGN_START = 57;
    private static final int FIRST_AMOUNT_SIGN_END = 58;
    private static final int SECOND_AMOUNT_START = 59;
    private static final int SECOND_AMOUNT_END = 73;
    private static final int SECOND_AMOUNT_SIGN_START = 74;
    private static final int SECOND_AMOUNT_SIGN_END = 75;
    private static final int THIRD_AMOUNT_START = 76;
    private static final int THIRD_AMOUNT_END = 90;
    private static final int THIRD_AMOUNT_SIGN_START = 91;
    private static final int THIRD_AMOUNT_SIGN_END = 92;
    private static final int FOURTH_AMOUNT_START = 93;
    private static final int FOURTH_AMOUNT_END = 107;
    private static final int FOURTH_AMOUNT_SIGN_START = 108;
    private static final int FOURTH_AMOUNT_SIGN_END = 109;
    private static final int FIFTH_AMOUNT_START = 110;
    private static final int FIFTH_AMOUNT_END = 124;
    private static final int FIFTH_AMOUNT_SIGN_START = 125;
    private static final int FIFTH_AMOUNT_SIGN_END = 126;
    private static final int SIXTH_AMOUNT_START = 127;
    private static final int SIXTH_AMOUNT_END = 141;
    private static final int SIXTH_AMOUNT_SIGN_START = 142;
    private static final int SIXTH_AMOUNT_SIGN_END = 143;
    private static final int RESERVED_FIELD_2_START = 144;
    private static final int RESERVED_FIELD_2_END = 168;

    // Validation patterns - FIXED for better compatibility
    private static final Pattern TRANSACTION_CODE_PATTERN = Pattern.compile("^46$");
    private static final Pattern TRANSACTION_CODE_QUALIFIER_PATTERN = Pattern.compile("^0$");
    private static final Pattern TRANSACTION_COMPONENT_SEQ_PATTERN = Pattern.compile("^1$");
    private static final Pattern DESTINATION_ID_PATTERN = Pattern.compile("^\\d{6}$");
    private static final Pattern NUMERIC_FIELD_PATTERN = Pattern.compile("^[0-9 ]{15}$");
    private static final Pattern AMOUNT_SIGN_PATTERN = Pattern.compile("^(CR|DB|  |)$"); // Allow empty signs

    // Configuration properties
    @Value("${moneysab.cardexis.processing.strict-validation:true}")
    private boolean strictValidation;

    @Value("${moneysab.cardexis.processing.skip-invalid-records:false}")
    private boolean skipInvalidRecords;

    @Value("${moneysab.cardexis.formats.vss120-tcr1.expected-length:168}")
    private int expectedRecordLength;

    /**
     * Parses a single VSS-120 TCR1 record line.
     */
    public Vss120Tcr1Record parseLine(String line, int lineNumber, FileProcessingJob fileProcessingJob,
                                      VssSubGroup4Record parentRecord) {
        if (line == null) {
            throw VssParsingException.missingRequiredField("record_line", lineNumber, "VSS-120-TCR1");
        }

        log.debug("Parsing VSS-120 TCR1 record at line {}: length={}", lineNumber, line.length());

        // Create new record instance
        Vss120Tcr1Record record = new Vss120Tcr1Record(fileProcessingJob, parentRecord);
        record.setLineNumber(lineNumber);
        record.setRawRecordLine(line);

        try {
            // Validate record length
            validateRecordLength(line, lineNumber);

            // Parse all fields according to VSS-120 TCR1 specification
            parseHeaderFields(record, line, lineNumber);
            parseIdentificationFields(record, line, lineNumber,parentRecord);
            parseCountAndAmountFields(record, line, lineNumber);
            parseReservedFields(record, line, lineNumber);

            // Validate the parsed record
            validateRecord(record, lineNumber,parentRecord);

            log.debug("Successfully parsed VSS-120 TCR1 record: {}", record.getRecordDescription());

        } catch (VssParsingException | VssValidationException e) {
            record.setIsValid(false);
            record.setValidationErrors(e.getMessage());

            if (strictValidation && !skipInvalidRecords) {
                throw e;
            }

            log.warn("Invalid VSS-120 TCR1 record at line {}: {}", lineNumber, e.getMessage());
        } catch (Exception e) {
            record.setIsValid(false);
            record.setValidationErrors("Unexpected parsing error: " + e.getMessage());

            VssParsingException parsingException = new VssParsingException(
                    "Unexpected error parsing VSS-120 TCR1 record", lineNumber, "VSS-120-TCR1", e);

            if (strictValidation && !skipInvalidRecords) {
                throw parsingException;
            }

            log.error("Unexpected error parsing VSS-120 TCR1 record at line {}", lineNumber, e);
        }

        return record;
    }

    /**
     * Parse header fields (positions 1-4).
     */
    private void parseHeaderFields(Vss120Tcr1Record record, String line, int lineNumber) {
        // Field 1: Transaction Code
        String transactionCode = extractField(line, TRANSACTION_CODE_START, TRANSACTION_CODE_END, "transaction_code", lineNumber);
        if (!TRANSACTION_CODE_PATTERN.matcher(transactionCode).matches()) {
            throw VssParsingException.invalidFieldFormat("transaction_code", "46", transactionCode, lineNumber, "VSS-120-TCR1");
        }
        record.setTransactionCode(transactionCode);

        // Field 2: Transaction Code Qualifier
        String qualifier = extractField(line, TRANSACTION_CODE_QUALIFIER_START, TRANSACTION_CODE_QUALIFIER_END, "transaction_code_qualifier", lineNumber);
        if (!TRANSACTION_CODE_QUALIFIER_PATTERN.matcher(qualifier).matches()) {
            if (strictValidation) {
                throw VssParsingException.invalidFieldFormat("transaction_code_qualifier", "0", qualifier, lineNumber, "VSS-120-TCR1");
            } else {
                log.debug("Non-standard transaction code qualifier at line {}: expected='0', actual='{}'", lineNumber, qualifier);
            }
        }
        record.setTransactionCodeQualifier(qualifier);

        // Field 3: Transaction Component Sequence Number
        String seqNumber = extractField(line, TRANSACTION_COMPONENT_SEQ_START, TRANSACTION_COMPONENT_SEQ_END, "transaction_component_sequence_number", lineNumber);
        if (!TRANSACTION_COMPONENT_SEQ_PATTERN.matcher(seqNumber).matches()) {
            throw VssParsingException.invalidFieldFormat("transaction_component_sequence_number", "1", seqNumber, lineNumber, "VSS-120-TCR1");
        }
        record.setTransactionComponentSequenceNumber(seqNumber);
    }

    /**
     * Parse identification fields (positions 5-11).
     * FIXED: Proper handling based on actual file format - TCR1 records don't have destination ID in positions 5-10
     */
    private void parseIdentificationFields(Vss120Tcr1Record record, String line, int lineNumber,VssSubGroup4Record parentRecord) {
        // For TCR1 records, the destination ID comes from the parent record
        // Positions 5-9 contain the Rate Table ID, not destination ID
        if (record.getParentVssRecord() != null) {
            record.setDestinationId(parentRecord.getDestinationId());
            log.debug("Set destination ID from parent: {}", record.getDestinationId());
        } else {
            // Fallback: use a default destination ID and log warning
            log.warn("TCR1 record at line {} has no parent record - using default destination ID", lineNumber);
            record.setDestinationId("000000");
        }

        // Field: Rate Table ID (positions 5-9)
        if (line.length() >= 9) {
            String rateTableId = extractField(line, RATE_TABLE_ID_START, RATE_TABLE_ID_END, "rate_table_id", lineNumber);
            String cleanRateTableId = rateTableId.trim();
            record.setRateTableId(cleanRateTableId);
            log.debug("Rate Table ID at line {}: '{}'", lineNumber, cleanRateTableId);
        }

        // Field: Reserved (positions 10-11)
        if (line.length() >= 11) {
            String reserved = extractField(line, RESERVED_FIELD_START, RESERVED_FIELD_END, "reserved", lineNumber);
            if (strictValidation && !reserved.trim().isEmpty()) {
                log.debug("Reserved field contains non-space characters at line {}: '{}'", lineNumber, reserved);
            }
            record.setReservedField(reserved);
        }
    }

    /**
     * Parse count and amount fields (positions 12-143).
     */
    private void parseCountAndAmountFields(Vss120Tcr1Record record, String line, int lineNumber) {
        // Field 7: First Count
        String firstCountStr = extractField(line, FIRST_COUNT_START, FIRST_COUNT_END, "first_count", lineNumber);
        Long firstCount = parseCount(firstCountStr, "first_count", lineNumber);
        record.setFirstCount(firstCount);

        // Field 8: Second Count
        String secondCountStr = extractField(line, SECOND_COUNT_START, SECOND_COUNT_END, "second_count", lineNumber);
        Long secondCount = parseCount(secondCountStr, "second_count", lineNumber);
        record.setSecondCount(secondCount);

        // Field 9: First Amount
        String firstAmountStr = extractField(line, FIRST_AMOUNT_START, FIRST_AMOUNT_END, "first_amount", lineNumber);
        BigDecimal firstAmount = parseAmount(firstAmountStr, "first_amount", lineNumber);
        record.setFirstAmount(firstAmount);

        // Field 10: First Amount Sign
        String firstAmountSign = extractField(line, FIRST_AMOUNT_SIGN_START, FIRST_AMOUNT_SIGN_END, "first_amount_sign", lineNumber);
        if (!AMOUNT_SIGN_PATTERN.matcher(firstAmountSign).matches()) {
            if (strictValidation) {
                throw VssParsingException.invalidFieldFormat("first_amount_sign", "CR, DB, or spaces", firstAmountSign, lineNumber, "VSS-120-TCR1");
            } else {
                log.debug("Non-standard first amount sign at line {}: '{}'", lineNumber, firstAmountSign);
            }
        }
        record.setFirstAmountSign(firstAmountSign);

        // Field 11: Second Amount
        String secondAmountStr = extractField(line, SECOND_AMOUNT_START, SECOND_AMOUNT_END, "second_amount", lineNumber);
        BigDecimal secondAmount = parseAmount(secondAmountStr, "second_amount", lineNumber);
        record.setSecondAmount(secondAmount);

        // Field 12: Second Amount Sign
        String secondAmountSign = extractField(line, SECOND_AMOUNT_SIGN_START, SECOND_AMOUNT_SIGN_END, "second_amount_sign", lineNumber);
        if (!AMOUNT_SIGN_PATTERN.matcher(secondAmountSign).matches()) {
            if (strictValidation) {
                throw VssParsingException.invalidFieldFormat("second_amount_sign", "CR, DB, or spaces", secondAmountSign, lineNumber, "VSS-120-TCR1");
            } else {
                log.debug("Non-standard second amount sign at line {}: '{}'", lineNumber, secondAmountSign);
            }
        }
        record.setSecondAmountSign(secondAmountSign);

        // Field 13: Third Amount
        String thirdAmountStr = extractField(line, THIRD_AMOUNT_START, THIRD_AMOUNT_END, "third_amount", lineNumber);
        BigDecimal thirdAmount = parseAmount(thirdAmountStr, "third_amount", lineNumber);
        record.setThirdAmount(thirdAmount);

        // Field 14: Third Amount Sign
        String thirdAmountSign = extractField(line, THIRD_AMOUNT_SIGN_START, THIRD_AMOUNT_SIGN_END, "third_amount_sign", lineNumber);
        if (!AMOUNT_SIGN_PATTERN.matcher(thirdAmountSign).matches()) {
            if (strictValidation) {
                throw VssParsingException.invalidFieldFormat("third_amount_sign", "CR, DB, or spaces", thirdAmountSign, lineNumber, "VSS-120-TCR1");
            } else {
                log.debug("Non-standard third amount sign at line {}: '{}'", lineNumber, thirdAmountSign);
            }
        }
        record.setThirdAmountSign(thirdAmountSign);

        // Field 15: Fourth Amount
        String fourthAmountStr = extractField(line, FOURTH_AMOUNT_START, FOURTH_AMOUNT_END, "fourth_amount", lineNumber);
        BigDecimal fourthAmount = parseAmount(fourthAmountStr, "fourth_amount", lineNumber);
        record.setFourthAmount(fourthAmount);

        // Field 16: Fourth Amount Sign
        String fourthAmountSign = extractField(line, FOURTH_AMOUNT_SIGN_START, FOURTH_AMOUNT_SIGN_END, "fourth_amount_sign", lineNumber);
        if (!AMOUNT_SIGN_PATTERN.matcher(fourthAmountSign).matches()) {
            if (strictValidation) {
                throw VssParsingException.invalidFieldFormat("fourth_amount_sign", "CR, DB, or spaces", fourthAmountSign, lineNumber, "VSS-120-TCR1");
            } else {
                log.debug("Non-standard fourth amount sign at line {}: '{}'", lineNumber, fourthAmountSign);
            }
        }
        record.setFourthAmountSign(fourthAmountSign);

        // Field 17: Fifth Amount
        String fifthAmountStr = extractField(line, FIFTH_AMOUNT_START, FIFTH_AMOUNT_END, "fifth_amount", lineNumber);
        BigDecimal fifthAmount = parseAmount(fifthAmountStr, "fifth_amount", lineNumber);
        record.setFifthAmount(fifthAmount);

        // Field 18: Fifth Amount Sign
        String fifthAmountSign = extractField(line, FIFTH_AMOUNT_SIGN_START, FIFTH_AMOUNT_SIGN_END, "fifth_amount_sign", lineNumber);
        if (!AMOUNT_SIGN_PATTERN.matcher(fifthAmountSign).matches()) {
            if (strictValidation) {
                throw VssParsingException.invalidFieldFormat("fifth_amount_sign", "CR, DB, or spaces", fifthAmountSign, lineNumber, "VSS-120-TCR1");
            } else {
                log.debug("Non-standard fifth amount sign at line {}: '{}'", lineNumber, fifthAmountSign);
            }
        }
        record.setFifthAmountSign(fifthAmountSign);

        // Field 19: Sixth Amount
        String sixthAmountStr = extractField(line, SIXTH_AMOUNT_START, SIXTH_AMOUNT_END, "sixth_amount", lineNumber);
        BigDecimal sixthAmount = parseAmount(sixthAmountStr, "sixth_amount", lineNumber);
        record.setSixthAmount(sixthAmount);

        // Field 20: Sixth Amount Sign
        String sixthAmountSign = extractField(line, SIXTH_AMOUNT_SIGN_START, SIXTH_AMOUNT_SIGN_END, "sixth_amount_sign", lineNumber);
        if (!AMOUNT_SIGN_PATTERN.matcher(sixthAmountSign).matches()) {
            if (strictValidation) {
                throw VssParsingException.invalidFieldFormat("sixth_amount_sign", "CR, DB, or spaces", sixthAmountSign, lineNumber, "VSS-120-TCR1");
            } else {
                log.debug("Non-standard sixth amount sign at line {}: '{}'", lineNumber, sixthAmountSign);
            }
        }
        record.setSixthAmountSign(sixthAmountSign);
    }

    /**
     * Parse reserved fields (positions 144-168).
     */
    private void parseReservedFields(Vss120Tcr1Record record, String line, int lineNumber) {
        // Field 21: Reserved field 2
        if (line.length() >= RESERVED_FIELD_2_END) {
            String reserved2 = extractField(line, RESERVED_FIELD_2_START, RESERVED_FIELD_2_END, "reserved2", lineNumber);
            if (strictValidation && !reserved2.trim().isEmpty()) {
                log.debug("Reserved field 2 contains non-space characters at line {}: '{}'", lineNumber, reserved2);
            }
            record.setReservedField2(reserved2);
        }
    }

    /**
     * Parses multiple VSS-120 TCR1 record lines.
     *
     * @param lines the list of record lines to parse
     * @param fileProcessingJob the associated file processing job
     * @param parentRecord the parent VSS-120 record
     * @return the list of parsed Vss120Tcr1Record objects
     */
    public List<Vss120Tcr1Record> parseFile(List<String> lines, FileProcessingJob fileProcessingJob,
                                            VssSubGroup4Record  parentRecord) {
        if (lines == null || lines.isEmpty()) {
            log.warn("No lines provided for VSS-120 TCR1 parsing");
            return new ArrayList<>();
        }

        log.info("Parsing VSS-120 TCR1 file with {} lines", lines.size());

        List<Vss120Tcr1Record> records = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            int lineNumber = i + 1;
            String line = lines.get(i);

            try {
                Vss120Tcr1Record record = parseLine(line, lineNumber, fileProcessingJob, parentRecord);
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
            log.warn("VSS-120 TCR1 file parsing completed with {} errors", errors.size());
            if (strictValidation && !skipInvalidRecords && !errors.isEmpty()) {
                throw new VssParsingException(
                        String.format("File parsing failed with %d errors: %s",
                                errors.size(), String.join("; ", errors.subList(0, Math.min(3, errors.size())))));
            }
        }

        log.info("Successfully parsed {} VSS-120 TCR1 records from {} lines", records.size(), lines.size());
        return records;
    }

    /**
     * Validates the record length.
     */
    private void validateRecordLength(String line, int lineNumber) {
        if (line.length() < MIN_RECORD_LENGTH) {
            throw VssParsingException.invalidRecordLength(expectedRecordLength, line.length(), lineNumber, "VSS-120-TCR1");
        }

        if (strictValidation && line.length() != expectedRecordLength) {
            log.warn("VSS-120 TCR1 record at line {} has unexpected length: expected={}, actual={}",
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
                throw VssParsingException.missingRequiredField(fieldName, lineNumber, "VSS-120-TCR1");
            }

            return line.substring(start, end);

        } catch (StringIndexOutOfBoundsException e) {
            throw VssParsingException.invalidFieldFormat(fieldName,
                    String.format("positions %d-%d", startPos, endPos),
                    "field extends beyond record length", lineNumber, "VSS-120-TCR1");
        }
    }

    /**
     * Parses a count field (15 characters).
     */
    private Long parseCount(String countStr, String fieldName, int lineNumber) {
        if (countStr == null || countStr.trim().isEmpty() || countStr.trim().equals("000000000000000")) {
            return 0L;
        }

        String trimmedCount = countStr.trim();

        // Validate format - be more lenient for count fields
        if (!trimmedCount.matches("^[0-9 ]+$")) {
            if (strictValidation) {
                throw VssParsingException.invalidFieldFormat(fieldName, "numeric", countStr, lineNumber, "VSS-120-TCR1");
            } else {
                log.debug("Non-numeric count field at line {}: '{}'", lineNumber, countStr);
                return 0L;
            }
        }

        try {
            // Remove leading zeros and spaces
            String cleanCount = trimmedCount.replaceFirst("^0+", "");
            if (cleanCount.isEmpty()) {
                return 0L;
            }

            return Long.parseLong(cleanCount.replaceAll("\\s", ""));

        } catch (NumberFormatException e) {
            if (strictValidation) {
                throw VssParsingException.invalidNumericValue(fieldName, countStr, lineNumber, "VSS-120-TCR1", e);
            } else {
                log.debug("Invalid count value at line {}: '{}'", lineNumber, countStr);
                return 0L;
            }
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

        // Validate format - be more lenient for amount fields
        if (!trimmedAmount.matches("^[0-9 ]+$")) {
            if (strictValidation) {
                throw VssParsingException.invalidFieldFormat(fieldName, "numeric", amountStr, lineNumber, "VSS-120-TCR1");
            } else {
                log.debug("Non-numeric amount field at line {}: '{}'", lineNumber, amountStr);
                return BigDecimal.ZERO;
            }
        }

        try {
            // Remove leading zeros and spaces
            String cleanAmount = trimmedAmount.replaceFirst("^0+", "");
            if (cleanAmount.isEmpty()) {
                return BigDecimal.ZERO;
            }

            // Parse as long and convert to BigDecimal with 2 decimal places
            long amountCents = Long.parseLong(cleanAmount.replaceAll("\\s", ""));
            return BigDecimal.valueOf(amountCents, 2);

        } catch (NumberFormatException e) {
            if (strictValidation) {
                throw VssParsingException.invalidNumericValue(fieldName, amountStr, lineNumber, "VSS-120-TCR1", e);
            } else {
                log.debug("Invalid amount value at line {}: '{}'", lineNumber, amountStr);
                return BigDecimal.ZERO;
            }
        }
    }

    /**
     * Validates the parsed record according to business rules.
     * FIXED: More lenient validation for TCR1 records
     */
    private void validateRecord(Vss120Tcr1Record record, int lineNumber,VssSubGroup4Record parentRecord) {
        List<String> errors = new ArrayList<>();

        // Validate basic header fields (more lenient validation)
        if (!Vss120Tcr1Record.TRANSACTION_CODE_TCR1.equals(record.getTransactionCode())) {
            errors.add("Invalid transaction code for TCR1 record: expected '46', got '" + record.getTransactionCode() + "'");
        }

        if (!Vss120Tcr1Record.TRANSACTION_CODE_QUALIFIER_DEFAULT.equals(record.getTransactionCodeQualifier())) {
            if (strictValidation) {
                errors.add("Invalid transaction code qualifier for TCR1 record: expected '0', got '" + record.getTransactionCodeQualifier() + "'");
            } else {
                log.debug("Non-standard transaction code qualifier in TCR1 at line {}: '{}'", lineNumber, record.getTransactionCodeQualifier());
            }
        }

        if (!Vss120Tcr1Record.TRANSACTION_COMPONENT_SEQ_TCR1.equals(record.getTransactionComponentSequenceNumber())) {
            errors.add("Invalid transaction component sequence number for TCR1 record: expected '1', got '" + record.getTransactionComponentSequenceNumber() + "'");
        }

        // Validate destination ID - be more lenient
        if (record.getDestinationId() == null || record.getDestinationId().trim().isEmpty()) {
            if (strictValidation) {
                errors.add("Missing destination ID in TCR1 record");
            } else {
                log.debug("TCR1 record at line {} has no destination ID", lineNumber);
            }
        } else if (!record.getDestinationId().matches("^\\d{6}$")) {
            if (strictValidation) {
                errors.add("Invalid destination ID format in TCR1 record: '" + record.getDestinationId() + "'");
            } else {
                log.debug("TCR1 record at line {} has non-standard destination ID: '{}'", lineNumber, record.getDestinationId());
            }
        }

        // Validate destination ID consistency with parent (only if we have a valid parent)
        if (record.getParentVssRecord() != null && parentRecord.getDestinationId() != null) {
            String parentDestId = parentRecord.getDestinationId();
            if (!parentDestId.equals(record.getDestinationId())) {
                log.debug("Destination ID mismatch between TCR1 and parent at line {}: parent={}, tcr1={}",
                        lineNumber, parentDestId, record.getDestinationId());
                // Don't treat this as a hard error unless in strict mode
                if (strictValidation) {
                    errors.add(String.format("Destination ID mismatch with parent: parent=%s, tcr1=%s",
                            parentDestId, record.getDestinationId()));
                }
            }
        }

        // Check that at least one meaningful data field is present (optional validation)
        boolean hasData = (record.getFirstCount() != null && record.getFirstCount() > 0) ||
                (record.getSecondCount() != null && record.getSecondCount() > 0) ||
                (record.getFirstAmount() != null && record.getFirstAmount().compareTo(BigDecimal.ZERO) != 0) ||
                (record.getSecondAmount() != null && record.getSecondAmount().compareTo(BigDecimal.ZERO) != 0) ||
                (record.getThirdAmount() != null && record.getThirdAmount().compareTo(BigDecimal.ZERO) != 0) ||
                (record.getFourthAmount() != null && record.getFourthAmount().compareTo(BigDecimal.ZERO) != 0) ||
                (record.getFifthAmount() != null && record.getFifthAmount().compareTo(BigDecimal.ZERO) != 0) ||
                (record.getSixthAmount() != null && record.getSixthAmount().compareTo(BigDecimal.ZERO) != 0);

        if (!hasData) {
            log.debug("TCR1 record at line {} contains no significant data (all zeros)", lineNumber);
        }

        // Only throw validation exception if there are real errors
        if (!errors.isEmpty()) {
            throw new VssValidationException(errors, lineNumber, "VSS-120-TCR1", record.getDestinationId());
        }
    }

    /**
     * Gets the expected record length for VSS-120 TCR1 files.
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