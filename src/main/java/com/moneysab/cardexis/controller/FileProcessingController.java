package com.moneysab.cardexis.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneysab.cardexis.config.DatabaseHealthIndicator;
import com.moneysab.cardexis.domain.entity.FileProcessingJob;
import com.moneysab.cardexis.domain.enums.ProcessingStatus;
import com.moneysab.cardexis.dto.FileProcessingJobDto;
import com.moneysab.cardexis.dto.report.ProcessingStatisticsDto;
import com.moneysab.cardexis.repository.FileProcessingJobRepository;
import com.moneysab.cardexis.service.IEpinFileProcessingService;
import com.moneysab.cardexis.service.impl.EpinFileProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for EPIN file processing operations.
 * 
 * This controller provides endpoints for uploading and processing Visa EPIN files,
 * monitoring processing status, and retrieving processing results. It supports
 * both VSS-110 and VSS-120 settlement data formats.
 * 
 * Key Features:
 * - File upload with validation
 * - Asynchronous processing status tracking
 * - Processing job management
 * - Error handling and reporting
 * - Client-specific filtering
 * 
 * @author EL.AHGOUNE
 * @version 1.0.0
 * @since 2024
 */
@RestController
@RequestMapping("/api/v1/epin")
@Tag(name = "EPIN File Processing", description = "Operations for processing Visa EPIN settlement files")
@SecurityRequirement(name = "bearerAuth")
public class FileProcessingController {

    private static final Logger logger = LoggerFactory.getLogger(FileProcessingController.class);

    @Autowired
    private EpinFileProcessingService fileProcessingService;
    
    @Autowired
    private DatabaseHealthIndicator databaseHealthIndicator;

    @Autowired
    private FileProcessingJobRepository jobRepository;

