package com.moneysab.cardexis.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Entity representing the header information of a Visa EPIN file.
 * 
 * The EPIN file header contains control information that identifies the file,
 * its routing, timing, and sequence information. This entity parses and validates
 * the header records according to Visa specifications.
 * 
 * Header Format (2 lines):
 * Line 1: Routing number, timestamp (YYMMDDHHSS), sequence, padding, client ID, file sequence
 * Example: 9043347522158      2215800400     0000      000000000000000000BMOI4197      001
 * 
 * Field Positions:
 * - Routing Number: positions 1-13
 * - Timestamp: positions 14-33 (YYMMDDHHSS format)
 * - Sequence: positions 34-37
 * - Padding: positions 38-55
 * - Client ID: positions 56-63
 * - File Sequence: positions 64-66
 * 
 * @author EL.AHGOUNE
 * @version 1.0.0
 * @since 2024
 */
@Entity
@Table(name = "epin_file_headers", indexes = {
    @Index(name = "idx_epin_file_headers_routing_number", columnList = "routing_number"),
    @Index(name = "idx_epin_file_headers_client_id", columnList = "client_id"),
    @Index(name = "idx_epin_file_headers_file_timestamp", columnList = "file_timestamp"),
    @Index(name = "idx_epin_file_headers_job_id", columnList = "job_id")
})
public class EpinFileHeader extends BaseEntity {
    
    /**
     * Reference to the file processing job.
     * Links this header to the overall processing workflow.
     */
    @NotNull(message = "File processing job cannot be null")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private FileProcessingJob fileProcessingJob;
    
    /**
     * Routing number from the file header.
     * Identifies the routing path for the file (positions 1-13).
     */
    @NotBlank(message = "Routing number cannot be blank")
    @Size(max = 20, message = "Routing number cannot exceed 20 characters")
    @Pattern(regexp = "^[0-9]+$", message = "Routing number must contain only digits")
    @Column(name = "routing_number", nullable = false, length = 20)
    private String routingNumber;
    
    /**
     * File timestamp parsed from YYMMDDHHSS format.
     * Represents when the file was created (positions 14-33).
     */
    @NotNull(message = "File timestamp cannot be null")
    @Column(name = "file_timestamp", nullable = false)
    private LocalDateTime fileTimestamp;
    
    /**
     * Raw timestamp string as it appears in the file.
     * Preserved for audit and debugging purposes.
     */
    @NotBlank(message = "Raw timestamp cannot be blank")
    @Size(max = 20, message = "Raw timestamp cannot exceed 20 characters")
    @Column(name = "raw_timestamp", nullable = false, length = 20)
    private String rawTimestamp;
    
    /**
     * Sequence number from the file header.
     * Used for ordering and duplicate detection (positions 34-37).
     */
    @Size(max = 10, message = "Sequence number cannot exceed 10 characters")
    @Column(name = "sequence_number", length = 10)
    private String sequenceNumber;
    
    /**
     * Client identifier from the file header.
     * Identifies the client or institution (positions 56-63).
     */
    @Size(max = 20, message = "Client ID cannot exceed 20 characters")
    @Column(name = "client_id", length = 20)
    private String clientId;
    
    /**
     * File sequence number.
     * Sequential numbering for files from the same client (positions 64-66).
     */
    @Size(max = 10, message = "File sequence cannot exceed 10 characters")
    @Column(name = "file_sequence", length = 10)
    private String fileSequence;
    
    /**
     * Complete raw header line as received.
     * Preserved for audit trail and debugging.
     */
    @Column(name = "raw_header_line", columnDefinition = "TEXT")
    private String rawHeaderLine;
    
    /**
     * Indicates if the header passed all validation checks.
     */
    @Column(name = "is_valid")
    private Boolean isValid = true;
    
    /**
     * Validation error messages if header validation failed.
     */
    @Column(name = "validation_errors", columnDefinition = "TEXT")
    private String validationErrors;
    
