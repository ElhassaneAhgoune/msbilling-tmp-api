package com.moneysab.cardexis.controller;

import com.moneysab.cardexis.domain.entity.Vss110SettlementRecord;
import com.moneysab.cardexis.domain.entity.Vss120SettlementRecord;
import com.moneysab.cardexis.dto.report.*;
import com.moneysab.cardexis.service.EpinReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * REST controller for EPIN reporting operations.
 * 
 * This controller provides endpoints for generating various reports related to EPIN transactions,
 * including revenue reports by BIN, country/channel, issuer KPIs, and data exports.
 */
@RestController
@RequestMapping("/api/v1/epin/reports")
@Tag(name = "EPIN Reporting", description = "Operations for generating EPIN transaction reports")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class EpinReportController {

    private static final Logger logger = LoggerFactory.getLogger(EpinReportController.class);

    private final EpinReportService reportService;

    /**
     * Constructor with dependency injection.
     * 
     * @param reportService the EPIN report service
     */
    public EpinReportController(EpinReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Get revenues grouped by BIN with optional date filtering.
     * 
     * @param startDate Optional start date for filtering (inclusive)
     * @param endDate Optional end date for filtering (inclusive)
     * @param pageable Pagination information
     * @return Page of BIN revenue data
     */
    @GetMapping("/bin-revenues")
    @Operation(summary = "Get revenues by BIN", 
               description = "Retrieves total revenues grouped by BIN, optionally filtered by date range")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Report generated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Page<BinRevenueDto>> getBinRevenues(
            @Parameter(description = "Start date (ISO format yyyy-MM-dd)")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            
            @Parameter(description = "End date (ISO format yyyy-MM-dd)")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            
            @Parameter(description = "Pagination parameters")
            Pageable pageable) {
        
        try {
            logger.info("Generating BIN revenue report, date range: {} to {}", startDate, endDate);
            Page<BinRevenueDto> report = reportService.getBinRevenues(startDate, endDate, pageable);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            logger.error("Error generating BIN revenue report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get revenues grouped by country and channel with optional date filtering.
     * 
     * @param startDate Optional start date for filtering (inclusive)
     * @param endDate Optional end date for filtering (inclusive)
     * @param pageable Pagination information
     * @return Page of country and channel revenue data
     */
    @GetMapping("/country-channel-revenues")
    @Operation(summary = "Get revenues by country and channel", 
               description = "Retrieves revenues grouped by country and channel, optionally filtered by date range")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Report generated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Page<CountryChannelRevenueDto>> getCountryChannelRevenues(
            @Parameter(description = "Start date (ISO format yyyy-MM-dd)")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            
            @Parameter(description = "End date (ISO format yyyy-MM-dd)")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            
            @Parameter(description = "Pagination parameters")
            Pageable pageable) {
        
        try {
            logger.info("Generating country/channel revenue report, date range: {} to {}", startDate, endDate);
            Page<CountryChannelRevenueDto> report = reportService.getCountryChannelRevenues(startDate, endDate, pageable);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            logger.error("Error generating country/channel revenue report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get KPIs grouped by issuer with optional date filtering.
     * 
     * @param startDate Optional start date for filtering (inclusive)
     * @param endDate Optional end date for filtering (inclusive)
     * @param pageable Pagination information
     * @return Page of issuer KPI data
     */
    @GetMapping("/issuer-kpis")
    @Operation(summary = "Get KPIs by issuer", 
               description = "Retrieves key performance indicators grouped by issuer, optionally filtered by date range")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Report generated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Page<IssuerKpiDto>> getIssuerKpis(
            @Parameter(description = "Start date (ISO format yyyy-MM-dd)")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            
            @Parameter(description = "End date (ISO format yyyy-MM-dd)")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            
            @Parameter(description = "Pagination parameters")
            Pageable pageable) {
        
        try {
            logger.info("Generating issuer KPI report, date range: {} to {}", startDate, endDate);
            Page<IssuerKpiDto> report = reportService.getIssuerKpis(startDate, endDate, pageable);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            logger.error("Error generating issuer KPI report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Export report data to CSV or Excel format.
     * 
     * @param format Export format ("csv" or "excel")
     * @param startDate Optional start date for filtering (inclusive)
     * @param endDate Optional end date for filtering (inclusive)
     * @param bin Optional BIN for filtering
     * @param country Optional country code for filtering
     * @return Resource containing the exported data
     */
    @GetMapping("/export")
    @Operation(summary = "Export report data", 
               description = "Exports report data in CSV or Excel format with optional filtering")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Export generated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Resource> exportReportData(
            @Parameter(description = "Export format (csv or excel)", required = true)
            @RequestParam 
            @Pattern(regexp = "^(csv|excel)$", message = "Format must be 'csv' or 'excel'") String format,
            
            @Parameter(description = "Start date (ISO format yyyy-MM-dd)")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            
            @Parameter(description = "End date (ISO format yyyy-MM-dd)")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            
            @Parameter(description = "BIN filter")
            @RequestParam(required = false) String bin,
            
            @Parameter(description = "Country code filter")
            @RequestParam(required = false) String country) {
        
        try {
            logger.info("Exporting report data in {} format, date range: {} to {}", format, startDate, endDate);
            logger.info("Filters - BIN: {}, Country: {}", bin, country);
            
            Resource resource = reportService.exportReportData(format, startDate, endDate, bin, country);
            
            // Set appropriate headers based on format
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=epin-report." + format);
            
            if ("csv".equalsIgnoreCase(format)) {
                headers.setContentType(MediaType.parseMediaType("text/csv"));
            } else {
                headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            }
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
            
        } catch (Exception e) {
            logger.error("Error exporting report data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/kpis")
    @Operation(summary = "Get KPIs ",
            description = "Retrieves key performance indicators r, optionally filtered by date range")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Report generated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<VisaSettlementStatsRecord> getKpisVisa(
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "currencyCode", required = false)  String currencyCode,
            @RequestParam(value = "binCode", required = false)  String binCode
    ) {

        Specification<Vss110SettlementRecord> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (binCode != null) {
                predicates.add(cb.like(root.get("destinationId"),"%"+ binCode+"%"));
            }
            if (currencyCode != null) {
                predicates.add(cb.equal(root.get("currencyCode"), currencyCode));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("settlementDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("settlementDate"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return ResponseEntity.ok(reportService.getVisaSettlementStats(spec));
    }

    @GetMapping("/kpis/interchange")
    @Operation(summary = "Get Interchange Details ",
            description = "Get Interchange Details, optionally filtered by date range")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Report generated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Vss120Report> getInterchangeVisa(
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "currencyCode", required = false)  String currencyCode,
            @RequestParam(value = "binCode", required = false)  String binCode
    ) {



        return ResponseEntity.ok(reportService.getVss120Report(currencyCode,startDate,endDate,binCode));
    }

    @GetMapping("/kpis/reimbursementFee")
    @Operation(summary = "Get reimbursementFee Details ",
            description = "Get reimbursementFee Details, optionally filtered by date range")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Report generated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Vss130Report> getReimbursementFeeVisa(
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "currencyCode", required = false)  String currencyCode,
            @RequestParam(value = "binCode", required = false)  String binCode
    ) {



        return ResponseEntity.ok(reportService.getVss130Report(currencyCode,startDate,endDate,binCode));
    }


    @GetMapping("/kpis/visaCharges")
    @Operation(summary = "Get Visa Charges Details ",
            description = "Get Visa Charges Details, optionally filtered by date range")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Report generated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Vss140Report> getChargesVisa(
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "currencyCode", required = false)  String currencyCode,
            @RequestParam(value = "binCode", required = false)  String binCode
    ) {



        return ResponseEntity.ok(reportService.getVss140Report(currencyCode,startDate,endDate,binCode));
    }
}