package com.moneysab.cardexis.dto.report;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * DTO for country and channel revenue reporting data.
 */
@Schema(description = "Country and channel revenue reporting data")
public class CountryChannelRevenueDto {

    @Schema(description = "Country code")
    private String countryCode;

    @Schema(description = "Country name")
    private String countryName;

    @Schema(description = "Channel identifier")
    private String channelId;

    @Schema(description = "Channel name")
    private String channelName;

    @Schema(description = "Total revenue for the country and channel")
    private BigDecimal totalRevenue;

    @Schema(description = "Total number of transactions")
    private Integer transactionCount;

    // Constructors
    public CountryChannelRevenueDto() {
    }

    public CountryChannelRevenueDto(String countryCode, String countryName, String channelId, 
                                   String channelName, BigDecimal totalRevenue, Integer transactionCount) {
        this.countryCode = countryCode;
        this.countryName = countryName;
        this.channelId = channelId;
        this.channelName = channelName;
        this.totalRevenue = totalRevenue;
        this.transactionCount = transactionCount;
    }

    // Getters and setters
    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
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