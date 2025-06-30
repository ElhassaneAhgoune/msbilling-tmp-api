package com.moneysab.cardexis.parser.vss;

import com.moneysab.cardexis.dto.vss.report.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for text-based VSS reports (like EP747.TXT).
 * Uses fixed-width field positioning as specified in ReadingReports.txt.
 * 
 * This parser identifies different report sections (VSS-110, VSS-120, etc.)
 * and extracts data using the field mappings:
 * 
 * Report 110: Position 24 (currency), 52 (count), 78 (credit), 104 (debit), 132 (total)
 * Report 120: Position 24 (settlement), 24 (clearing), 52 (table), 67 (count), 90 (clearing), 104 (credits), 130 (debits)
 * Report 130: Position 24 (currency), 62 (count), 87 (interchange), 110 (credits), 132 (debits)
 * Report 140: Position 24 (currency), 67 (count), 90 (interchange), 111 (credits), 132 (debits)
 * Report 900: Position 22 (currency), 67 (count), 89 (clearing), 106 (total count), 131 (total clearing)
 * 
 * @author EL.AHGOUNE
 * @version 1.0.0
 * @since 2024
 */
@Component
public class VssTextReportParser {

    private static final Logger log = LoggerFactory.getLogger(VssTextReportParser.class);

    // Report type patterns
    private static final Pattern REPORT_110_PATTERN = Pattern.compile("REPORT ID:\\s*VSS-110");
    private static final Pattern REPORT_120_PATTERN = Pattern.compile("REPORT ID:\\s*VSS-120");
    private static final Pattern REPORT_130_PATTERN = Pattern.compile("REPORT ID:\\s*VSS-130");
    private static final Pattern REPORT_140_PATTERN = Pattern.compile("REPORT ID:\\s*VSS-140");
    private static final Pattern REPORT_900_PATTERN = Pattern.compile("REPORT ID:\\s*VSS-900-S");

    // Date patterns
    private static final Pattern PROC_DATE_PATTERN = Pattern.compile("PROC DATE:\\s*(\\d{2}[A-Z]{3}\\d{2})");
    private static final Pattern REPORT_DATE_PATTERN = Pattern.compile("REPORT DATE:\\s*(\\d{2}[A-Z]{3}\\d{2})");
    
    // Date formatter for DDMMMYY format (e.g., 17FEB22)
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("ddMMMuu", Locale.ENGLISH);

    // Section end markers
    private static final Pattern END_REPORT_PATTERN = Pattern.compile("\\*\\*\\*\\s*END OF VSS-\\d+(-S)?\\s*REPORT\\s*\\*\\*\\*");

    /**
     * Parse a complete VSS text file and extract all report data.
     *
     * @param fileContent the complete file content
     * @param fileName the source file name
     * @return map of report type to list of parsed data
     */
    public Map<String, List<?>> parseVssTextFile(String fileContent, String fileName) {
        Map<String, List<?>> results = new HashMap<>();
        
        String[] lines = fileContent.split("\\r?\\n");
        
        String currentReportType = null;
        List<String> currentReportLines = new ArrayList<>();
        LocalDate processingDate = null;
        LocalDate reportDate = null;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            
            // Check for report start
            String reportType = identifyReportType(line);
            if (reportType != null) {
                // Process previous report if exists
                if (currentReportType != null && !currentReportLines.isEmpty()) {
                    processReport(currentReportType, currentReportLines, fileName, processingDate, reportDate, results);
                }
                
                // Start new report
                currentReportType = reportType;
                currentReportLines = new ArrayList<>();
                
                // Extract dates from header
                processingDate = extractProcessingDate(line);
                reportDate = extractReportDate(line);
                
                log.debug("Found report type: {} at line {}", reportType, i + 1);
                continue;
            }
            
            // Check for report end
            if (END_REPORT_PATTERN.matcher(line).find()) {
                if (currentReportType != null && !currentReportLines.isEmpty()) {
                    processReport(currentReportType, currentReportLines, fileName, processingDate, reportDate, results);
                }
                currentReportType = null;
                currentReportLines = new ArrayList<>();
                continue;
            }
            
            // Collect lines for current report
            if (currentReportType != null) {
                currentReportLines.add(line);
            }
        }
        
