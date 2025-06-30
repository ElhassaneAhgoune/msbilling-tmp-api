package com.moneysab.cardexis.service;

import com.moneysab.cardexis.domain.entity.FileProcessingJob;
import com.moneysab.cardexis.service.impl.EpinFileProcessingService;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IEpinFileProcessingService  {

   /* @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class, timeout = 60)
    FileProcessingJob processEpinFile(String originalFilename, String fileContent, Long fileSizeBytes);

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class, timeout = 30)
    EpinFileProcessingService.ProcessingBatchResult processBatch(FileProcessingJob job, List<String> lines, int startLineNumber);

    // Other existing methods remain the same...
    Optional<FileProcessingJob> getProcessingJob(UUID jobId);

    List<FileProcessingJob> getProcessingJobsForClient(String clientId);

    FileProcessingJob retryProcessingJob(UUID jobId, String fileContent);*/
}
