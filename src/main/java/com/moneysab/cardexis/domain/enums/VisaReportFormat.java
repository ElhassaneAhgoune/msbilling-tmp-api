package com.moneysab.cardexis.domain.enums;

/**
 * Enumeration of supported Visa report formats.
 *
 * @author EL.AHGOUNE
 * @version 2.0.0
 * @since 2024
 */
public enum VisaReportFormat {

    /**
     * VSS-110 format - TC46, TCR0, Report Group V, Subgroup 2.
     * Settlement summary data with detailed fee breakdown by category.
     */
    VSS_110("VSS-110", "Settlement Summary with Fee Details", "V2110"),

    /**
     * VSS-111 format - TC46, TCR0, Report Group V, Subgroup 2.
     * Settlement summary data with totals only.
     */
    VSS_111("VSS-111", "Settlement Summary Totals", "V2111"),

    /**
     * VSS-120 format - TC46, TCR0, Report Group V, Subgroup 4.
     * Client settlement data with transaction details.
     */
    VSS_120("VSS-120", "Client Settlement Data", "V4120"),

    /**
     * VSS-130 format - TC46, TCR0, Report Group V, Subgroup 4.
     * Client settlement data with interchange details.
     */
    VSS_130("VSS-130", "Client Settlement with Interchange", "V4130"),

    /**
     * VSS-131 format - TC46, TCR0, Report Group V, Subgroup 4.
     * Client settlement data with additional details.
     */
    VSS_131("VSS-131", "Client Settlement Extended", "V4131"),

    /**
     * VSS-135 format - TC46, TCR0, Report Group V, Subgroup 4.
     * Daily client settlement data.
     */
    VSS_135("VSS-135", "Daily Client Settlement", "V4135"),

    /**
     * VSS-136 format - TC46, TCR0, Report Group V, Subgroup 4.
     * Client settlement data with enhanced details.
     */
    VSS_136("VSS-136", "Enhanced Client Settlement", "V4136"),

    /**
     * VSS-140 format - TC46, TCR0, Report Group V, Subgroup 4.
     * Interchange amount details.
     */
    VSS_140("VSS-140", "Interchange Amount Details", "V4140"),

    /**
     * VSS-210 format - TC46, TCR0, Report Group V, Subgroup 4.
     * Visa charges and fees details.
     */
    VSS_210("VSS-210", "Visa Charges and Fees", "V4210"),

    /**
     * VSS-215 format - TC46, TCR0, Report Group V, Subgroup 4.
     * International service assessment details.
     */
    VSS_215("VSS-215", "International Service Assessment", "V4215"),

    /**
     * VSS-230 format - TC46, TCR0, Report Group V, Subgroup 4.
     * Visa charges details.
     */
    VSS_230("VSS-230", "Visa Charges Details", "V4230"),

    /**
     * VSS-640 format - TC46, TCR0, Report Group V, Subgroup 4.
     * Interchange amount in settlement currency.
     */
    VSS_640("VSS-640", "Interchange in Settlement Currency", "V4640"),

    /**
     * Mixed format - File contains multiple report types.
     */
    MIXED("MIXED", "Multiple Report Types", "MIXED"),

    /**
     * Unknown format - Unable to determine report type.
     */
    UNKNOWN("UNKNOWN", "Unknown Report Format", "UNKNOWN");

    private final String code;
    private final String description;
    private final String identifier;

    VisaReportFormat(String code, String description, String identifier) {
        this.code = code;
        this.description = description;
        this.identifier = identifier;
    }

    /**
     * Gets the format code.
     *
     * @return the format code
     */
    public String getCode() {
        return code;
    }

    /**
     * Gets the format description.
     *
     * @return the format description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the format identifier used in records.
     *
     * @return the format identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Gets the report subgroup for this format.
     *
     * @return the report subgroup ("2" for VSS-110/111, "4" for others)
     */
    public String getReportSubgroup() {
        return switch (this) {
            case VSS_110, VSS_111 -> "2";
            case VSS_120, VSS_130, VSS_131, VSS_135, VSS_136, VSS_140,
                    VSS_210, VSS_215, VSS_230, VSS_640 -> "4";
            default -> "UNKNOWN";
        };
    }

    /**
     * Checks if this format is a settlement summary format (VSS-110/111).
     *
     * @return true if this is a settlement summary format
     */
    public boolean isSettlementSummary() {
        return this == VSS_110 || this == VSS_111;
    }

    /**
     * Checks if this format is a client settlement format (VSS-4xx series).
     *
     * @return true if this is a client settlement format
     */
    public boolean isClientSettlement() {
        return getReportSubgroup().equals("4");
    }

    /**
     * Gets the VisaReportFormat from a report identifier.
     *
     * @param identifier the report identifier (e.g., "V2110", "V4120")
     * @return the corresponding VisaReportFormat, or UNKNOWN if not found
     */
    public static VisaReportFormat fromIdentifier(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return UNKNOWN;
        }

        for (VisaReportFormat format : values()) {
            if (format.getIdentifier().equalsIgnoreCase(identifier.trim())) {
                return format;
            }
        }

        return UNKNOWN;
    }

    /**
     * Gets the VisaReportFormat from a report ID number and subgroup.
     *
     * @param reportIdNumber the report ID number (e.g., "110", "120")
     * @param reportSubgroup the report subgroup (e.g., "2", "4")
     * @return the corresponding VisaReportFormat, or UNKNOWN if not found
     */
    public static VisaReportFormat fromReportId(String reportIdNumber, String reportSubgroup) {
        if (reportIdNumber == null || reportSubgroup == null) {
            return UNKNOWN;
        }

        String identifier = "V" + reportSubgroup + reportIdNumber;
        return fromIdentifier(identifier);
    }

    /**
     * Gets the VisaReportFormat from a format code.
     *
     * @param code the format code (e.g., "VSS-110", "VSS-120")
     * @return the corresponding VisaReportFormat, or UNKNOWN if not found
     */
    public static VisaReportFormat fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return UNKNOWN;
        }

        for (VisaReportFormat format : values()) {
            if (format.getCode().equalsIgnoreCase(code.trim())) {
                return format;
            }
        }

        return UNKNOWN;
    }

    /**
     * Checks if the given format is supported for parsing.
     *
     * @param format the format to check
     * @return true if the format is supported
     */
    public static boolean isSupported(VisaReportFormat format) {
        return format != null && format != UNKNOWN;
    }

    /**
     * Gets all VSS-4 series formats (client settlement formats).
     *
     * @return array of VSS-4 series formats
     */
    public static VisaReportFormat[] getVss4Formats() {
        return new VisaReportFormat[] {
                VSS_120, VSS_130, VSS_131, VSS_135, VSS_136, VSS_140,
                VSS_210, VSS_215, VSS_230, VSS_640
        };
    }

    /**
     * Gets all VSS-2 series formats (settlement summary formats).
     *
     * @return array of VSS-2 series formats
     */
    public static VisaReportFormat[] getVss2Formats() {
        return new VisaReportFormat[] { VSS_110, VSS_111 };
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", code, description);
    }
}