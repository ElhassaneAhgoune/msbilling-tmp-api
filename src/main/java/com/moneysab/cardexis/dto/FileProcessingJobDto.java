package com.moneysab.cardexis.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.moneysab.cardexis.domain.entity.FileProcessingJob;

import java.time.LocalDateTime;
import java.util.UUID;

public record FileProcessingJobDto(
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        UUID id,
        String originalFilename,
        String fileType,
        String status,
        String reportFormat,
        String clientId,
        Long fileSizeBytes,
        Integer totalRecords,
        Integer processedRecords,
        Integer failedRecords,
        LocalDateTime processingStartedAt,
        LocalDateTime processingCompletedAt,
        String errorMessage,
        Integer retryCount,
        Integer maxRetries,
        double successRate
) {
    public static FileProcessingJobDto toDto(FileProcessingJob job) {
        if (job == null) return null;

        return new FileProcessingJobDto(
                job.getId(),
                job.getOriginalFilename(),
                job.getFileType() != null ? job.getFileType().name() : null,
                job.getStatus() != null ? job.getStatus().name() : null,
                job.getReportFormat() != null ? job.getReportFormat().name() : null,
                job.getClientId(),
                job.getFileSizeBytes(),
                job.getTotalRecords(),
                job.getProcessedRecords(),
                job.getFailedRecords(),
                job.getProcessingStartedAt(),
                job.getProcessingCompletedAt(),
                job.getErrorMessage(),
                job.getRetryCount(),
                job.getMaxRetries(),
                job.getSuccessRate()
        );
    }
}