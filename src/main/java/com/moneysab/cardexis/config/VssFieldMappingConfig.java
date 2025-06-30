package com.moneysab.cardexis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Configuration for VSS report field position mappings.
 * Based on ReadingReports.txt specifications.
 * 
 * @author EL.AHGOUNE
 * @version 1.0.0
 * @since 2024
 */
@Configuration
@ConfigurationProperties(prefix = "vss.field-mapping")
@Data
public class VssFieldMappingConfig {

    private Report110Fields report110 = new Report110Fields();
    private Report120Fields report120 = new Report120Fields();
    private Report130Fields report130 = new Report130Fields();
    private Report140Fields report140 = new Report140Fields();
    private Report900Fields report900 = new Report900Fields();

    @Data
    public static class Report110Fields {
        private FieldPosition settlementCurrency = new FieldPosition(24, ReadDirection.LEFT_TO_RIGHT, 3);
        private FieldPosition count = new FieldPosition(52, ReadDirection.RIGHT_TO_LEFT, 15);
        private FieldPosition creditAmount = new FieldPosition(78, ReadDirection.RIGHT_TO_LEFT, 20);
        private FieldPosition debitAmount = new FieldPosition(104, ReadDirection.RIGHT_TO_LEFT, 20);
        private FieldPosition totalAmount = new FieldPosition(132, ReadDirection.RIGHT_TO_LEFT, 20);
    }

    @Data
    public static class Report120Fields {
        private FieldPosition settlementCurrency = new FieldPosition(24, ReadDirection.LEFT_TO_RIGHT, 3);
        private FieldPosition clearingCurrency = new FieldPosition(24, ReadDirection.LEFT_TO_RIGHT, 3);
        private FieldPosition tableId = new FieldPosition(52, ReadDirection.RIGHT_TO_LEFT, 10);
        private FieldPosition count = new FieldPosition(67, ReadDirection.RIGHT_TO_LEFT, 15);
        private FieldPosition clearingAmount = new FieldPosition(90, ReadDirection.RIGHT_TO_LEFT, 20);
        private FieldPosition interchangeCredits = new FieldPosition(104, ReadDirection.RIGHT_TO_LEFT, 20);
        private FieldPosition interchangeDebits = new FieldPosition(130, ReadDirection.RIGHT_TO_LEFT, 20);
    }

    @Data
    public static class Report130Fields {
        private FieldPosition settlementCurrency = new FieldPosition(24, ReadDirection.LEFT_TO_RIGHT, 3);
        private FieldPosition count = new FieldPosition(62, ReadDirection.RIGHT_TO_LEFT, 15);
        private FieldPosition interchangeAmount = new FieldPosition(87, ReadDirection.RIGHT_TO_LEFT, 20);
        private FieldPosition reimbursementFeeCredits = new FieldPosition(110, ReadDirection.RIGHT_TO_LEFT, 20);
        private FieldPosition reimbursementFeeDebits = new FieldPosition(132, ReadDirection.RIGHT_TO_LEFT, 20);
    }

    @Data
    public static class Report140Fields {
        private FieldPosition settlementCurrency = new FieldPosition(24, ReadDirection.LEFT_TO_RIGHT, 3);
        private FieldPosition count = new FieldPosition(67, ReadDirection.RIGHT_TO_LEFT, 15);
        private FieldPosition interchangeAmount = new FieldPosition(90, ReadDirection.RIGHT_TO_LEFT, 20);
        private FieldPosition visaChargesCredits = new FieldPosition(111, ReadDirection.RIGHT_TO_LEFT, 20);
        private FieldPosition visaChargesDebits = new FieldPosition(132, ReadDirection.RIGHT_TO_LEFT, 20);
    }

    @Data
    public static class Report900Fields {
        private FieldPosition clearingCurrency = new FieldPosition(22, ReadDirection.LEFT_TO_RIGHT, 3);
        private FieldPosition count = new FieldPosition(67, ReadDirection.RIGHT_TO_LEFT, 15);
        private FieldPosition clearingAmount = new FieldPosition(89, ReadDirection.RIGHT_TO_LEFT, 20);
        private FieldPosition totalCount = new FieldPosition(106, ReadDirection.RIGHT_TO_LEFT, 15);
        private FieldPosition totalClearingAmount = new FieldPosition(131, ReadDirection.RIGHT_TO_LEFT, 20);
    }

    @Data
    public static class FieldPosition {
        private int position;
        private ReadDirection direction;
        private int maxLength;

        public FieldPosition() {}

        public FieldPosition(int position, ReadDirection direction, int maxLength) {
            this.position = position;
            this.direction = direction;
            this.maxLength = maxLength;
        }
    }

    public enum ReadDirection {
        LEFT_TO_RIGHT,
        RIGHT_TO_LEFT
    }
} 