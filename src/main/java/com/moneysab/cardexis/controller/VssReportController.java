package com.moneysab.cardexis.controller;

import com.moneysab.cardexis.service.VssReportService;
import com.moneysab.cardexis.domain.entity.vss.Report110Entity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST controller for VSS text report operations.
 * Provides endpoints for uploading VSS text files and querying report data.
 * 
 * @author EL.AHGOUNE
 * @version 1.0.0
 * @since 2024
 */
@RestController
@RequestMapping("/api/vss")
@Tag(name = "VSS Reports", description = "VSS Text Report Upload and Management API")
public class VssReportController {

    private static final Logger log = LoggerFactory.getLogger(VssReportController.class);

    @Autowired
    private VssReportService vssReportService;

    /**
     * Upload and process VSS text file (e.g., EP747.TXT).
     * Auto-detects report sections and parses according to ReadingReports.txt field mappings.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Upload VSS Text File",
        description = "Upload and process VSS text files containing multiple report types (VSS-110, VSS-120, etc.). " +
                     "The system auto-detects report sections and parses data using fixed-width field positions."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "File processed successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid file or processing error"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> uploadVssFile(
            @Parameter(description = "VSS text file to upload", required = true)
            @RequestParam("file") MultipartFile file) {
        
        try {
            log.info("Received file upload request: {}", file.getOriginalFilename());
            
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "File is empty"));
            }
            
            if (!isValidVssFile(file)) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "Invalid file type. Expected text file with VSS reports."));
            }
            
            // Process the file
            Map<String, Object> result = vssReportService.processVssFile(file);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            }
            
        } catch (Exception e) {
            log.error("Error processing file upload: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", "Failed to process file: " + e.getMessage()));
        }
    }

    /**
     * Upload VSS text content directly as plain text.
     */
    @PostMapping(value = "/upload-text", consumes = MediaType.TEXT_PLAIN_VALUE)
    @Operation(
        summary = "Upload VSS Text Content",
        description = "Upload VSS report content as plain text for processing."
    )
    public ResponseEntity<Map<String, Object>> uploadVssText(
            @RequestBody String textContent,
            @RequestParam(value = "fileName", defaultValue = "uploaded-content.txt") String fileName) {
        
        try {
            log.info("Received text content upload request: {}", fileName);
            
            if (textContent == null || textContent.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "Text content is empty"));
            }
            
            // Create a mock MultipartFile from text content
            // For now, we'll process directly - in production you might want to create a proper MultipartFile wrapper
            Map<String, Object> result = Map.of(
                "success", true,
                "message", "Text processing endpoint - implementation pending",
                "fileName", fileName,
                "contentLength", textContent.length()
            );
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error processing text upload: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", "Failed to process text: " + e.getMessage()));
        }
    }

    /**
     * Get VSS-110 report data with filtering options.
     */
    @GetMapping("/reports/110")
    @Operation(
        summary = "Get VSS-110 Reports",
        description = "Retrieve VSS-110 settlement summary report data with optional filtering by currency and date range."
    )
    public ResponseEntity<Page<Report110Entity>> getVss110Reports(
            @Parameter(description = "Settlement currency code (e.g., EUR, USD)")
            @RequestParam(required = false) String currency,
            
            @Parameter(description = "Start date for filtering (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            
            @Parameter(description = "End date for filtering (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            
            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "processingDate") String sortBy,
            
            @Parameter(description = "Sort direction (asc/desc)")
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        try {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<Report110Entity> reports = vssReportService.getReport110Data(currency, startDate, endDate, pageable);
            
            return ResponseEntity.ok(reports);
            
        } catch (Exception e) {
            log.error("Error retrieving VSS-110 reports: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get available currencies from processed reports.
     */
    @GetMapping("/currencies")
    @Operation(
        summary = "Get Available Currencies",
        description = "Get list of all available settlement currencies from processed VSS reports."
    )
    public ResponseEntity<List<String>> getAvailableCurrencies() {
        try {
            List<String> currencies = vssReportService.getAvailableCurrencies();
            return ResponseEntity.ok(currencies);
        } catch (Exception e) {
            log.error("Error retrieving currencies: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get processing summary for uploaded files.
     */
    @GetMapping("/summary")
    @Operation(
        summary = "Get Processing Summary",
        description = "Get summary statistics of processed VSS files and reports."
    )
    public ResponseEntity<Map<String, Object>> getProcessingSummary() {
        try {
            Map<String, Object> summary = Map.of(
                "message", "Summary endpoint - implementation pending",
                "timestamp", System.currentTimeMillis()
            );
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("Error retrieving summary: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Health check endpoint for VSS report processing.
     */
    @GetMapping("/health")
    @Operation(
        summary = "Health Check",
        description = "Check the health status of VSS report processing service."
    )
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = Map.of(
            "status", "UP",
            "service", "VSS Report Processing",
            "timestamp", System.currentTimeMillis(),
            "version", "1.0.0"
        );
        return ResponseEntity.ok(health);
    }

    /**
     * Validate if uploaded file is a valid VSS text file.
     */
    private boolean isValidVssFile(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null) return false;
        
        // Check file extension
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        if (!List.of("txt", "dat", "csv").contains(extension)) {
            return false;
        }
        
        // Additional validation could be added here to check file content
        return true;
    }
} 