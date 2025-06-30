package com.moneysab.cardexis.service.impl;

import com.moneysab.cardexis.domain.entity.*;
import com.moneysab.cardexis.domain.enums.FileType;
import com.moneysab.cardexis.domain.enums.ProcessingStatus;
import com.moneysab.cardexis.domain.enums.VisaReportFormat;
import com.moneysab.cardexis.dto.FileProcessingJobDto;
import com.moneysab.cardexis.dto.report.ProcessingStatisticsDto;
import com.moneysab.cardexis.parser.Vss110FileParser;
import com.moneysab.cardexis.parser.VssSubgroup4Parser ;
import com.moneysab.cardexis.parser.Vss120Tcr1Parser;
import com.moneysab.cardexis.repository.*;
import com.moneysab.cardexis.repository.vss.Report130Repository;
import com.moneysab.cardexis.repository.vss.Report140Repository;
import com.moneysab.cardexis.service.IEpinFileProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core service for processing Visa EPIN files containing VSS-110, VSS-120 and other settlement data.
 *
 * Updated to handle both V2110 (VSS-110) and V4120 (VSS-120) record types and stop processing
 * when unknown record types are encountered while preserving successfully processed records.
 */
@Service
public class EpinFileProcessingService implements IEpinFileProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(EpinFileProcessingService.class);
    private static final int BATCH_SIZE = 100; // Optimized batch size for large files

    @Autowired
    private FileProcessingJobRepository jobRepository;

    @Autowired
    private EpinFileHeaderRepository headerRepository;

    @Autowired
    private Vss110SettlementRecordRepository vss110Repository;

    @Autowired
    private Vss120SettlementRecordRepository vss120Repository;

    @Autowired
    private Vss130SettlementRecordRepository vss130Repository;

    @Autowired
    private Vss140SettlementRecordRepository vss140Repository;

    @Autowired
    private Vss110FileParser vss110Parser;

    @Autowired
    private VssSubgroup4Parser  vssSubGroup4Parser;

    @Autowired
    private Vss120Tcr1RecordRepository vss120Tcr1Repository;

    @Autowired
    private Vss120Tcr1Parser vss120Tcr1Parser;

    @Autowired
    private Report130Repository report130Repository;

    @Autowired
    private Report140Repository report140Repository;

    /**
     * Process a complete EPIN file with improved memory management and larger batches.
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class, timeout = 300)
    public FileProcessingJob processEpinFile(String originalFilename, String fileContent, Long fileSizeBytes) {
        logger.info("Starting EPIN file processing for file: {}, size: {} bytes", originalFilename, fileSizeBytes);

        // Create and initialize processing job
        FileProcessingJob job = new FileProcessingJob(originalFilename, FileType.EPIN, fileSizeBytes);
        job = jobRepository.save(job);

        try {
            // Start processing
            job.startProcessing();
            job = jobRepository.saveAndFlush(job); // Ensure immediate persistence

            // Parse file content in optimized batches
            ProcessingResult result = parseFileContentInOptimizedBatches(job, fileContent);

            // Update job with processing results
            job.setTotalRecords(result.getTotalRecords());
            job.setProcessedRecords(result.getValidRecords());
            job.setFailedRecords(result.getInvalidRecords());

            // Determine final status
            if (result.getValidRecords() > 0) {
                job.completeProcessing();
                if (result.hasErrors()) {
                    String message = String.format("Completed with warnings. Processed %d valid records out of %d total. %s",
                            result.getValidRecords(), result.getTotalRecords(), result.getErrorSummary());
                    job.setErrorMessage(message);
                }
                logger.info("EPIN file processing completed successfully for job: {}, processed: {}/{}",
                        job.getId(), result.getValidRecords(), result.getTotalRecords());
            } else {
                job.failProcessing(result.getErrorSummary());
                logger.error("EPIN file processing failed for job: {} - no valid records processed", job.getId());
            }

        } catch (Exception e) {
            logger.error("Error processing EPIN file for job: {}", job.getId(), e);
            job.failProcessing("Processing failed: " + e.getMessage());
        }

        return jobRepository.saveAndFlush(job);
    }

    /**
     * Parse file content in optimized batches that reduces memory usage and transaction overhead.
     */
    private ProcessingResult parseFileContentInOptimizedBatches(FileProcessingJob job, String fileContent) throws IOException {
        ProcessingResult result = new ProcessingResult();
        
        // Use streaming approach to avoid loading entire file into memory
        try (BufferedReader reader = new BufferedReader(new StringReader(fileContent))) {
            String line;
            int lineNumber = 0;
            boolean foundVisaRecords = false;
            boolean finishedVisaSection = false;

            List<String> batch = new ArrayList<>(BATCH_SIZE);
            EpinFileHeader header = null;
            VssSubGroup4Record lastVss120Record = null; // Maintain parent record across batches

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (line.trim().isEmpty()) {
                    continue;
                }

                String recordType = detectRecordType(line);

                if (isVisaSettlementRecord(recordType)) {
                    foundVisaRecords = true;
                    finishedVisaSection = false;
                    batch.add(line);

                    // Process batch when it reaches optimal size
                    if (batch.size() >= BATCH_SIZE) {
                        ProcessingBatchResult batchResult = processBatchWithRetry(job, batch, lineNumber - batch.size() + 1, lastVss120Record);
                        result.merge(batchResult);
                        
                        // Update lastVss120Record from batch result
                        if (batchResult.getLastVss120Record() != null) {
                            lastVss120Record = batchResult.getLastVss120Record();
                        }
                        
                        // Clear batch and force garbage collection of processed data
                        batch.clear();
                        
                        if (batchResult.getHeader() != null) {
                            header = batchResult.getHeader();
                        }
                        
                        // Log progress every 1000 records
                        if (result.getTotalRecords() % 1000 == 0) {
                            logger.info("Processing progress: {} records processed", result.getTotalRecords());
                        }
                    }
                } else if (recordType.equals("HEADER")) {
                    batch.add(line);
                    
                    if (batch.size() >= BATCH_SIZE) {
                        ProcessingBatchResult batchResult = processBatchWithRetry(job, batch, lineNumber - batch.size() + 1, lastVss120Record);
                        result.merge(batchResult);
                        batch.clear();

                        if (batchResult.getHeader() != null) {
                            header = batchResult.getHeader();
                        }
                    }
                } else {
                    // Don't stop processing early - continue through the entire file
                    // as VSS records can be interspersed with control records
                    if (!foundVisaRecords) {
                        result.incrementTotal();
                        result.incrementInvalid();
                        result.addError(lineNumber, "Skipped non-Visa record: " + recordType);
                    }
                }
            }

            // Process remaining records in final batch
            if (!batch.isEmpty()) {
                ProcessingBatchResult batchResult = processBatchWithRetry(job, batch, lineNumber - batch.size() + 1, lastVss120Record);
                result.merge(batchResult);
            }

            if (foundVisaRecords) {
                logger.info("Completed processing VSS records (V2110/V4120/V4130/V4140). Total processed: {} valid records.", result.getValidRecords());
            } else {
                logger.warn("No VSS records found in the file.");
            }

            return result;
        }
    }

    /**
     * Process batch with retry logic for transient failures.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class, timeout = 120)
    public ProcessingBatchResult processBatchWithRetry(FileProcessingJob job, List<String> lines, int startLineNumber, VssSubGroup4Record lastVss120Record) {
        int retryCount = 0;
        int maxRetries = 3;
        
        while (retryCount <= maxRetries) {
            try {
                return processBatch(job, lines, startLineNumber, lastVss120Record);
            } catch (Exception e) {
                retryCount++;
                if (retryCount > maxRetries) {
                    logger.error("Failed to process batch after {} retries, starting at line {}: {}", 
                               maxRetries, startLineNumber, e.getMessage());
                    throw e;
                }
                
                logger.warn("Batch processing failed, retry {} of {}: {}", retryCount, maxRetries, e.getMessage());
                
                // Wait before retry (exponential backoff)
                try {
                    Thread.sleep(1000 * retryCount);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Processing interrupted", ie);
                }
            }
        }
        
        throw new RuntimeException("Should not reach here");
    }

    /**
     * Check if a record type is a Visa settlement record or its related TCR1.
     */
    private boolean isVisaSettlementRecord(String recordType) {
        return "V2110".equals(recordType) ||
                "V4120".equals(recordType) ||
                "V4130".equals(recordType) ||
                "V4140".equals(recordType) ||
                "V4TCR1".equals(recordType) ||
                "V2TCR1".equals(recordType) ||
                "TCR1".equals(recordType);
    }

    /**
     * Process a single batch of records in its own transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class, timeout = 30)
    public ProcessingBatchResult processBatch(FileProcessingJob job, List<String> lines, int startLineNumber, VssSubGroup4Record lastVss120Record) {
        ProcessingBatchResult result = new ProcessingBatchResult();
        int lineNumber = startLineNumber;

        for (String line : lines) {
            try {
                String recordType = detectRecordType(line);

                switch (recordType) {
                    case "HEADER":
                        EpinFileHeader header = processHeaderRecord(job, line, lineNumber);
                        result.setHeader(header);
                        result.incrementTotal();
                        if (header != null && header.getIsValid()) {
                            result.incrementValid();
                        } else {
                            result.incrementInvalid();
                            result.addError(lineNumber, "Invalid header record");
                        }
                        break;

                    case "V2110":
                        Vss110SettlementRecord vss110 = processVss110Record(job, line, lineNumber);
                        result.incrementTotal();
                        if (vss110 != null && vss110.getIsValid()) {
                            result.incrementValid();
                        } else {
                            result.incrementInvalid();
                            result.addError(lineNumber, "Invalid VSS-110 record");
                        }
                        break;

                    case "V4120":
                        Vss120SettlementRecord vss120 = processVss120Record(job, line, lineNumber);
                        lastVss120Record = vss120; // Keep reference for potential TCR1 records
                        result.incrementTotal();
                        if (vss120 != null && vss120.getIsValid()) {
                            result.incrementValid();
                        } else {
                            result.incrementInvalid();
                            result.addError(lineNumber, "Invalid VSS-120 record");
                        }
                        break;

                    case "V4130":
                        Vss130SettlementRecord vss130 = processVss130Record(job, line, lineNumber);
                        lastVss120Record = vss130; // Keep reference for potential TCR1 records
                        result.incrementTotal();
                        if (vss130 != null && vss130.getIsValid()) {
                            result.incrementValid();
                        } else {
                            result.incrementInvalid();
                            result.addError(lineNumber, "Invalid VSS-130 record");
                        }
                        break;

                    case "V4140":
                        Vss140SettlementRecord vss140 = processVss140Record(job, line, lineNumber);
                        lastVss120Record = vss140; // Keep reference for potential TCR1 records
                        result.incrementTotal();
                        if (vss140 != null && vss140.getIsValid()) {
                            result.incrementValid();
                        } else {
                            result.incrementInvalid();
                            result.addError(lineNumber, "Invalid VSS-140 record");
                        }
                        break;

                    case "TCR1":
                        // Process TCR1 record - determine type based on parent record
                        Vss120Tcr1Record tcr1 = processTcr1Record(job, line, lineNumber, lastVss120Record);
                        result.incrementTotal();
                        if (tcr1 != null && tcr1.getIsValid()) {
                            result.incrementValid();
                        } else {
                            result.incrementInvalid();
                            result.addError(lineNumber, "Invalid TCR1 record");
                        }
                        break;


                    default:
                        // This shouldn't happen since we pre-filter, but handle gracefully
                        //logger.warn("Unexpected record type '{}' in batch at line {}", recordType, lineNumber);
                        //result.incrementTotal();
                       // result.incrementInvalid();
                       // result.addError(lineNumber, "Unexpected record type: " + recordType);
                        break;
                }

            } catch (Exception e) {
                logger.error("Error processing line {}: {}", lineNumber, e.getMessage());
                result.incrementTotal();
                result.incrementInvalid();
                result.addError(lineNumber, "Processing error: " + e.getMessage());
            }

            lineNumber++;
        }

        result.setLastVss120Record(lastVss120Record);
        return result;
    }

    /**
     * Detect record type from the line based on EPIN format specifications.
     * Updated with better debugging and more robust detection logic.
     */
    private String detectRecordType(String line) {
        if (line == null || line.length() < 10) return "UNKNOWN";

        // Log the first few characters for debugging
        String debugPrefix = line.length() >= 10 ? line.substring(0, 10) : line;
        logger.debug("Detecting record type for line starting with: '{}'", debugPrefix);

        // Check for V2110 (VSS-110 - Report Subgroup 2)
        if (line.contains("V2110")) {
            logger.debug("Detected V2110 record");
            return "V2110";
        }

        // Check for V4120 (VSS-120 - Report Subgroup 4)
        if (line.contains("V4120")) {
            logger.debug("Detected V4120 record");
            return "V4120";
        }

        // Check for V4130 (VSS-130 - Report Subgroup 4)
        if (line.contains("V4130")) {
            logger.debug("Detected V4130 record");
            return "V4130";
        }

        // Check for V4140 (VSS-140 - Report Subgroup 4)
        if (line.contains("V4140")) {
            logger.debug("Detected V4140 record");
            return "V4140";
        }

        // Check for TCR1 records (Transaction Component Sequence Number = 1)
        // TCR1 records start with "46" (transaction code) followed by "0" (qualifier) and "1" (sequence)
        if (line.length() >= 4 && line.startsWith("460") && (!line.contains("V"))  ) {
            char sequenceNumber = line.charAt(3);
            logger.debug("Found 460 prefix, sequence number: '{}'", sequenceNumber);

            if (sequenceNumber == '1') {
                // This is a TCR1 record
                // Check report group/subgroup to determine type
                if (line.length() >= 60) {
                    String reportGroup = line.substring(58, 59); // Position 59 (0-based: 58)
                    String reportSubgroup = line.substring(59, 60); // Position 60 (0-based: 59)
                    logger.debug("TCR1 record - Report Group: '{}', Subgroup: '{}'", reportGroup, reportSubgroup);

                    if ("V".equals(reportGroup)) {
                        if ("2".equals(reportSubgroup)) {
                            logger.debug("Detected V2TCR1 record");
                            return "V2TCR1"; // TCR1 for VSS-110
                        } else if ("4".equals(reportSubgroup)) {
                            logger.debug("Detected V4TCR1 record");
                            return "V4TCR1"; // TCR1 for VSS-120
                        }
                    }
                }
                logger.debug("Detected generic TCR1 record");
                return "TCR1"; // Generic TCR1 if we can't determine the subgroup
            } else if (sequenceNumber == '0') {
                // This is a TCR0 record, check for report identifiers
                if (line.contains("V2110")) {
                    logger.debug("Detected V2110 record (TCR0)");
                    return "V2110";
                }
                if (line.contains("V4120")) {
                    logger.debug("Detected V4120 record (TCR0)");
                    return "V4120";
                }
                if (line.contains("V4130")) {
                    logger.debug("Detected V4130 record (TCR0)");
                    return "V4130";
                }
                if (line.contains("V4140")) {
                    logger.debug("Detected V4140 record (TCR0)");
                    return "V4140";
                }

                // Check positions for report group/subgroup directly (fallback)
                if (line.length() >= 60) {
                    String reportGroup = line.substring(58, 59); // Position 59
                    String reportSubgroup = line.substring(59, 60); // Position 60
                    logger.debug("TCR0 record without V identifier - Report Group: '{}', Subgroup: '{}'", reportGroup, reportSubgroup);

                    if ("V".equals(reportGroup)) {
                        if ("2".equals(reportSubgroup)) {
                            logger.debug("Inferred V2110 record from positions");
                            return "V2110"; // Assume VSS-110 if subgroup 2
                        } else if ("4".equals(reportSubgroup)) {
                            logger.debug("Inferred V4120 record from positions");
                            return "V4120"; // Assume VSS-120 if subgroup 4
                        }
                    }
                }
            }
        }

        // Check for header records - these typically start with numeric client ID
        if (line.matches("^[0-9]{13}\\s+.*") && !line.contains("V2110") && !line.contains("V4120")) {
            logger.debug("Detected HEADER record");
            return "HEADER";
        }

        // Additional check for records starting with "330" - these appear to be a different format
        if (line.startsWith("330")) {
            logger.debug("Found record starting with '330' - likely different format, marking as UNKNOWN");
            return "UNKNOWN";
        }

        logger.debug("Record type UNKNOWN");
        return "UNKNOWN";
    }

    /**
     * Process EPIN header record with better error handling.
     */
    private EpinFileHeader processHeaderRecord(FileProcessingJob job, String line, int lineNumber) {
        try {
            EpinFileHeader header = new EpinFileHeader(job);
            header.parseHeaderLine(line);

            // Update job with client ID from header
            if (header.getClientId() != null) {
                job.setClientId(header.getClientId());
                jobRepository.save(job);
            }

            return headerRepository.save(header);

        } catch (Exception e) {
            logger.error("Error parsing header record at line {}: {}", lineNumber, e.getMessage());
            // Create invalid header record for audit trail
            EpinFileHeader invalidHeader = new EpinFileHeader(job);
            invalidHeader.setRawHeaderLine(line);
            invalidHeader.setIsValid(false);
            invalidHeader.setValidationErrors("Parsing failed: " + e.getMessage());
            return headerRepository.save(invalidHeader);
        }
    }

    /**
     * Process VSS-110 settlement record with better error handling.
     */
    private Vss110SettlementRecord processVss110Record(FileProcessingJob job, String line, int lineNumber) {
        try {
            Vss110SettlementRecord record = vss110Parser.parseLine(line, lineNumber, job);

            // Update job report format if not set
            if (job.getReportFormat() == null) {
                job.setReportFormat(VisaReportFormat.VSS_110);
                jobRepository.save(job);
            }

            return vss110Repository.save(record);

        } catch (Exception e) {
            logger.error("Error parsing VSS-110 record at line {}: {}", lineNumber, e.getMessage());
            // Create invalid record for audit trail
            Vss110SettlementRecord invalidRecord = new Vss110SettlementRecord(job);
            invalidRecord.setRawRecordLine(line);
            invalidRecord.setLineNumber(lineNumber);
            invalidRecord.setIsValid(false);
            invalidRecord.setValidationErrors("Parsing failed: " + e.getMessage());
            return vss110Repository.save(invalidRecord);
        }
    }

    /**
     * Process VSS-120 settlement record with better error handling.
     */
    private Vss120SettlementRecord processVss120Record(FileProcessingJob job, String line, int lineNumber) {
        try {
            Vss120SettlementRecord record = vssSubGroup4Parser.parseLine(line, lineNumber, job, Vss120SettlementRecord.class);

            // Update job report format if not set or add as additional format
            if (job.getReportFormat() == null) {
                job.setReportFormat(VisaReportFormat.VSS_120);
                jobRepository.save(job);
            } else if (job.getReportFormat() == VisaReportFormat.VSS_110) {
                // File contains both VSS-110 and VSS-120 records
                job.setReportFormat(VisaReportFormat.MIXED);
                jobRepository.save(job);
            }

            return vss120Repository.save(record);

        } catch (Exception e) {
            logger.error("Error parsing VSS-120 record at line {}: {}", lineNumber, e.getMessage());
            // Create invalid record for audit trail
            Vss120SettlementRecord invalidRecord = new Vss120SettlementRecord(job);
            invalidRecord.setRawRecordLine(line);
            invalidRecord.setLineNumber(lineNumber);
            invalidRecord.setIsValid(false);
            invalidRecord.setValidationErrors("Parsing failed: " + e.getMessage());
            return vss120Repository.save(invalidRecord);
        }
    }
    /**
     * Process VSS-130 settlement record with better error handling.
     */
    private Vss130SettlementRecord processVss130Record(FileProcessingJob job, String line, int lineNumber) {
        try {
            Vss130SettlementRecord record = vssSubGroup4Parser.parseLine(line, lineNumber, job, Vss130SettlementRecord.class);

            // Update job report format if not set or add as additional format
            if (job.getReportFormat() == null) {
                job.setReportFormat(VisaReportFormat.VSS_130);
                jobRepository.save(job);
            } else if (job.getReportFormat() == VisaReportFormat.VSS_110) {
                // File contains both VSS-110 and VSS-SubGroup4 records
                job.setReportFormat(VisaReportFormat.MIXED);
                jobRepository.save(job);
            }

            return vss130Repository.save(record);

        } catch (Exception e) {
            logger.error("Error parsing VSS-130 record at line {}: {}", lineNumber, e.getMessage());
            // Create invalid record for audit trail
            Vss130SettlementRecord invalidRecord = new Vss130SettlementRecord(job);
            invalidRecord.setRawRecordLine(line);
            invalidRecord.setLineNumber(lineNumber);
            invalidRecord.setIsValid(false);
            invalidRecord.setValidationErrors("Parsing failed: " + e.getMessage());
            return vss130Repository.save(invalidRecord);
        }
    }
    /**
     * Process VSS-140 settlement record with better error handling.
     */
    private Vss140SettlementRecord processVss140Record(FileProcessingJob job, String line, int lineNumber) {
        try {
            Vss140SettlementRecord record = vssSubGroup4Parser.parseLine(line, lineNumber, job, Vss140SettlementRecord.class);

            // Update job report format if not set or add as additional format
            if (job.getReportFormat() == null) {
                job.setReportFormat(VisaReportFormat.VSS_140);
                jobRepository.save(job);
            } else if (job.getReportFormat() == VisaReportFormat.VSS_110) {
                // File contains both VSS-110 and VSS-140 records
                job.setReportFormat(VisaReportFormat.MIXED);
                jobRepository.save(job);
            }

            return vss140Repository.save(record);

        } catch (Exception e) {
            logger.error("Error parsing VSS-140 record at line {}: {}", lineNumber, e.getMessage());
            // Create invalid record for audit trail
            Vss140SettlementRecord invalidRecord = new Vss140SettlementRecord(job);
            invalidRecord.setRawRecordLine(line);
            invalidRecord.setLineNumber(lineNumber);
            invalidRecord.setIsValid(false);
            invalidRecord.setValidationErrors("Parsing failed: " + e.getMessage());
            return vss140Repository.save(invalidRecord);
        }
    }

    /**
     * Process TCR1 record with improved error handling and parent record resolution.
     * Handles TCR1 records for VSS-120, VSS-130, and VSS-140.
     */
    private Vss120Tcr1Record processTcr1Record(FileProcessingJob job, String line, int lineNumber,
                                               VssSubGroup4Record parentRecord) {
        try {
            // If no parent record is provided, try to find the most recent one for this job
            VssSubGroup4Record actualParent = parentRecord;
            if (actualParent == null) {
                logger.warn("No parent record provided for TCR1 at line {}. Attempting to find most recent parent.", lineNumber);
                
                // Try to find the most recent VSS SubGroup 4 record for this job
                // Search in reverse order of preference (most recent first)
                try {
                    // Try VSS-140 records first (most recent)
                    actualParent = vss140Repository.findTopByFileProcessingJobOrderByLineNumberDesc(job)
                            .orElse(null);
                    
                    if (actualParent == null) {
                        // Try VSS-130 records
                        actualParent = vss130Repository.findTopByFileProcessingJobOrderByLineNumberDesc(job)
                                .orElse(null);
                    }
                    
                    if (actualParent == null) {
                        // Try VSS-120 records
                        actualParent = vss120Repository.findTopByFileProcessingJobOrderByLineNumberDesc(job)
                                .orElse(null);
                    }
                    
                    if (actualParent != null) {
                        logger.info("Found parent record type {} with ID {} for TCR1 at line {}", 
                                   actualParent.getClass().getSimpleName(), actualParent.getId(), lineNumber);
                    } else {
                        logger.warn("No suitable parent record found for TCR1 at line {}", lineNumber);
                    }
                } catch (Exception e) {
                    logger.warn("Error searching for parent record for TCR1 at line {}: {}", lineNumber, e.getMessage());
                }
            } else {
                logger.debug("Using provided parent record type {} with ID {} for TCR1 at line {}", 
                           actualParent.getClass().getSimpleName(), actualParent.getId(), lineNumber);
            }
            
            Vss120Tcr1Record record = vss120Tcr1Parser.parseLine(line, lineNumber, job, actualParent);
            
            // Set the correct parent report number based on the parent record type
            if (actualParent != null && actualParent.getReportIdNumber() != null) {
                record.setParentReportNumber(actualParent.getReportIdNumber());
                logger.debug("Set parent report number {} for TCR1 at line {}", 
                           actualParent.getReportIdNumber(), lineNumber);
            } else {
                logger.warn("Parent record has no report ID number for TCR1 at line {}", lineNumber);
            }
            
            return vss120Tcr1Repository.save(record);

        } catch (Exception e) {
            logger.warn("Error parsing VSS-120 TCR1 record at line {}: {} - Creating invalid record for audit", lineNumber, e.getMessage());
            
            // Create invalid record for audit trail
            Vss120Tcr1Record invalidRecord = new Vss120Tcr1Record(job, parentRecord);
            invalidRecord.setRawRecordLine(line);
            invalidRecord.setLineNumber(lineNumber);
            invalidRecord.setIsValid(false);
            invalidRecord.setValidationErrors("Parsing failed: " + e.getMessage());

            // Set basic fields to avoid constraint violations
            if (parentRecord != null && parentRecord.getDestinationId() != null) {
                invalidRecord.setDestinationId(parentRecord.getDestinationId());
                // Set parent report number if available
                if (parentRecord.getReportIdNumber() != null) {
                    invalidRecord.setParentReportNumber(parentRecord.getReportIdNumber());
                } else {
                    invalidRecord.setParentReportNumber("120"); // Default fallback
                }
            } else {
                // Extract destination ID from the line if possible
                try {
                    if (line.length() >= 10) {
                        String destinationId = line.substring(4, 10); // Positions 5-10
                        invalidRecord.setDestinationId(destinationId);
                    } else {
                        invalidRecord.setDestinationId("000000"); // Default fallback
                    }
                } catch (Exception ex) {
                    invalidRecord.setDestinationId("000000"); // Default fallback
                }
                invalidRecord.setParentReportNumber("120"); // Default fallback
            }
            
            invalidRecord.setTransactionCode("46");
            invalidRecord.setTransactionCodeQualifier("0");
            invalidRecord.setTransactionComponentSequenceNumber("1");

            return vss120Tcr1Repository.save(invalidRecord);
        }
    }

    public Optional<FileProcessingJob> getProcessingJob(UUID jobId) {
        return jobRepository.findById(jobId);
    }

    public List<FileProcessingJob> getProcessingJobsForClient(String clientId) {
        return jobRepository.findByClientIdOrderByCreatedAtDesc(clientId);
    }

    public FileProcessingJob retryProcessingJob(UUID jobId, String fileContent) {
        Optional<FileProcessingJob> existingJob = jobRepository.findById(jobId);
        if (existingJob.isEmpty()) {
            throw new IllegalArgumentException("Job not found: " + jobId);
        }

        FileProcessingJob job = existingJob.get();
        if (!job.canRetry()) {
            throw new IllegalStateException("Job cannot be retried. Status: " + job.getStatus() +
                    ", Retry count: " + job.getRetryCount());
        }

        // Clean up previous attempt data
        cleanupJobData(job);

        // Increment retry count and reset for retry
        job.incrementRetryCount();
        job.setStatus(ProcessingStatus.UPLOADED);
        job.setErrorMessage(null);
        job.setTotalRecords(null);
        job.setProcessedRecords(null);
        job.setFailedRecords(null);
        job = jobRepository.save(job);

        // Reprocess the file
        return processEpinFile(job.getOriginalFilename(), fileContent, job.getFileSizeBytes());
    }

    private void cleanupJobData(FileProcessingJob job) {
        try {
            // Clean up settlement records (these have fileProcessingJob field)
            vss110Repository.deleteByFileProcessingJob(job);
            vss120Repository.deleteByFileProcessingJob(job);
            vss130Repository.deleteByFileProcessingJob(job);
            vss140Repository.deleteByFileProcessingJob(job);
            vss120Tcr1Repository.deleteByFileProcessingJob(job);
            headerRepository.deleteByFileProcessingJob(job);
            
            // Clean up report records (these use sourceFileName)
            report130Repository.deleteBySourceFileName(job.getOriginalFilename());
            report140Repository.deleteBySourceFileName(job.getOriginalFilename());
        } catch (Exception e) {
            logger.warn("Error cleaning up job data for job {}: {}", job.getId(), e.getMessage());
        }
    }

    /**
     * Processing result container.
     */
    private static class ProcessingResult {
        private int totalRecords = 0;
        private int validRecords = 0;
        private int invalidRecords = 0;
        private final List<String> errors = new ArrayList<>();

        public void incrementTotal() { totalRecords++; }
        public void incrementValid() { validRecords++; }
        public void incrementInvalid() { invalidRecords++; }

        public void addError(int lineNumber, String error) {
            errors.add("Line " + lineNumber + ": " + error);
        }

        public int getTotalRecords() { return totalRecords; }
        public int getValidRecords() { return validRecords; }
        public int getInvalidRecords() { return invalidRecords; }

        public boolean hasErrors() { return !errors.isEmpty(); }

        public String getErrorSummary() {
            if (errors.isEmpty()) return null;
            return String.join("; ", errors.subList(0, Math.min(errors.size(), 10))) +
                    (errors.size() > 10 ? " (and " + (errors.size() - 10) + " more)" : "");
        }

        public void merge(ProcessingBatchResult batchResult) {
            this.totalRecords += batchResult.getTotalRecords();
            this.validRecords += batchResult.getValidRecords();
            this.invalidRecords += batchResult.getInvalidRecords();
            this.errors.addAll(batchResult.getErrors());
        }
    }

    /**
     * Batch processing result container.
     */
    private static class ProcessingBatchResult {
        private int totalRecords = 0;
        private int validRecords = 0;
        private int invalidRecords = 0;
        private final List<String> errors = new ArrayList<>();
        private EpinFileHeader header;
        private VssSubGroup4Record lastVss120Record;

        public void incrementTotal() { totalRecords++; }
        public void incrementValid() { validRecords++; }
        public void incrementInvalid() { invalidRecords++; }

        public void addError(int lineNumber, String error) {
            errors.add("Line " + lineNumber + ": " + error);
        }

        public int getTotalRecords() { return totalRecords; }
        public int getValidRecords() { return validRecords; }
        public int getInvalidRecords() { return invalidRecords; }
        public List<String> getErrors() { return errors; }

        public EpinFileHeader getHeader() { return header; }
        public void setHeader(EpinFileHeader header) { this.header = header; }
        public void setLastVss120Record(VssSubGroup4Record lastVss120Record) { this.lastVss120Record = lastVss120Record; }
        public VssSubGroup4Record getLastVss120Record() { return lastVss120Record; }
    }


    public ProcessingStatisticsDto getStatistics() {
        List<FileProcessingJob> allJobs = jobRepository.findAll();

        long totalJobs = allJobs.size();
        long completedJobs = jobRepository.countByStatus(ProcessingStatus.COMPLETED);
        long failedJobs = jobRepository.countByStatus(ProcessingStatus.FAILED);
        long activeJobs = jobRepository.countByStatus(ProcessingStatus.PROCESSING);
        double successRate = totalJobs > 0 ? (double) completedJobs / totalJobs * 100 : 0.0;

        double averageProcessingTime = allJobs.stream()
                .filter(j -> j.getProcessingStartedAt() != null && j.getProcessingCompletedAt() != null)
                .mapToLong(j -> java.time.Duration.between(j.getProcessingStartedAt(), j.getProcessingCompletedAt()).getSeconds())
                .average()
                .orElse(0.0);

        double averageRecordsPerJob = allJobs.stream()
                .filter(j -> j.getTotalRecords() != null)
                .mapToInt(FileProcessingJob::getTotalRecords)
                .average()
                .orElse(0.0);

        int largestJobRecords = allJobs.stream()
                .filter(j -> j.getTotalRecords() != null)
                .mapToInt(FileProcessingJob::getTotalRecords)
                .max()
                .orElse(0);

        int smallestJobRecords = allJobs.stream()
                .filter(j -> j.getTotalRecords() != null)
                .mapToInt(FileProcessingJob::getTotalRecords)
                .min()
                .orElse(0);


        Map<String, Long> statusDistribution = allJobs.stream()
                .collect(Collectors.groupingBy(
                        job -> job.getStatus() != null ? job.getStatus().name() : "UNKNOWN",
                        Collectors.counting()
                ));

        Page<FileProcessingJob> recentJobsPage = jobRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(0, 5)
        );
        List<FileProcessingJobDto> recentJobs = recentJobsPage.getContent().stream()
                .map(FileProcessingJobDto::toDto)
                .toList();

        return new ProcessingStatisticsDto(
                totalJobs,
                activeJobs,
                completedJobs,
                failedJobs,
                successRate,
                averageProcessingTime,
                averageRecordsPerJob,
                largestJobRecords,
                smallestJobRecords,
                statusDistribution,
                recentJobs
        );
    }
}