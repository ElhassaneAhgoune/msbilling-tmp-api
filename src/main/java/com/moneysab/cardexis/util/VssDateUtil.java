package com.moneysab.cardexis.util;

import com.moneysab.cardexis.exception.VssParsingException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.regex.Pattern;

/**
 * Enhanced utility class for parsing and formatting dates in VSS (Visa Settlement Service) files.
 *
 * VSS files use multiple date formats:
 * - CCYYDDD: 7-digit format (CC=Century, YY=Year, DDD=Day of year)
 * - CCYDDD: 6-digit format (CC=Century, Y=Year digit, DDD=Day of year) - used for Funds Transfer Date
 *
 * Examples:
 * - 2024001 = January 1, 2024 (CCYYDDD - day 1 of 2024)
 * - 202401 = January 1, 2024 (CCYDDD - day 1 of 2024, abbreviated year)
 * - 2024365 = December 30, 2024 (day 365 of 2024, non-leap year)
 *
 * @author EL.AHGOUNE
 * @version 2.0.0
 * @since 2024
 */
public final class VssDateUtil {

    /**
     * Pattern for validating CCYYDDD date format (7 digits).
     */
    private static final Pattern CCYYDDD_PATTERN = Pattern.compile("^\\d{7}$");

    /**
     * Pattern for validating CCYDDD date format (6 digits).
     */
    private static final Pattern CCYDDD_PATTERN = Pattern.compile("^\\d{6}$");

    /**
     * Pattern for validating YYDDD date format (5 digits).
     */
    private static final Pattern YYDDD_PATTERN = Pattern.compile("^\\d{5}$");

    /**
     * Formatter for CCYYDDD date format (7 digits).
     */
    private static final DateTimeFormatter CCYYDDD_FORMATTER = DateTimeFormatter.ofPattern("yyyyDDD");

    /**
     * Minimum valid year for VSS dates.
     */
    private static final int MIN_YEAR = 1900;

    /**
     * Maximum valid year for VSS dates.
     */
    private static final int MAX_YEAR = 2100;

    /**
     * Default year to use when parsing fails.
     */
    private static final int DEFAULT_YEAR = 2024;

    /**
     * Default day of year to use when parsing fails.
     */
    private static final int DEFAULT_DAY_OF_YEAR = 1;

    /**
     * Private constructor to prevent instantiation.
     */
    private VssDateUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Parses a date string in CCYYDDD format (7 digits) to LocalDate.
     *
     * @param dateStr the date string in CCYYDDD format (e.g., "2024158")
     * @return the parsed LocalDate
     * @throws VssParsingException if the date string is invalid or cannot be parsed
     */
    public static LocalDate parseCCYYDDD(String dateStr) {
        return parseCCYYDDD(dateStr, -1, null);
    }

    /**
     * Parses a date string in CCYYDDD format (7 digits) to LocalDate with context information.
     *
     * @param dateStr the date string in CCYYDDD format (e.g., "2024158")
     * @param lineNumber the line number for error reporting
     * @param recordType the record type for error reporting
     * @return the parsed LocalDate
     * @throws VssParsingException if the date string is invalid or cannot be parsed
     */
    public static LocalDate parseCCYYDDD(String dateStr, int lineNumber, String recordType) {
        if (dateStr == null) {
            throw VssParsingException.missingRequiredField("settlement_date", lineNumber, recordType);
        }

        String trimmedDate = dateStr.trim();

        // Handle both full CCYYDDD (7 digits) and truncated YYDDD (5 digits) formats
        if (trimmedDate.length() == 5) {
            // Assume 20xx century for 5-digit dates (YYDDD format)
            trimmedDate = "20" + trimmedDate;
        }

        // Validate format
        if (!CCYYDDD_PATTERN.matcher(trimmedDate).matches()) {
            throw VssParsingException.invalidDateValue("settlement_date", dateStr, "CCYYDDD or YYDDD", lineNumber, recordType, null);
        }

        try {
            // Extract year and day of year
            int year = Integer.parseInt(trimmedDate.substring(0, 4));
            int dayOfYear = Integer.parseInt(trimmedDate.substring(4, 7));

            return parseAndValidateDate(year, dayOfYear, dateStr, lineNumber, recordType, "settlement_date");

        } catch (NumberFormatException e) {
            throw VssParsingException.invalidDateValue("settlement_date", dateStr, "CCYYDDD", lineNumber, recordType, e);
        } catch (DateTimeParseException e) {
            throw VssParsingException.invalidDateValue("settlement_date", dateStr, "CCYYDDD", lineNumber, recordType, e);
        }
    }

