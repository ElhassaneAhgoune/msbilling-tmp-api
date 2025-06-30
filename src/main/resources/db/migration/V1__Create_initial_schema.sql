-- Visa EPIN Settlement Service Database Schema
-- Initial migration for VSS-110 and VSS-120 processing

-- File Processing Jobs Table
CREATE TABLE file_processing_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    original_filename VARCHAR(255) NOT NULL,
    file_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    report_format VARCHAR(20),
    client_id VARCHAR(50),
    file_size_bytes BIGINT,
    total_records INTEGER,
    processed_records INTEGER,
    failed_records INTEGER,
    processing_started_at TIMESTAMP,
    processing_completed_at TIMESTAMP,
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- File Processing Job Metadata Table
CREATE TABLE file_processing_job_metadata (
    job_id UUID NOT NULL,
    metadata_key VARCHAR(255) NOT NULL,
    metadata_value VARCHAR(1000),
    PRIMARY KEY (job_id, metadata_key),
    FOREIGN KEY (job_id) REFERENCES file_processing_jobs(id) ON DELETE CASCADE
);

-- EPIN File Headers Table
CREATE TABLE epin_file_headers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL,
    routing_number VARCHAR(20) NOT NULL,
    file_timestamp TIMESTAMP NOT NULL,
    raw_timestamp VARCHAR(20) NOT NULL,
    sequence_number VARCHAR(10),
    client_id VARCHAR(20),
    file_sequence VARCHAR(10),
    raw_header_line TEXT,
    is_valid BOOLEAN DEFAULT TRUE,
    validation_errors TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (job_id) REFERENCES file_processing_jobs(id) ON DELETE CASCADE
);

-- VSS-110 Settlement Records Table
CREATE TABLE vss110_settlement_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL,
    destination_identifier VARCHAR(50),
    record_type VARCHAR(10) NOT NULL,
    settlement_date DATE NOT NULL,
    raw_date VARCHAR(10) NOT NULL,
    fee_category VARCHAR(5) NOT NULL,
    amount DECIMAL(15,2),
    credit_debit_indicator VARCHAR(5),
    currency_code VARCHAR(3) DEFAULT 'USD',
    transaction_count INTEGER,
    raw_record_line TEXT,
    is_valid BOOLEAN DEFAULT TRUE,
    validation_errors TEXT,
    line_number INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (job_id) REFERENCES file_processing_jobs(id) ON DELETE CASCADE
);

-- VSS-120 Settlement Records Table
CREATE TABLE vss120_settlement_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL,
    destination_identifier VARCHAR(50),
    record_type VARCHAR(10) NOT NULL,
    settlement_date DATE NOT NULL,
    raw_date VARCHAR(10) NOT NULL,
    business_mode VARCHAR(5),
    transaction_cycle VARCHAR(5),
    reporting_for_sre_identifier VARCHAR(50),
    currency_code VARCHAR(3) DEFAULT 'USD',
    raw_record_line TEXT,
    is_valid BOOLEAN DEFAULT TRUE,
    validation_errors TEXT,
    line_number INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (job_id) REFERENCES file_processing_jobs(id) ON DELETE CASCADE
);

-- VSS-120 Financial Details Table
CREATE TABLE vss120_financial_details (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL,
    settlement_record_id UUID NOT NULL,
    rate_table_id VARCHAR(20),
    transaction_volume BIGINT,
    amount1 DECIMAL(15,2),
    amount2 DECIMAL(15,2),
    amount3 DECIMAL(15,2),
    amount4 DECIMAL(15,2),
    amount5 DECIMAL(15,2),
    amount6 DECIMAL(15,2),
    sign1 VARCHAR(5),
    sign2 VARCHAR(5),
    sign3 VARCHAR(5),
    sign4 VARCHAR(5),
    sign5 VARCHAR(5),
    sign6 VARCHAR(5),
    currency_code VARCHAR(3) DEFAULT 'USD',
    raw_record_line TEXT,
    is_valid BOOLEAN DEFAULT TRUE,
    validation_errors TEXT,
    line_number INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (job_id) REFERENCES file_processing_jobs(id) ON DELETE CASCADE,
    FOREIGN KEY (settlement_record_id) REFERENCES vss120_settlement_records(id) ON DELETE CASCADE
);

-- Indexes for Performance
CREATE INDEX idx_file_processing_jobs_status ON file_processing_jobs(status);
CREATE INDEX idx_file_processing_jobs_client_id ON file_processing_jobs(client_id);
CREATE INDEX idx_file_processing_jobs_created_at ON file_processing_jobs(created_at);
CREATE INDEX idx_file_processing_jobs_filename ON file_processing_jobs(original_filename);

CREATE INDEX idx_epin_file_headers_routing_number ON epin_file_headers(routing_number);
CREATE INDEX idx_epin_file_headers_client_id ON epin_file_headers(client_id);
CREATE INDEX idx_epin_file_headers_file_timestamp ON epin_file_headers(file_timestamp);
CREATE INDEX idx_epin_file_headers_job_id ON epin_file_headers(job_id);

CREATE INDEX idx_vss110_settlement_date ON vss110_settlement_records(settlement_date);
CREATE INDEX idx_vss110_job_id ON vss110_settlement_records(job_id);
CREATE INDEX idx_vss110_fee_category ON vss110_settlement_records(fee_category);
CREATE INDEX idx_vss110_destination_id ON vss110_settlement_records(destination_identifier);

CREATE INDEX idx_vss120_settlement_date ON vss120_settlement_records(settlement_date);
CREATE INDEX idx_vss120_job_id ON vss120_settlement_records(job_id);
CREATE INDEX idx_vss120_business_mode ON vss120_settlement_records(business_mode);
CREATE INDEX idx_vss120_transaction_cycle ON vss120_settlement_records(transaction_cycle);
CREATE INDEX idx_vss120_sre_identifier ON vss120_settlement_records(reporting_for_sre_identifier);

CREATE INDEX idx_vss120_financial_details_job_id ON vss120_financial_details(job_id);
CREATE INDEX idx_vss120_financial_details_settlement_record_id ON vss120_financial_details(settlement_record_id);
CREATE INDEX idx_vss120_financial_details_rate_table_id ON vss120_financial_details(rate_table_id);

-- Comments for Documentation
COMMENT ON TABLE file_processing_jobs IS 'Main table tracking EPIN file processing jobs';
COMMENT ON TABLE epin_file_headers IS 'EPIN file header information with routing and control data';
COMMENT ON TABLE vss110_settlement_records IS 'VSS-110 settlement summary records with fee categories';
COMMENT ON TABLE vss120_settlement_records IS 'VSS-120 enhanced settlement records with business modes';
COMMENT ON TABLE vss120_financial_details IS 'Financial detail records (4601) associated with VSS-120';

COMMENT ON COLUMN file_processing_jobs.status IS 'Processing status: UPLOADED, PROCESSING, COMPLETED, FAILED, CANCELLED';
COMMENT ON COLUMN file_processing_jobs.report_format IS 'Detected Visa report format: VSS_110, VSS_120, etc.';
COMMENT ON COLUMN vss110_settlement_records.fee_category IS 'Fee category: I1-I9 (Interchange), F1-F9 (Processing), C1-C9 (Chargeback), T1-T9 (Total)';
COMMENT ON COLUMN vss120_settlement_records.business_mode IS 'Business mode: A (Acquirer), I (Issuer), O (Other), T (Total)';
COMMENT ON COLUMN vss120_settlement_records.transaction_cycle IS 'Transaction cycle: 1 (Original), 2 (Chargeback), 3 (Representment), 4 (Dispute)';