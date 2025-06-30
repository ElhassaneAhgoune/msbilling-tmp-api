package com.moneysab.cardexis.service;

import com.moneysab.cardexis.config.VssFieldMappingConfig;
import com.moneysab.cardexis.dto.vss.report.*;
import com.moneysab.cardexis.domain.entity.vss.*;
import com.moneysab.cardexis.repository.vss.*;
import com.moneysab.cardexis.util.VssFieldExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for processing VSS text reports using configuration-based field mapping.
 * Based on ReadingReports.txt specifications through VssFieldMappingConfig.
 * 
 * @author EL.AHGOUNE
 * @version 1.0.0
 * @since 2024
 */
@Service
@Transactional
public class VssReportService {

    private static final Logger log = LoggerFactory.getLogger(VssReportService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("ddMMMuu", Locale.ENGLISH);

    @Autowired
    private VssFieldMappingConfig fieldConfig;

    @Autowired
    private VssFieldExtractor fieldExtractor;

    @Autowired
    private Report110Repository report110Repository;

    @Autowired
    private Report120Repository report120Repository;

    @Autowired
    private Report130Repository report130Repository;

    @Autowired
    private Report140Repository report140Repository;

    @Autowired
    private Report900Repository report900Repository;

    /**
     * Process uploaded VSS text file.
     */
    public Map<String, Object> processVssFile(MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String fileName = file.getOriginalFilename();
            String fileContent = new String(file.getBytes());
            
            log.info("Processing VSS file: {}", fileName);
            
            Map<String, List<?>> parsedData = parseVssTextFile(fileContent, fileName);
            Map<String, Integer> savedCounts = saveToDatabase(parsedData);
            
            result.put("success", true);
            result.put("fileName", fileName);
            result.put("fileSize", file.getSize());
            result.put("parsedSections", parsedData.keySet());
            result.put("savedCounts", savedCounts);
            result.put("message", "File processed successfully");
            
            log.info("Successfully processed file: {} with sections: {}", fileName, parsedData.keySet());
            
        } catch (IOException e) {
            log.error("IO error processing file: {}", e.getMessage());
            result.put("success", false);
            result.put("error", "Failed to read file: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error processing VSS file: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", "Processing failed: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Parse VSS text file content.
     */
    private Map<String, List<?>> parseVssTextFile(String fileContent, String fileName) {
        Map<String, List<?>> results = new HashMap<>();
        String[] lines = fileContent.split("\\r?\\n");
        
        String currentReportType = null;
        List<String> currentReportLines = new ArrayList<>();
        LocalDate processingDate = LocalDate.now();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            
            String reportType = identifyReportType(line);
            if (reportType != null) {
                if (currentReportType != null && !currentReportLines.isEmpty()) {
                    processReport(currentReportType, currentReportLines, fileName, processingDate, results);
                }
                currentReportType = reportType;
                currentReportLines = new ArrayList<>();
                processingDate = extractProcessingDate(line);
                continue;
            }
            
            if (line.contains("*** END OF VSS-")) {
                if (currentReportType != null && !currentReportLines.isEmpty()) {
                    processReport(currentReportType, currentReportLines, fileName, processingDate, results);
                }
                currentReportType = null;
                currentReportLines = new ArrayList<>();
                continue;
            }
            
            if (currentReportType != null) {
                currentReportLines.add(line);
            }
        }
        
        if (currentReportType != null && !currentReportLines.isEmpty()) {
            processReport(currentReportType, currentReportLines, fileName, processingDate, results);
        }
        
        return results;
    }

    private String identifyReportType(String line) {
        if (line.contains("REPORT ID:  VSS-110")) return "VSS-110";
        if (line.contains("REPORT ID:  VSS-120")) return "VSS-120";
        if (line.contains("REPORT ID:  VSS-130")) return "VSS-130";
        if (line.contains("REPORT ID:  VSS-140")) return "VSS-140";
        if (line.contains("REPORT ID:  VSS-900-S")) return "VSS-900";
        return null;
    }

    private LocalDate extractProcessingDate(String line) {
        try {
            Pattern pattern = Pattern.compile("PROC DATE:\\s*(\\d{2}[A-Z]{3}\\d{2})");
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                return LocalDate.parse(matcher.group(1), DATE_FORMATTER);
            }
        } catch (Exception e) {
            log.warn("Failed to parse processing date from: {}", line);
        }
        return LocalDate.now();
    }

    private void processReport(String reportType, List<String> lines, String fileName, 
                              LocalDate processingDate, Map<String, List<?>> results) {
        log.info("Processing report {} with {} lines", reportType, lines.size());
        
        // Log first few lines to verify section splitting
        for (int i = 0; i < Math.min(5, lines.size()); i++) {
            log.debug("{} Line {}: '{}'", reportType, i, lines.get(i).trim());
        }
        
        switch (reportType) {
            case "VSS-110" -> results.put("VSS-110", parseReport110(lines, fileName, processingDate));
            case "VSS-120" -> results.put("VSS-120", parseReport120(lines, fileName, processingDate));
            case "VSS-130" -> results.put("VSS-130", parseReport130(lines, fileName, processingDate));
            case "VSS-140" -> results.put("VSS-140", parseReport140(lines, fileName, processingDate));
            case "VSS-900" -> {
                // VSS-900-S appears multiple times - accumulate all data
                @SuppressWarnings("unchecked")
                List<Report900Dto> existingData = (List<Report900Dto>) results.get("VSS-900");
                if (existingData == null) {
                    results.put("VSS-900", parseReport900(lines, fileName, processingDate));
                } else {
                    List<Report900Dto> newData = parseReport900(lines, fileName, processingDate);
                    existingData.addAll(newData);
                    log.info("Accumulated VSS-900 data: {} existing + {} new = {} total", 
                        existingData.size() - newData.size(), newData.size(), existingData.size());
                }
            }
            default -> log.debug("Report type {} not implemented yet", reportType);
        }
    }

    /**
     * Parse VSS-110 using configuration-based field extraction.
     */
    private List<Report110Dto> parseReport110(List<String> lines, String fileName, LocalDate processingDate) {
        List<Report110Dto> results = new ArrayList<>();
        String currentSection = null;
        VssFieldMappingConfig.Report110Fields fields = fieldConfig.getReport110();
        
        // Extract currency from header lines instead of data lines
        String settlementCurrency = extractSettlementCurrency(lines);
        log.debug("VSS-110 Settlement currency extracted from headers: {}", settlementCurrency);
        
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            
            if (fieldExtractor.isHeaderOrFormatLine(line)) {
                String section = fieldExtractor.extractSectionName(line);
                if (section != null) currentSection = section;
                continue;
            }
            
            if (fieldExtractor.hasFinancialData(line)) {
                try {
                    Long count = fieldExtractor.extractCount(line, fields.getCount());
                    
                    log.debug("VSS-110 Line: {}", line);
                    log.debug("Using header currency: '{}', count: {}", settlementCurrency, count);
                    
                    Report110Dto dto = Report110Dto.builder()
                        .settlementCurrency(settlementCurrency)
                        .count(count)
                        .creditAmount(fieldExtractor.extractAmount(line, fields.getCreditAmount()))
                        .debitAmount(fieldExtractor.extractAmount(line, fields.getDebitAmount()))
                        .totalAmount(fieldExtractor.extractAmount(line, fields.getTotalAmount()))
                        .sectionName(currentSection)
                        .processingDate(processingDate)
                        .reportDate(processingDate)
                        .sourceFileName(fileName)
                        .lineNumber(i + 1)
                        .rawLineContent(line)
                        .build();
                    results.add(dto);
                } catch (Exception e) {
                    log.error("Error parsing VSS-110 line {}: {}", i + 1, e.getMessage());
                }
            }
        }
        return results;
    }