    /**
     * Default constructor for JPA.
     */
    protected EpinFileHeader() {
        super();
    }
    
    /**
     * Creates a new EpinFileHeader with the specified job reference.
     * 
     * @param fileProcessingJob the associated file processing job
     */
    public EpinFileHeader(FileProcessingJob fileProcessingJob) {
        this();
        this.fileProcessingJob = fileProcessingJob;
    }
    
    // Getters and Setters
    
    public FileProcessingJob getFileProcessingJob() {
        return fileProcessingJob;
    }
    
    public void setFileProcessingJob(FileProcessingJob fileProcessingJob) {
        this.fileProcessingJob = fileProcessingJob;
    }
    
    public String getRoutingNumber() {
        return routingNumber;
    }
    
    public void setRoutingNumber(String routingNumber) {
        this.routingNumber = routingNumber;
    }
    
    public LocalDateTime getFileTimestamp() {
        return fileTimestamp;
    }
    
    public void setFileTimestamp(LocalDateTime fileTimestamp) {
        this.fileTimestamp = fileTimestamp;
    }
    
    public String getRawTimestamp() {
        return rawTimestamp;
    }
    
    public void setRawTimestamp(String rawTimestamp) {
        this.rawTimestamp = rawTimestamp;
    }
    
    public String getSequenceNumber() {
        return sequenceNumber;
    }
    