    /**
     * Parses a date string in CCYDDD format (6 digits) to LocalDate.
     * This format is used for Funds Transfer Date field.
     *
     * @param dateStr the date string in CCYDDD format (e.g., "202401" for day 1 of 2024)
     * @return the parsed LocalDate
     * @throws VssParsingException if the date string is invalid or cannot be parsed
     */
    public static LocalDate parseCCYDDD(String dateStr) {
        return parseCCYDDD(dateStr, -1, null);
    }

    /**
     * Parses a date string in CCYDDD format (6 digits) to LocalDate with context information.
     * This format is used for Funds Transfer Date field.
     *
     * @param dateStr the date string in CCYDDD format (e.g., "202401")
     * @param lineNumber the line number for error reporting
     * @param recordType the record type for error reporting
     * @return the parsed LocalDate
     * @throws VssParsingException if the date string is invalid or cannot be parsed
     */
    public static LocalDate parseCCYDDD(String dateStr, int lineNumber, String recordType) {
        if (dateStr == null) {
            throw VssParsingException.missingRequiredField("funds_transfer_date", lineNumber, recordType);
        }

        String trimmedDate = dateStr.trim();

        // Validate format - should be exactly 6 digits
        if (!CCYDDD_PATTERN.matcher(trimmedDate).matches()) {
            throw VssParsingException.invalidDateValue("funds_transfer_date", dateStr, "CCYDDD (6 digits)", lineNumber, recordType, null);
        }

        try {
            // Extract century, year digit, and day of year
            // Format: CCYDDD where CC=century, Y=year digit, DDD=day of year
            int century = Integer.parseInt(trimmedDate.substring(0, 2));
            int yearDigit = Integer.parseInt(trimmedDate.substring(2, 3));
            int dayOfYear = Integer.parseInt(trimmedDate.substring(3, 6));

            // Construct full year: century * 100 + decade (0) + year digit
            // For example: 20 + 0 + 4 = 2004, or 20 + 1 + 0 = 2010
            // This assumes the decade is 0 for simplicity, but could be enhanced
            int year = century * 100 + yearDigit;

            // Handle special cases for year construction
            if (year < 2000) {
                // If calculated year is less than 2000, assume 20xx century
                year = 2000 + yearDigit;
            }

            return parseAndValidateDate(year, dayOfYear, dateStr, lineNumber, recordType, "funds_transfer_date");

        } catch (NumberFormatException e) {
            throw VssParsingException.invalidDateValue("funds_transfer_date", dateStr, "CCYDDD", lineNumber, recordType, e);
        } catch (DateTimeParseException e) {
            throw VssParsingException.invalidDateValue("funds_transfer_date", dateStr, "CCYDDD", lineNumber, recordType, e);
        }
    }

    /**
     * Common validation logic for parsed dates.
     */
    private static LocalDate parseAndValidateDate(int year, int dayOfYear, String originalDateStr,
                                                  int lineNumber, String recordType, String fieldName) {
        // Validate year range
        if (year < MIN_YEAR || year > MAX_YEAR) {
            throw VssParsingException.invalidDateValue(fieldName, originalDateStr,
                    String.format("Year must be between %d and %d", MIN_YEAR, MAX_YEAR),
                    lineNumber, recordType, null);
        }

        // Validate day of year range
        if (dayOfYear < 1 || dayOfYear > 366) {
            throw VssParsingException.invalidDateValue(fieldName, originalDateStr,
                    "Day of year must be between 001 and 366",
                    lineNumber, recordType, null);
        }

        // Create LocalDate using year and day of year
        LocalDate date = LocalDate.ofYearDay(year, dayOfYear);

        // Additional validation: check if day of year is valid for the specific year
        int maxDayOfYear = date.isLeapYear() ? 366 : 365;
        if (dayOfYear > maxDayOfYear) {
            throw VssParsingException.invalidDateValue(fieldName, originalDateStr,
                    String.format("Day %d is invalid for year %d (max: %d)", dayOfYear, year, maxDayOfYear),
                    lineNumber, recordType, null);
        }

        return date;
    }

    /**
     * Parses a date string in CCYYDDD format to LocalDate with lenient parsing.
     * Returns a default date if parsing fails instead of throwing an exception.
     *
     * @param dateStr the date string in CCYYDDD format
     * @return the parsed LocalDate, or a default date if parsing fails
     */
    public static LocalDate parseCCYYDDDLenient(String dateStr) {
        try {
            return parseCCYYDDD(dateStr);
        } catch (VssParsingException e) {
            // Return default date instead of throwing exception
            return LocalDate.of(DEFAULT_YEAR, 1, DEFAULT_DAY_OF_YEAR);
        }
    }

