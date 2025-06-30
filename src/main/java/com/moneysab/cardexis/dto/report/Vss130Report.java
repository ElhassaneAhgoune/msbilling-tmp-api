package com.moneysab.cardexis.dto.report;

import java.math.BigDecimal;
import java.util.List;

public record Vss130Report(
        List<BusinessModeReport> businessModes
) {

    public record BusinessModeReport(
            String businessMode,
            List<TransactionTypeReport> transactionTypes,
            Long totalCount,
            BigDecimal totalClearingAmount,
            BigDecimal totalReimbursementFeeCredits,
            BigDecimal totalReimbursementFeeDebits,
            BigDecimal netAmount,
            String amountSign
    ) {}

    public record TransactionTypeReport(
            String transactionType,
            List<TransactionCycleReport> cycles,
            Long totalCount,
            BigDecimal totalClearingAmount,
            BigDecimal totalReimbursementFeeCredits,
            BigDecimal totalReimbursementFeeDebits,
            BigDecimal netAmount,
            String amountSign
    ) {}

    public record TransactionCycleReport(
            String transactionCycle,
            String jurisdiction,
            String routing,
            String feeLevelDescription,

            Long count,
            BigDecimal clearingAmount,
            BigDecimal reimbursementFeeCredits,
            BigDecimal reimbursementFeeDebits,
            BigDecimal netAmount,
            String amountSign
    ) {}
}

