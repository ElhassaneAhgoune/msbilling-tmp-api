-- Fix VSS-110 batch insert issues
-- Migration to resolve field length and constraint issues causing batch insert failures

-- First, let's ensure the raw_settlement_date constraint is properly updated
-- (This should already be done by V8, but let's make sure)
ALTER TABLE vss110_settlement_records
DROP CONSTRAINT IF EXISTS chk_vss110_raw_settlement_date;

ALTER TABLE vss110_settlement_records
ADD CONSTRAINT chk_vss110_raw_settlement_date CHECK (
    raw_settlement_date ~ '^[0-9]{5,7}$' OR raw_settlement_date IS NULL
);

-- Fix potential issues with field lengths that might cause truncation
-- Increase settlement_service length to handle longer values if needed
ALTER TABLE vss110_settlement_records
ALTER COLUMN settlement_service TYPE VARCHAR(20);

-- Ensure reporting_sre_id can handle longer values
ALTER TABLE vss110_settlement_records
ALTER COLUMN reporting_sre_id TYPE VARCHAR(100);

-- Ensure rollup_sre_id can handle longer values  
ALTER TABLE vss110_settlement_records
ALTER COLUMN rollup_sre_id TYPE VARCHAR(100);

-- Ensure funds_transfer_sre_id can handle longer values
ALTER TABLE vss110_settlement_records
ALTER COLUMN funds_transfer_sre_id TYPE VARCHAR(100);

-- Update raw_report_date constraint to be more flexible
ALTER TABLE vss110_settlement_records
DROP CONSTRAINT IF EXISTS chk_vss110_raw_report_date;

ALTER TABLE vss110_settlement_records
ADD CONSTRAINT chk_vss110_raw_report_date CHECK (
    raw_report_date ~ '^[0-9]{5,7}$' OR raw_report_date IS NULL
);

-- Add comments explaining the changes
COMMENT ON CONSTRAINT chk_vss110_raw_settlement_date ON vss110_settlement_records IS 
'Raw settlement date must be 5-7 digits to accommodate both truncated (YYDDD) and full (CCYYDDD) formats';

COMMENT ON CONSTRAINT chk_vss110_raw_report_date ON vss110_settlement_records IS 
'Raw report date must be 5-7 digits to accommodate both truncated (YYDDD) and full (CCYYDDD) formats';

COMMENT ON COLUMN vss110_settlement_records.settlement_service IS 
'Settlement service code - increased length to handle various service identifiers';

COMMENT ON COLUMN vss110_settlement_records.reporting_sre_id IS 
'Reporting SRE ID - increased length to handle longer identifiers';

COMMENT ON COLUMN vss110_settlement_records.rollup_sre_id IS 
'Rollup SRE ID - increased length to handle longer identifiers';

COMMENT ON COLUMN vss110_settlement_records.funds_transfer_sre_id IS
'Funds Transfer SRE ID - increased length to handle longer identifiers';

-- Fix amount_sign constraint to handle spaces and empty values
ALTER TABLE vss110_settlement_records
DROP CONSTRAINT IF EXISTS chk_vss110_amount_sign;

ALTER TABLE vss110_settlement_records
ADD CONSTRAINT chk_vss110_amount_sign CHECK (
    amount_sign IN ('CR', 'DB') OR
    amount_sign IS NULL OR
    TRIM(amount_sign) = '' OR
    amount_sign ~ '^\\s*$'
);

COMMENT ON CONSTRAINT chk_vss110_amount_sign ON vss110_settlement_records IS
'Amount sign must be CR (Credit), DB (Debit), NULL, or contain only spaces';