package com.moneysab.cardexis.dto.report;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * DTO for issuer KPI reporting data.
 */
@Schema(description = "Issuer KPI reporting data")
public class IssuerKpiDto {

    @Schema(description = "Issuer identifier")
    private String issuerId;

    @Schema(description = "Issuer name")
    private String issuerName;

    @Schema(description = "Total transaction amount")
    private BigDecimal totalTransactionAmount;

    @Schema(description = "Number of TCRs (Transaction Clearing Records)")
    private Integer tcrCount;

    @Schema(description = "Success rate (percentage)")
    private BigDecimal successRate;

    @Schema(description = "Average transaction amount")
    private BigDecimal averageTransactionAmount;

    @Schema(description = "Total number of transactions")
    private Integer transactionCount;

    // Constructors
    public IssuerKpiDto() {
    }

    public IssuerKpiDto(String issuerId, String issuerName, BigDecimal totalTransactionAmount,
                       Integer tcrCount, BigDecimal successRate, BigDecimal averageTransactionAmount,
                       Integer transactionCount) {
        this.issuerId = issuerId;
        this.issuerName = issuerName;
        this.totalTransactionAmount = totalTransactionAmount;
        this.tcrCount = tcrCount;
        this.successRate = successRate;
        this.averageTransactionAmount = averageTransactionAmount;
        this.transactionCount = transactionCount;
    }

    // Getters and setters
    public String getIssuerId() {
        return issuerId;
    }

    public void setIssuerId(String issuerId) {
        this.issuerId = issuerId;
    }

    public String getIssuerName() {
        return issuerName;
    }

    public void setIssuerName(String issuerName) {
        this.issuerName = issuerName;
    }

    public BigDecimal getTotalTransactionAmount() {
        return totalTransactionAmount;
    }

    public void setTotalTransactionAmount(BigDecimal totalTransactionAmount) {
        this.totalTransactionAmount = totalTransactionAmount;
    }

    public Integer getTcrCount() {
        return tcrCount;
    }

    public void setTcrCount(Integer tcrCount) {
        this.tcrCount = tcrCount;
    }

    public BigDecimal getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(BigDecimal successRate) {
        this.successRate = successRate;
    }

    public BigDecimal getAverageTransactionAmount() {
        return averageTransactionAmount;
    }

    public void setAverageTransactionAmount(BigDecimal averageTransactionAmount) {
        this.averageTransactionAmount = averageTransactionAmount;
    }

    public Integer getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(Integer transactionCount) {
        this.transactionCount = transactionCount;
    }
}