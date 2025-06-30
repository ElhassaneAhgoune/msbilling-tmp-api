package com.moneysab.cardexis.dto.report;

import java.math.BigDecimal;
import java.util.List;

public record Vss140Report(
        List<BusinessModeReport> businessModes
) {

    public record BusinessModeReport(
            String businessMode,
            List<ChargeTypeReport> chargeTypes,
            Long totalCount,
            BigDecimal totalInterchangeAmount,
            BigDecimal totalVisaChargesCredits,
            BigDecimal totalVisaChargesDebits,
            BigDecimal netAmount,
            String amountSign
    ) {}

    public record ChargeTypeReport(
            String chargeTypeCode,
            List<TransactionTypeReport> transactionTypes,
            Long totalCount,
            BigDecimal totalInterchangeAmount,
            BigDecimal totalVisaChargesCredits,
            BigDecimal totalVisaChargesDebits,
            BigDecimal netAmount,
            String amountSign
    ) {}

    public record TransactionTypeReport(
            String transactionType,
            List<TransactionCycleReport> cycles,
            Long totalCount,
            BigDecimal totalInterchangeAmount,
            BigDecimal totalVisaChargesCredits,
            BigDecimal totalVisaChargesDebits,
            BigDecimal netAmount,
            String amountSign
    ) {}

    public record TransactionCycleReport(
            String transactionCycle,
            List<JurisdictionReport> jurisdictions,
            Long totalCount,
            BigDecimal totalInterchangeAmount,
            BigDecimal totalVisaChargesCredits,
            BigDecimal totalVisaChargesDebits,
            BigDecimal netAmount,
            String amountSign
    ) {}

    public record JurisdictionReport(
            String jurisdictionCode,
            List<RoutingReport> routings,
            Long totalCount,
            BigDecimal totalInterchangeAmount,
            BigDecimal totalVisaChargesCredits,
            BigDecimal totalVisaChargesDebits,
            BigDecimal netAmount,
            String amountSign
    ) {}

    public record RoutingReport(
            String routing,
            Long count,
            BigDecimal interchangeAmount,
            BigDecimal visaChargesCredits,
            BigDecimal visaChargesDebits,
            BigDecimal netAmount,
            String amountSign
    ) {}
}
