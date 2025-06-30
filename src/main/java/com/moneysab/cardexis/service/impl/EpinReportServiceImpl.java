package com.moneysab.cardexis.service.impl;

import com.moneysab.cardexis.domain.entity.*;
import com.moneysab.cardexis.dto.report.*;
import com.moneysab.cardexis.repository.*;
import com.moneysab.cardexis.service.EpinReportService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of the EPIN reporting service.
 * 
 * This service provides methods for generating various reports related to EPIN transactions,
 * including revenue reports by BIN, country/channel, issuer KPIs, and data exports.
 */
@Service
@Transactional(readOnly = true)
public class EpinReportServiceImpl implements EpinReportService {

    private static final Logger logger = LoggerFactory.getLogger(EpinReportServiceImpl.class);

    @Autowired
    private Vss110SettlementRecordRepository vss110Repository;

    @Autowired
    private Vss120SettlementRecordRepository vss120Repository;

    @Autowired
    private Vss130SettlementRecordRepository vss130Repository;

    @Autowired
    private Vss140SettlementRecordRepository vss140Repository;

    @Autowired
    private BusinessTransactionTypeRepository transactionTypeRepository;

    @Autowired
    private ChargeTypesRepository chargeTypesRepository;

    @Autowired
    private CountriesRepository countriesRepository;


    /**
     * {@inheritDoc}
     */
    @Override
    public Page<BinRevenueDto> getBinRevenues(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        logger.info("Generating BIN revenue report from {} to {}", startDate, endDate);
        
        // Adjust dates if not provided
        LocalDate effectiveStartDate = startDate != null ? startDate : LocalDate.now().minusMonths(1);
        LocalDate effectiveEndDate = endDate != null ? endDate : LocalDate.now();
        
        // Execute query to get BIN revenue data
        List<Object[]> binRevenueData = vss110Repository.findAll().stream()
            .filter(record -> {
                LocalDate recordDate = record.getSettlementDate();
                return recordDate != null &&
                       !recordDate.isBefore(effectiveStartDate) &&
                       !recordDate.isAfter(effectiveEndDate);
            })
            .filter(record -> record.getDestinationId() != null && record.getDestinationId().length() >= 6)
            // Extract BIN (first 6 digits of destination ID)
            .collect(Collectors.groupingBy(
                record -> record.getDestinationId().substring(0, 6),
                Collectors.toList()
            ))
            .entrySet().stream()
            .map(entry -> {
                String bin = entry.getKey();
                List<Vss110SettlementRecord> records = entry.getValue();
                
                // Calculate totals for this BIN
                BigDecimal totalRevenue = records.stream()
                    .filter(r -> r.getNetAmount() != null)
                    .map(Vss110SettlementRecord::getNetAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                Integer transactionCount = records.stream()
                    .filter(r -> r.getTransactionCount() != null)
                    .mapToInt(Vss110SettlementRecord::getTransactionCount)
                    .sum();
                
                return new Object[] { bin, totalRevenue, transactionCount };
            })
            .collect(Collectors.toList());
        
        // Convert to DTOs
        List<BinRevenueDto> results = binRevenueData.stream()
            .map(data -> new BinRevenueDto(
                (String) data[0],
                (BigDecimal) data[1],
                (Integer) data[2]
            ))
            .collect(Collectors.toList());
        
        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), results.size());
        
        // Handle case where start might be out of bounds
        if (start >= results.size()) {
            return new PageImpl<>(new ArrayList<>(), pageable, results.size());
        }
        
        List<BinRevenueDto> pageContent = results.subList(start, end);
        return new PageImpl<>(pageContent, pageable, results.size());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<CountryChannelRevenueDto> getCountryChannelRevenues(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        logger.info("Generating country/channel revenue report from {} to {}", startDate, endDate);
        
        // Adjust dates if not provided
        LocalDate effectiveStartDate = startDate != null ? startDate : LocalDate.now().minusMonths(1);
        LocalDate effectiveEndDate = endDate != null ? endDate : LocalDate.now();
        
        // Map to store country codes to names
        Map<String, String> countryNames = new HashMap<>();
        countryNames.put("MA", "Morocco");
        countryNames.put("FR", "France");
        countryNames.put("US", "United States");
        countryNames.put("GB", "United Kingdom");
        countryNames.put("DE", "Germany");
        // Add more country mappings as needed
        
        // Map to store channel IDs to names
        Map<String, String> channelNames = new HashMap<>();
        channelNames.put("WEB", "Web Channel");
        channelNames.put("MOB", "Mobile Channel");
        channelNames.put("POS", "Point of Sale");
        channelNames.put("ATM", "ATM Network");
        // Add more channel mappings as needed
        
        // Execute query to get country/channel revenue data
        // In a real implementation, this would use a repository method with proper JPA query
        // For this example, we'll simulate the data grouping
        
        List<Object[]> countryChannelData = vss110Repository.findAll().stream()
            .filter(record -> {
                LocalDate recordDate = record.getSettlementDate();
                return recordDate != null &&
                       !recordDate.isBefore(effectiveStartDate) &&
                       !recordDate.isAfter(effectiveEndDate);
            })
            .filter(record -> record.getDestinationId() != null && record.getDestinationId().length() >= 2)
            // Extract country code (first 2 characters of destination ID) and channel (next 3 characters)
            .collect(Collectors.groupingBy(
                record -> {
                    String countryCode = record.getDestinationId().substring(0, 2);
                    String channelId = record.getDestinationId().length() >= 5 ?
                                      record.getDestinationId().substring(2, 5) : "UNK";
                    return countryCode + ":" + channelId;
                },
                Collectors.toList()
            ))
            .entrySet().stream()
            .map(entry -> {
                String[] parts = entry.getKey().split(":");
                String countryCode = parts[0];
                String channelId = parts.length > 1 ? parts[1] : "UNK";
                List<Vss110SettlementRecord> records = entry.getValue();
                
                // Calculate totals for this country/channel combination
                BigDecimal totalRevenue = records.stream()
                    .filter(r -> r.getNetAmount() != null)
                    .map(Vss110SettlementRecord::getNetAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                Integer transactionCount = records.stream()
                    .filter(r -> r.getTransactionCount() != null)
                    .mapToInt(Vss110SettlementRecord::getTransactionCount)
                    .sum();
                
                return new Object[] {
                    countryCode,
                    countryNames.getOrDefault(countryCode, "Unknown Country"),
                    channelId,
                    channelNames.getOrDefault(channelId, "Unknown Channel"),
                    totalRevenue,
                    transactionCount
                };
            })
            .collect(Collectors.toList());
        
        // Convert to DTOs
        List<CountryChannelRevenueDto> results = countryChannelData.stream()
            .map(data -> new CountryChannelRevenueDto(
                (String) data[0],
                (String) data[1],
                (String) data[2],
                (String) data[3],
                (BigDecimal) data[4],
                (Integer) data[5]
            ))
            .collect(Collectors.toList());
        
        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), results.size());
        