    /**
     * Parses a date string in CCYDDD format to LocalDate with lenient parsing.
     * Returns a default date if parsing fails instead of throwing an exception.
     *
     * @param dateStr the date string in CCYDDD format
     * @return the parsed LocalDate, or a default date if parsing fails
     */
    public static LocalDate parseCCYDDDLenient(String dateStr) {
        try {
            return parseCCYDDD(dateStr);
        } catch (VssParsingException e) {
            // Return default date instead of throwing exception
            return LocalDate.of(DEFAULT_YEAR, 1, DEFAULT_DAY_OF_YEAR);
        }
    }

    /**
     * Formats a LocalDate to CCYYDDD format (7 digits).
     *
     * @param date the LocalDate to format
     * @return the formatted date string in CCYYDDD format
     * @throws IllegalArgumentException if the date is null
     */
    public static String formatToCCYYDDD(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }

        return date.format(CCYYDDD_FORMATTER);
    }

    /**
     * Formats a LocalDate to CCYDDD format (6 digits).
     * This format is used for Funds Transfer Date field.
     *
     * @param date the LocalDate to format
     * @return the formatted date string in CCYDDD format
     * @throws IllegalArgumentException if the date is null
     */
    public static String formatToCCYDDD(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }

        int year = date.getYear();
        int dayOfYear = date.getDayOfYear();
        int century = year / 100;
        int yearDigit = year % 10; // Get last digit of year

        return String.format("%02d%d%03d", century, yearDigit, dayOfYear);
    }

    /**
     * Validates if a date string is in valid CCYYDDD format (7 digits).
     *
     * @param dateStr the date string to validate
     * @return true if the date string is valid, false otherwise
     */
    public static boolean isValidCCYYDDD(String dateStr) {
        try {
            parseCCYYDDD(dateStr);
            return true;
        } catch (VssParsingException e) {
            return false;
        }
    }

    /**
     * Validates if a date string is in valid CCYDDD format (6 digits).
     *
     * @param dateStr the date string to validate
     * @return true if the date string is valid, false otherwise
     */
    public static boolean isValidCCYDDD(String dateStr) {
        try {
            parseCCYDDD(dateStr);
            return true;
        } catch (VssParsingException e) {
            return false;
        }
    }

    /**
     * Extracts the year from a CCYYDDD date string (7 digits).
     *
     * @param dateStr the date string in CCYYDDD format
     * @return the year, or -1 if extraction fails
     */
    public static int extractYear(String dateStr) {
        if (dateStr == null || dateStr.length() < 4) {
            return -1;
        }

        try {
            return Integer.parseInt(dateStr.substring(0, 4));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Extracts the year from a CCYDDD date string (6 digits).
     *
     * @param dateStr the date string in CCYDDD format
     * @return the year, or -1 if extraction fails
     */
    public static int extractYearFromCCYDDD(String dateStr) {
        if (dateStr == null || dateStr.length() != 6) {
            return -1;
        }

        try {
            int century = Integer.parseInt(dateStr.substring(0, 2));
            int yearDigit = Integer.parseInt(dateStr.substring(2, 3));
            int year = century * 100 + yearDigit;

            // Handle special cases
            if (year < 2000) {
                year = 2000 + yearDigit;
            }

            return year;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Extracts the day of year from a CCYYDDD date string (7 digits).
     *
     * @param dateStr the date string in CCYYDDD format
     * @return the day of year, or -1 if extraction fails
     */
    public static int extractDayOfYear(String dateStr) {
        if (dateStr == null || dateStr.length() < 7) {
            return -1;
        }

        try {
            return Integer.parseInt(dateStr.substring(4, 7));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Extracts the day of year from a CCYDDD date string (6 digits).
     *
     * @param dateStr the date string in CCYDDD format
     * @return the day of year, or -1 if extraction fails
     */
    public static int extractDayOfYearFromCCYDDD(String dateStr) {
        if (dateStr == null || dateStr.length() != 6) {
            return -1;
        }

        try {
            return Integer.parseInt(dateStr.substring(3, 6));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Checks if a year is a leap year.
     *
     * @param year the year to check
     * @return true if the year is a leap year, false otherwise
     */
    public static boolean isLeapYear(int year) {
        return LocalDate.of(year, 1, 1).isLeapYear();
    }

    /**
     * Gets the maximum day of year for a given year.
     *
     * @param year the year
     * @return 366 for leap years, 365 for non-leap years
     */
    public static int getMaxDayOfYear(int year) {
        return isLeapYear(year) ? 366 : 365;
    }

    /**
     * Validates if a day of year is valid for a given year.
     *
     * @param year the year
     * @param dayOfYear the day of year to validate
     * @return true if the day of year is valid for the year, false otherwise
     */
    public static boolean isValidDayOfYear(int year, int dayOfYear) {
        if (dayOfYear < 1) {
            return false;
        }

        int maxDay = getMaxDayOfYear(year);
        return dayOfYear <= maxDay;
    }

    /**
     * Converts a LocalDate to day of year.
     *
     * @param date the LocalDate
     * @return the day of year (1-366)
     * @throws IllegalArgumentException if the date is null
     */
    public static int toDayOfYear(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }

        return date.get(ChronoField.DAY_OF_YEAR);
    }

    /**
     * Creates a LocalDate from year and day of year.
     *
     * @param year the year
     * @param dayOfYear the day of year (1-366)
     * @return the LocalDate
     * @throws IllegalArgumentException if the parameters are invalid
     */
    public static LocalDate fromYearAndDayOfYear(int year, int dayOfYear) {
        if (year < MIN_YEAR || year > MAX_YEAR) {
            throw new IllegalArgumentException(String.format("Year must be between %d and %d", MIN_YEAR, MAX_YEAR));
        }

        if (!isValidDayOfYear(year, dayOfYear)) {
            throw new IllegalArgumentException(String.format("Day of year %d is invalid for year %d", dayOfYear, year));
        }

        return LocalDate.ofYearDay(year, dayOfYear);
    }

    /**
     * Checks if a date falls within a valid range for VSS processing.
     *
     * @param date the date to check
     * @param minDate the minimum allowed date (inclusive)
     * @param maxDate the maximum allowed date (inclusive)
     * @return true if the date is within the valid range, false otherwise
     */
    public static boolean isDateInRange(LocalDate date, LocalDate minDate, LocalDate maxDate) {
        if (date == null) {
            return false;
        }

        if (minDate != null && date.isBefore(minDate)) {
            return false;
        }

        if (maxDate != null && date.isAfter(maxDate)) {
            return false;
        }

        return true;
    }

    /**
     * Gets the current date in CCYYDDD format.
     *
     * @return the current date formatted as CCYYDDD
     */
    public static String getCurrentDateAsCCYYDDD() {
        return formatToCCYYDDD(LocalDate.now());
    }

    /**
     * Gets the current date in CCYDDD format.
     *
     * @return the current date formatted as CCYDDD
     */
    public static String getCurrentDateAsCCYDDD() {
        return formatToCCYDDD(LocalDate.now());
    }

    /**
     * Parses multiple date formats commonly found in VSS files.
     * Attempts CCYYDDD first, then CCYDDD, then falls back to other common formats.
     *
     * @param dateStr the date string to parse
     * @param lineNumber the line number for error reporting
     * @param recordType the record type for error reporting
     * @return the parsed LocalDate
     * @throws VssParsingException if none of the formats can parse the date
     */
    public static LocalDate parseFlexibleDate(String dateStr, int lineNumber, String recordType) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            throw VssParsingException.missingRequiredField("date", lineNumber, recordType);
        }

        String trimmedDate = dateStr.trim();

        // Try CCYYDDD format first (7 digits)
        if (CCYYDDD_PATTERN.matcher(trimmedDate).matches()) {
            return parseCCYYDDD(trimmedDate, lineNumber, recordType);
        }

        // Try CCYDDD format (6 digits)
        if (CCYDDD_PATTERN.matcher(trimmedDate).matches()) {
            return parseCCYDDD(trimmedDate, lineNumber, recordType);
        }

        // Try YYDDD format (5 digits)
        if (YYDDD_PATTERN.matcher(trimmedDate).matches()) {
            return parseCCYYDDD("20" + trimmedDate, lineNumber, recordType);
        }

        // Try other common formats
        DateTimeFormatter[] fallbackFormatters = {
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("yyyyMMdd"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy")
        };

        for (DateTimeFormatter formatter : fallbackFormatters) {
            try {
                return LocalDate.parse(trimmedDate, formatter);
            } catch (DateTimeParseException e) {
                // Continue to next format
            }
        }

        // If all formats fail, throw exception
        throw VssParsingException.invalidDateValue("date", dateStr,
                "CCYYDDD, CCYDDD, YYDDD, yyyy-MM-dd, yyyyMMdd, MM/dd/yyyy, or dd/MM/yyyy",
                lineNumber, recordType, null);
    }
}