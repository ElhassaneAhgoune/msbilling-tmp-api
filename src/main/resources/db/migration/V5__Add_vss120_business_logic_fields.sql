-- VSS-120 Business Logic Enhancement Migration
-- Adds missing fields required for comprehensive VSS-120 business logic implementation

-- Add missing fields to vss120_settlement_records table
ALTER TABLE vss120_settlement_records
ADD COLUMN IF NOT EXISTS summary_level VARCHAR(5),
ADD COLUMN IF NOT EXISTS product_code VARCHAR(10),
ADD COLUMN IF NOT EXISTS region_code VARCHAR(5),
ADD COLUMN IF NOT EXISTS country_code VARCHAR(3),
ADD COLUMN IF NOT EXISTS processing_date DATE;

-- Update vss120_financial_details table to match entity field names
-- Rename sign columns to match entity naming convention
ALTER TABLE vss120_financial_details
RENAME COLUMN sign1 TO amount1_sign;

ALTER TABLE vss120_financial_details
RENAME COLUMN sign2 TO amount2_sign;

ALTER TABLE vss120_financial_details
RENAME COLUMN sign3 TO amount3_sign;

ALTER TABLE vss120_financial_details
RENAME COLUMN sign4 TO amount4_sign;

ALTER TABLE vss120_financial_details
RENAME COLUMN sign5 TO amount5_sign;

ALTER TABLE vss120_financial_details
RENAME COLUMN sign6 TO amount6_sign;

-- Add missing record_type field to vss120_financial_details
ALTER TABLE vss120_financial_details
ADD COLUMN IF NOT EXISTS record_type VARCHAR(10) DEFAULT 'TCR1';

-- Add indexes for new fields to support business logic queries
CREATE INDEX IF NOT EXISTS idx_vss120_summary_level ON vss120_settlement_records(summary_level);
CREATE INDEX IF NOT EXISTS idx_vss120_currency_code ON vss120_settlement_records(currency_code);
CREATE INDEX IF NOT EXISTS idx_vss120_destination_identifier ON vss120_settlement_records(destination_identifier);

-- Add indexes for vss120_financial_details to support business logic
CREATE INDEX IF NOT EXISTS idx_vss120_financial_settlement_record ON vss120_financial_details(settlement_record_id);
CREATE INDEX IF NOT EXISTS idx_vss120_financial_job_id ON vss120_financial_details(job_id);
CREATE INDEX IF NOT EXISTS idx_vss120_financial_rate_table ON vss120_financial_details(rate_table_id);
CREATE INDEX IF NOT EXISTS idx_vss120_financial_currency ON vss120_financial_details(currency_code);

-- Add composite indexes for common business logic queries
CREATE INDEX IF NOT EXISTS idx_vss120_settlement_date_destination ON vss120_settlement_records(settlement_date, destination_identifier);
CREATE INDEX IF NOT EXISTS idx_vss120_business_mode_currency ON vss120_settlement_records(business_mode, currency_code);
CREATE INDEX IF NOT EXISTS idx_vss120_currency_summary_level ON vss120_settlement_records(currency_code, summary_level);

-- Add comments for documentation
COMMENT ON COLUMN vss120_settlement_records.summary_level IS 'Summary level indicator (01=Business Mode Total, 02=Business Mode Net, 05=Business Transaction Total, 06=Business Transaction Net, 07=Rate Table Total)';
COMMENT ON COLUMN vss120_settlement_records.business_mode IS 'Business mode indicator (1=Acquirer, 2=Issuer, 3=Other)';
COMMENT ON COLUMN vss120_settlement_records.transaction_cycle IS 'Transaction cycle indicator (1=Original, 2=Chargeback, 3=Representment, 4=Dispute)';
COMMENT ON COLUMN vss120_settlement_records.currency_code IS 'Currency code (978=EUR, 691=UAH, 840=USD)';

COMMENT ON COLUMN vss120_financial_details.rate_table_id IS 'Rate table identifier for currency conversion (e.g., A0293, A0294, A0295, A0296)';
COMMENT ON COLUMN vss120_financial_details.amount1_sign IS 'Sign indicator for Amount1 (CR=Credit, DR=Debit)';
COMMENT ON COLUMN vss120_financial_details.amount2_sign IS 'Sign indicator for Amount2 (CR=Credit, DR=Debit)';
COMMENT ON COLUMN vss120_financial_details.amount3_sign IS 'Sign indicator for Amount3 (CR=Credit, DR=Debit)';
COMMENT ON COLUMN vss120_financial_details.amount4_sign IS 'Sign indicator for Amount4 (CR=Credit, DR=Debit)';
COMMENT ON COLUMN vss120_financial_details.amount5_sign IS 'Sign indicator for Amount5 (CR=Credit, DR=Debit)';
COMMENT ON COLUMN vss120_financial_details.amount6_sign IS 'Sign indicator for Amount6 (CR=Credit, DR=Debit)';

-- Update transaction_volume to INTEGER to match entity
ALTER TABLE vss120_financial_details
ALTER COLUMN transaction_volume TYPE INTEGER;

-- Add check constraints for business logic validation
ALTER TABLE vss120_settlement_records
ADD CONSTRAINT chk_vss120_business_mode
CHECK (business_mode IS NULL OR business_mode IN ('1', '2', '3'));

ALTER TABLE vss120_settlement_records
ADD CONSTRAINT chk_vss120_transaction_cycle
CHECK (transaction_cycle IS NULL OR transaction_cycle IN ('1', '2', '3', '4'));

ALTER TABLE vss120_settlement_records
ADD CONSTRAINT chk_vss120_summary_level
CHECK (summary_level IS NULL OR summary_level IN ('01', '02', '05', '06', '07'));

ALTER TABLE vss120_financial_details
ADD CONSTRAINT chk_vss120_amount_signs
CHECK (
    (amount1_sign IS NULL OR amount1_sign IN ('CR', 'DR')) AND
    (amount2_sign IS NULL OR amount2_sign IN ('CR', 'DR')) AND
    (amount3_sign IS NULL OR amount3_sign IN ('CR', 'DR')) AND
    (amount4_sign IS NULL OR amount4_sign IN ('CR', 'DR')) AND
    (amount5_sign IS NULL OR amount5_sign IN ('CR', 'DR')) AND
    (amount6_sign IS NULL OR amount6_sign IN ('CR', 'DR'))
);

-- Create a view for business logic analysis
CREATE OR REPLACE VIEW vss120_business_analysis AS
SELECT
    sr.settlement_date,
    sr.destination_identifier,
    sr.business_mode,
    sr.currency_code,
    sr.summary_level,
    COUNT(fd.id) as financial_detail_count,
    SUM(fd.transaction_volume) as total_transaction_volume,
    SUM(CASE WHEN fd.amount1_sign = 'CR' THEN fd.amount1 ELSE 0 END) as total_credit_amount1,
    SUM(CASE WHEN fd.amount1_sign = 'DR' THEN fd.amount1 ELSE 0 END) as total_debit_amount1,
    SUM(fd.amount6) as total_net_amount,
    COUNT(DISTINCT fd.rate_table_id) as unique_rate_tables
FROM vss120_settlement_records sr
LEFT JOIN vss120_financial_details fd ON sr.id = fd.settlement_record_id
WHERE sr.is_valid = true AND (fd.is_valid = true OR fd.is_valid IS NULL)
GROUP BY sr.settlement_date, sr.destination_identifier, sr.business_mode, sr.currency_code, sr.summary_level;

COMMENT ON VIEW vss120_business_analysis IS 'Business analysis view for VSS-120 records with aggregated financial metrics';