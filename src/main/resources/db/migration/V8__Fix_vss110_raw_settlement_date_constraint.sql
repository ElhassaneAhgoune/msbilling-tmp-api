-- Fix VSS-110 raw settlement date constraint
-- Migration to handle variable length raw settlement dates

-- Drop the existing constraint that requires exactly 7 digits
ALTER TABLE vss110_settlement_records
DROP CONSTRAINT IF EXISTS chk_vss110_raw_settlement_date;

-- Add a more flexible constraint that allows 5-7 digits
-- This accommodates both truncated dates (like '20221') and full CCYYDDD format
ALTER TABLE vss110_settlement_records
ADD CONSTRAINT chk_vss110_raw_settlement_date CHECK (
    raw_settlement_date ~ '^[0-9]{5,7}$' OR raw_settlement_date IS NULL
);

-- Add comment explaining the constraint
COMMENT ON CONSTRAINT chk_vss110_raw_settlement_date ON vss110_settlement_records IS 
'Raw settlement date must be 5-7 digits to accommodate both truncated (YYDDD) and full (CCYYDDD) formats';