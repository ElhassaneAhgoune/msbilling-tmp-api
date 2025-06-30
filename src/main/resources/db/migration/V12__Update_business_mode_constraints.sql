-- Update business_mode constraints to allow space characters and any values
-- Migration V12: Update business_mode constraints
-- Date: 2025-06-21
-- Author: System Update

-- Drop existing constraints
ALTER TABLE vss110_settlement_records DROP CONSTRAINT IF EXISTS chk_vss110_business_mode;
ALTER TABLE vss120_settlement_records DROP CONSTRAINT IF EXISTS chk_vss120_business_mode;

-- Add new more permissive constraints that allow spaces and other characters
ALTER TABLE vss110_settlement_records 
ADD CONSTRAINT chk_vss110_business_mode 
CHECK (business_mode IS NULL OR LENGTH(business_mode) <= 1);

ALTER TABLE vss120_settlement_records 
ADD CONSTRAINT chk_vss120_business_mode 
CHECK (business_mode IS NULL OR LENGTH(business_mode) <= 1);

-- Add comments explaining the change
COMMENT ON CONSTRAINT chk_vss110_business_mode ON vss110_settlement_records 
IS 'Business mode constraint updated to allow any single character including spaces (was restricted to 1,2,3,9)';

COMMENT ON CONSTRAINT chk_vss120_business_mode ON vss120_settlement_records 
IS 'Business mode constraint updated to allow any single character including spaces (was restricted to 1,2,3)';

-- Update column comments to reflect the change
COMMENT ON COLUMN vss110_settlement_records.business_mode 
IS 'Business mode (position 95): Any single character - commonly 1=Acquirer, 2=Issuer, 3=Other, 9=Total, or space';

COMMENT ON COLUMN vss120_settlement_records.business_mode 
IS 'Business mode: Any single character - commonly 1=Acquirer, 2=Issuer, 3=Other, or space'; 