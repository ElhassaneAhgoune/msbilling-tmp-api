/*
package com.moneysab.cardexis.parser;

import com.moneysab.cardexis.domain.entity.FileProcessingJob;
import com.moneysab.cardexis.domain.entity.Vss110SettlementRecord;
import com.moneysab.cardexis.domain.enums.ProcessingStatus;
import com.moneysab.cardexis.exception.VssParsingException;
import com.moneysab.cardexis.exception.VssValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

*/
/**
 * Test class for VSS-110 file parser.
 * 
 * Tests the parsing of VSS-110 settlement records according to the Visa specification.
 * Includes tests for valid records, invalid records, edge cases, and error handling.
 * 
 * @author EL.AHGOUNE
 * @version 1.0.0
 * @since 2024
 *//*

class Vss110FileParserTest {
    
    private Vss110FileParser parser;
    private FileProcessingJob testJob;
    
    // Sample valid VSS-110 record (168 characters)
    private static final String VALID_VSS110_RECORD = 
        "46  123456    SRETEST001 2024158                         V2110                                 I1          000000000012345000000000000678900000000001234500";
    
    // Sample valid VSS-111 record (168 characters)
    private static final String VALID_VSS111_RECORD = 
        "46  654321    SRETEST002 2024159                         V2111                                  9          000000000098765000000000000432100000000009876500";
    
    // Sample invalid record (too short)
    private static final String INVALID_SHORT_RECORD = 
        "46  123456    SRETEST001 2024158";
    
    // Sample invalid record (wrong transaction code)
    private static final String INVALID_TRANSACTION_CODE_RECORD = 
        "99  123456    SRETEST001 2024158                         V2110                                 I1          000000000012345000000000000678900000000001234500";
    
    @BeforeEach
    void setUp() {
        parser = new Vss110FileParser();
        
        // Set configuration properties for testing
        ReflectionTestUtils.setField(parser, "strictValidation", true);
        ReflectionTestUtils.setField(parser, "skipInvalidRecords", false);
        ReflectionTestUtils.setField(parser, "expectedRecordLength", 168);
        
        // Create test job
        testJob = new FileProcessingJob("test_vss110.txt",
                                       com.moneysab.cardexis.domain.enums.FileType.VSS,
                                       1000L);
        testJob.setStatus(ProcessingStatus.PROCESSING);
    }
    
    @Test
    void testParseValidVss110Record() {
        // Given
        int lineNumber = 1;
        
        // When
        Vss110SettlementRecord record = parser.parseLine(VALID_VSS110_RECORD, lineNumber, testJob);
        
        // Then
        assertNotNull(record);
        assertTrue(record.getIsValid());
        assertNull(record.getValidationErrors());
        
        // Verify header fields
        assertEquals("46", record.getTransactionCode());
        assertEquals("123456", record.getDestinationId());
        assertEquals("SRETEST001", record.getReportingSreId());
        assertEquals(LocalDate.of(2024, 6, 6), record.getSettlementDate()); // Day 158 of 2024
        assertEquals("2024158", record.getRawSettlementDate());
        
        // Verify report identification
        assertEquals("V", record.getReportGroup());
        assertEquals("2", record.getReportSubgroup());
        assertEquals("110", record.getReportIdNumber());
        
        // Verify business classification
        assertEquals("I", record.getAmountType());
        assertEquals("1", record.getBusinessMode());
        
        // Verify financial amounts
        assertEquals(new BigDecimal("123.45"), record.getCreditAmount());
        assertEquals(new BigDecimal("6789.00"), record.getDebitAmount());
        assertEquals(new BigDecimal("123.45"), record.getNetAmount());
        
        // Verify metadata
        assertEquals("USD", record.getCurrencyCode());
        assertEquals(lineNumber, record.getLineNumber());
        assertEquals(VALID_VSS110_RECORD, record.getRawRecordLine());
        assertEquals(testJob, record.getFileProcessingJob());
    }
    
    @Test
    void testParseValidVss111Record() {
        // Given
        int lineNumber = 2;
        
        // When
        Vss110SettlementRecord record = parser.parseLine(VALID_VSS111_RECORD, lineNumber, testJob);
        
        // Then
        assertNotNull(record);
        assertTrue(record.getIsValid());
        
        // Verify this is a summary record
        assertEquals("111", record.getReportIdNumber());
        assertEquals(" ", record.getAmountType());
        assertEquals("9", record.getBusinessMode());
        assertTrue(record.isSummaryRecord());
        assertTrue(record.isTotalRecord());
        
        // Verify amounts
        assertEquals(new BigDecimal("987.65"), record.getCreditAmount());
        assertEquals(new BigDecimal("4321.00"), record.getDebitAmount());
        assertEquals(new BigDecimal("987.65"), record.getNetAmount());
    }
    
    @Test
    void testParseInvalidShortRecord() {
        // Given
        int lineNumber = 1;
        
        // When & Then
        VssParsingException exception = assertThrows(VssParsingException.class, 
            () -> parser.parseLine(INVALID_SHORT_RECORD, lineNumber, testJob));
        
        assertTrue(exception.getMessage().contains("Invalid record length"));
        assertEquals(lineNumber, exception.getLineNumber());
        assertEquals("VSS-110", exception.getRecordType());
    }
    
    @Test
    void testParseInvalidTransactionCode() {
        // Given
        int lineNumber = 1;
        
        // When & Then
        VssParsingException exception = assertThrows(VssParsingException.class, 
            () -> parser.parseLine(INVALID_TRANSACTION_CODE_RECORD, lineNumber, testJob));
        
        assertTrue(exception.getMessage().contains("Invalid format"));
        assertTrue(exception.getMessage().contains("transaction_code"));
        assertEquals("transaction_code", exception.getFieldName());
    }
    
    @Test
    void testParseNullRecord() {
        // Given
        int lineNumber = 1;
        
        // When & Then
        VssParsingException exception = assertThrows(VssParsingException.class, 
            () -> parser.parseLine(null, lineNumber, testJob));
        
        assertTrue(exception.getMessage().contains("Required field is missing"));
    }
    
    @Test
    void testParseFileWithMultipleRecords() {
        // Given
        List<String> lines = Arrays.asList(
            VALID_VSS110_RECORD,
            VALID_VSS111_RECORD
        );
        
        // When
        List<Vss110SettlementRecord> records = parser.parseFile(lines, testJob);
        
        // Then
        assertNotNull(records);
        assertEquals(2, records.size());
        
        // Verify first record (VSS-110)
        Vss110SettlementRecord record1 = records.get(0);
        assertTrue(record1.isDetailedRecord());
        assertEquals("110", record1.getReportIdNumber());
        assertEquals("I", record1.getAmountType());
        
        // Verify second record (VSS-111)
        Vss110SettlementRecord record2 = records.get(1);
        assertTrue(record2.isSummaryRecord());
        assertEquals("111", record2.getReportIdNumber());
        assertEquals(" ", record2.getAmountType());
    }
    
    @Test
    void testParseFileWithSkipInvalidRecords() {
        // Given
        ReflectionTestUtils.setField(parser, "skipInvalidRecords", true);
        ReflectionTestUtils.setField(parser, "strictValidation", false);
        
        List<String> lines = Arrays.asList(
            VALID_VSS110_RECORD,
            INVALID_SHORT_RECORD,
            VALID_VSS111_RECORD
        );
        
        // When
        List<Vss110SettlementRecord> records = parser.parseFile(lines, testJob);
        
        // Then
        assertEquals(2, records.size()); // Only valid records should be returned
        assertTrue(records.get(0).getIsValid());
        assertTrue(records.get(1).getIsValid());
    }
    
    @Test
    void testBusinessLogicMethods() {
        // Given
        Vss110SettlementRecord record = parser.parseLine(VALID_VSS110_RECORD, 1, testJob);
        
        // When & Then
        assertTrue(record.isInterchangeRecord());
        assertFalse(record.isProcessingRecord());
        assertFalse(record.isChargebackRecord());
        assertFalse(record.isTotalRecord());
        assertTrue(record.isDetailedRecord());
        assertFalse(record.isSummaryRecord());
        
        assertEquals("Acquirer", record.getBusinessModeDescription());
        assertEquals("Interchange", record.getAmountTypeDescription());
        
        assertTrue(record.isValidForProcessing());
        assertTrue(record.isAmountTypeValidForReportId());
    }
    
    @Test
    void testAmountCalculation() {
        // Given
        Vss110SettlementRecord record = new Vss110SettlementRecord(testJob);
        record.setCreditAmount(new BigDecimal("1000.00"));
        record.setDebitAmount(new BigDecimal("250.00"));
        
        // When
        record.calculateNetAmount();
        
        // Then
        assertEquals(new BigDecimal("750.00"), record.getNetAmount());
        assertTrue(record.hasNetCredit());
        assertFalse(record.hasNetDebit());
        assertEquals(new BigDecimal("750.00"), record.getAbsoluteNetAmount());
    }
    
    @Test
    void testDateParsing() {
        // Test various date formats
        String recordWith2024001 = VALID_VSS110_RECORD.replace("2024158", "2024001");
        String recordWith2024366 = VALID_VSS110_RECORD.replace("2024158", "2024366");
        
        Vss110SettlementRecord record1 = parser.parseLine(recordWith2024001, 1, testJob);
        assertEquals(LocalDate.of(2024, 1, 1), record1.getSettlementDate());
        
        Vss110SettlementRecord record2 = parser.parseLine(recordWith2024366, 2, testJob);
        assertEquals(LocalDate.of(2024, 12, 31), record2.getSettlementDate()); // 2024 is leap year
    }
    
    @Test
    void testValidationWithInconsistentAmounts() {
        // Given - Create a record with inconsistent amounts
        String inconsistentRecord = VALID_VSS110_RECORD.replace("00000000001234500", "00000000009999900"); // Change net amount
        
        // When & Then
        VssValidationException exception = assertThrows(VssValidationException.class, 
            () -> parser.parseLine(inconsistentRecord, 1, testJob));
        
        assertTrue(exception.getMessage().contains("Amount inconsistency"));
    }
    
    @Test
    void testRecordDescription() {
        // Given
        Vss110SettlementRecord record = parser.parseLine(VALID_VSS110_RECORD, 1, testJob);
        
        // When
        String description = record.getRecordDescription();
        
        // Then
        assertNotNull(description);
        assertTrue(description.contains("VSS-110"));
        assertTrue(description.contains("123456"));
        assertTrue(description.contains("Acquirer"));
        assertTrue(description.contains("Interchange"));
    }
    
    @Test
    void testParserConfiguration() {
        // When & Then
        assertEquals(168, parser.getExpectedRecordLength());
        assertTrue(parser.isStrictValidation());
        assertFalse(parser.isSkipInvalidRecords());
    }
    
    @Test
    void testAmountTypeValidation() {
        // Test VSS-110 with valid amount types
        String[] validAmountTypes = {"I", "F", "C", "T"};
        for (String amountType : validAmountTypes) {
            String record = VALID_VSS110_RECORD.replace("I1", amountType + "1");
            Vss110SettlementRecord parsed = parser.parseLine(record, 1, testJob);
            assertTrue(parsed.isAmountTypeValidForReportId());
        }
        
        // Test VSS-111 with valid amount types
        String validSummaryRecord = VALID_VSS111_RECORD.replace(" 9", "T9");
        Vss110SettlementRecord summaryRecord = parser.parseLine(validSummaryRecord, 1, testJob);
        assertTrue(summaryRecord.isAmountTypeValidForReportId());
    }
    
    @Test
    void testEmptyFileHandling() {
        // Given
        List<String> emptyLines = Arrays.asList();
        
        // When
        List<Vss110SettlementRecord> records = parser.parseFile(emptyLines, testJob);
        
        // Then
        assertNotNull(records);
        assertTrue(records.isEmpty());
    }
    
    @Test
    void testLenientModeWithInvalidDate() {
        // Given
        ReflectionTestUtils.setField(parser, "strictValidation", false);
        String invalidDateRecord = VALID_VSS110_RECORD.replace("2024158", "9999999");
        
        // When
        Vss110SettlementRecord record = parser.parseLine(invalidDateRecord, 1, testJob);
        
        // Then
        assertNotNull(record);
        assertNotNull(record.getSettlementDate()); // Should have default date
        assertEquals("9999999", record.getRawSettlementDate());
    }
}*/
