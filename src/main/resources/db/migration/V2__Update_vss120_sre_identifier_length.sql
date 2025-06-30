-- Migration to increase the length of reporting_for_sre_identifier column
-- in vss120_settlement_records table from 20 to 50 characters

ALTER TABLE vss120_settlement_records 
ALTER COLUMN reporting_for_sre_identifier TYPE VARCHAR(50);

-- Update the comment to reflect the new length
COMMENT ON COLUMN vss120_settlement_records.reporting_for_sre_identifier IS 
'Reporting for SRE identifier - identifies the SRE (Settlement Reporting Entity) in the hierarchy (max 50 characters)';