    /**
     * Upload and process an EPIN file.
     * 
     * @param file the EPIN file to process
     * @return the processing job with initial status
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload EPIN file for processing", 
               description = "Uploads a Visa EPIN file and starts processing. Supports VSS-110 and VSS-120 formats.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "File uploaded successfully",
                    content = @Content(schema = @Schema(implementation = FileProcessingJob.class))),
        @ApiResponse(responseCode = "400", description = "Invalid file or request"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<FileProcessingJob> uploadFile(
            @Parameter(description = "EPIN file to upload", required = true)
            @RequestParam("file") MultipartFile file) {
        
        try {
            // Check database health before processing
            if (!databaseHealthIndicator.checkDatabaseHealth()) {
                logger.error("Database health check failed - cannot process file upload");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
            }
            
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            // Validate file extension
            String filename = file.getOriginalFilename();
            if (filename == null /*|| (!filename.endsWith(".TXT") && !filename.endsWith(".dat"))*/) {
                return ResponseEntity.badRequest().build();
            }
            
            // Convert file content to string
            String fileContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            
            // Process the file
            FileProcessingJob job = fileProcessingService.processEpinFile(
                filename, fileContent, file.getSize());
            
            logger.info("File uploaded and processing started: {} (Job ID: {})", 
                       filename, job.getId());
            
            return ResponseEntity.ok(job);
            
        } catch (IOException e) {
            logger.error("Error reading uploaded file: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            logger.error("Error processing uploaded file: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get processing job status by ID.
     * 
     * @param jobId the processing job ID
     * @return the processing job with current status
     */
    @GetMapping("/jobs/{jobId}")
    @Operation(summary = "Get processing job status", 
               description = "Retrieves the current status and details of a processing job.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Job found"),
            @ApiResponse(responseCode = "404", description = "Job not found")
    })
    public ResponseEntity<FileProcessingJobDto> getJobStatus(
            @Parameter(description = "Processing job ID", required = true)
            @PathVariable UUID jobId) {
        
        Optional<FileProcessingJob> job = fileProcessingService.getProcessingJob(jobId);
        if (job.isPresent()) {
            FileProcessingJobDto dto = FileProcessingJobDto.toDto(job.get());
            return ResponseEntity.ok(dto);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all processing jobs
     *
     *
     * @return list of processing jobs
     */
    @GetMapping("/jobs")
    @Operation(summary = "Get processing jobs ",
            description = "Retrieves all processing jobs , ordered by creation date.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Jobs retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid ")
    })
    public ResponseEntity< Page<FileProcessingJobDto>> getJobs(
            @RequestParam(value = "jobsName", required = false) String jobsName,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

            Specification<FileProcessingJob> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();

                if (jobsName != null) {
                    predicates.add(cb.like(cb.lower(root.get("originalFilename")), "%" + jobsName.toLowerCase() + "%"));
                }
                if (status != null) {
                    predicates.add(cb.equal(root.get("status"), ProcessingStatus.valueOf(status)));
                }
                if (startDate != null) {
                    LocalDateTime debut = startDate.atStartOfDay();
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), debut));
                }
                if (endDate != null) {
                    LocalDateTime fin = endDate.atTime(23, 59, 59);
                    predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), fin));
                }

                return cb.and(predicates.toArray(new Predicate[0]));
            };

            Page<FileProcessingJobDto> jobsPage = jobRepository.findAll(spec, pageable).map(FileProcessingJobDto::toDto);
            return ResponseEntity.ok(jobsPage);
        } catch (Exception e) {
            logger.error("Error retrieving jobs : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all processing jobs for a client.
     * 
     * @param clientId the client identifier
     * @return list of processing jobs for the client
     */
    @GetMapping("/jobs/client")
    @Operation(summary = "Get processing jobs for client", 
               description = "Retrieves all processing jobs for a specific client, ordered by creation date.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Jobs retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid client ID")
    })
    public ResponseEntity<List<FileProcessingJobDto>> getJobsForClient(
            @Parameter(description = "Client identifier", required = true)
            @RequestParam @NotBlank String clientId) {
        
        try {
            List<FileProcessingJob> jobs = fileProcessingService.getProcessingJobsForClient(clientId);
            List<FileProcessingJobDto> jobsDtos = jobs.stream()
                    .map(FileProcessingJobDto::toDto)
                    .toList();
            return ResponseEntity.ok(jobsDtos);
        } catch (Exception e) {
            logger.error("Error retrieving jobs for client {}: {}", clientId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Retry a failed processing job.
     * 
     * @param jobId the processing job ID to retry
     * @param file the file content to reprocess (optional, uses original if not provided)
     * @return the updated processing job
     */
    @PostMapping("/jobs/{jobId}/retry")
    @Operation(summary = "Retry failed processing job", 
               description = "Retries a failed processing job with optional new file content.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Job retried successfully",
                    content = @Content(schema = @Schema(implementation = FileProcessingJob.class))),
        @ApiResponse(responseCode = "400", description = "Job cannot be retried"),
        @ApiResponse(responseCode = "404", description = "Job not found")
    })
    public ResponseEntity<FileProcessingJob> retryJob(
            @Parameter(description = "Processing job ID", required = true)
            @PathVariable UUID jobId,
            @Parameter(description = "New file content (optional)")
            @RequestParam(value = "file", required = false) MultipartFile file) {
        
        try {
            String fileContent = null;
            
            if (file != null && !file.isEmpty()) {
                fileContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            }
            
            FileProcessingJob job = fileProcessingService.retryProcessingJob(jobId, fileContent);
            
            logger.info("Processing job retried: {} (Job ID: {})", 
                       job.getOriginalFilename(), job.getId());
            
            return ResponseEntity.ok(job);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid retry request for job {}: {}", jobId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            logger.warn("Job {} cannot be retried: {}", jobId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            logger.error("Error reading retry file for job {}: {}", jobId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            logger.error("Error retrying job {}: {}", jobId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Cancel a processing job.
     * 
     * @param jobId the processing job ID to cancel
     * @return the updated processing job
     */
    @PostMapping("/jobs/{jobId}/cancel")
    @Operation(summary = "Cancel processing job", 
               description = "Cancels an active processing job.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Job cancelled successfully",
                    content = @Content(schema = @Schema(implementation = FileProcessingJob.class))),
        @ApiResponse(responseCode = "400", description = "Job cannot be cancelled"),
        @ApiResponse(responseCode = "404", description = "Job not found")
    })
    public ResponseEntity<FileProcessingJob> cancelJob(
            @Parameter(description = "Processing job ID", required = true)
            @PathVariable UUID jobId) {
        
        try {
            Optional<FileProcessingJob> optionalJob = fileProcessingService.getProcessingJob(jobId);
            
            if (optionalJob.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            FileProcessingJob job = optionalJob.get();
            
            if (!job.isActive()) {
                return ResponseEntity.badRequest().build();
            }
            
            job.cancelProcessing();
            
            logger.info("Processing job cancelled: {} (Job ID: {})", 
                       job.getOriginalFilename(), job.getId());
            
            return ResponseEntity.ok(job);
            
        } catch (Exception e) {
            logger.error("Error cancelling job {}: {}", jobId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get processing statistics summary.
     * 
     * @return processing statistics
     */
    @GetMapping("/stats")
    @Operation(summary = "Get processing statistics", 
               description = "Retrieves overall processing statistics and metrics.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    })
    public ResponseEntity<ProcessingStatisticsDto> getProcessingStats() {

        try {

            ProcessingStatisticsDto statistics= fileProcessingService.getStatistics();

            return ResponseEntity.ok(statistics);

        } catch (Exception e) {
            logger.error("Error retrieving processing statistics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/jobs/{jobId}/progress")
    @Operation(summary = "Get processing job progress")
    public ResponseEntity<Map<String, Object>> getProcessingProgress(@PathVariable UUID jobId) {
        Optional<FileProcessingJob> job = fileProcessingService.getProcessingJob(jobId);
        
        if (job.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> progress = new HashMap<>();
        progress.put("status", job.get().getStatus());
        progress.put("totalRecords", job.get().getTotalRecords());
        progress.put("processedRecords", job.get().getProcessedRecords());
        progress.put("failedRecords", job.get().getFailedRecords());
        
        if (job.get().getTotalRecords() != null && job.get().getTotalRecords() > 0) {
            double progressPercentage = (double) job.get().getProcessedRecords() / job.get().getTotalRecords() * 100;
            progress.put("progressPercentage", Math.round(progressPercentage * 100.0) / 100.0);
        }
        
        return ResponseEntity.ok(progress);
    }

}