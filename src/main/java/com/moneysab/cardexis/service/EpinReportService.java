package com.moneysab.cardexis.service;

import com.moneysab.cardexis.domain.entity.Vss120SettlementRecord;
import com.moneysab.cardexis.dto.report.*;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/**
 * Service interface for EPIN reporting operations.
 * 
 * This service provides methods for generating various reports related to EPIN transactions,
 * including revenue reports by BIN, country/channel, issuer KPIs, and data exports.
 */
public interface EpinReportService {

    /**
     * Get revenues grouped by BIN with optional date filtering.
     *
     * @param startDate Optional start date for filtering (inclusive)
     * @param endDate Optional end date for filtering (inclusive)
     * @param pageable Pagination information
     * @return Page of BIN revenue data
     */
    Page<BinRevenueDto> getBinRevenues(LocalDate startDate, LocalDate endDate, Pageable pageable);

    /**
     * Get revenues grouped by country and channel with optional date filtering.
     *
     * @param startDate Optional start date for filtering (inclusive)
     * @param endDate Optional end date for filtering (inclusive)
     * @param pageable Pagination information
     * @return Page of country and channel revenue data
     */
    Page<CountryChannelRevenueDto> getCountryChannelRevenues(LocalDate startDate, LocalDate endDate, Pageable pageable);

    /**
     * Get KPIs grouped by issuer with optional date filtering.
     *
     * @param startDate Optional start date for filtering (inclusive)
     * @param endDate Optional end date for filtering (inclusive)
     * @param pageable Pagination information
     * @return Page of issuer KPI data
     */
    Page<IssuerKpiDto> getIssuerKpis(LocalDate startDate, LocalDate endDate, Pageable pageable);

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
    Resource exportReportData(String format, LocalDate startDate, LocalDate endDate, 
                             String bin, String country);

    /**
     * Get KPIs .
     *

     * @param spec Currency Code startDate endDate
     * @return VisaSettlementStatsRecord containing settlement statistics
     */
    VisaSettlementStatsRecord getVisaSettlementStats(Specification spec);

    Vss120Report getVss120Report(String currencyCode, LocalDate startDate , LocalDate endDate,String binCode);

    Vss130Report getVss130Report(String currencyCode, LocalDate startDate , LocalDate endDate,String binCode);

    Vss140Report getVss140Report(String currencyCode, LocalDate startDate , LocalDate endDate,String binCode);

}