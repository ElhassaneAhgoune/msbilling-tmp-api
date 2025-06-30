-- Update VSS-110 Settlement Records Table Structure
-- Migration to align with new comprehensive VSS-110 entity specification

-- Drop the existing VSS-110 table and recreate with proper structure
DROP TABLE IF EXISTS vss110_settlement_records CASCADE;

-- Create new VSS-110 Settlement Records Table with complete field mapping
CREATE TABLE vss110_settlement_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL,
    
    -- Header Fields (Positions 1-58)
    transaction_code VARCHAR(2) NOT NULL,
    destination_id VARCHAR(6) NOT NULL,
    reporting_sre_id VARCHAR(50),
    settlement_date DATE NOT NULL,
    raw_settlement_date VARCHAR(7) NOT NULL,
    
    -- Report Identification (Positions 59-63)
    report_group VARCHAR(1) NOT NULL,
    report_subgroup VARCHAR(1) NOT NULL,
    report_id_number VARCHAR(3) NOT NULL,
    
    -- Business Classification (Positions 94-95)
    amount_type VARCHAR(1),
    business_mode VARCHAR(1) NOT NULL,
    
    -- Financial Amounts (Positions 111-155)
    credit_amount DECIMAL(15,2),
    debit_amount DECIMAL(15,2),
    net_amount DECIMAL(15,2),
    
    -- Additional Metadata
    currency_code VARCHAR(3) DEFAULT 'USD',
    transaction_count INTEGER,
    raw_record_line TEXT,
    is_valid BOOLEAN DEFAULT TRUE,
    validation_errors TEXT,
    line_number INTEGER,
    
    -- Audit Fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    
    -- Foreign Key Constraints
    FOREIGN KEY (job_id) REFERENCES file_processing_jobs(id) ON DELETE CASCADE,
    
    -- Check Constraints for Data Integrity
    CONSTRAINT chk_vss110_transaction_code CHECK (transaction_code = '46'),
    CONSTRAINT chk_vss110_destination_id CHECK (destination_id ~ '^[0-9]{6}$'),
    CONSTRAINT chk_vss110_report_group CHECK (report_group = 'V'),
    CONSTRAINT chk_vss110_report_subgroup CHECK (report_subgroup = '2'),
    CONSTRAINT chk_vss110_report_id CHECK (report_id_number IN ('110', '111')),
    CONSTRAINT chk_vss110_amount_type CHECK (amount_type IN ('I', 'F', 'C', 'T', ' ') OR amount_type IS NULL),
    CONSTRAINT chk_vss110_business_mode CHECK (business_mode IN ('1', '2', '3', '9')),
    CONSTRAINT chk_vss110_currency_code CHECK (currency_code ~ '^[A-Z]{3}$'),
    CONSTRAINT chk_vss110_raw_settlement_date CHECK (raw_settlement_date ~ '^[0-9]{7}$')
);

-- Create Indexes for Performance
CREATE INDEX idx_vss110_settlement_date ON vss110_settlement_records(settlement_date);
CREATE INDEX idx_vss110_job_id ON vss110_settlement_records(job_id);
CREATE INDEX idx_vss110_transaction_code ON vss110_settlement_records(transaction_code);
CREATE INDEX idx_vss110_destination_id ON vss110_settlement_records(destination_id);
CREATE INDEX idx_vss110_report_id ON vss110_settlement_records(report_id_number);
CREATE INDEX idx_vss110_amount_type ON vss110_settlement_records(amount_type);
CREATE INDEX idx_vss110_business_mode ON vss110_settlement_records(business_mode);
CREATE INDEX idx_vss110_is_valid ON vss110_settlement_records(is_valid);
CREATE INDEX idx_vss110_line_number ON vss110_settlement_records(line_number);

-- Composite Indexes for Common Query Patterns
CREATE INDEX idx_vss110_settlement_date_destination ON vss110_settlement_records(settlement_date, destination_id);
CREATE INDEX idx_vss110_job_report_type ON vss110_settlement_records(job_id, report_id_number);
CREATE INDEX idx_vss110_business_mode_amount_type ON vss110_settlement_records(business_mode, amount_type);

-- Partial Indexes for Specific Use Cases
CREATE INDEX idx_vss110_invalid_records ON vss110_settlement_records(job_id, line_number) WHERE is_valid = false;
CREATE INDEX idx_vss110_detailed_records ON vss110_settlement_records(settlement_date, destination_id) WHERE report_id_number = '110';
CREATE INDEX idx_vss110_summary_records ON vss110_settlement_records(settlement_date, destination_id) WHERE report_id_number = '111';

-- Add Comments for Documentation
COMMENT ON TABLE vss110_settlement_records IS 'VSS-110 settlement records with complete field mapping per Visa specification';

COMMENT ON COLUMN vss110_settlement_records.transaction_code IS 'Transaction code (positions 1-2): Must be "46" for VSS-110/111';
COMMENT ON COLUMN vss110_settlement_records.destination_id IS 'Destination ID (positions 5-10): 6-digit settlement destination identifier';
COMMENT ON COLUMN vss110_settlement_records.reporting_sre_id IS 'Reporting SRE ID (positions 17-26): Settlement Reporting Entity identifier';
COMMENT ON COLUMN vss110_settlement_records.settlement_date IS 'Settlement date parsed from CCYYDDD format (positions 27-33)';
COMMENT ON COLUMN vss110_settlement_records.raw_settlement_date IS 'Raw settlement date string in CCYYDDD format';
COMMENT ON COLUMN vss110_settlement_records.report_group IS 'Report group (position 59): Must be "V" for Visa reports';
COMMENT ON COLUMN vss110_settlement_records.report_subgroup IS 'Report subgroup (position 60): Must be "2" for VSS reports';
COMMENT ON COLUMN vss110_settlement_records.report_id_number IS 'Report ID (positions 61-63): "110" for detailed, "111" for summary';
COMMENT ON COLUMN vss110_settlement_records.amount_type IS 'Amount type (position 94): I=Interchange, F=Processing, C=Chargeback, T=Total, Space=Summary';
COMMENT ON COLUMN vss110_settlement_records.business_mode IS 'Business mode (position 95): 1=Acquirer, 2=Issuer, 3=Other, 9=Total';
COMMENT ON COLUMN vss110_settlement_records.credit_amount IS 'Credit amount (positions 111-125): 15-digit amount with implied 2 decimals';
COMMENT ON COLUMN vss110_settlement_records.debit_amount IS 'Debit amount (positions 126-140): 15-digit amount with implied 2 decimals';
COMMENT ON COLUMN vss110_settlement_records.net_amount IS 'Net amount (positions 141-155): Credit minus Debit amount';
COMMENT ON COLUMN vss110_settlement_records.raw_record_line IS 'Complete 168-character raw record line for audit purposes';

-- Create trigger for updated_at timestamp
CREATE OR REPLACE FUNCTION update_vss110_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_vss110_updated_at
    BEFORE UPDATE ON vss110_settlement_records
    FOR EACH ROW
    EXECUTE FUNCTION update_vss110_updated_at();

-- Grant appropriate permissions (adjust as needed for your security model)
-- GRANT SELECT, INSERT, UPDATE, DELETE ON vss110_settlement_records TO cardexis_app_role;
-- GRANT USAGE ON SEQUENCE vss110_settlement_records_id_seq TO cardexis_app_role;