        // Process last report if exists
        if (currentReportType != null && !currentReportLines.isEmpty()) {
            processReport(currentReportType, currentReportLines, fileName, processingDate, reportDate, results);
        }
        
        return results;
    }

    /**
     * Identify the report type from a line.
     */
    private String identifyReportType(String line) {
        if (REPORT_110_PATTERN.matcher(line).find()) return "VSS-110";
        if (REPORT_120_PATTERN.matcher(line).find()) return "VSS-120";
        if (REPORT_130_PATTERN.matcher(line).find()) return "VSS-130";
        if (REPORT_140_PATTERN.matcher(line).find()) return "VSS-140";
        if (REPORT_900_PATTERN.matcher(line).find()) return "VSS-900";
        return null;
    }

    /**
     * Extract processing date from header line.
     */
    private LocalDate extractProcessingDate(String line) {
        Matcher matcher = PROC_DATE_PATTERN.matcher(line);
        if (matcher.find()) {
            return parseDate(matcher.group(1));
        }
        return null;
    }

    /**
     * Extract report date from header line.
     */
    private LocalDate extractReportDate(String line) {
        Matcher matcher = REPORT_DATE_PATTERN.matcher(line);
        if (matcher.find()) {
            return parseDate(matcher.group(1));
        }
        return null;
    }

    /**
     * Parse date in DDMMMYY format.
     */
    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            log.warn("Failed to parse date: {}", dateStr);
            return null;
        }
    }

    /**
     * Process a complete report section.
     */
    private void processReport(String reportType, List<String> lines, String fileName, 
                              LocalDate processingDate, LocalDate reportDate, Map<String, List<?>> results) {
        
        switch (reportType) {
            case "VSS-110" -> {
                List<Report110Dto> report110Data = parseReport110(lines, fileName, processingDate, reportDate);
                results.put("VSS-110", report110Data);
            }
            case "VSS-120" -> {
                List<Report120Dto> report120Data = parseReport120(lines, fileName, processingDate, reportDate);
                results.put("VSS-120", report120Data);
            }
            case "VSS-130" -> {
                List<Report130Dto> report130Data = parseReport130(lines, fileName, processingDate, reportDate);
                results.put("VSS-130", report130Data);
            }
            case "VSS-140" -> {
                List<Report140Dto> report140Data = parseReport140(lines, fileName, processingDate, reportDate);
                results.put("VSS-140", report140Data);
            }
            case "VSS-900" -> {
                List<Report900Dto> report900Data = parseReport900(lines, fileName, processingDate, reportDate);
                results.put("VSS-900", report900Data);
            }
            default -> log.warn("Unknown report type: {}", reportType);
        }
    }

    /**
     * Parse VSS-110 Settlement Summary Report.
     * Field mappings from ReadingReports.txt:
     * - SETTLEMENT CURRENCY: Position 24 read three from left to right
     * - COUNT: Position 52 read from right to left  
     * - CREDIT AMOUNT: Position 78 read from right to left
     * - DEBIT AMOUNT: Position 104 read from right to left
     * - TOTAL AMOUNT: Position 132 read from right to left
     */
    private List<Report110Dto> parseReport110(List<String> lines, String fileName, 
                                             LocalDate processingDate, LocalDate reportDate) {
        List<Report110Dto> results = new ArrayList<>();
        String currentSection = null;
        
        // Extract currency context from header lines
        CurrencyContext currencyContext = extractCurrencyContext(lines);
        
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            
            // Skip header and formatting lines
            if (isHeaderOrFormatLine(line)) {
                continue;
            }
            
            // Detect section names
            String sectionName = extractSectionName(line);
            if (sectionName != null) {
                currentSection = sectionName;
                continue;
            }
            
            // Parse data lines with financial data
            if (hasFinancialData(line)) {
                try {
                    Report110Dto dto = Report110Dto.builder()
                        .settlementCurrency(currencyContext.getSettlementCurrency())
                        .count(extractCount(line, 52))
                        .creditAmount(extractAmount(line, 78))
                        .debitAmount(extractAmount(line, 104))
                        .totalAmount(extractAmount(line, 132))
                        .sectionName(currentSection)
                        .processingDate(processingDate)
                        .reportDate(reportDate)
                        .sourceFileName(fileName)
                        .lineNumber(i + 1)
                        .rawLineContent(line)
                        .build();
                    
                    results.add(dto);
                    log.debug("Parsed VSS-110 record: {} with currency: {}", dto.getSectionName(), dto.getSettlementCurrency());
                    
                } catch (Exception e) {
                    log.error("Error parsing VSS-110 line {}: {}", i + 1, e.getMessage());
                }
            }
        }
        
        return results;
    }

    /**
     * Parse VSS-120 Interchange Value Report.
     */
    private List<Report120Dto> parseReport120(List<String> lines, String fileName, 
                                             LocalDate processingDate, LocalDate reportDate) {
        List<Report120Dto> results = new ArrayList<>();
        String currentTransactionType = null;
        String currentTransactionDetail = null;
        
        // Extract currency context from header lines
        CurrencyContext currencyContext = extractCurrencyContext(lines);
        
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            
            if (isHeaderOrFormatLine(line)) {
                continue;
            }
            
            // Extract transaction context
            String transactionType = extractTransactionType(line);
            if (transactionType != null) {
                currentTransactionType = transactionType;
                continue;
            }
            
            String transactionDetail = extractTransactionDetail(line);
            if (transactionDetail != null) {
                currentTransactionDetail = transactionDetail;
                continue;
            }
            
            // Parse data lines
            if (hasFinancialData(line)) {
                try {
                    Report120Dto dto = Report120Dto.builder()
                        .settlementCurrency(currencyContext.getSettlementCurrency())
                        .clearingCurrency(currencyContext.getClearingCurrency())
                        .tableId(extractTableId(line, 52))
                        .count(extractCount(line, 67))
                        .clearingAmount(extractAmount(line, 90))
                        .interchangeCredits(extractAmount(line, 104))
                        .interchangeDebits(extractAmount(line, 130))
                        .transactionType(currentTransactionType)
                        .transactionDetail(currentTransactionDetail)
                        .processingDate(processingDate)
                        .reportDate(reportDate)
                        .sourceFileName(fileName)
                        .lineNumber(i + 1)
                        .rawLineContent(line)
                        .build();
                    
                    results.add(dto);
                    log.debug("Parsed VSS-120 record: {} - {} with currencies: {}/{}", 
                        currentTransactionType, currentTransactionDetail, 
                        dto.getSettlementCurrency(), dto.getClearingCurrency());
                    
                } catch (Exception e) {
                    log.error("Error parsing VSS-120 line {}: {}", i + 1, e.getMessage());
                }
            }
        }
        
        return results;
    }

    /**
     * Parse VSS-130 Reimbursement Fees Report.
     */
    private List<Report130Dto> parseReport130(List<String> lines, String fileName, 
                                             LocalDate processingDate, LocalDate reportDate) {
        List<Report130Dto> results = new ArrayList<>();
        String currentTransactionType = null;
        String currentTransactionDetail = null;
        String currentFeeCategory = null;
        
        // Extract currency context from header lines
        CurrencyContext currencyContext = extractCurrencyContext(lines);
        
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            
            if (isHeaderOrFormatLine(line)) {
                continue;
            }
            
            // Extract context information
            String transactionType = extractTransactionType(line);
            if (transactionType != null) {
                currentTransactionType = transactionType;
                continue;
            }
            
            String transactionDetail = extractTransactionDetail(line);
            if (transactionDetail != null) {
                currentTransactionDetail = transactionDetail;
                continue;
            }
            
            String feeCategory = extractFeeCategory(line);
            if (feeCategory != null) {
                currentFeeCategory = feeCategory;
                continue;
            }
            
            // Parse data lines
            if (hasFinancialData(line)) {
                try {
                    Report130Dto dto = Report130Dto.builder()
                        .settlementCurrency(currencyContext.getSettlementCurrency())
                        .count(extractCount(line, 62))
                        .interchangeAmount(extractAmount(line, 87))
                        .reimbursementFeeCredits(extractAmount(line, 110))
                        .reimbursementFeeDebits(extractAmount(line, 132))
                        .transactionType(currentTransactionType)
                        .transactionDetail(currentTransactionDetail)
                        .feeCategory(currentFeeCategory)
                        .processingDate(processingDate)
                        .reportDate(reportDate)
                        .sourceFileName(fileName)
                        .lineNumber(i + 1)
                        .rawLineContent(line)
                        .build();
                    
                    results.add(dto);
                    log.debug("Parsed VSS-130 record: {} - {} with currency: {}", 
                        currentTransactionType, currentFeeCategory, dto.getSettlementCurrency());
                    
                } catch (Exception e) {
                    log.error("Error parsing VSS-130 line {}: {}", i + 1, e.getMessage());
                }
            }
        }
        
        return results;
    }

    /**
     * Parse VSS-140 Visa Charges Report.
     */
    private List<Report140Dto> parseReport140(List<String> lines, String fileName, 
                                             LocalDate processingDate, LocalDate reportDate) {
        List<Report140Dto> results = new ArrayList<>();
        String currentChargeType = null;
        String currentTransactionType = null;
        String currentTransactionDetail = null;
        String currentRegion = null;
        
        // Extract currency context from header lines
        CurrencyContext currencyContext = extractCurrencyContext(lines);
        
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            
            if (isHeaderOrFormatLine(line)) {
                continue;
            }
            
            // Extract context information
            String chargeType = extractChargeType(line);
            if (chargeType != null) {
                currentChargeType = chargeType;
                continue;
            }
            
            String transactionType = extractTransactionType(line);
            if (transactionType != null) {
                currentTransactionType = transactionType;
                continue;
            }
            
            String transactionDetail = extractTransactionDetail(line);
            if (transactionDetail != null) {
                currentTransactionDetail = transactionDetail;
                continue;
            }
            
            String region = extractRegion(line);
            if (region != null) {
                currentRegion = region;
                continue;
            }
            
            // Parse data lines
            if (hasFinancialData(line)) {
                try {
                    Report140Dto dto = Report140Dto.builder()
                        .settlementCurrency(currencyContext.getSettlementCurrency())
                        .count(extractCount(line, 67))
                        .interchangeAmount(extractAmount(line, 90))
                        .visaChargesCredits(extractAmount(line, 111))
                        .visaChargesDebits(extractAmount(line, 132))
                        .chargeType(currentChargeType)
                        .transactionType(currentTransactionType)
                        .transactionDetail(currentTransactionDetail)
                        .region(currentRegion)
                        .processingDate(processingDate)
                        .reportDate(reportDate)
                        .sourceFileName(fileName)
                        .lineNumber(i + 1)
                        .rawLineContent(line)
                        .build();
                    
                    results.add(dto);
                    log.debug("Parsed VSS-140 record: {} - {} with currency: {}", 
                        currentChargeType, currentTransactionType, dto.getSettlementCurrency());
                    
                } catch (Exception e) {
                    log.error("Error parsing VSS-140 line {}: {}", i + 1, e.getMessage());
                }
            }
        }
        
        return results;
    }

    /**
     * Parse VSS-900 Summary Reconciliation Report.
     */
    private List<Report900Dto> parseReport900(List<String> lines, String fileName, 
                                             LocalDate processingDate, LocalDate reportDate) {
        List<Report900Dto> results = new ArrayList<>();
        String currentTransactionCategory = null;
        String currentTransactionDirection = null;
        String currentClearingCurrency = null; // VSS-900 has multiple clearing currencies
        
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            
            // Extract clearing currency from header lines specific to VSS-900
            String trimmed = line.trim();
            if (trimmed.startsWith("CLEARING CURRENCY:")) {
                String currencyPart = trimmed.substring("CLEARING CURRENCY:".length()).trim();
                if (!currencyPart.isEmpty()) {
                    currentClearingCurrency = currencyPart.length() >= 3 ? currencyPart.substring(0, 3) : currencyPart;
                }
                continue;
            }
            
            if (isHeaderOrFormatLine(line)) {
                continue;
            }
            
            // Extract context information
            String transactionCategory = extractTransactionCategory(line);
            if (transactionCategory != null) {
                currentTransactionCategory = transactionCategory;
                continue;
            }
            
            String transactionDirection = extractTransactionDirection(line);
            if (transactionDirection != null) {
                currentTransactionDirection = transactionDirection;
                continue;
            }
            
            // Parse data lines
            if (hasFinancialData(line)) {
                try {
                    Report900Dto dto = Report900Dto.builder()
                        .clearingCurrency(currentClearingCurrency)
                        .count(extractCount(line, 67))
                        .clearingAmount(extractAmount(line, 89))
                        .totalCount(extractCount(line, 106))
                        .totalClearingAmount(extractAmount(line, 131))
                        .transactionCategory(currentTransactionCategory)
                        .transactionDirection(currentTransactionDirection)
                        .processingDate(processingDate)
                        .reportDate(reportDate)
                        .sourceFileName(fileName)
                        .lineNumber(i + 1)
                        .rawLineContent(line)
                        .build();
                    
                    results.add(dto);
                    log.debug("Parsed VSS-900 record: {} - {} with clearing currency: {}", 
                        currentTransactionCategory, currentTransactionDirection, dto.getClearingCurrency());
                    
                } catch (Exception e) {
                    log.error("Error parsing VSS-900 line {}: {}", i + 1, e.getMessage());
                }
            }
        }
        
        return results;
    }

    // Helper methods for field extraction

    /**
     * Extract currency code from specified position (read left to right).
     */
    private String extractCurrency(String line, int position) {
        if (line.length() < position + 2) return null;
        String currency = line.substring(position - 1, position + 2).trim();
        return currency.isEmpty() ? null : currency;
    }

    /**
     * Extract count from specified position (read right to left).
     */
    private Long extractCount(String line, int position) {
        try {
            String countStr = extractRightAlignedField(line, position, 15).trim();
            if (countStr.isEmpty() || countStr.equals("0")) return 0L;
            return Long.parseLong(countStr.replaceAll(",", ""));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extract amount from specified position (read right to left).
     */
    private BigDecimal extractAmount(String line, int position) {
        try {
            String amountStr = extractRightAlignedField(line, position, 20).trim();
            if (amountStr.isEmpty() || amountStr.equals("0.00")) return BigDecimal.ZERO;
            
            // Remove commas and handle CR/DB indicators
            amountStr = amountStr.replaceAll(",", "");
            boolean isCredit = amountStr.endsWith("CR");
            boolean isDebit = amountStr.endsWith("DB");
            
            if (isCredit || isDebit) {
                amountStr = amountStr.substring(0, amountStr.length() - 2).trim();
            }
            
            BigDecimal amount = new BigDecimal(amountStr).setScale(2, RoundingMode.HALF_UP);
            return isDebit ? amount.negate() : amount;
            
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extract table ID from specified position.
     */
    private String extractTableId(String line, int position) {
        try {
            return extractRightAlignedField(line, position, 10).trim();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extract a right-aligned field from the specified position.
     */
    private String extractRightAlignedField(String line, int endPosition, int maxLength) {
        int startPos = Math.max(0, endPosition - maxLength);
        int endPos = Math.min(line.length(), endPosition);
        
        if (startPos >= endPos) return "";
        return line.substring(startPos, endPos);
    }

    // Helper methods for line classification and context extraction

    private boolean isHeaderOrFormatLine(String line) {
        return line.trim().isEmpty() || 
               line.contains("VISANET SETTLEMENT SERVICE") ||
               line.contains("PAGE:") ||
               line.contains("REPORTING FOR:") ||
               line.contains("ROLLUP TO:") ||
               line.contains("FUNDS XFER ENTITY:") ||
               line.contains("SETTLEMENT CURRENCY:") ||
               line.contains("CLEARING CURRENCY:") ||
               line.contains("---") ||
               line.contains("===") ||
               line.matches("^\\s*[A-Z\\s]+[A-Z\\s]*$") && !hasFinancialData(line);
    }

    private boolean hasFinancialData(String line) {
        // Look for patterns that indicate financial data
        return line.matches(".*\\d+\\.\\d{2}.*") || 
               line.matches(".*\\d+CR.*") ||
               line.matches(".*\\d+DB.*") ||
               line.matches(".*\\d{1,3}(,\\d{3})*\\.\\d{2}.*");
    }

    private String extractSectionName(String line) {
        String trimmed = line.trim();
        if (trimmed.matches("^[A-Z\\s]+$") && trimmed.length() > 3 && trimmed.length() < 50) {
            if (trimmed.contains("INTERCHANGE") || trimmed.contains("REIMBURSEMENT") || 
                trimmed.contains("VISA CHARGES") || trimmed.contains("TOTAL")) {
                return trimmed;
            }
        }
        return null;
    }

    private String extractTransactionType(String line) {
        String trimmed = line.trim();
        if (trimmed.equals("PURCHASE") || trimmed.equals("MANUAL CASH") || trimmed.contains("CASH")) {
            return trimmed;
        }
        return null;
    }

    private String extractTransactionDetail(String line) {
        String trimmed = line.trim();
        if (trimmed.equals("ORIGINAL SALE") || trimmed.equals("ORIGINAL ADVANCE") || 
            trimmed.contains("ORIGINAL")) {
            return trimmed;
        }
        return null;
    }

    private String extractFeeCategory(String line) {
        String trimmed = line.trim();
        if (trimmed.contains("VISA") && (trimmed.contains("CEMEA") || trimmed.contains("INTERNATIONAL"))) {
            return trimmed;
        }
        return null;
    }

    private String extractChargeType(String line) {
        String trimmed = line.trim();
        if (trimmed.contains("IAF CHARGE") || trimmed.contains("CHARGE")) {
            return trimmed;
        }
        return null;
    }

    private String extractRegion(String line) {
        String trimmed = line.trim();
        if (trimmed.contains("C.E.M.E.A") || trimmed.contains("E.U.") || trimmed.contains("CEMEA")) {
            return trimmed;
        }
        return null;
    }

    private String extractTransactionCategory(String line) {
        String trimmed = line.trim();
        if (trimmed.contains("FINANCIAL TRANSACTIONS") || trimmed.contains("NON-FINANCIAL TRANSACTIONS")) {
            return trimmed;
        }
        return null;
    }

    private String extractTransactionDirection(String line) {
        String trimmed = line.trim();
        if (trimmed.contains("SENT TO VISA") || trimmed.contains("RECEIVED FROM VISA")) {
            return trimmed;
        }
        return null;
    }

    /**
     * Extract currency codes from header lines instead of data line positions.
     * This method looks for currency declarations in the report header.
     */
    private CurrencyContext extractCurrencyContext(List<String> lines) {
        String settlementCurrency = null;
        String clearingCurrency = null;
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            // Extract settlement currency
            if (trimmed.startsWith("SETTLEMENT CURRENCY:")) {
                String currencyPart = trimmed.substring("SETTLEMENT CURRENCY:".length()).trim();
                if (!currencyPart.isEmpty() && currencyPart.length() >= 3) {
                    settlementCurrency = currencyPart.substring(0, 3);
                    log.debug("Found settlement currency: {}", settlementCurrency);
                }
            }
            
            // Extract clearing currency  
            if (trimmed.startsWith("CLEARING CURRENCY:")) {
                String currencyPart = trimmed.substring("CLEARING CURRENCY:".length()).trim();
                if (!currencyPart.isEmpty() && currencyPart.length() >= 3) {
                    clearingCurrency = currencyPart.substring(0, 3);
                    log.debug("Found clearing currency: {}", clearingCurrency);
                } else if (currencyPart.isEmpty()) {
                    log.debug("Found empty clearing currency declaration");
                }
            }
        }
        
        log.debug("Currency context extracted - Settlement: {}, Clearing: {}", settlementCurrency, clearingCurrency);
        return new CurrencyContext(settlementCurrency, clearingCurrency);
    }
    
    /**
     * Helper class to hold currency context for a report.
     */
    private static class CurrencyContext {
        private final String settlementCurrency;
        private final String clearingCurrency;
        
        public CurrencyContext(String settlementCurrency, String clearingCurrency) {
            this.settlementCurrency = settlementCurrency;
            this.clearingCurrency = clearingCurrency;
        }
        
        public String getSettlementCurrency() { return settlementCurrency; }
        public String getClearingCurrency() { return clearingCurrency; }
    }
} 