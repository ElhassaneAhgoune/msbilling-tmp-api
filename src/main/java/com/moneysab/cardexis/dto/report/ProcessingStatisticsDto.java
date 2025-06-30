package com.moneysab.cardexis.dto.report;

import com.moneysab.cardexis.dto.FileProcessingJobDto;

import java.util.List;
import java.util.Map;

public record ProcessingStatisticsDto(
        long totalJobs,
        long activeJobs,
        long completedJobs,
        long failedJobs,
        double successRate,
        double averageProcessingTimeSeconds,
        double averageRecordsPerJob,
        int largestJobRecords,
        int smallestJobRecords,
        Map<String, Long> jobStatusDistribution,
        List<FileProcessingJobDto> recentJobs
) {
}
