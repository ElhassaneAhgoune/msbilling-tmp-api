package com.moneysab.cardexis.dto.report;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * DTO for BIN revenue reporting data.
 */
@Schema(description = "BIN revenue reporting data")
public class BinRevenueDto {

    @Schema(description = "Bank Identification Number (BIN)")
    private String bin;

    @Schema(description = "Total revenue for the BIN")
    private BigDecimal totalRevenue;

    @Schema(description = "Total number of transactions for the BIN")
    private Integer transactionCount;

    // Constructors
    public BinRevenueDto() {
    }

    public BinRevenueDto(String bin, BigDecimal totalRevenue, Integer transactionCount) {
        this.bin = bin;
        this.totalRevenue = totalRevenue;
        this.transactionCount = transactionCount;
    }

    // Getters and setters
    public String getBin() {
        return bin;
    }

    public void setBin(String bin) {
        this.bin = bin;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public Integer getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(Integer transactionCount) {
        this.transactionCount = transactionCount;
    }
}