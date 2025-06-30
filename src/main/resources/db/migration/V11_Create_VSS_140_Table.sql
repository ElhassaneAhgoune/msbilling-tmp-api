CREATE TABLE vss140_settlement_records (
                                           id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                           job_id UUID NOT NULL,
                                           transaction_code VARCHAR(2) NOT NULL,
                                           transaction_code_qualifier VARCHAR(1),
                                           transaction_component_seq_number VARCHAR(1),
                                           destination_id VARCHAR(6) NOT NULL,
                                           source_identifier VARCHAR(6),
                                           reporting_sre_id VARCHAR(100),
                                           rollup_sre_id VARCHAR(100),
                                           funds_transfer_sre_id VARCHAR(100),
                                           settlement_service VARCHAR(20),
                                           settlement_currency_code VARCHAR(3) DEFAULT '978',
                                           clearing_currency_code VARCHAR(3),
                                           business_mode VARCHAR(1) NOT NULL,
                                           no_data_indicator VARCHAR(1),
                                           reserved_field VARCHAR(1),
                                           report_group VARCHAR(1) NOT NULL,
                                           report_subgroup VARCHAR(1) NOT NULL,
                                           report_id_number VARCHAR(3) NOT NULL,
                                           report_id_suffix VARCHAR(2),
                                           settlement_date DATE NOT NULL,
                                           raw_settlement_date VARCHAR(7) NOT NULL,
                                           report_date DATE,
                                           raw_report_date VARCHAR(7),
                                           from_date DATE,
                                           raw_from_date VARCHAR(7),
                                           to_date DATE,
                                           raw_to_date VARCHAR(7),
                                           charge_type_code VARCHAR(3),
                                           business_transaction_type VARCHAR(3),
                                           business_transaction_cycle VARCHAR(1),
                                           reversal_indicator VARCHAR(1),
                                           return_indicator VARCHAR(1),
                                           jurisdiction_code VARCHAR(2),
                                           interregional_routing_indicator VARCHAR(1),
                                           source_country_code VARCHAR(3),
                                           destination_country_code VARCHAR(3),
                                           source_region_code VARCHAR(2),
                                           destination_region_code VARCHAR(2),
                                           fee_level_descriptor VARCHAR(16),
                                           cr_db_net_indicator VARCHAR(1),
                                           summary_level VARCHAR(2),
                                           reserved_field_2 VARCHAR(2),
                                           reserved_field_3 VARCHAR(31),
                                           reimbursement_attribute VARCHAR(1),
                                           raw_record_line TEXT,
                                           is_valid BOOLEAN DEFAULT true,
                                           validation_errors TEXT,
                                           line_number INTEGER,
                                           created_at TIMESTAMP,
                                           created_by VARCHAR(50),
                                           updated_at TIMESTAMP,
                                           updated_by VARCHAR(50),
                                           FOREIGN KEY (job_id) REFERENCES file_processing_jobs(id),
                                           version BIGINT  DEFAULT 0 NOT NULL
);

-- Create indexes
CREATE INDEX idx_vss140_settlement_date ON vss140_settlement_records(settlement_date);
CREATE INDEX idx_vss140_job_id ON vss140_settlement_records(job_id);
CREATE INDEX idx_vss140_transaction_code ON vss140_settlement_records(transaction_code);
CREATE INDEX idx_vss140_destination_id ON vss140_settlement_records(destination_id);
CREATE INDEX idx_vss140_report_id ON vss140_settlement_records(report_id_number);
CREATE INDEX idx_vss140_business_mode ON vss140_settlement_records(business_mode);
CREATE INDEX idx_vss140_source_id ON vss140_settlement_records(source_identifier);
CREATE INDEX idx_vss140_from_date ON vss140_settlement_records(from_date);
CREATE INDEX idx_vss140_to_date ON vss140_settlement_records(to_date);