        // Handle case where start might be out of bounds
        if (start >= results.size()) {
            return new PageImpl<>(new ArrayList<>(), pageable, results.size());
        }
        
        List<CountryChannelRevenueDto> pageContent = results.subList(start, end);
        return new PageImpl<>(pageContent, pageable, results.size());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<IssuerKpiDto> getIssuerKpis(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        logger.info("Generating issuer KPI report from {} to {}", startDate, endDate);
        
        // Adjust dates if not provided
        LocalDate effectiveStartDate = startDate != null ? startDate : LocalDate.now().minusMonths(1);
        LocalDate effectiveEndDate = endDate != null ? endDate : LocalDate.now();
        
        // Map to store issuer IDs to names
        Map<String, String> issuerNames = new HashMap<>();
        issuerNames.put("ISS001", "Issuer One");
        issuerNames.put("ISS002", "Issuer Two");
        issuerNames.put("ISS003", "Issuer Three");
        // Add more issuer mappings as needed
        
        // Execute query to get issuer KPI data
        // In a real implementation, this would use a repository method with proper JPA query
        // For this example, we'll simulate the data grouping
        
        List<Object[]> issuerKpiData = vss110Repository.findAll().stream()
            .filter(record -> {
                LocalDate recordDate = record.getSettlementDate();
                return recordDate != null &&
                       !recordDate.isBefore(effectiveStartDate) &&
                       !recordDate.isAfter(effectiveEndDate);
            })
            .filter(record -> record.getReportingSreId() != null)
            // Group by issuer ID (reporting SRE ID)
            .collect(Collectors.groupingBy(
                Vss110SettlementRecord::getReportingSreId,
                Collectors.toList()
            ))
            .entrySet().stream()
            .map(entry -> {
                String issuerId = entry.getKey();
                List<Vss110SettlementRecord> records = entry.getValue();
                
                // Calculate KPIs for this issuer
                BigDecimal totalTransactionAmount = records.stream()
                    .filter(r -> r.getNetAmount() != null)
                    .map(Vss110SettlementRecord::getNetAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                Integer tcrCount = records.size();
                
                Integer transactionCount = records.stream()
                    .filter(r -> r.getTransactionCount() != null)
                    .mapToInt(Vss110SettlementRecord::getTransactionCount)
                    .sum();
                
                // Calculate success rate (valid records / total records)
                long validRecords = records.stream()
                    .filter(r -> r.getIsValid() != null && r.getIsValid())
                    .count();
                
                BigDecimal successRate = BigDecimal.valueOf(validRecords)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(tcrCount), 2, RoundingMode.HALF_UP);
                
                // Calculate average transaction amount
                BigDecimal averageTransactionAmount = transactionCount > 0 ?
                    totalTransactionAmount.divide(BigDecimal.valueOf(transactionCount), 2, RoundingMode.HALF_UP) :
                    BigDecimal.ZERO;
                
                return new Object[] {
                    issuerId,
                    issuerNames.getOrDefault(issuerId, "Unknown Issuer"),
                    totalTransactionAmount,
                    tcrCount,
                    successRate,
                    averageTransactionAmount,
                    transactionCount
                };
            })
            .collect(Collectors.toList());
        
        // Convert to DTOs
        List<IssuerKpiDto> results = issuerKpiData.stream()
            .map(data -> new IssuerKpiDto(
                (String) data[0],
                (String) data[1],
                (BigDecimal) data[2],
                (Integer) data[3],
                (BigDecimal) data[4],
                (BigDecimal) data[5],
                (Integer) data[6]
            ))
            .collect(Collectors.toList());
        
        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), results.size());
        
        // Handle case where start might be out of bounds
        if (start >= results.size()) {
            return new PageImpl<>(new ArrayList<>(), pageable, results.size());
        }
        
        List<IssuerKpiDto> pageContent = results.subList(start, end);
        return new PageImpl<>(pageContent, pageable, results.size());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Resource exportReportData(String format, LocalDate startDate, LocalDate endDate,
                                    String bin, String country) {
        logger.info("Exporting report data in {} format from {} to {}", format, startDate, endDate);
        logger.info("Filters - BIN: {}, Country: {}", bin, country);
        
        // Adjust dates if not provided
        LocalDate effectiveStartDate = startDate != null ? startDate : LocalDate.now().minusMonths(1);
        LocalDate effectiveEndDate = endDate != null ? endDate : LocalDate.now();
        
        // Get filtered data
        List<Vss110SettlementRecord> filteredRecords = vss110Repository.findAll().stream()
            .filter(record -> {
                LocalDate recordDate = record.getSettlementDate();
                return recordDate != null &&
                       !recordDate.isBefore(effectiveStartDate) &&
                       !recordDate.isAfter(effectiveEndDate);
            })
            .filter(record -> {
                // Apply BIN filter if provided
                if (bin != null && !bin.isEmpty() && record.getDestinationId() != null) {
                    return record.getDestinationId().startsWith(bin);
                }
                return true;
            })
            .filter(record -> {
                // Apply country filter if provided
                if (country != null && !country.isEmpty() && record.getDestinationId() != null &&
                    record.getDestinationId().length() >= 2) {
                    return record.getDestinationId().substring(0, 2).equalsIgnoreCase(country);
                }
                return true;
            })
            .collect(Collectors.toList());
        
        // Generate export based on format
        if ("csv".equalsIgnoreCase(format)) {
            return generateCsvExport(filteredRecords);
        } else if ("excel".equalsIgnoreCase(format)) {
            return generateExcelExport(filteredRecords);
        } else {
            throw new IllegalArgumentException("Unsupported export format: " + format);
        }
    }
    
    /**
     * Generates a CSV export from the provided records.
     *
     * @param records the records to export
     * @return a Resource containing the CSV data
     */
    private Resource generateCsvExport(List<Vss110SettlementRecord> records) {
        StringBuilder csv = new StringBuilder();
        
        // Add CSV header
        csv.append("BIN,SettlementDate,CurrencyCode,AmountType,BusinessMode,TransactionCount,CreditAmount,DebitAmount,NetAmount\n");
        
        // Add data rows
        for (Vss110SettlementRecord record : records) {
            String bin = record.getDestinationId() != null && record.getDestinationId().length() >= 6 ?
                        record.getDestinationId().substring(0, 6) : "";
            
            csv.append(bin).append(",")
               .append(record.getSettlementDate()).append(",")
               .append(record.getCurrencyCode()).append(",")
               .append(record.getAmountType()).append(",")
               .append(record.getBusinessMode()).append(",")
               .append(record.getTransactionCount()).append(",")
               .append(record.getCreditAmount()).append(",")
               .append(record.getDebitAmount()).append(",")
               .append(record.getNetAmount()).append("\n");
        }
        
        return new ByteArrayResource(csv.toString().getBytes());
    }
    
    /**
     * Generates an Excel-like export from the provided records using CSV format.
     *
     * @param records the records to export
     * @return a Resource containing the Excel-like data
     */
    private Resource generateExcelExport(List<Vss110SettlementRecord> records) {
        // For simplicity, we'll use CSV format with Excel-compatible formatting
        // In a real implementation, you would use Apache POI for proper Excel files
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            CSVPrinter csvPrinter = new CSVPrinter(
                new java.io.OutputStreamWriter(outputStream),
                CSVFormat.EXCEL.withHeader(
                    "BIN", "Settlement Date", "Currency Code", "Amount Type", "Business Mode",
                    "Transaction Count", "Credit Amount", "Debit Amount", "Net Amount"
                )
            );
            
            // Add data rows
            for (Vss110SettlementRecord record : records) {
                String bin = record.getDestinationId() != null && record.getDestinationId().length() >= 6 ?
                            record.getDestinationId().substring(0, 6) : "";
                
                csvPrinter.printRecord(
                    bin,
                    record.getSettlementDate(),
                    record.getCurrencyCode(),
                    record.getAmountType(),
                    record.getBusinessMode(),
                    record.getTransactionCount(),
                    record.getCreditAmount(),
                    record.getDebitAmount(),
                    record.getNetAmount()
                );
            }
            
            csvPrinter.flush();
            csvPrinter.close();
            
            return new ByteArrayResource(outputStream.toByteArray());
            
        } catch (IOException e) {
            logger.error("Error generating Excel-like export: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate Excel-like export", e);
        }
    }
   @Override
    public VisaSettlementStatsRecord getVisaSettlementStats(Specification spec) {
        List<Vss110SettlementRecord> rows = vss110Repository.findAll(spec);


        Map<String, Map<String, List<Vss110SettlementRecord>>> grouped = rows.stream()
                .collect(Collectors.groupingBy(
                        Vss110SettlementRecord::getAmountType,
                        Collectors.groupingBy(Vss110SettlementRecord::getBusinessMode)
                ));
       // On groupe par amountType puis par businessMode
        return new VisaSettlementStatsRecord(
                buildSection(grouped.get("I")),
                buildSection(grouped.get("F")),
                buildSection(grouped.get("C")),
                buildSection(grouped.get("T"))
        );
    }

    private VisaSettlementStatsRecord.Section buildSection(Map<String, List<Vss110SettlementRecord>> map) {
        if (map == null) return emptySection();

        return new VisaSettlementStatsRecord.Section(
                aggregateStatLine(map.get("1")), // Acquirer
                aggregateStatLine(map.get("2")), // Issuer
                aggregateStatLine(map.get("3")), // Other
                aggregateStatLine(map.get("9"))  // Total
        );
    }

    private VisaSettlementStatsRecord.StatLine aggregateStatLine(List<Vss110SettlementRecord> list) {
        if (list == null || list.isEmpty()) {
            return new VisaSettlementStatsRecord.StatLine(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "");
        }

        int creditCount = list.stream().mapToInt(e -> e.getTransactionCount() != null ? e.getTransactionCount() : 0).sum();
        BigDecimal creditAmount = list.stream()
                .map(e -> e.getCreditAmount() != null ? e.getCreditAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal debitAmount = list.stream()
                .map(e -> e.getDebitAmount() != null ? e.getDebitAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAmount = list.stream()
                .map(e -> {
                    BigDecimal val = e.getNetAmount() != null ? e.getNetAmount() : BigDecimal.ZERO;
                    return "DB".equalsIgnoreCase(e.getAmountSign()) ? val.negate() : val;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String sign =  totalAmount.compareTo(BigDecimal.ZERO) > 0 ? "CR" :
                       totalAmount.compareTo(BigDecimal.ZERO) < 0 ? "DB" : "";

        return new VisaSettlementStatsRecord.StatLine(creditCount, creditAmount, debitAmount, totalAmount.abs(), sign);
    }
    private VisaSettlementStatsRecord.Section emptySection() {
        VisaSettlementStatsRecord.StatLine empty = new VisaSettlementStatsRecord.StatLine(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "");
        return new VisaSettlementStatsRecord.Section(empty, empty, empty, empty);
    }

    @Override
    public Vss120Report getVss120Report(String currencyCode, LocalDate startDate , LocalDate endDate,String binCode) {
        List<Object[]> rawRows = vss120Repository.findAllWithTcr1(currencyCode,startDate,endDate,binCode);

        Map<String, String> codeToLabel = transactionTypeRepository.findAll()
                .stream()
                .collect(Collectors.toMap(BusinessTransactionType::getCode, BusinessTransactionType::getLabel));

        Map<String, String> codeToBusinessModeLabel = Map.of(
                "1", "Acquirer",
                "2", "Issuer",
                "3", "Other"
        );

        Map<String, String> cycleCodeToLabel = Map.of(
                "0", "The transaction does not have a cycle",
                "1", "Originals",
                "2", "Chargebacks",
                "3", "Representments",
                "4", "Second Chargebacks",
                "5", "Debit Adjustments",
                "6", "Credit Adjustments",
                "7", "Dispute Financial",
                "8", "Dispute Response Financial"
        );


        Map<String, Map<String, List<Object[]>>> grouped = rawRows.stream()
                .collect(Collectors.groupingBy(
                        row -> ((Vss120SettlementRecord) row[0]).getBusinessMode(),
                        Collectors.groupingBy(
                                row -> ((Vss120SettlementRecord) row[0]).getBusinessTransactionType()
                        )
                ));

        List<Vss120Report.BusinessModeReport> businessModes = new ArrayList<>();

        for (var bmEntry : grouped.entrySet()) {
            String businessMode = bmEntry.getKey();
            var transactionTypesMap = bmEntry.getValue();

            List<Vss120Report.TransactionTypeReport> transactionTypes = new ArrayList<>();

            long bmTotalCount = 0;
            BigDecimal bmTotalClearingAmount = BigDecimal.ZERO;
            BigDecimal bmTotalInterchangeCredits = BigDecimal.ZERO;
            BigDecimal bmTotalInterchangeDebits = BigDecimal.ZERO;

            for (var ttEntry : transactionTypesMap.entrySet()) {
                String transactionType = ttEntry.getKey();
                List<Object[]> rows = ttEntry.getValue();

                List<Vss120Report.TransactionCycleReport> cycles = new ArrayList<>();

                long ttTotalCount = 0;
                BigDecimal ttTotalClearingAmount = BigDecimal.ZERO;
                BigDecimal ttTotalInterchangeCredits = BigDecimal.ZERO;
                BigDecimal ttTotalInterchangeDebits = BigDecimal.ZERO;

                for (Object[] row : rows) {
                    Vss120Tcr1Record tcr1 = (Vss120Tcr1Record) row[1];

                    long count = Optional.ofNullable(tcr1.getFirstCount()).orElse(0L);

                    BigDecimal clearingAmount = Optional.ofNullable(tcr1.getFirstAmount()).orElse(BigDecimal.ZERO);
                    if ("DB".equalsIgnoreCase(Optional.ofNullable(tcr1.getFirstAmountSign()).orElse("CR"))) {
                        clearingAmount = clearingAmount.negate();
                    }

                    BigDecimal secondAmount = Optional.ofNullable(tcr1.getSecondAmount()).orElse(BigDecimal.ZERO);
                    String secondSign = Optional.ofNullable(tcr1.getSecondAmountSign()).orElse("CR");

                    BigDecimal interchangeCredits = "CR".equalsIgnoreCase(secondSign) ? secondAmount : secondAmount.negate();

                    BigDecimal thirdAmount = Optional.ofNullable(tcr1.getThirdAmount()).orElse(BigDecimal.ZERO);
                    String thirdSign = Optional.ofNullable(tcr1.getThirdAmountSign()).orElse("DB");

                    BigDecimal interchangeDebits = "DB".equalsIgnoreCase(thirdSign) ? thirdAmount : thirdAmount.negate();


                    BigDecimal netAmount = interchangeCredits.subtract(interchangeDebits);
                    String amountSign = netAmount.compareTo(BigDecimal.ZERO) >= 0 ? "CR" : "DB";

                    String transactionCycle = ((Vss120SettlementRecord) row[0]).getBusinessTransactionCycle();
                    String rateTableId = tcr1.getRateTableId();

                    cycles.add(new Vss120Report.TransactionCycleReport(
                            cycleCodeToLabel.getOrDefault(transactionCycle, transactionCycle),
                            rateTableId,
                            count,
                            clearingAmount,
                            interchangeCredits,
                            interchangeDebits,
                            netAmount.abs(),
                            amountSign
                    ));


                    ttTotalCount += count;
                    ttTotalClearingAmount = ttTotalClearingAmount.add(clearingAmount);
                    ttTotalInterchangeCredits = ttTotalInterchangeCredits.add(interchangeCredits);
                    ttTotalInterchangeDebits = ttTotalInterchangeDebits.add(interchangeDebits);
                }

                BigDecimal ttNetAmount = ttTotalInterchangeCredits.subtract(ttTotalInterchangeDebits);
                String ttAmountSign = ttNetAmount.compareTo(BigDecimal.ZERO) >= 0 ? "CR" : "DB";

                transactionTypes.add(new Vss120Report.TransactionTypeReport(
                        codeToLabel.getOrDefault(transactionType, transactionType),
                        cycles,
                        ttTotalCount,
                        ttTotalClearingAmount,
                        ttTotalInterchangeCredits,
                        ttTotalInterchangeDebits,
                        ttNetAmount.abs(),
                        ttAmountSign
                ));


                bmTotalCount += ttTotalCount;
                bmTotalClearingAmount = bmTotalClearingAmount.add(ttTotalClearingAmount);
                bmTotalInterchangeCredits = bmTotalInterchangeCredits.add(ttTotalInterchangeCredits);
                bmTotalInterchangeDebits = bmTotalInterchangeDebits.add(ttTotalInterchangeDebits);
            }

            BigDecimal bmNetAmount = bmTotalInterchangeCredits.subtract(bmTotalInterchangeDebits);
            String bmAmountSign = bmNetAmount.compareTo(BigDecimal.ZERO) >= 0 ? "CR" : "DB";

            businessModes.add(new Vss120Report.BusinessModeReport(
                    codeToBusinessModeLabel.getOrDefault(businessMode, businessMode),
                    transactionTypes,
                    bmTotalCount,
                    bmTotalClearingAmount,
                    bmTotalInterchangeCredits,
                    bmTotalInterchangeDebits,
                    bmNetAmount.abs(),
                    bmAmountSign
            ));
        }

        return new Vss120Report(businessModes);
    }


    @Override
    public Vss130Report getVss130Report(String currencyCode, LocalDate startDate, LocalDate endDate,String binCode) {
        List<Object[]> rawRows = vss130Repository.findAllWithTcr1(currencyCode, startDate, endDate,binCode);

        Map<String, String> codeToLabel = transactionTypeRepository.findAll().stream()
                .collect(Collectors.toMap(BusinessTransactionType::getCode, BusinessTransactionType::getLabel));

        Map<String, String> codeToCountryName = countriesRepository.findAll()
                .stream()
                .collect(Collectors.toMap(Countries::getCountryCode, Countries::getCountryName));

        Map<String, String> codeToBusinessModeLabel = Map.of(
                "1", "Acquirer",
                "2", "Issuer",
                "3", "Other"
        );

        Map<String, String> cycleCodeToLabel = Map.of(
                "0", "The transaction does not have a cycle",
                "1", "Originals",
                "2", "Chargebacks",
                "3", "Representments",
                "4", "Second Chargebacks",
                "5", "Debit Adjustments",
                "6", "Credit Adjustments",
                "7", "Dispute Financial",
                "8", "Dispute Response Financial"
        );

        Map<String, String> regionCodeToLabel = Map.of(
                "US", "U.S.A.",
                "CA", "Canada",
                "EU", "E.U",
                "AP", "A.P",
                "LA", "L.A.C",
                "ME", "C.E.M.E.A"
        );

        Map<String, String> jurisdictionCodeToLabel = Map.ofEntries(
                Map.entry("00", "Visa International"),
                Map.entry("01", "Visa Canada"),
                Map.entry("02", "Visa CEMEA"),
                Map.entry("03", "Visa EU"),
                Map.entry("04", "Visa AP"),
                Map.entry("05", "Visa LAC"),
                Map.entry("06", "Visa USA"),
                Map.entry("07", "Plus USA"),
                Map.entry("08", "Interlink USA"),
                Map.entry("09", "Interlink International"),
                Map.entry("10", "Visa Germany"),
                Map.entry("11", "Visa UK")
        );


        Map<String, Map<String, List<Object[]>>> grouped = rawRows.stream()
                .collect(Collectors.groupingBy(
                        row -> ((Vss130SettlementRecord) row[0]).getBusinessMode(),
                        Collectors.groupingBy(
                                row -> ((Vss130SettlementRecord) row[0]).getBusinessTransactionType()
                        )
                ));

        List<Vss130Report.BusinessModeReport> businessModes = new ArrayList<>();

        for (var bmEntry : grouped.entrySet()) {
            String businessMode = bmEntry.getKey();
            var transactionTypesMap = bmEntry.getValue();

            List<Vss130Report.TransactionTypeReport> transactionTypes = new ArrayList<>();

            long bmTotalCount = 0;
            BigDecimal bmTotalClearingAmount = BigDecimal.ZERO;
            BigDecimal bmTotalReimbCredits = BigDecimal.ZERO;
            BigDecimal bmTotalReimbDebits = BigDecimal.ZERO;

            for (var ttEntry : transactionTypesMap.entrySet()) {
                String transactionType = ttEntry.getKey();
                List<Object[]> rows = ttEntry.getValue();

                List<Vss130Report.TransactionCycleReport> cycles = new ArrayList<>();

                long ttTotalCount = 0;
                BigDecimal ttTotalClearingAmount = BigDecimal.ZERO;
                BigDecimal ttTotalReimbCredits = BigDecimal.ZERO;
                BigDecimal ttTotalReimbDebits = BigDecimal.ZERO;

                for (Object[] row : rows) {
                    Vss130SettlementRecord tcr0 = (Vss130SettlementRecord) row[0];
                    Vss120Tcr1Record tcr1 = (Vss120Tcr1Record) row[1];


                    long count = Optional.ofNullable(tcr1.getFirstCount()).orElse(0L);


                    BigDecimal clearingAmount = Optional.ofNullable(tcr1.getFirstAmount()).orElse(BigDecimal.ZERO);
                    if ("DB".equalsIgnoreCase(Optional.ofNullable(tcr1.getFirstAmountSign()).orElse("CR"))) {
                        clearingAmount = clearingAmount.negate();
                    }


                    BigDecimal secondAmount = Optional.ofNullable(tcr1.getSecondAmount()).orElse(BigDecimal.ZERO);
                    String secondSign = Optional.ofNullable(tcr1.getSecondAmountSign()).orElse("CR");
                    BigDecimal reimbursementCredits = "CR".equalsIgnoreCase(secondSign) ? secondAmount : secondAmount.negate();


                    BigDecimal thirdAmount = Optional.ofNullable(tcr1.getThirdAmount()).orElse(BigDecimal.ZERO);
                    String thirdSign = Optional.ofNullable(tcr1.getThirdAmountSign()).orElse("DB");
                    BigDecimal reimbursementDebits = "DB".equalsIgnoreCase(thirdSign) ? thirdAmount : thirdAmount.negate();

                    BigDecimal netAmount = reimbursementCredits.subtract(reimbursementDebits);
                    String amountSign = netAmount.compareTo(BigDecimal.ZERO) >= 0 ? "CR" : "DB";

                    String transactionCycle = tcr0.getBusinessTransactionCycle();


                    String jurisdiction = tcr0.getJurisdictionCode();


                    String routing;
                    if (StringUtils.isNoneBlank(tcr0.getSourceRegionCode(), tcr0.getDestinationRegionCode())) {
                        String source = regionCodeToLabel.getOrDefault(tcr0.getSourceRegionCode(),tcr0.getSourceRegionCode());
                        String destination=regionCodeToLabel.getOrDefault(tcr0.getDestinationRegionCode(),tcr0.getDestinationRegionCode());
                        routing = source + " - " + destination;
                    } else {
                        String sourceCountry = tcr0.getSourceCountryCode();
                        String destinationCountry = tcr0.getDestinationCountryCode();
                        routing = codeToCountryName.getOrDefault(sourceCountry,sourceCountry) + " - " + codeToCountryName.getOrDefault(destinationCountry,destinationCountry);
                    }

                    String feeLevelDescription = Optional.ofNullable(tcr0.getFeeLevelDescriptor()).orElse("N/A");

                    cycles.add(new Vss130Report.TransactionCycleReport(
                            cycleCodeToLabel.getOrDefault(transactionCycle, transactionCycle),
                            jurisdictionCodeToLabel.getOrDefault(jurisdiction,jurisdiction),
                            routing,
                            feeLevelDescription,
                            count,
                            clearingAmount,
                            reimbursementCredits,
                            reimbursementDebits,
                            netAmount.abs(),
                            amountSign
                    ));


                    ttTotalCount += count;
                    ttTotalClearingAmount = ttTotalClearingAmount.add(clearingAmount);
                    ttTotalReimbCredits = ttTotalReimbCredits.add(reimbursementCredits);
                    ttTotalReimbDebits = ttTotalReimbDebits.add(reimbursementDebits);
                }

                BigDecimal ttNetAmount = ttTotalReimbCredits.subtract(ttTotalReimbDebits);
                String ttAmountSign = ttNetAmount.compareTo(BigDecimal.ZERO) >= 0 ? "CR" : "DB";

                transactionTypes.add(new Vss130Report.TransactionTypeReport(
                        codeToLabel.getOrDefault(transactionType, transactionType),
                        cycles,
                        ttTotalCount,
                        ttTotalClearingAmount,
                        ttTotalReimbCredits,
                        ttTotalReimbDebits,
                        ttNetAmount.abs(),
                        ttAmountSign
                ));


                bmTotalCount += ttTotalCount;
                bmTotalClearingAmount = bmTotalClearingAmount.add(ttTotalClearingAmount);
                bmTotalReimbCredits = bmTotalReimbCredits.add(ttTotalReimbCredits);
                bmTotalReimbDebits = bmTotalReimbDebits.add(ttTotalReimbDebits);
            }

            BigDecimal bmNetAmount = bmTotalReimbCredits.subtract(bmTotalReimbDebits);
            String bmAmountSign = bmNetAmount.compareTo(BigDecimal.ZERO) >= 0 ? "CR" : "DB";

            businessModes.add(new Vss130Report.BusinessModeReport(
                    codeToBusinessModeLabel.getOrDefault(businessMode, businessMode),
                    transactionTypes,
                    bmTotalCount,
                    bmTotalClearingAmount,
                    bmTotalReimbCredits,
                    bmTotalReimbDebits,
                    bmNetAmount.abs(),
                    bmAmountSign
            ));
        }

        return new Vss130Report(businessModes);
    }


    @Override
    public Vss140Report getVss140Report(String currencyCode, LocalDate startDate, LocalDate endDate,String binCode) {
        List<Object[]> rawRows = vss140Repository.findAllWithTcr1(currencyCode, startDate, endDate,binCode);

        Map<String, String> codeToLabel = transactionTypeRepository.findAll().stream()
                .collect(Collectors.toMap(BusinessTransactionType::getCode, BusinessTransactionType::getLabel));

        Map<String, String> chargeTypeToLabel = chargeTypesRepository.findAll().stream()
                .collect(Collectors.toMap(ChargeTypes::getCode, ChargeTypes::getChargeType));


        Map<String, String> codeToCountryName = countriesRepository.findAll()
                .stream()
                .collect(Collectors.toMap(Countries::getCountryCode, Countries::getCountryName));

        Map<String, String> businessModeLabels = Map.of(
                "1", "Acquirer",
                "2", "Issuer",
                "3", "Other"
        );

        Map<String, String> cycleLabels = Map.of(
                "0", "The transaction does not have a cycle",
                "1", "Originals",
                "2", "Chargebacks",
                "3", "Representments",
                "4", "Second Chargebacks",
                "5", "Debit Adjustments",
                "6", "Credit Adjustments",
                "7", "Dispute Financial",
                "8", "Dispute Response Financial"
        );

        Map<String, String> regionLabels = Map.of(
                "US", "U.S.A.",
                "CA", "Canada",
                "EU", "E.U",
                "AP", "A.P",
                "LA", "L.A.C",
                "ME", "C.E.M.E.A"
        );

        Map<String, String> jurisdictionLabels = Map.ofEntries(
                Map.entry("00", "Visa International"),
                Map.entry("01", "Visa Canada"),
                Map.entry("02", "Visa CEMEA"),
                Map.entry("03", "Visa EU"),
                Map.entry("04", "Visa AP"),
                Map.entry("05", "Visa LAC"),
                Map.entry("06", "Visa USA"),
                Map.entry("07", "Plus USA"),
                Map.entry("08", "Interlink USA"),
                Map.entry("09", "Interlink International"),
                Map.entry("10", "Visa Germany"),
                Map.entry("11", "Visa UK")
        );

        Map<String, Map<String, List<Object[]>>> grouped = rawRows.stream()
                .collect(Collectors.groupingBy(
                        row -> ((Vss140SettlementRecord) row[0]).getBusinessMode(),
                        Collectors.groupingBy(
                                row -> ((Vss140SettlementRecord) row[0]).getChargeTypeCode()
                        )
                ));

        List<Vss140Report.BusinessModeReport> businessModes = new ArrayList<>();

        for (var bmEntry : grouped.entrySet()) {
            String businessMode = bmEntry.getKey();
            var chargeTypesMap = bmEntry.getValue();

            List<Vss140Report.ChargeTypeReport> chargeTypes = new ArrayList<>();

            long bmCount = 0;
            BigDecimal bmInterchange = BigDecimal.ZERO;
            BigDecimal bmVisaCredits = BigDecimal.ZERO;
            BigDecimal bmVisaDebits = BigDecimal.ZERO;

            for (var ctEntry : chargeTypesMap.entrySet()) {
                String chargeType = ctEntry.getKey();
                List<Object[]> rows = ctEntry.getValue();

                Map<String, Map<String, Map<String, Map<String, List<Object[]>>>>> structured =
                        rows.stream().collect(Collectors.groupingBy(
                                row -> ((Vss140SettlementRecord) row[0]).getBusinessTransactionType(),
                                Collectors.groupingBy(
                                        row -> ((Vss140SettlementRecord) row[0]).getBusinessTransactionCycle(),
                                        Collectors.groupingBy(
                                                row -> ((Vss140SettlementRecord) row[0]).getJurisdictionCode(),
                                                Collectors.groupingBy(
                                                        row -> {
                                                            Vss140SettlementRecord tcr0 = (Vss140SettlementRecord) row[0];
                                                            if (StringUtils.isNoneBlank(tcr0.getSourceRegionCode(), tcr0.getDestinationRegionCode())) {
                                                                String source = regionLabels.getOrDefault(tcr0.getSourceRegionCode(), tcr0.getSourceRegionCode());
                                                                String dest = regionLabels.getOrDefault(tcr0.getDestinationRegionCode(), tcr0.getDestinationRegionCode());
                                                                return source + " - " + dest;
                                                            } else {
                                                                String source = codeToCountryName.getOrDefault(tcr0.getSourceCountryCode(), tcr0.getSourceCountryCode());
                                                                String dest = codeToCountryName.getOrDefault(tcr0.getDestinationCountryCode(), tcr0.getDestinationCountryCode());
                                                                return source + " - " + dest;
                                                            }
                                                        }
                                                )
                                        )
                                )
                        ));

                List<Vss140Report.TransactionTypeReport> transactionTypes = new ArrayList<>();

                long ctCount = 0;
                BigDecimal ctInterchange = BigDecimal.ZERO;
                BigDecimal ctVisaCredits = BigDecimal.ZERO;
                BigDecimal ctVisaDebits = BigDecimal.ZERO;

                for (var ttEntry : structured.entrySet()) {
                    String transactionType = ttEntry.getKey();
                    var cycleMap = ttEntry.getValue();

                    List<Vss140Report.TransactionCycleReport> cycles = new ArrayList<>();

                    long ttCount = 0;
                    BigDecimal ttInterchange = BigDecimal.ZERO;
                    BigDecimal ttVisaCredits = BigDecimal.ZERO;
                    BigDecimal ttVisaDebits = BigDecimal.ZERO;

                    for (var cycleEntry : cycleMap.entrySet()) {
                        String cycle = cycleEntry.getKey();
                        var jurisdictionMap = cycleEntry.getValue();

                        List<Vss140Report.JurisdictionReport> jurisdictions = new ArrayList<>();

                        long cycleCount = 0;
                        BigDecimal cycleInterchange = BigDecimal.ZERO;
                        BigDecimal cycleVisaCredits = BigDecimal.ZERO;
                        BigDecimal cycleVisaDebits = BigDecimal.ZERO;

                        for (var jEntry : jurisdictionMap.entrySet()) {
                            String jurisdiction = jEntry.getKey();
                            var routingMap = jEntry.getValue();

                            List<Vss140Report.RoutingReport> routings = new ArrayList<>();

                            long jCount = 0;
                            BigDecimal jInterchange = BigDecimal.ZERO;
                            BigDecimal jVisaCredits = BigDecimal.ZERO;
                            BigDecimal jVisaDebits = BigDecimal.ZERO;

                            for (var rEntry : routingMap.entrySet()) {
                                String routing = rEntry.getKey();
                                List<Object[]> routingRows = rEntry.getValue();

                                long rCount = 0;
                                BigDecimal rInterchange = BigDecimal.ZERO;
                                BigDecimal rVisaCredits = BigDecimal.ZERO;
                                BigDecimal rVisaDebits = BigDecimal.ZERO;

                                for (Object[] row : routingRows) {
                                    Vss120Tcr1Record tcr1 = (Vss120Tcr1Record) row[1];

                                    long count = Optional.ofNullable(tcr1.getFirstCount()).orElse(0L);

                                    BigDecimal interchange = Optional.ofNullable(tcr1.getFirstAmount()).orElse(BigDecimal.ZERO);
                                    if ("DB".equalsIgnoreCase(Optional.ofNullable(tcr1.getFirstAmountSign()).orElse("CR"))) {
                                        interchange = interchange.negate();
                                    }

                                    BigDecimal credit = Optional.ofNullable(tcr1.getSecondAmount()).orElse(BigDecimal.ZERO);
                                    if (!"CR".equalsIgnoreCase(Optional.ofNullable(tcr1.getSecondAmountSign()).orElse("CR"))) {
                                        credit = credit.negate();
                                    }

                                    BigDecimal debit = Optional.ofNullable(tcr1.getThirdAmount()).orElse(BigDecimal.ZERO);
                                    if (!"DB".equalsIgnoreCase(Optional.ofNullable(tcr1.getThirdAmountSign()).orElse("DB"))) {
                                        debit = debit.negate();
                                    }

                                    rCount += count;
                                    rInterchange = rInterchange.add(interchange);
                                    rVisaCredits = rVisaCredits.add(credit);
                                    rVisaDebits = rVisaDebits.add(debit);
                                }

                                BigDecimal net = rVisaCredits.subtract(rVisaDebits);
                                String sign = net.compareTo(BigDecimal.ZERO) >= 0 ? "CR" : "DB";

                                routings.add(new Vss140Report.RoutingReport(
                                        routing, rCount, rInterchange, rVisaCredits, rVisaDebits, net.abs(), sign
                                ));

                                jCount += rCount;
                                jInterchange = jInterchange.add(rInterchange);
                                jVisaCredits = jVisaCredits.add(rVisaCredits);
                                jVisaDebits = jVisaDebits.add(rVisaDebits);
                            }

                            BigDecimal net = jVisaCredits.subtract(jVisaDebits);
                            String sign = net.compareTo(BigDecimal.ZERO) >= 0 ? "CR" : "DB";

                            jurisdictions.add(new Vss140Report.JurisdictionReport(
                                    jurisdictionLabels.getOrDefault(jurisdiction, jurisdiction),
                                    routings,
                                    jCount, jInterchange, jVisaCredits, jVisaDebits, net.abs(), sign
                            ));

                            cycleCount += jCount;
                            cycleInterchange = cycleInterchange.add(jInterchange);
                            cycleVisaCredits = cycleVisaCredits.add(jVisaCredits);
                            cycleVisaDebits = cycleVisaDebits.add(jVisaDebits);
                        }

                        BigDecimal net = cycleVisaCredits.subtract(cycleVisaDebits);
                        String sign = net.compareTo(BigDecimal.ZERO) >= 0 ? "CR" : "DB";

                        cycles.add(new Vss140Report.TransactionCycleReport(
                                cycleLabels.getOrDefault(cycle, cycle),
                                jurisdictions,
                                cycleCount, cycleInterchange, cycleVisaCredits, cycleVisaDebits, net.abs(), sign
                        ));

                        ttCount += cycleCount;
                        ttInterchange = ttInterchange.add(cycleInterchange);
                        ttVisaCredits = ttVisaCredits.add(cycleVisaCredits);
                        ttVisaDebits = ttVisaDebits.add(cycleVisaDebits);
                    }

                    BigDecimal net = ttVisaCredits.subtract(ttVisaDebits);
                    String sign = net.compareTo(BigDecimal.ZERO) >= 0 ? "CR" : "DB";

                    transactionTypes.add(new Vss140Report.TransactionTypeReport(
                            codeToLabel.getOrDefault(transactionType, transactionType),
                            cycles,
                            ttCount, ttInterchange, ttVisaCredits, ttVisaDebits, net.abs(), sign
                    ));

                    ctCount += ttCount;
                    ctInterchange = ctInterchange.add(ttInterchange);
                    ctVisaCredits = ctVisaCredits.add(ttVisaCredits);
                    ctVisaDebits = ctVisaDebits.add(ttVisaDebits);
                }

                BigDecimal net = ctVisaCredits.subtract(ctVisaDebits);
                String sign = net.compareTo(BigDecimal.ZERO) >= 0 ? "CR" : "DB";

                chargeTypes.add(new Vss140Report.ChargeTypeReport(
                        chargeTypeToLabel.getOrDefault(chargeType, chargeType),
                        transactionTypes,
                        ctCount, ctInterchange, ctVisaCredits, ctVisaDebits, net.abs(), sign
                ));

                bmCount += ctCount;
                bmInterchange = bmInterchange.add(ctInterchange);
                bmVisaCredits = bmVisaCredits.add(ctVisaCredits);
                bmVisaDebits = bmVisaDebits.add(ctVisaDebits);
            }

            BigDecimal net = bmVisaCredits.subtract(bmVisaDebits);
            String sign = net.compareTo(BigDecimal.ZERO) >= 0 ? "CR" : "DB";

            businessModes.add(new Vss140Report.BusinessModeReport(
                    businessModeLabels.getOrDefault(businessMode, businessMode),
                    chargeTypes,
                    bmCount, bmInterchange, bmVisaCredits, bmVisaDebits, net.abs(), sign
            ));
        }

        return new Vss140Report(businessModes);
    }


}