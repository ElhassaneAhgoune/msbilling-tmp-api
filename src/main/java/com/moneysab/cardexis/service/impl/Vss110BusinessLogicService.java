package com.moneysab.cardexis.service.impl;

import com.moneysab.cardexis.domain.entity.Vss110SettlementRecord;
import com.moneysab.cardexis.repository.Vss110SettlementRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Business logic service for VSS 110 settlement records.
 * 
 * Implements the business rules for VSS TC46, TCR 0 Report Group V, Subgroup 2 - Complete Record Set Analysis.
 * This service handles the specific business logic for VSS 110 records including:
 * - Record structure validation
 * - Financial calculations and validations
 * - Business mode analysis
 * - Amount type processing
 * 
 * @author EL.AHGOUNE
 * @version 1.0.0
 * @since 2024
 */
@Service
@Transactional(readOnly = true)
public class Vss110BusinessLogicService {
    
    private static final Logger log = LoggerFactory.getLogger(Vss110BusinessLogicService.class);
    
    @Autowired
    private Vss110SettlementRecordRepository vss110Repository;
    
    /**
     * Analyzes VSS 110 records for a specific settlement date and destination.
     * 
     * @param settlementDate the settlement date
     * @param destinationId the destination ID
     * @return analysis summary
     */
    public Vss110AnalysisSummary analyzeSettlementRecords(LocalDate settlementDate, String destinationId) {
        log.info("Analyzing VSS 110 records for settlement date {} and destination {}", settlementDate, destinationId);
        
        List<Vss110SettlementRecord> records = vss110Repository.findBySettlementDateAndDestinationId(settlementDate, destinationId);
        
        if (records.isEmpty()) {
            log.warn("No VSS 110 records found for settlement date {} and destination {}", settlementDate, destinationId);
            return new Vss110AnalysisSummary(settlementDate, destinationId, Collections.emptyList());
        }
        
        return performAnalysis(records, settlementDate, destinationId);
    }
    
    /**
     * Validates the complete record set according to VSS 110 business rules.
     * 
     * @param records the list of records to validate
     * @return validation result
     */
    public Vss110ValidationResult validateCompleteRecordSet(List<Vss110SettlementRecord> records) {
        log.debug("Validating complete record set with {} records", records.size());
        
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Group records by amount type and business mode
        Map<String, Map<String, List<Vss110SettlementRecord>>> groupedRecords = groupRecordsByAmountTypeAndBusinessMode(records);
        
        // Validate expected record structure
        validateRecordStructure(groupedRecords, errors, warnings);
        
        // Validate financial calculations
        validateFinancialCalculations(groupedRecords, errors, warnings);
        
        // Validate business mode totals
        validateBusinessModeTotals(groupedRecords, errors, warnings);
        
        boolean isValid = errors.isEmpty();
        
        log.info("Record set validation completed: {} errors, {} warnings", errors.size(), warnings.size());
        
        return new Vss110ValidationResult(isValid, errors, warnings);
    }
    
