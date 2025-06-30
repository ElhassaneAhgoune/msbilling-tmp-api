package com.moneysab.cardexis.dto.report;

import java.math.BigDecimal;
import java.util.List;

public record Vss120Report(
        List<BusinessModeReport> businessModes
) {

    public record BusinessModeReport(
            String businessMode,
            List<TransactionTypeReport> transactionTypes,
            Long totalCount,
            BigDecimal totalClearingAmount,
            BigDecimal totalInterchangeCredits,
            BigDecimal totalInterchangeDebits,
            BigDecimal netAmount,
            String amountSign
    ) {}

    public record TransactionTypeReport(
            String transactionType,
            List<TransactionCycleReport> cycles,
            Long totalCount,
            BigDecimal totalClearingAmount,
            BigDecimal totalInterchangeCredits,
            BigDecimal totalInterchangeDebits,
            BigDecimal netAmount,
            String amountSign
    ) {}

    public record TransactionCycleReport(
            String transactionCycle,
            String rateTableId,
            Long count,
            BigDecimal clearingAmount,
            BigDecimal interchangeCredits,
            BigDecimal interchangeDebits,
            BigDecimal netAmount,
            String amountSign
    ) {}
}
