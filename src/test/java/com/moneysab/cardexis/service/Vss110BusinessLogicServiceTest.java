package com.moneysab.cardexis.service;

import com.moneysab.cardexis.domain.entity.FileProcessingJob;
import com.moneysab.cardexis.domain.entity.Vss110SettlementRecord;
import com.moneysab.cardexis.domain.enums.ProcessingStatus;
import com.moneysab.cardexis.parser.Vss110FileParser;
import com.moneysab.cardexis.service.impl.Vss110BusinessLogicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for VSS 110 business logic service.
 * 
 * Tests the business logic implementation for VSS TC46, TCR 0 Report Group V, Subgroup 2.
 * Uses sample data from the VSS 110 specification to validate parsing and business rules.
 * 
 * @author EL.AHGOUNE
 * @version 1.0.0
 * @since 2024
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class Vss110BusinessLogicServiceTest {
    
    @Autowired
    private Vss110BusinessLogicService businessLogicService;
    
    @Autowired
    private Vss110FileParser parser;
    
    private FileProcessingJob testJob;
    private List<String> sampleVss110Data;
    
    @BeforeEach
    void setUp() {
        // Create test job
        testJob = new FileProcessingJob("vss110_test.txt",
            com.moneysab.cardexis.domain.enums.FileType.EPIN, 1000L);
        testJob.setClientId("TEST_CLIENT");
        testJob.setStatus(ProcessingStatus.PROCESSING);
        
        // Sample VSS 110 data from specification
        sampleVss110Data = Arrays.asList(
            // I1 - Interchange Acquirer
            "4600433475000000900057848710000287721000028772001978      V2110  20221582022158              I1000000000001074000000007294143000000000000000000000007294143CR          0",
            // I2 - Interchange Issuer (No Activity)
            "4600433475000000900057848710000287721000028772001978      V2110  20221582022158              I2000000000000000000000000000000000000000000000000000000000000            0",
            // I3 - Interchange Other (No Activity)
            "4600433475000000900057848710000287721000028772001978      V2110  20221582022158              I3000000000000000000000000000000000000000000000000000000000000            0",
            // I9 - Interchange Total
            "4600433475000000900057848710000287721000028772001978      V2110  20221582022158              I9000000000001074000000007294143000000000000000000000007294143CR          0",
            // F1 - Reimbursement Fees Acquirer
            "4600433475000000900057848710000287721000028772001978      V2110  20221582022158              F1               000000000000361000000000107155000000000106794DB          0",
            // F2 - Reimbursement Fees Issuer (No Activity)
            "4600433475000000900057848710000287721000028772001978      V2110  20221582022158              F2               000000000000000000000000000000000000000000000            0",
            // F3 - Reimbursement Fees Other (No Activity)
            "4600433475000000900057848710000287721000028772001978      V2110  20221582022158              F3               000000000000000000000000000000000000000000000            0",
            // F9 - Reimbursement Fees Total
            "4600433475000000900057848710000287721000028772001978      V2110  20221582022158              F9               000000000000361000000000107155000000000106794DB          0",
            // C1 - Visa Charges Acquirer
            "4600433475000000900057848710000287721000028772001978      V2110  20221582022158              C1               000000000000000000000000014159000000000014159DB          0",
            // C2 - Visa Charges Issuer (No Activity)
            "4600433475000000900057848710000287721000028772001978      V2110  20221582022158              C2               000000000000000000000000000000000000000000000            0",
            // C3 - Visa Charges Other (No Activity)
            "4600433475000000900057848710000287721000028772001978      V2110  20221582022158              C3               000000000000000000000000000000000000000000000            0",
            // C9 - Visa Charges Total
            "4600433475000000900057848710000287721000028772001978      V2110  20221582022158              C9               000000000000000000000000014159000000000014159DB          0",
            // T1 - Total Acquirer
            "4600433475000000900057848710000287721000028772001978      V2110  20221582022158              T1               000000007294504000000000121314000000007173190CR          0",
            // T2 - Total Issuer (No Activity)
            "4600433475000000900057848710000287721000028772001978      V2110  20221582022158              T2               000000000000000000000000000000000000000000000            0",
            // T3 - Total Other (No Activity)
            "4600433475000000900057848710000287721000028772001978      V2110  20221582022158              T3               000000000000000000000000000000000000000000000            0",
            // T9 - Grand Total
            "4600433475000000900057848710000287721000028772001978      V2110  20221582022158              T9               000000007294504000000000121314000000007173190CR          0"
        );
    }
    
    @Test
    void testParseVss110SampleData() {
        // Parse all sample records
        List<Vss110SettlementRecord> records = parser.parseFile(sampleVss110Data, testJob);
        
        // Verify we have all 16 records
        assertEquals(16, records.size(), "Should parse all 16 VSS 110 records");
        
        // Verify all records are valid
        long validRecords = records.stream().filter(r -> r.getIsValid() == null || r.getIsValid()).count();
        assertEquals(16, validRecords, "All records should be valid");
        
        // Verify common fields
        for (Vss110SettlementRecord record : records) {
            assertEquals("46", record.getTransactionCode(), "Transaction code should be 46");
            assertEquals("433475", record.getDestinationId(), "Destination ID should be 433475");
            assertEquals("V", record.getReportGroup(), "Report group should be V");
            assertEquals("2", record.getReportSubgroup(), "Report subgroup should be 2");
            assertEquals("110", record.getReportIdNumber(), "Report ID should be 110");
            assertEquals(LocalDate.of(2022, 6, 7), record.getSettlementDate(), "Settlement date should be June 7, 2022");
        }
    }
    
    @Test
    void testInterchangeRecords() {
        List<Vss110SettlementRecord> records = parser.parseFile(sampleVss110Data, testJob);
        
        // Filter interchange records
        List<Vss110SettlementRecord> interchangeRecords = records.stream()
            .filter(r -> "I".equals(r.getAmountType()))
            .toList();
        
        assertEquals(4, interchangeRecords.size(), "Should have 4 interchange records (I1, I2, I3, I9)");
        
        // Test I1 (Acquirer) record
        Vss110SettlementRecord i1Record = interchangeRecords.stream()
            .filter(r -> "1".equals(r.getBusinessMode()))
            .findFirst()
            .orElseThrow();
        
        assertEquals(new BigDecimal("0.00"), i1Record.getCreditAmount(), "I1 credit amount should be 0.00");
        assertEquals(new BigDecimal("729.41"), i1Record.getDebitAmount(), "I1 debit amount should be 729.41");
        assertEquals(new BigDecimal("729.41"), i1Record.getNetAmount(), "I1 net amount should be 729.41");
        assertEquals(1074, i1Record.getTransactionCount(), "I1 transaction count should be 1074");
        
        // Test I9 (Total) record
        Vss110SettlementRecord i9Record = interchangeRecords.stream()
            .filter(r -> "9".equals(r.getBusinessMode()))
            .findFirst()
            .orElseThrow();
        
        assertEquals(new BigDecimal("0.00"), i9Record.getCreditAmount(), "I9 credit amount should be 0.00");
        assertEquals(new BigDecimal("729.41"), i9Record.getDebitAmount(), "I9 debit amount should be 729.41");
        assertEquals(new BigDecimal("729.41"), i9Record.getNetAmount(), "I9 net amount should be 729.41");
        assertEquals(1074, i9Record.getTransactionCount(), "I9 transaction count should be 1074");
    }
    
    @Test
    void testReimbursementFeesRecords() {
        List<Vss110SettlementRecord> records = parser.parseFile(sampleVss110Data, testJob);
        
        // Filter reimbursement fees records
        List<Vss110SettlementRecord> feesRecords = records.stream()
            .filter(r -> "F".equals(r.getAmountType()))
            .toList();
        
        assertEquals(4, feesRecords.size(), "Should have 4 reimbursement fees records (F1, F2, F3, F9)");
        
        // Test F1 (Acquirer) record
        Vss110SettlementRecord f1Record = feesRecords.stream()
            .filter(r -> "1".equals(r.getBusinessMode()))
            .findFirst()
            .orElseThrow();
        
        assertEquals(new BigDecimal("0.04"), f1Record.getCreditAmount(), "F1 credit amount should be 0.04");
        assertEquals(new BigDecimal("10.72"), f1Record.getDebitAmount(), "F1 debit amount should be 10.72");
        assertEquals(new BigDecimal("10.68"), f1Record.getNetAmount(), "F1 net amount should be 10.68");
    }
    
    @Test
    void testVisaChargesRecords() {
        List<Vss110SettlementRecord> records = parser.parseFile(sampleVss110Data, testJob);
        
        // Filter Visa charges records
        List<Vss110SettlementRecord> chargesRecords = records.stream()
            .filter(r -> "C".equals(r.getAmountType()))
            .toList();
        
        assertEquals(4, chargesRecords.size(), "Should have 4 Visa charges records (C1, C2, C3, C9)");
        
        // Test C1 (Acquirer) record
        Vss110SettlementRecord c1Record = chargesRecords.stream()
            .filter(r -> "1".equals(r.getBusinessMode()))
            .findFirst()
            .orElseThrow();
        
        assertEquals(new BigDecimal("0.00"), c1Record.getCreditAmount(), "C1 credit amount should be 0.00");
        assertEquals(new BigDecimal("1.42"), c1Record.getDebitAmount(), "C1 debit amount should be 1.42");
        assertEquals(new BigDecimal("1.42"), c1Record.getNetAmount(), "C1 net amount should be 1.42");
    }
    
    @Test
    void testTotalRecords() {
        List<Vss110SettlementRecord> records = parser.parseFile(sampleVss110Data, testJob);
        
        // Filter total records
        List<Vss110SettlementRecord> totalRecords = records.stream()
            .filter(r -> "T".equals(r.getAmountType()))
            .toList();
        
        assertEquals(4, totalRecords.size(), "Should have 4 total records (T1, T2, T3, T9)");
        
        // Test T1 (Acquirer) record
        Vss110SettlementRecord t1Record = totalRecords.stream()
            .filter(r -> "1".equals(r.getBusinessMode()))
            .findFirst()
            .orElseThrow();
        
        assertEquals(new BigDecimal("729.45"), t1Record.getCreditAmount(), "T1 credit amount should be 729.45");
        assertEquals(new BigDecimal("12.13"), t1Record.getDebitAmount(), "T1 debit amount should be 12.13");
        assertEquals(new BigDecimal("717.32"), t1Record.getNetAmount(), "T1 net amount should be 717.32");
        
        // Test T9 (Grand Total) record
        Vss110SettlementRecord t9Record = totalRecords.stream()
            .filter(r -> "9".equals(r.getBusinessMode()))
            .findFirst()
            .orElseThrow();
        
        assertEquals(new BigDecimal("729.45"), t9Record.getCreditAmount(), "T9 credit amount should be 729.45");
        assertEquals(new BigDecimal("12.13"), t9Record.getDebitAmount(), "T9 debit amount should be 12.13");
        assertEquals(new BigDecimal("717.32"), t9Record.getNetAmount(), "T9 net amount should be 717.32");
    }
    
    @Test
    void testBusinessLogicValidation() {
        List<Vss110SettlementRecord> records = parser.parseFile(sampleVss110Data, testJob);
        
        // Test business logic validation
        Vss110BusinessLogicService.Vss110ValidationResult validation = 
            businessLogicService.validateCompleteRecordSet(records);
        
        assertTrue(validation.isValid(), "Record set should be valid");
        assertTrue(validation.getErrors().isEmpty(), "Should have no validation errors");
    }
    
    @Test
    void testFinancialSummaryByAmountType() {
        List<Vss110SettlementRecord> records = parser.parseFile(sampleVss110Data, testJob);
        
        Map<String, Vss110BusinessLogicService.FinancialSummary> summary = 
            businessLogicService.calculateFinancialSummaryByAmountType(records);
        
        // Test Interchange summary
        Vss110BusinessLogicService.FinancialSummary interchangeSummary = summary.get("I");
        assertNotNull(interchangeSummary, "Should have interchange summary");
        assertEquals(new BigDecimal("0.00"), interchangeSummary.getCreditAmount(), "Interchange total credit should be 0.00");
        assertEquals(new BigDecimal("1458.82"), interchangeSummary.getDebitAmount(), "Interchange total debit should be 1458.82");
        assertEquals(new BigDecimal("-1458.82"), interchangeSummary.getNetAmount(), "Interchange net should be -1458.82");
        assertEquals(2148, interchangeSummary.getTransactionCount(), "Interchange transaction count should be 2148");
        
        // Test Total summary
        Vss110BusinessLogicService.FinancialSummary totalSummary = summary.get("T");
        assertNotNull(totalSummary, "Should have total summary");
        assertEquals(new BigDecimal("1458.90"), totalSummary.getCreditAmount(), "Total credit should be 1458.90");
        assertEquals(new BigDecimal("24.26"), totalSummary.getDebitAmount(), "Total debit should be 24.26");
        assertEquals(new BigDecimal("1434.64"), totalSummary.getNetAmount(), "Total net should be 1434.64");
    }
    
    @Test
    void testFinancialSummaryByBusinessMode() {
        List<Vss110SettlementRecord> records = parser.parseFile(sampleVss110Data, testJob);
        
        Map<String, Vss110BusinessLogicService.FinancialSummary> summary = 
            businessLogicService.calculateFinancialSummaryByBusinessMode(records);
        
        // Test Acquirer (1) summary
        Vss110BusinessLogicService.FinancialSummary acquirerSummary = summary.get("1");
        assertNotNull(acquirerSummary, "Should have acquirer summary");
        assertEquals(new BigDecimal("729.45"), acquirerSummary.getCreditAmount(), "Acquirer total credit should be 729.45");
        assertEquals(new BigDecimal("12.13"), acquirerSummary.getDebitAmount(), "Acquirer total debit should be 12.13");
        assertEquals(new BigDecimal("717.32"), acquirerSummary.getNetAmount(), "Acquirer net should be 717.32");
        
        // Test Issuer (2) and Other (3) should have zero activity
        Vss110BusinessLogicService.FinancialSummary issuerSummary = summary.get("2");
        assertNotNull(issuerSummary, "Should have issuer summary");
        assertEquals(BigDecimal.ZERO, issuerSummary.getNetAmount(), "Issuer should have zero activity");
        
        Vss110BusinessLogicService.FinancialSummary otherSummary = summary.get("3");
        assertNotNull(otherSummary, "Should have other summary");
        assertEquals(BigDecimal.ZERO, otherSummary.getNetAmount(), "Other should have zero activity");
    }
}