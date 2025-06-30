package com.moneysab.cardexis.util;

import com.moneysab.cardexis.config.VssFieldMappingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Utility for extracting fields from VSS report lines using configuration-based field positions.
 * 
 * @author EL.AHGOUNE
 * @version 1.0.0
 * @since 2024
 */
@Component
public class VssFieldExtractor {

    private static final Logger log = LoggerFactory.getLogger(VssFieldExtractor.class);

    /**
     * Extract string field based on position configuration.
     */
    public String extractString(String line, VssFieldMappingConfig.FieldPosition fieldPosition) {
        if (line == null || fieldPosition == null) {
            return null;
        }

        try {
            if (fieldPosition.getDirection() == VssFieldMappingConfig.ReadDirection.LEFT_TO_RIGHT) {
                return extractLeftToRight(line, fieldPosition.getPosition(), fieldPosition.getMaxLength());
            } else {
                return extractRightToLeft(line, fieldPosition.getPosition(), fieldPosition.getMaxLength());
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extract currency code (3 characters) from specified position.
     */
    public String extractCurrency(String line, VssFieldMappingConfig.FieldPosition fieldPosition) {
        String currency = extractString(line, fieldPosition);
        if (currency == null || currency.trim().isEmpty()) {
            return null;
        }
        currency = currency.trim();
        return currency.length() == 3 ? currency : null;
    }

    /**
     * Extract numeric count from specified position.
     */
    public Long extractCount(String line, VssFieldMappingConfig.FieldPosition fieldPosition) {
        String countStr = extractString(line, fieldPosition);
        if (countStr == null || countStr.trim().isEmpty()) {
            return 0L;
        }

        try {
            String cleanCount = countStr.trim().replaceAll(",", "");
            if (cleanCount.isEmpty() || cleanCount.equals("0")) {
                return 0L;
            }
            return Long.parseLong(cleanCount);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Extract decimal amount with CR/DB handling from specified position.
     */
    public BigDecimal extractAmount(String line, VssFieldMappingConfig.FieldPosition fieldPosition) {
        String amountStr = extractString(line, fieldPosition);
        if (amountStr == null || amountStr.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        String originalAmount = amountStr;
        try {
            amountStr = amountStr.trim();
            
            // Handle CR/DB indicators first (before removing commas)
            boolean isCredit = amountStr.endsWith("CR");
            boolean isDebit = amountStr.endsWith("DB");
            
            if (isCredit || isDebit) {
                amountStr = amountStr.substring(0, amountStr.length() - 2).trim();
            }
            
            // Remove commas after handling CR/DB to avoid index issues
            amountStr = amountStr.replaceAll(",", "");
            
            if (amountStr.isEmpty() || amountStr.equals("0.00") || amountStr.equals("0")) {
                return BigDecimal.ZERO;
            }

            BigDecimal amount = new BigDecimal(amountStr);
            BigDecimal result = isDebit ? amount.negate() : amount;
            
            // Enhanced logging for CR/DB handling
            if (isCredit || isDebit) {
                log.debug("Amount extraction: '{}' -> {} {} = {}", 
                    originalAmount.trim(), amountStr, (isCredit ? "CR" : "DB"), result);
            }
            
            return result;

        } catch (Exception e) {
            log.warn("Failed to parse amount '{}' from position {}: {}", 
                originalAmount.trim(), fieldPosition.getPosition(), e.getMessage());
            return null;
        }
    }

    /**
     * Extract table ID or similar identifier from specified position.
     */
    public String extractTableId(String line, VssFieldMappingConfig.FieldPosition fieldPosition) {
        String tableId = extractString(line, fieldPosition);
        return tableId != null ? tableId.trim() : null;
    }

    /**
     * Extract field reading from left to right.
     */
    private String extractLeftToRight(String line, int position, int maxLength) {
        if (line.length() < position) {
            return "";
        }

        int startPos = position - 1; // Convert to 0-based index
        int endPos = Math.min(line.length(), startPos + maxLength);
        
        return line.substring(startPos, endPos);
    }

    /**
     * Extract field reading from right to left.
     */
    private String extractRightToLeft(String line, int endPosition, int maxLength) {
        if (line.length() < endPosition) {
            return "";
        }

        int endPos = endPosition; // Position is 1-based, but we want inclusive end
        int startPos = Math.max(0, endPos - maxLength);
        
        if (startPos >= line.length()) {
            return "";
        }
        
        endPos = Math.min(line.length(), endPos);
        return line.substring(startPos, endPos);
    }

    /**
     * Check if line contains financial data patterns.
     */
    public boolean hasFinancialData(String line) {
        if (line == null || line.trim().isEmpty()) {
            return false;
        }
        
        // Enhanced patterns to catch all CR/DB and decimal amount variations
        return line.matches(".*\\d+\\.\\d{2}.*") ||                    // Basic decimal: 1.23
               line.matches(".*\\d+\\.\\d{2}CR.*") ||                  // Decimal with CR: 1.23CR  
               line.matches(".*\\d+\\.\\d{2}DB.*") ||                  // Decimal with DB: 1.23DB
               line.matches(".*\\d+CR.*") ||                           // Integer with CR: 123CR
               line.matches(".*\\d+DB.*") ||                           // Integer with DB: 123DB
               line.matches(".*\\d{1,3}(,\\d{3})*\\.\\d{2}.*") ||      // Comma-separated: 1,234.56
               line.matches(".*\\d{1,3}(,\\d{3})*\\.\\d{2}CR.*") ||    // Comma-separated with CR: 1,234.56CR
               line.matches(".*\\d{1,3}(,\\d{3})*\\.\\d{2}DB.*");      // Comma-separated with DB: 1,234.56DB
    }

    /**
     * Check if line is a header or formatting line.
     */
    public boolean isHeaderOrFormatLine(String line) {
        if (line == null) return true;
        
        String trimmed = line.trim();
        return trimmed.isEmpty() || 
               trimmed.contains("VISANET SETTLEMENT SERVICE") ||
               trimmed.contains("PAGE:") ||
               trimmed.contains("REPORTING FOR:") ||
               trimmed.contains("ROLLUP TO:") ||
               trimmed.contains("FUNDS XFER ENTITY:") ||
               trimmed.contains("SETTLEMENT CURRENCY:") ||
               trimmed.contains("CLEARING CURRENCY:") ||
               trimmed.contains("---") ||
               trimmed.contains("===") ||
               (trimmed.matches("^\\s*[A-Z\\s]+[A-Z\\s]*$") && !hasFinancialData(line));
    }

    /**
     * Extract section name from line.
     */
    public String extractSectionName(String line) {
        if (line == null) return null;
        
        String trimmed = line.trim();
        if (trimmed.matches("^[A-Z\\s]+$") && trimmed.length() > 3 && trimmed.length() < 50) {
            if (trimmed.contains("INTERCHANGE") || trimmed.contains("REIMBURSEMENT") || 
                trimmed.contains("VISA CHARGES") || trimmed.contains("TOTAL")) {
                return trimmed;
            }
        }
        return null;
    }

    /**
     * Extract transaction type from line.
     */
    public String extractTransactionType(String line) {
        if (line == null) return null;
        
        String trimmed = line.trim();
        if (trimmed.equals("PURCHASE") || trimmed.equals("MANUAL CASH") || trimmed.contains("CASH")) {
            return trimmed;
        }
        return null;
    }
} 