    /**
     * Calculates financial summary by amount type.
     * 
     * @param records the records to analyze
     * @return financial summary
     */
    public Map<String, FinancialSummary> calculateFinancialSummaryByAmountType(List<Vss110SettlementRecord> records) {
        Map<String, FinancialSummary> summary = new HashMap<>();
        
        Map<String, List<Vss110SettlementRecord>> byAmountType = records.stream()
            .collect(Collectors.groupingBy(r -> r.getAmountType() != null ? r.getAmountType() : "UNKNOWN"));
        
        for (Map.Entry<String, List<Vss110SettlementRecord>> entry : byAmountType.entrySet()) {
            String amountType = entry.getKey();
            List<Vss110SettlementRecord> typeRecords = entry.getValue();
            
            BigDecimal totalCredit = typeRecords.stream()
                .map(r -> r.getCreditAmount() != null ? r.getCreditAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal totalDebit = typeRecords.stream()
                .map(r -> r.getDebitAmount() != null ? r.getDebitAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal netAmount = totalCredit.subtract(totalDebit);
            
            int transactionCount = typeRecords.stream()
                .mapToInt(r -> r.getTransactionCount() != null ? r.getTransactionCount() : 0)
                .sum();
            
            summary.put(amountType, new FinancialSummary(totalCredit, totalDebit, netAmount, transactionCount));
        }
        
        return summary;
    }
    
    /**
     * Calculates financial summary by business mode.
     * 
     * @param records the records to analyze
     * @return financial summary
     */
    public Map<String, FinancialSummary> calculateFinancialSummaryByBusinessMode(List<Vss110SettlementRecord> records) {
        Map<String, FinancialSummary> summary = new HashMap<>();
        
        Map<String, List<Vss110SettlementRecord>> byBusinessMode = records.stream()
            .collect(Collectors.groupingBy(r -> r.getBusinessMode() != null ? r.getBusinessMode() : "UNKNOWN"));
        
        for (Map.Entry<String, List<Vss110SettlementRecord>> entry : byBusinessMode.entrySet()) {
            String businessMode = entry.getKey();
            List<Vss110SettlementRecord> modeRecords = entry.getValue();
            
            BigDecimal totalCredit = modeRecords.stream()
                .map(r -> r.getCreditAmount() != null ? r.getCreditAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal totalDebit = modeRecords.stream()
                .map(r -> r.getDebitAmount() != null ? r.getDebitAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal netAmount = totalCredit.subtract(totalDebit);
            
            int transactionCount = modeRecords.stream()
                .mapToInt(r -> r.getTransactionCount() != null ? r.getTransactionCount() : 0)
                .sum();
            
            summary.put(businessMode, new FinancialSummary(totalCredit, totalDebit, netAmount, transactionCount));
        }
        
        return summary;
    }
    
    private Vss110AnalysisSummary performAnalysis(List<Vss110SettlementRecord> records, LocalDate settlementDate, String destinationId) {
        Map<String, FinancialSummary> byAmountType = calculateFinancialSummaryByAmountType(records);
        Map<String, FinancialSummary> byBusinessMode = calculateFinancialSummaryByBusinessMode(records);
        Vss110ValidationResult validation = validateCompleteRecordSet(records);
        
        return new Vss110AnalysisSummary(settlementDate, destinationId, records, byAmountType, byBusinessMode, validation);
    }
    
    private Map<String, Map<String, List<Vss110SettlementRecord>>> groupRecordsByAmountTypeAndBusinessMode(List<Vss110SettlementRecord> records) {
        return records.stream()
            .collect(Collectors.groupingBy(
                r -> r.getAmountType() != null ? r.getAmountType() : "UNKNOWN",
                Collectors.groupingBy(r -> r.getBusinessMode() != null ? r.getBusinessMode() : "UNKNOWN")
            ));
    }
    
    private void validateRecordStructure(Map<String, Map<String, List<Vss110SettlementRecord>>> groupedRecords, 
                                       List<String> errors, List<String> warnings) {
        // Expected structure for VSS 110: I, F, C, T amount types with 1, 2, 3, 9 business modes
        String[] expectedAmountTypes = {"I", "F", "C", "T"};
        String[] expectedBusinessModes = {"1", "2", "3", "9"};
        
        for (String amountType : expectedAmountTypes) {
            if (!groupedRecords.containsKey(amountType)) {
                warnings.add("Missing amount type: " + amountType);
                continue;
            }
            
            Map<String, List<Vss110SettlementRecord>> businessModes = groupedRecords.get(amountType);
            for (String businessMode : expectedBusinessModes) {
                if (!businessModes.containsKey(businessMode)) {
                    warnings.add("Missing business mode " + businessMode + " for amount type " + amountType);
                }
            }
        }
    }
    
    private void validateFinancialCalculations(Map<String, Map<String, List<Vss110SettlementRecord>>> groupedRecords,
                                             List<String> errors, List<String> warnings) {
        for (Map.Entry<String, Map<String, List<Vss110SettlementRecord>>> amountTypeEntry : groupedRecords.entrySet()) {
            String amountType = amountTypeEntry.getKey();
            
            for (Map.Entry<String, List<Vss110SettlementRecord>> businessModeEntry : amountTypeEntry.getValue().entrySet()) {
                String businessMode = businessModeEntry.getKey();
                List<Vss110SettlementRecord> records = businessModeEntry.getValue();
                
                for (Vss110SettlementRecord record : records) {
                    if (record.getCreditAmount() != null && record.getDebitAmount() != null && record.getNetAmount() != null) {
                        BigDecimal calculatedNet = record.getCreditAmount().subtract(record.getDebitAmount());
                        if (calculatedNet.compareTo(record.getNetAmount()) != 0) {
                            errors.add(String.format("Amount calculation error for %s-%s: Credit(%s) - Debit(%s) ≠ Net(%s)",
                                amountType, businessMode, record.getCreditAmount(), record.getDebitAmount(), record.getNetAmount()));
                        }
                    }
                }
            }
        }
    }
    
    private void validateBusinessModeTotals(Map<String, Map<String, List<Vss110SettlementRecord>>> groupedRecords,
                                          List<String> errors, List<String> warnings) {
        for (Map.Entry<String, Map<String, List<Vss110SettlementRecord>>> amountTypeEntry : groupedRecords.entrySet()) {
            String amountType = amountTypeEntry.getKey();
            Map<String, List<Vss110SettlementRecord>> businessModes = amountTypeEntry.getValue();
            
            if (businessModes.containsKey("9")) { // Total business mode
                List<Vss110SettlementRecord> totalRecords = businessModes.get("9");
                if (totalRecords.size() == 1) {
                    Vss110SettlementRecord totalRecord = totalRecords.get(0);
                    
                    // Calculate sum of business modes 1, 2, 3
                    BigDecimal sumCredit = BigDecimal.ZERO;
                    BigDecimal sumDebit = BigDecimal.ZERO;
                    int sumTransactions = 0;
                    
                    for (String mode : Arrays.asList("1", "2", "3")) {
                        if (businessModes.containsKey(mode)) {
                            for (Vss110SettlementRecord record : businessModes.get(mode)) {
                                if (record.getCreditAmount() != null) {
                                    sumCredit = sumCredit.add(record.getCreditAmount());
                                }
                                if (record.getDebitAmount() != null) {
                                    sumDebit = sumDebit.add(record.getDebitAmount());
                                }
                                if (record.getTransactionCount() != null) {
                                    sumTransactions += record.getTransactionCount();
                                }
                            }
                        }
                    }
                    
                    // Validate totals
                    if (totalRecord.getCreditAmount() != null && sumCredit.compareTo(totalRecord.getCreditAmount()) != 0) {
                        errors.add(String.format("Credit total mismatch for amount type %s: expected %s, got %s",
                            amountType, sumCredit, totalRecord.getCreditAmount()));
                    }
                    
                    if (totalRecord.getDebitAmount() != null && sumDebit.compareTo(totalRecord.getDebitAmount()) != 0) {
                        errors.add(String.format("Debit total mismatch for amount type %s: expected %s, got %s",
                            amountType, sumDebit, totalRecord.getDebitAmount()));
                    }
                    
                    if (totalRecord.getTransactionCount() != null && sumTransactions != totalRecord.getTransactionCount()) {
                        errors.add(String.format("Transaction count mismatch for amount type %s: expected %d, got %d",
                            amountType, sumTransactions, totalRecord.getTransactionCount()));
                    }
                }
            }
        }
    }
    
    // Inner classes for data transfer
    
    public static class Vss110AnalysisSummary {
        private final LocalDate settlementDate;
        private final String destinationId;
        private final List<Vss110SettlementRecord> records;
        private final Map<String, FinancialSummary> byAmountType;
        private final Map<String, FinancialSummary> byBusinessMode;
        private final Vss110ValidationResult validation;
        
        public Vss110AnalysisSummary(LocalDate settlementDate, String destinationId, List<Vss110SettlementRecord> records) {
            this(settlementDate, destinationId, records, Collections.emptyMap(), Collections.emptyMap(), 
                 new Vss110ValidationResult(true, Collections.emptyList(), Collections.emptyList()));
        }
        
        public Vss110AnalysisSummary(LocalDate settlementDate, String destinationId, List<Vss110SettlementRecord> records,
                                   Map<String, FinancialSummary> byAmountType, Map<String, FinancialSummary> byBusinessMode,
                                   Vss110ValidationResult validation) {
            this.settlementDate = settlementDate;
            this.destinationId = destinationId;
            this.records = records;
            this.byAmountType = byAmountType;
            this.byBusinessMode = byBusinessMode;
            this.validation = validation;
        }
        
        // Getters
        public LocalDate getSettlementDate() { return settlementDate; }
        public String getDestinationId() { return destinationId; }
        public List<Vss110SettlementRecord> getRecords() { return records; }
        public Map<String, FinancialSummary> getByAmountType() { return byAmountType; }
        public Map<String, FinancialSummary> getByBusinessMode() { return byBusinessMode; }
        public Vss110ValidationResult getValidation() { return validation; }
    }
    
    public static class Vss110ValidationResult {
        private final boolean isValid;
        private final List<String> errors;
        private final List<String> warnings;
        
        public Vss110ValidationResult(boolean isValid, List<String> errors, List<String> warnings) {
            this.isValid = isValid;
            this.errors = errors;
            this.warnings = warnings;
        }
        
        // Getters
        public boolean isValid() { return isValid; }
        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }
    }
    
    public static class FinancialSummary {
        private final BigDecimal creditAmount;
        private final BigDecimal debitAmount;
        private final BigDecimal netAmount;
        private final int transactionCount;
        
        public FinancialSummary(BigDecimal creditAmount, BigDecimal debitAmount, BigDecimal netAmount, int transactionCount) {
            this.creditAmount = creditAmount;
            this.debitAmount = debitAmount;
            this.netAmount = netAmount;
            this.transactionCount = transactionCount;
        }
        
        // Getters
        public BigDecimal getCreditAmount() { return creditAmount; }
        public BigDecimal getDebitAmount() { return debitAmount; }
        public BigDecimal getNetAmount() { return netAmount; }
        public int getTransactionCount() { return transactionCount; }
    }
}