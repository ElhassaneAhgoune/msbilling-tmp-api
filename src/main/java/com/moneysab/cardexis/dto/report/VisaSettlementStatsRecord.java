package com.moneysab.cardexis.dto.report;

import java.math.BigDecimal;

public record VisaSettlementStatsRecord(
        Section interchangeValue,
        Section reimbursementFees,
        Section visaCharges,
        Section total
) {
    public record Section(
            StatLine acquirer,
            StatLine issuer,
            StatLine other,
            StatLine total
    ) {}

    public record StatLine(
            int creditCount,
            BigDecimal creditAmount,
            BigDecimal debitAmount,
            BigDecimal totalAmount,
            String totalAmountSign
    ) {}
}