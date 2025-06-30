package com.moneysab.cardexis.domain.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * Enumeration of supported file types in the Visa EPIN system.
 * 
 * This enum defines the various file formats that can be processed by the settlement service,
 * providing validation methods and format-specific information for each type.
 * 
 * @author EL.AHGOUNE
 * @version 1.0.0
 * @since 2024
 */
public enum FileType {
    
    /**
     * Electronic Payment Information Network (EPIN) files containing settlement data.
     * These files can contain multiple record types including VSS-110 and VSS-120.
     */
    EPIN("EPIN", "Electronic Payment Information Network", ".txt", ".dat"),
    
    /**
     * Visa Settlement Service (VSS) files containing various settlement report formats.
     * This is a broader category that encompasses VSS-110, VSS-120, and future formats.
     */
    VSS("VSS", "Visa Settlement Service", ".txt", ".dat", ".csv");
    
    private final String code;
    private final String description;
    private final String[] supportedExtensions;
    
    /**
     * Constructs a FileType enum with the specified parameters.
     * 
     * @param code the short code identifier for the file type
     * @param description human-readable description of the file type
     * @param supportedExtensions array of file extensions supported by this type
     */
    FileType(String code, String description, String... supportedExtensions) {
        this.code = code;
        this.description = description;
        this.supportedExtensions = supportedExtensions;
    }
    
    /**
     * Gets the short code identifier for this file type.
     * 
     * @return the file type code
     */
    public String getCode() {
        return code;
    }
    
    /**
     * Gets the human-readable description of this file type.
     * 
     * @return the file type description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Gets the array of supported file extensions for this type.
     * 
     * @return array of supported file extensions (including the dot)
     */
    public String[] getSupportedExtensions() {
        return supportedExtensions.clone();
    }
    
    /**
     * Validates if the given file extension is supported by this file type.
     * 
     * @param extension the file extension to validate (with or without leading dot)
     * @return true if the extension is supported, false otherwise
     */
    public boolean supportsExtension(String extension) {
        if (extension == null || extension.trim().isEmpty()) {
            return false;
        }
        
        String normalizedExtension = extension.startsWith(".") ? extension : "." + extension;
        return Arrays.stream(supportedExtensions)
                .anyMatch(ext -> ext.equalsIgnoreCase(normalizedExtension));
    }
    
    /**
     * Determines the file type based on the filename extension.
     * 
     * @param filename the name of the file to analyze
     * @return Optional containing the detected FileType, or empty if no match found
     */
    public static Optional<FileType> fromFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return Optional.empty();
        }
        
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return Optional.empty();
        }
        
        String extension = filename.substring(lastDotIndex);
        
        return Arrays.stream(values())
                .filter(fileType -> fileType.supportsExtension(extension))
                .findFirst();
    }
    
    /**
     * Finds a FileType by its code identifier.
     * 
     * @param code the code to search for (case-insensitive)
     * @return Optional containing the matching FileType, or empty if no match found
     */
    public static Optional<FileType> fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return Optional.empty();
        }
        
        return Arrays.stream(values())
                .filter(fileType -> fileType.getCode().equalsIgnoreCase(code.trim()))
                .findFirst();
    }
    
    /**
     * Checks if this file type is compatible with EPIN processing.
     * 
     * @return true if the file type can be processed as EPIN data
     */
    public boolean isEpinCompatible() {
        return this == EPIN || this == VSS;
    }
    
    /**
     * Gets a formatted string representation of supported extensions.
     * 
     * @return comma-separated list of supported extensions
     */
    public String getSupportedExtensionsAsString() {
        return String.join(", ", supportedExtensions);
    }
}