    /**
     * Parse VSS-120 using configuration-based field extraction.
     */
    private List<Report120Dto> parseReport120(List<String> lines, String fileName, LocalDate processingDate) {
        List<Report120Dto> results = new ArrayList<>();
        String currentTransactionType = null;
        VssFieldMappingConfig.Report120Fields fields = fieldConfig.getReport120();
        
        // Extract currencies from header lines
        String settlementCurrency = extractSettlementCurrency(lines);
        String clearingCurrency = extractClearingCurrency(lines);
        log.debug("VSS-120 Currencies extracted from headers - Settlement: {}, Clearing: {}", 
            settlementCurrency, clearingCurrency);
        
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            
            if (fieldExtractor.isHeaderOrFormatLine(line)) {
                String transactionType = fieldExtractor.extractTransactionType(line);
                if (transactionType != null) currentTransactionType = transactionType;
                continue;
            }
            
            if (fieldExtractor.hasFinancialData(line)) {
                try {
                    Report120Dto dto = Report120Dto.builder()
                        .settlementCurrency(settlementCurrency)
                        .clearingCurrency(clearingCurrency)
                        .tableId(fieldExtractor.extractTableId(line, fields.getTableId()))
                        .count(fieldExtractor.extractCount(line, fields.getCount()))
                        .clearingAmount(fieldExtractor.extractAmount(line, fields.getClearingAmount()))
                        .interchangeCredits(fieldExtractor.extractAmount(line, fields.getInterchangeCredits()))
                        .interchangeDebits(fieldExtractor.extractAmount(line, fields.getInterchangeDebits()))
                        .transactionType(currentTransactionType)
                        .processingDate(processingDate)
                        .reportDate(processingDate)
                        .sourceFileName(fileName)
                        .lineNumber(i + 1)
                        .rawLineContent(line)
                        .build();
                    results.add(dto);
                } catch (Exception e) {
                    log.error("Error parsing VSS-120 line {}: {}", i + 1, e.getMessage());
                }
            }
        }
        return results;
    }

    /**
     * Parse VSS-130 using configuration-based field extraction.
     */
    private List<Report130Dto> parseReport130(List<String> lines, String fileName, LocalDate processingDate) {
        List<Report130Dto> results = new ArrayList<>();
        VssFieldMappingConfig.Report130Fields fields = fieldConfig.getReport130();
        
        // Extract currency from header lines
        String settlementCurrency = extractSettlementCurrency(lines);
        log.debug("VSS-130 Settlement currency extracted from headers: {}", settlementCurrency);
        
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            
            if (fieldExtractor.isHeaderOrFormatLine(line)) continue;
            
            if (fieldExtractor.hasFinancialData(line)) {
                try {
                    Report130Dto dto = Report130Dto.builder()
                        .settlementCurrency(settlementCurrency)
                        .count(fieldExtractor.extractCount(line, fields.getCount()))
                        .interchangeAmount(fieldExtractor.extractAmount(line, fields.getInterchangeAmount()))
                        .reimbursementFeeCredits(fieldExtractor.extractAmount(line, fields.getReimbursementFeeCredits()))
                        .reimbursementFeeDebits(fieldExtractor.extractAmount(line, fields.getReimbursementFeeDebits()))
                        .processingDate(processingDate)
                        .reportDate(processingDate)
                        .sourceFileName(fileName)
                        .lineNumber(i + 1)
                        .rawLineContent(line)
                        .build();
                    results.add(dto);
                } catch (Exception e) {
                    log.error("Error parsing VSS-130 line {}: {}", i + 1, e.getMessage());
                }
            }
        }
        return results;
    }

    /**
     * Parse VSS-140 using configuration-based field extraction.
     */
    private List<Report140Dto> parseReport140(List<String> lines, String fileName, LocalDate processingDate) {
        List<Report140Dto> results = new ArrayList<>();
        VssFieldMappingConfig.Report140Fields fields = fieldConfig.getReport140();
        
        // Extract currency from header lines
        String settlementCurrency = extractSettlementCurrency(lines);
        log.debug("VSS-140 Settlement currency extracted from headers: {}", settlementCurrency);
        
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            
            if (fieldExtractor.isHeaderOrFormatLine(line)) continue;
            
            if (fieldExtractor.hasFinancialData(line)) {
                try {
                    Report140Dto dto = Report140Dto.builder()
                        .settlementCurrency(settlementCurrency)
                        .count(fieldExtractor.extractCount(line, fields.getCount()))
                        .interchangeAmount(fieldExtractor.extractAmount(line, fields.getInterchangeAmount()))
                        .visaChargesCredits(fieldExtractor.extractAmount(line, fields.getVisaChargesCredits()))
                        .visaChargesDebits(fieldExtractor.extractAmount(line, fields.getVisaChargesDebits()))
                        .processingDate(processingDate)
                        .reportDate(processingDate)
                        .sourceFileName(fileName)
                        .lineNumber(i + 1)
                        .rawLineContent(line)
                        .build();
                    results.add(dto);
                } catch (Exception e) {
                    log.error("Error parsing VSS-140 line {}: {}", i + 1, e.getMessage());
                }
            }
        }
        return results;
    }

    /**
     * Parse VSS-900 using configuration-based field extraction.
     * VSS-900-S has multiple clearing currency sections that change throughout the report.
     */
    private List<Report900Dto> parseReport900(List<String> lines, String fileName, LocalDate processingDate) {
        List<Report900Dto> results = new ArrayList<>();
        VssFieldMappingConfig.Report900Fields fields = fieldConfig.getReport900();
        String currentClearingCurrency = null;
        
        log.info("Processing VSS-900-S with {} lines", lines.size());
        
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String trimmed = line.trim();
            
            // Extract clearing currency from header lines - VSS-900 has multiple currency sections
            if (trimmed.startsWith("CLEARING CURRENCY:")) {
                String currencyPart = trimmed.substring("CLEARING CURRENCY:".length()).trim();
                if (!currencyPart.isEmpty()) {
                    currentClearingCurrency = currencyPart.length() >= 3 ? currencyPart.substring(0, 3) : currencyPart;
                    log.info("VSS-900 found new clearing currency section: {}", currentClearingCurrency);
                }
                continue;
            }
            
            if (fieldExtractor.isHeaderOrFormatLine(line)) {
                log.debug("VSS-900 skipping header/format line: '{}'", line);
                continue;
            }
            
            boolean hasFinancialData = fieldExtractor.hasFinancialData(line);
            log.debug("VSS-900 checking line '{}' -> hasFinancialData: {}", line.trim(), hasFinancialData);
            
            if (hasFinancialData) {
                try {
                    log.debug("VSS-900 parsing line with currency '{}': {}", currentClearingCurrency, line);
                    
                    Report900Dto dto = Report900Dto.builder()
                        .clearingCurrency(currentClearingCurrency)
                        .count(fieldExtractor.extractCount(line, fields.getCount()))
                        .clearingAmount(fieldExtractor.extractAmount(line, fields.getClearingAmount()))
                        .totalCount(fieldExtractor.extractCount(line, fields.getTotalCount()))
                        .totalClearingAmount(fieldExtractor.extractAmount(line, fields.getTotalClearingAmount()))
                        .processingDate(processingDate)
                        .reportDate(processingDate)
                        .sourceFileName(fileName)
                        .lineNumber(i + 1)
                        .rawLineContent(line)
                        .build();
                    results.add(dto);
                    log.debug("Added VSS-900 record with clearing currency: {}", currentClearingCurrency);
                } catch (Exception e) {
                    log.error("Error parsing VSS-900 line {}: {}", i + 1, e.getMessage());
                }
            }
        }
        
        log.info("VSS-900 parsing completed. Found {} records", results.size());
        return results;
    }

    /**
     * Save parsed data to database.
     */
    private Map<String, Integer> saveToDatabase(Map<String, List<?>> parsedData) {
        Map<String, Integer> savedCounts = new HashMap<>();
        
        if (parsedData.containsKey("VSS-110")) {
            @SuppressWarnings("unchecked")
            List<Report110Dto> report110Data = (List<Report110Dto>) parsedData.get("VSS-110");
            List<Report110Entity> entities = report110Data.stream().map(this::convertToEntity).toList();
            report110Repository.saveAll(entities);
            savedCounts.put("VSS-110", entities.size());
        }
        
        if (parsedData.containsKey("VSS-120")) {
            @SuppressWarnings("unchecked")
            List<Report120Dto> report120Data = (List<Report120Dto>) parsedData.get("VSS-120");
            List<Report120Entity> entities = report120Data.stream().map(this::convertToEntity).toList();
            report120Repository.saveAll(entities);
            savedCounts.put("VSS-120", entities.size());
        }
        
        if (parsedData.containsKey("VSS-130")) {
            @SuppressWarnings("unchecked")
            List<Report130Dto> report130Data = (List<Report130Dto>) parsedData.get("VSS-130");
            List<Report130Entity> entities = report130Data.stream().map(this::convertToEntity).toList();
            report130Repository.saveAll(entities);
            savedCounts.put("VSS-130", entities.size());
        }
        
        if (parsedData.containsKey("VSS-140")) {
            @SuppressWarnings("unchecked")
            List<Report140Dto> report140Data = (List<Report140Dto>) parsedData.get("VSS-140");
            List<Report140Entity> entities = report140Data.stream().map(this::convertToEntity).toList();
            report140Repository.saveAll(entities);
            savedCounts.put("VSS-140", entities.size());
        }
        
        if (parsedData.containsKey("VSS-900")) {
            @SuppressWarnings("unchecked")
            List<Report900Dto> report900Data = (List<Report900Dto>) parsedData.get("VSS-900");
            List<Report900Entity> entities = report900Data.stream().map(this::convertToEntity).toList();
            report900Repository.saveAll(entities);
            savedCounts.put("VSS-900", entities.size());
        }
        
        return savedCounts;
    }

    // Entity conversion methods
    private Report110Entity convertToEntity(Report110Dto dto) {
        return Report110Entity.builder()
            .settlementCurrency(dto.getSettlementCurrency())
            .count(dto.getCount())
            .creditAmount(dto.getCreditAmount())
            .debitAmount(dto.getDebitAmount())
            .totalAmount(dto.getTotalAmount())
            .sectionName(dto.getSectionName())
            .processingDate(dto.getProcessingDate())
            .reportDate(dto.getReportDate())
            .sourceFileName(dto.getSourceFileName())
            .lineNumber(dto.getLineNumber())
            .rawLineContent(dto.getRawLineContent())
            .isValid(true)
            .build();
    }

    private Report120Entity convertToEntity(Report120Dto dto) {
        return Report120Entity.builder()
            .settlementCurrency(dto.getSettlementCurrency())
            .clearingCurrency(dto.getClearingCurrency())
            .tableId(dto.getTableId())
            .count(dto.getCount())
            .clearingAmount(dto.getClearingAmount())
            .interchangeCredits(dto.getInterchangeCredits())
            .interchangeDebits(dto.getInterchangeDebits())
            .transactionType(dto.getTransactionType())
            .transactionDetail(dto.getTransactionDetail())
            .processingDate(dto.getProcessingDate())
            .reportDate(dto.getReportDate())
            .sourceFileName(dto.getSourceFileName())
            .lineNumber(dto.getLineNumber())
            .rawLineContent(dto.getRawLineContent())
            .isValid(true)
            .build();
    }

    private Report130Entity convertToEntity(Report130Dto dto) {
        return Report130Entity.builder()
            .settlementCurrency(dto.getSettlementCurrency())
            .count(dto.getCount())
            .interchangeAmount(dto.getInterchangeAmount())
            .reimbursementFeeCredits(dto.getReimbursementFeeCredits())
            .reimbursementFeeDebits(dto.getReimbursementFeeDebits())
            .processingDate(dto.getProcessingDate())
            .reportDate(dto.getReportDate())
            .sourceFileName(dto.getSourceFileName())
            .lineNumber(dto.getLineNumber())
            .rawLineContent(dto.getRawLineContent())
            .isValid(true)
            .build();
    }

    private Report140Entity convertToEntity(Report140Dto dto) {
        return Report140Entity.builder()
            .settlementCurrency(dto.getSettlementCurrency())
            .count(dto.getCount())
            .interchangeAmount(dto.getInterchangeAmount())
            .visaChargesCredits(dto.getVisaChargesCredits())
            .visaChargesDebits(dto.getVisaChargesDebits())
            .processingDate(dto.getProcessingDate())
            .reportDate(dto.getReportDate())
            .sourceFileName(dto.getSourceFileName())
            .lineNumber(dto.getLineNumber())
            .rawLineContent(dto.getRawLineContent())
            .isValid(true)
            .build();
    }

    private Report900Entity convertToEntity(Report900Dto dto) {
        return Report900Entity.builder()
            .clearingCurrency(dto.getClearingCurrency())
            .count(dto.getCount())
            .clearingAmount(dto.getClearingAmount())
            .totalCount(dto.getTotalCount())
            .totalClearingAmount(dto.getTotalClearingAmount())
            .processingDate(dto.getProcessingDate())
            .reportDate(dto.getReportDate())
            .sourceFileName(dto.getSourceFileName())
            .lineNumber(dto.getLineNumber())
            .rawLineContent(dto.getRawLineContent())
            .isValid(true)
            .build();
    }

    // Query methods
    public Page<Report110Entity> getReport110Data(String currency, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        if (currency != null && startDate != null && endDate != null) {
            return report110Repository.findBySettlementCurrency(currency, pageable);
        } else if (startDate != null && endDate != null) {
            return report110Repository.findByProcessingDateBetween(startDate, endDate, pageable);
        } else if (currency != null) {
            return report110Repository.findBySettlementCurrency(currency, pageable);
        } else {
            return report110Repository.findAll(pageable);
        }
    }

    public List<String> getAvailableCurrencies() {
        return report110Repository.findDistinctSettlementCurrencies();
    }

    public Map<String, Object> getFileSummary(String fileName) {
        Map<String, Object> summary = new HashMap<>();
        
        long vss110Count = report110Repository.findBySourceFileName(fileName, Pageable.unpaged()).getTotalElements();
        long vss120Count = report120Repository.findBySourceFileName(fileName, Pageable.unpaged()).getTotalElements();
        
        summary.put("fileName", fileName);
        summary.put("vss110Records", vss110Count);
        summary.put("vss120Records", vss120Count);
        summary.put("totalRecords", vss110Count + vss120Count);
        
        return summary;
    }

    /**
     * Extract settlement currency from header lines.
     */
    private String extractSettlementCurrency(List<String> lines) {
        log.info("=== EXTRACTING SETTLEMENT CURRENCY FROM {} LINES ===", lines.size());
        
        // Log all lines to see what we're working with
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String trimmed = line.trim();
            log.info("Settlement Line {}: '{}'", i, trimmed);
            
            if (trimmed.startsWith("SETTLEMENT CURRENCY:")) {
                String currencyPart = trimmed.substring("SETTLEMENT CURRENCY:".length()).trim();
                log.warn("*** FOUND SETTLEMENT CURRENCY LINE ***, currency part: '{}'", currencyPart);
                if (!currencyPart.isEmpty() && currencyPart.length() >= 3) {
                    String currency = currencyPart.substring(0, 3);
                    log.error("*** RETURNING SETTLEMENT CURRENCY: {} ***", currency);
                    return currency;
                }
            }
        }
        log.error("*** NO SETTLEMENT CURRENCY FOUND IN {} LINES ***", lines.size());
        return null;
    }

    /**
     * Extract clearing currency from header lines.
     */
    private String extractClearingCurrency(List<String> lines) {
        log.info("=== EXTRACTING CLEARING CURRENCY FROM {} LINES ===", lines.size());
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("CLEARING CURRENCY:")) {
                String currencyPart = trimmed.substring("CLEARING CURRENCY:".length()).trim();
                log.warn("*** FOUND CLEARING CURRENCY LINE ***, currency part: '{}'", currencyPart);
                if (!currencyPart.isEmpty() && currencyPart.length() >= 3) {
                    String currency = currencyPart.substring(0, 3);
                    log.error("*** RETURNING CLEARING CURRENCY: {} ***", currency);
                    return currency;
                } else if (currencyPart.isEmpty()) {
                    log.error("*** FOUND EMPTY CLEARING CURRENCY ***");
                }
            }
        }
        log.error("*** NO CLEARING CURRENCY FOUND IN {} LINES ***", lines.size());
        return null;
    }
} 