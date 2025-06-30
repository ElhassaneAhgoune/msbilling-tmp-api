-- Add additional fields to VSS-110 Settlement Records Table
-- Migration to support VSS 110 TC46, TCR 0 Report Group V, Subgroup 2 specification

-- Add new columns for VSS 110 business logic
ALTER TABLE vss110_settlement_records
ADD COLUMN rollup_sre_id VARCHAR(50),
ADD COLUMN funds_transfer_sre_id VARCHAR(50),
ADD COLUMN settlement_service VARCHAR(10),
ADD COLUMN report_date DATE,
ADD COLUMN raw_report_date VARCHAR(7),
ADD COLUMN amount_sign VARCHAR(2);

-- Update currency code default to EUR (978) as per VSS 110 spec
ALTER TABLE vss110_settlement_records
ALTER COLUMN currency_code SET DEFAULT '978';

-- Add check constraints for new fields
ALTER TABLE vss110_settlement_records
ADD CONSTRAINT chk_vss110_amount_sign CHECK (amount_sign IN ('CR', 'DB') OR amount_sign IS NULL),
ADD CONSTRAINT chk_vss110_raw_report_date CHECK (raw_report_date ~ '^[0-9]{7}$' OR raw_report_date IS NULL);

-- Create indexes for new fields
CREATE INDEX idx_vss110_rollup_sre_id ON vss110_settlement_records(rollup_sre_id);
CREATE INDEX idx_vss110_funds_transfer_sre_id ON vss110_settlement_records(funds_transfer_sre_id);
CREATE INDEX idx_vss110_settlement_service ON vss110_settlement_records(settlement_service);
CREATE INDEX idx_vss110_report_datae ON vss110_settlement_records(report_date);
CREATE INDEX idx_vss110_amount_sign ON vss110_settlement_records(amount_sign);

-- Add comments for new fields
COMMENT ON COLUMN vss110_settlement_records.rollup_sre_id IS 'Rollup Settlement Reporting Entity identifier';
COMMENT ON COLUMN vss110_settlement_records.funds_transfer_sre_id IS 'Funds Transfer Settlement Reporting Entity identifier';
COMMENT ON COLUMN vss110_settlement_records.settlement_service IS 'Settlement service code (e.g., 001)';
COMMENT ON COLUMN vss110_settlement_records.report_date IS 'Report generation date (same as settlement date for VSS 110)';
COMMENT ON COLUMN vss110_settlement_records.raw_report_date IS 'Raw report date string in CCYYDDD format';
COMMENT ON COLUMN vss110_settlement_records.amount_sign IS 'Amount sign indicator (CR=Credit, DB=Debit)';