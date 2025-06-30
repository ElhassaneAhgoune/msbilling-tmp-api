-- Fix VSS-110 currency code constraint to support numeric ISO codes
-- Migration to allow both alphabetic (USD, EUR) and numeric (840, 978) currency codes

-- Drop the existing constraint that only allows 3 uppercase letters
ALTER TABLE vss110_settlement_records 
DROP CONSTRAINT IF EXISTS chk_vss110_currency_code;

-- Add new constraint that allows either:
-- 1. 3 uppercase letters (USD, EUR, GBP, etc.)
-- 2. 3 digits (840, 978, 826, etc.)
ALTER TABLE vss110_settlement_records 
ADD CONSTRAINT chk_vss110_currency_code 
CHECK (currency_code ~ '^([A-Z]{3}|[0-9]{3})$');

-- Update the comment to reflect the new constraint
COMMENT ON COLUMN vss110_settlement_records.currency_code IS 'Currency code: ISO 4217 alphabetic (USD, EUR) or numeric (840, 978) format';