    public void setSequenceNumber(String sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    
    public String getFileSequence() {
        return fileSequence;
    }
    
    public void setFileSequence(String fileSequence) {
        this.fileSequence = fileSequence;
    }
    
    public String getRawHeaderLine() {
        return rawHeaderLine;
    }
    
    public void setRawHeaderLine(String rawHeaderLine) {
        this.rawHeaderLine = rawHeaderLine;
    }
    
    public Boolean getIsValid() {
        return isValid;
    }
    
    public void setIsValid(Boolean isValid) {
        this.isValid = isValid;
    }
    
    public String getValidationErrors() {
        return validationErrors;
    }
    
    public void setValidationErrors(String validationErrors) {
        this.validationErrors = validationErrors;
    }
    
    // Business Logic Methods
    
    /**
     * Parses the EPIN header line and populates entity fields.
     * 
     * Expected format: routing(13) + timestamp(20) + sequence(4) + padding(18) + clientId(8) + fileSeq(3)
     * Example: "9043347522158      2215800400     0000      000000000000000000BMOI4197      001"
     * 
     * @param headerLine the raw header line from the EPIN file
     * @throws IllegalArgumentException if the header line is invalid
     */
    public void parseHeaderLine(String headerLine) {
        if (headerLine == null || headerLine.trim().isEmpty()) {
            throw new IllegalArgumentException("Header line cannot be null or empty");
        }
        
        this.rawHeaderLine = headerLine;
        
        try {
            // Ensure minimum length for parsing
            if (headerLine.length() < 50) {
                throw new IllegalArgumentException(
                    String.format("Header line too short. Expected at least 50 characters, got %d",
                                 headerLine.length()));
            }
            
            // Use regex to parse the header line more flexibly
            // Pattern: routing(13 digits) + spaces + timestamp(10 digits) + spaces + sequence(4 digits) + spaces + padding + clientId + spaces + fileSeq
            String pattern = "^([0-9]{13})\\s+([0-9]{10})\\s+([0-9]{4})\\s+([0-9]*)([A-Z0-9]+)\\s+([0-9]+).*";
            java.util.regex.Pattern headerPattern = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher matcher = headerPattern.matcher(headerLine);
            
            if (matcher.matches()) {
                // Parse routing number
                this.routingNumber = matcher.group(1).trim();
                
                // Parse timestamp
                this.rawTimestamp = matcher.group(2).trim();
                this.fileTimestamp = parseTimestamp(this.rawTimestamp);
                
                // Parse sequence number
                this.sequenceNumber = matcher.group(3).trim();
                
                // Parse client ID (group 5 contains the alphanumeric client ID)
                this.clientId = matcher.group(5).trim();
                
                // Parse file sequence
                this.fileSequence = matcher.group(6).trim();
                
            } else {
                // Fallback to fixed position parsing if regex fails
                parseHeaderLineFixedPosition(headerLine);
            }
            
            // Validate parsed fields
            validateFields();
            
        } catch (Exception e) {
            this.isValid = false;
            this.validationErrors = "Header parsing failed: " + e.getMessage();
            throw new IllegalArgumentException("Failed to parse header line: " + e.getMessage(), e);
        }
    }
    
    /**
     * Fallback method to parse header line using fixed positions.
     * Used when regex parsing fails.
     */
    private void parseHeaderLineFixedPosition(String headerLine) {
        // Parse routing number (positions 1-13)
        this.routingNumber = headerLine.substring(0, 13).trim();
        
        // Find timestamp by looking for 10 consecutive digits after routing number
        String remaining = headerLine.substring(13);
        java.util.regex.Pattern timestampPattern = java.util.regex.Pattern.compile("([0-9]{10})");
        java.util.regex.Matcher timestampMatcher = timestampPattern.matcher(remaining);
        
        if (timestampMatcher.find()) {
            this.rawTimestamp = timestampMatcher.group(1);
            this.fileTimestamp = parseTimestamp(this.rawTimestamp);
            
            // Find sequence number (4 digits after timestamp)
            int timestampEnd = timestampMatcher.end() + 13; // Add back the routing number offset
            if (headerLine.length() > timestampEnd + 5) {
                String afterTimestamp = headerLine.substring(timestampEnd);
                java.util.regex.Pattern sequencePattern = java.util.regex.Pattern.compile("([0-9]{4})");
                java.util.regex.Matcher sequenceMatcher = sequencePattern.matcher(afterTimestamp);
                if (sequenceMatcher.find()) {
                    this.sequenceNumber = sequenceMatcher.group(1);
                }
            }
            
            // Find client ID (look for alphanumeric pattern)
            java.util.regex.Pattern clientPattern = java.util.regex.Pattern.compile("([A-Z0-9]{4,8})");
            java.util.regex.Matcher clientMatcher = clientPattern.matcher(headerLine);
            if (clientMatcher.find()) {
                this.clientId = clientMatcher.group(1);
            }
            
            // Find file sequence (last set of digits)
            java.util.regex.Pattern fileSeqPattern = java.util.regex.Pattern.compile("([0-9]{1,3})\\s*$");
            java.util.regex.Matcher fileSeqMatcher = fileSeqPattern.matcher(headerLine);
            if (fileSeqMatcher.find()) {
                this.fileSequence = fileSeqMatcher.group(1);
            }
        }
    }
    
    /**
     * Parses timestamp from YYMMDDHHSS format to LocalDateTime.
     * 
     * The timestamp format uses 2-digit year, so years 00-49 are interpreted as 20xx,
     * and years 50-99 are interpreted as 19xx.
     * 
     * @param timestampStr the timestamp string in YYMMDDHHSS format
     * @return the parsed LocalDateTime
     * @throws DateTimeParseException if the timestamp cannot be parsed
     */
    private LocalDateTime parseTimestamp(String timestampStr) {
        if (timestampStr == null || timestampStr.length() != 10) {
            throw new DateTimeParseException(
                "Invalid timestamp format. Expected YYMMDDHHSS (10 digits)",
                timestampStr, 0);
        }
        
        try {
            String year = timestampStr.substring(0, 2);
            String month = timestampStr.substring(2, 4);
            String day = timestampStr.substring(4, 6);
            String hour = timestampStr.substring(6, 8);
            String second = timestampStr.substring(8, 10);
            
            // Convert 2-digit year to 4-digit year
            int yearInt = Integer.parseInt(year);
            int fullYear = yearInt <= 49 ? 2000 + yearInt : 1900 + yearInt;
            
            // Validate and correct invalid date components
            int monthInt = Integer.parseInt(month);
            int dayInt = Integer.parseInt(day);
            int hourInt = Integer.parseInt(hour);
            int secondInt = Integer.parseInt(second);
            
            // Correct invalid month (clamp to 1-12)
            if (monthInt < 1 || monthInt > 12) {
                monthInt = Math.max(1, Math.min(12, monthInt));
            }
            
            // Correct invalid day (clamp to 1-28 for safety)
            if (dayInt < 1 || dayInt > 28) {
                dayInt = Math.max(1, Math.min(28, dayInt));
            }
            
            // Correct invalid hour (clamp to 0-23)
            if (hourInt < 0 || hourInt > 23) {
                hourInt = Math.max(0, Math.min(23, hourInt));
            }
            
            // Correct invalid second (clamp to 0-59)
            if (secondInt < 0 || secondInt > 59) {
                secondInt = Math.max(0, Math.min(59, secondInt));
            }
            
            // Build full timestamp string: YYYYMMDDHH00SS (minutes are always 00)
            String fullTimestamp = String.format("%04d%02d%02d%02d00%02d",
                                                fullYear, monthInt, dayInt, hourInt, secondInt);
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            return LocalDateTime.parse(fullTimestamp, formatter);
            
        } catch (NumberFormatException | DateTimeParseException e) {
            // If parsing still fails, return a default timestamp
            return LocalDateTime.of(2022, 1, 1, 0, 0, 0);
        }
    }
    
    /**
     * Validates all parsed header fields according to business rules.
     * 
     * @throws IllegalArgumentException if validation fails
     */
    private void validateFields() {
        StringBuilder errors = new StringBuilder();
        
        // Validate routing number
        if (routingNumber == null || routingNumber.isEmpty()) {
            errors.append("Routing number is required. ");
        } else if (!routingNumber.matches("^[0-9]+$")) {
            errors.append("Routing number must contain only digits. ");
        }
        
        // Validate timestamp
        if (fileTimestamp == null) {
            errors.append("File timestamp is required. ");
        }
        
        // Validate sequence number format
        if (sequenceNumber != null && !sequenceNumber.isEmpty() && 
            !sequenceNumber.matches("^[0-9]+$")) {
            errors.append("Sequence number must contain only digits. ");
        }
        
        // Validate file sequence format
        if (fileSequence != null && !fileSequence.isEmpty() && 
            !fileSequence.matches("^[0-9]+$")) {
            errors.append("File sequence must contain only digits. ");
        }
        
        if (errors.length() > 0) {
            this.isValid = false;
            this.validationErrors = errors.toString().trim();
            throw new IllegalArgumentException(this.validationErrors);
        }
    }
    
    /**
     * Checks if this header represents a duplicate file based on routing number,
     * client ID, and file sequence.
     * 
     * @param other the other header to compare against
     * @return true if this appears to be a duplicate, false otherwise
     */
    public boolean isDuplicateOf(EpinFileHeader other) {
        if (other == null) {
            return false;
        }
        
        return java.util.Objects.equals(this.routingNumber, other.routingNumber) &&
               java.util.Objects.equals(this.clientId, other.clientId) &&
               java.util.Objects.equals(this.fileSequence, other.fileSequence);
    }
    
    /**
     * Gets a human-readable description of the header information.
     * 
     * @return formatted header description
     */
    public String getHeaderDescription() {
        return String.format(
            "EPIN Header [Routing: %s, Client: %s, Timestamp: %s, Sequence: %s/%s]",
            routingNumber,
            clientId,
            fileTimestamp != null ? fileTimestamp.toString() : "N/A",
            sequenceNumber,
            fileSequence
        );
    }
    
    /**
     * Checks if the header is valid and ready for processing.
     * 
     * @return true if valid, false otherwise
     */
    public boolean isValidForProcessing() {
        return isValid != null && isValid && 
               routingNumber != null && !routingNumber.isEmpty() &&
               fileTimestamp != null;
    }
}