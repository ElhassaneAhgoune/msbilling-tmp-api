create sequence base_entity_seq
    increment by 50;

alter sequence base_entity_seq owner to dev_user;

create table file_processing_jobs
(
    id                      uuid      default gen_random_uuid() not null
        primary key,
    original_filename       varchar(255)                        not null,
    file_type               varchar(20)                         not null,
    status                  varchar(20)                         not null,
    report_format           varchar(20),
    client_id               varchar(50),
    file_size_bytes         bigint,
    total_records           integer,
    processed_records       integer,
    failed_records          integer,
    processing_started_at   timestamp,
    processing_completed_at timestamp,
    error_message           text,
    retry_count             integer   default 0,
    max_retries             integer   default 3,
    created_at              timestamp default CURRENT_TIMESTAMP not null,
    updated_at              timestamp default CURRENT_TIMESTAMP not null,
    version                 bigint    default 0                 not null
);

comment on table file_processing_jobs is 'Main table tracking EPIN file processing jobs';

comment on column file_processing_jobs.status is 'Processing status: UPLOADED, PROCESSING, COMPLETED, FAILED, CANCELLED';

comment on column file_processing_jobs.report_format is 'Detected Visa report format: VSS_110, VSS_120, etc.';

alter table file_processing_jobs
    owner to dev_user;

create index idx_file_processing_jobs_status
    on file_processing_jobs (status);

create index idx_file_processing_jobs_client_id
    on file_processing_jobs (client_id);

create index idx_file_processing_jobs_created_at
    on file_processing_jobs (created_at);

create index idx_file_processing_jobs_filename
    on file_processing_jobs (original_filename);

create table file_processing_job_metadata
(
    job_id         uuid         not null
        references file_processing_jobs
            on delete cascade,
    metadata_key   varchar(255) not null,
    metadata_value varchar(1000),
    primary key (job_id, metadata_key)
);

alter table file_processing_job_metadata
    owner to dev_user;

create table epin_file_headers
(
    id                uuid      default gen_random_uuid() not null
        primary key,
    job_id            uuid                                not null
        references file_processing_jobs
            on delete cascade,
    routing_number    varchar(20)                         not null,
    file_timestamp    timestamp                           not null,
    raw_timestamp     varchar(20)                         not null,
    sequence_number   varchar(10),
    client_id         varchar(20),
    file_sequence     varchar(10),
    raw_header_line   text,
    is_valid          boolean   default true,
    validation_errors text,
    created_at        timestamp default CURRENT_TIMESTAMP not null,
    updated_at        timestamp default CURRENT_TIMESTAMP not null,
    version           bigint    default 0                 not null
);

comment on table epin_file_headers is 'EPIN file header information with routing and control data';

alter table epin_file_headers
    owner to dev_user;

create index idx_epin_file_headers_routing_number
    on epin_file_headers (routing_number);

create index idx_epin_file_headers_client_id
    on epin_file_headers (client_id);

create index idx_epin_file_headers_file_timestamp
    on epin_file_headers (file_timestamp);

create index idx_epin_file_headers_job_id
    on epin_file_headers (job_id);

create table vss110_settlement_records
(
    id                               uuid       default gen_random_uuid() not null
        primary key,
    job_id                           uuid                                 not null
        references file_processing_jobs
            on delete cascade,
    transaction_code                 varchar(2)                           not null
        constraint chk_vss110_transaction_code
            check ((transaction_code)::text = '46'::text),
    destination_id                   varchar(6)                           not null
        constraint chk_vss110_destination_id
            check ((destination_id)::text ~ '^[0-9]{6}$'::text),
    reporting_sre_id                 varchar(100),
    settlement_date                  date                                 not null,
    raw_settlement_date              varchar(7)                           not null
        constraint chk_vss110_raw_settlement_date
            check (((raw_settlement_date)::text ~ '^[0-9]{5,7}$'::text) OR (raw_settlement_date IS NULL)),
    report_group                     varchar(1)                           not null
        constraint chk_vss110_report_group
            check ((report_group)::text = 'V'::text),
    report_subgroup                  varchar(1)                           not null
        constraint chk_vss110_report_subgroup
            check ((report_subgroup)::text = '2'::text),
    report_id_number                 varchar(3)                           not null
        constraint chk_vss110_report_id
            check ((report_id_number)::text = ANY
                   ((ARRAY ['110'::character varying, '111'::character varying])::text[])),
    amount_type                      varchar(1)
        constraint chk_vss110_amount_type
            check (((amount_type)::text = ANY
                    ((ARRAY ['I'::character varying, 'F'::character varying, 'C'::character varying, 'T'::character varying, ' '::character varying])::text[])) OR
                   (amount_type IS NULL)),
    business_mode                    varchar(1)                           not null
        constraint chk_vss110_business_mode
            check ((business_mode)::text = ANY
                   ((ARRAY ['1'::character varying, '2'::character varying, '3'::character varying, '9'::character varying])::text[])),
    credit_amount                    numeric(15, 2),
    debit_amount                     numeric(15, 2),
    net_amount                       numeric(15, 2),
    currency_code                    varchar(3) default '978'::character varying
        constraint chk_vss110_currency_code
            check ((currency_code)::text ~ '^([A-Z]{3}|[0-9]{3})$'::text),
    transaction_count                integer,
    raw_record_line                  text,
    is_valid                         boolean    default true,
    validation_errors                text,
    line_number                      integer,
    created_at                       timestamp  default CURRENT_TIMESTAMP not null,
    updated_at                       timestamp  default CURRENT_TIMESTAMP not null,
    version                          bigint     default 0                 not null,
    rollup_sre_id                    varchar(100),
    funds_transfer_sre_id            varchar(100),
    settlement_service               varchar(20),
    report_date                      date,
    raw_report_date                  varchar(7)
        constraint chk_vss110_raw_report_date
            check (((raw_report_date)::text ~ '^[0-9]{5,7}$'::text) OR (raw_report_date IS NULL)),
    amount_sign                      varchar(2)
        constraint chk_vss110_amount_sign
            check (((amount_sign)::text = ANY ((ARRAY ['CR'::character varying, 'DB'::character varying])::text[])) OR
                   (amount_sign IS NULL) OR (TRIM(BOTH FROM amount_sign) = ''::text) OR
                   ((amount_sign)::text ~ '^\\s*$'::text)),
    transaction_code_qualifier       varchar(1),
    transaction_component_seq_number varchar(1),
    source_identifier                varchar(6),
    no_data_indicator                varchar(1),
    reserved_field_1                 varchar(5),
    report_id_suffix                 varchar(2),
    from_date                        date,
    raw_from_date                    varchar(7),
    to_date                          date,
    raw_to_date                      varchar(7),
    funds_transfer_date              date,
    raw_funds_transfer_date          varchar(7),
    reserved_field_2                 varchar(3),
    reimbursement_attribute          varchar(1)
);

comment on table vss110_settlement_records is 'VSS-110 settlement records with complete field mapping per Visa specification';

comment on column vss110_settlement_records.transaction_code is 'Transaction code (positions 1-2): Must be "46" for VSS-110/111';

comment on column vss110_settlement_records.destination_id is 'Destination ID (positions 5-10): 6-digit settlement destination identifier';

comment on column vss110_settlement_records.reporting_sre_id is 'Reporting SRE ID - increased length to handle longer identifiers';

comment on column vss110_settlement_records.settlement_date is 'Settlement date parsed from CCYYDDD format (positions 27-33)';

comment on column vss110_settlement_records.raw_settlement_date is 'Raw settlement date string in CCYYDDD format';

comment on constraint chk_vss110_raw_settlement_date on vss110_settlement_records is 'Raw settlement date must be 5-7 digits to accommodate both truncated (YYDDD) and full (CCYYDDD) formats';

comment on column vss110_settlement_records.report_group is 'Report group (position 59): Must be "V" for Visa reports';

comment on column vss110_settlement_records.report_subgroup is 'Report subgroup (position 60): Must be "2" for VSS reports';

comment on column vss110_settlement_records.report_id_number is 'Report ID (positions 61-63): "110" for detailed, "111" for summary';

comment on column vss110_settlement_records.amount_type is 'Amount type (position 94): I=Interchange, F=Processing, C=Chargeback, T=Total, Space=Summary';

comment on column vss110_settlement_records.business_mode is 'Business mode (position 95): 1=Acquirer, 2=Issuer, 3=Other, 9=Total';

comment on column vss110_settlement_records.credit_amount is 'Credit amount (positions 111-125): 15-digit amount with implied 2 decimals';

comment on column vss110_settlement_records.debit_amount is 'Debit amount (positions 126-140): 15-digit amount with implied 2 decimals';

comment on column vss110_settlement_records.net_amount is 'Net amount (positions 141-155): Credit minus Debit amount';

comment on column vss110_settlement_records.currency_code is 'Currency code: ISO 4217 alphabetic (USD, EUR) or numeric (840, 978) format';

comment on column vss110_settlement_records.raw_record_line is 'Complete 168-character raw record line for audit purposes';

comment on column vss110_settlement_records.rollup_sre_id is 'Rollup SRE ID - increased length to handle longer identifiers';

comment on column vss110_settlement_records.funds_transfer_sre_id is 'Funds Transfer SRE ID - increased length to handle longer identifiers';

comment on column vss110_settlement_records.settlement_service is 'Settlement service code - increased length to handle various service identifiers';

comment on column vss110_settlement_records.report_date is 'Report generation date (same as settlement date for VSS 110)';

comment on column vss110_settlement_records.raw_report_date is 'Raw report date string in CCYYDDD format';

comment on constraint chk_vss110_raw_report_date on vss110_settlement_records is 'Raw report date must be 5-7 digits to accommodate both truncated (YYDDD) and full (CCYYDDD) formats';

comment on column vss110_settlement_records.amount_sign is 'Amount sign indicator (CR=Credit, DB=Debit)';

comment on constraint chk_vss110_amount_sign on vss110_settlement_records is 'Amount sign must be CR (Credit), DB (Debit), NULL, or contain only spaces';

alter table vss110_settlement_records
    owner to dev_user;

create index idx_vss110_settlement_date
    on vss110_settlement_records (settlement_date);

create index idx_vss110_job_id
    on vss110_settlement_records (job_id);

create index idx_vss110_transaction_code
    on vss110_settlement_records (transaction_code);

create index idx_vss110_destination_id
    on vss110_settlement_records (destination_id);

create index idx_vss110_report_id
    on vss110_settlement_records (report_id_number);

create index idx_vss110_amount_type
    on vss110_settlement_records (amount_type);

create index idx_vss110_business_mode
    on vss110_settlement_records (business_mode);

create index idx_vss110_is_valid
    on vss110_settlement_records (is_valid);

create index idx_vss110_line_number
    on vss110_settlement_records (line_number);

create index idx_vss110_settlement_date_destination
    on vss110_settlement_records (settlement_date, destination_id);

create index idx_vss110_job_report_type
    on vss110_settlement_records (job_id, report_id_number);

create index idx_vss110_business_mode_amount_type
    on vss110_settlement_records (business_mode, amount_type);

create index idx_vss110_invalid_records
    on vss110_settlement_records (job_id, line_number)
    where (is_valid = false);

create index idx_vss110_detailed_records
    on vss110_settlement_records (settlement_date, destination_id)
    where ((report_id_number)::text = '110'::text);

create index idx_vss110_summary_records
    on vss110_settlement_records (settlement_date, destination_id)
    where ((report_id_number)::text = '111'::text);

create index idx_vss110_report_date
    on vss110_settlement_records (report_date);

create index idx_vss110_amount_sign
    on vss110_settlement_records (amount_sign);

create index idx_vss110_settlement_service
    on vss110_settlement_records (settlement_service);

create index idx_vss110_rollup_sre_id
    on vss110_settlement_records (rollup_sre_id);

create index idx_vss110_funds_transfer_sre_id
    on vss110_settlement_records (funds_transfer_sre_id);

create index idx_vss110_source_id
    on vss110_settlement_records (source_identifier);

create index idx_vss110_from_date
    on vss110_settlement_records (from_date);

create index idx_vss110_to_date
    on vss110_settlement_records (to_date);

create table vss120_settlement_records
(
    id                               uuid                     default gen_random_uuid() not null
        primary key,
    created_at                       timestamp with time zone default CURRENT_TIMESTAMP not null,
    updated_at                       timestamp with time zone default CURRENT_TIMESTAMP not null,
    created_by                       varchar(255),
    updated_by                       varchar(255),
    version                          bigint                   default 0,
    job_id                           uuid                                               not null
        constraint fk_vss120_settlement_job
            references file_processing_jobs
            on delete cascade,
    transaction_code                 varchar(2)                                         not null
        constraint vss120_settlement_records_transaction_code_check
            check ((transaction_code)::text = '46'::text),
    transaction_code_qualifier       varchar(1)
        constraint vss120_settlement_records_transaction_code_qualifier_check
            check ((transaction_code_qualifier)::text = '0'::text),
    transaction_component_seq_number varchar(1)
        constraint vss120_settlement_records_transaction_component_seq_numbe_check
            check ((transaction_component_seq_number)::text = '0'::text),
    destination_id                   varchar(6)                                         not null
        constraint vss120_settlement_records_destination_id_check
            check (length((destination_id)::text) = 6),
    source_identifier                varchar(6),
    reporting_sre_id                 varchar(100),
    rollup_sre_id                    varchar(100),
    funds_transfer_sre_id            varchar(100),
    settlement_service               varchar(20),
    settlement_currency_code         varchar(3)               default '978'::character varying,
    clearing_currency_code           varchar(3),
    business_mode                    varchar(1)                                         not null
        constraint vss120_settlement_records_business_mode_check
            check ((business_mode)::text = ANY
                   ((ARRAY ['1'::character varying, '2'::character varying, '3'::character varying, '9'::character varying])::text[])),
    no_data_indicator                varchar(1)
        constraint vss120_settlement_records_no_data_indicator_check
            check ((no_data_indicator)::text = ANY
                   ((ARRAY ['Y'::character varying, ' '::character varying, ''::character varying])::text[])),
    reserved_field                   varchar(1),
    report_group                     varchar(1)                                         not null
        constraint vss120_settlement_records_report_group_check
            check ((report_group)::text = 'V'::text),
    report_subgroup                  varchar(1)                                         not null
        constraint vss120_settlement_records_report_subgroup_check
            check ((report_subgroup)::text = '4'::text),
    report_id_number                 varchar(3)                                         not null
        constraint vss120_settlement_records_report_id_number_check
            check ((report_id_number)::text = ANY
                   ((ARRAY ['120'::character varying, '130'::character varying, '131'::character varying, '135'::character varying, '136'::character varying, '140'::character varying, '210'::character varying, '215'::character varying, '230'::character varying, '640'::character varying])::text[])),
    report_id_suffix                 varchar(2),
    settlement_date                  date                                               not null,
    raw_settlement_date              varchar(7)                                         not null
        constraint vss120_settlement_records_raw_settlement_date_check
            check ((raw_settlement_date)::text ~ '^[0-9]{5,7}$'::text),
    report_date                      date,
    raw_report_date                  varchar(7),
    from_date                        date,
    raw_from_date                    varchar(7),
    to_date                          date,
    raw_to_date                      varchar(7),
    charge_type_code                 varchar(3),
    business_transaction_type        varchar(3),
    business_transaction_cycle       varchar(1),
    reversal_indicator               varchar(1)
        constraint vss120_settlement_records_reversal_indicator_check
            check ((reversal_indicator)::text = ANY
                   ((ARRAY ['Y'::character varying, 'N'::character varying, ' '::character varying, ''::character varying])::text[])),
    return_indicator                 varchar(1)
        constraint vss120_settlement_records_return_indicator_check
            check ((return_indicator)::text = ANY
                   ((ARRAY ['Y'::character varying, 'N'::character varying, ' '::character varying, ''::character varying])::text[])),
    jurisdiction_code                varchar(2),
    interregional_routing_indicator  varchar(1)
        constraint vss120_settlement_records_interregional_routing_indicator_check
            check ((interregional_routing_indicator)::text = ANY
                   ((ARRAY ['Y'::character varying, 'N'::character varying, ' '::character varying, ''::character varying])::text[])),
    source_country_code              varchar(3),
    destination_country_code         varchar(3),
    source_region_code               varchar(2),
    destination_region_code          varchar(2),
    fee_level_descriptor             varchar(16),
    cr_db_net_indicator              varchar(1)
        constraint vss120_settlement_records_cr_db_net_indicator_check
            check ((cr_db_net_indicator)::text = ANY
                   ((ARRAY ['C'::character varying, 'D'::character varying, 'N'::character varying, ' '::character varying, ''::character varying])::text[])),
    summary_level                    varchar(2),
    reserved_field_2                 varchar(2),
    reserved_field_3                 varchar(31),
    reimbursement_attribute          varchar(1),
    raw_record_line                  text,
    is_valid                         boolean                  default true,
    validation_errors                text,
    line_number                      integer
);

comment on table vss120_settlement_records is 'VSS-120 settlement records from Visa EPIN files (TC46, TCR0, Report Group V, Subgroup 4)';

comment on column vss120_settlement_records.transaction_code is 'Must be "46" for VSS-120 records';

comment on column vss120_settlement_records.destination_id is '6-digit identifier for the settlement destination';

comment on column vss120_settlement_records.business_mode is '1=Acquirer, 2=Issuer, 3=Other, 9=Total';

comment on column vss120_settlement_records.report_id_number is 'Valid values: 120, 130, 131, 135, 136, 140, 210, 215, 230, 640';

comment on column vss120_settlement_records.raw_settlement_date is 'Settlement date in CCYYDDD format as received';

alter table vss120_settlement_records
    owner to dev_user;

create index idx_vss120_settlement_date
    on vss120_settlement_records (settlement_date);

create index idx_vss120_job_id
    on vss120_settlement_records (job_id);

create index idx_vss120_transaction_code
    on vss120_settlement_records (transaction_code);

create index idx_vss120_destination_id
    on vss120_settlement_records (destination_id);

create index idx_vss120_report_id
    on vss120_settlement_records (report_id_number);

create index idx_vss120_business_mode
    on vss120_settlement_records (business_mode);

create index idx_vss120_source_id
    on vss120_settlement_records (source_identifier);

create index idx_vss120_from_date
    on vss120_settlement_records (from_date);

create index idx_vss120_to_date
    on vss120_settlement_records (to_date);

create table vss120_tcr1_records
(
    id                               uuid                     default gen_random_uuid() not null
        primary key,
    created_at                       timestamp with time zone default CURRENT_TIMESTAMP not null,
    updated_at                       timestamp with time zone default CURRENT_TIMESTAMP not null,
    created_by                       varchar(255),
    updated_by                       varchar(255),
    version                          bigint                   default 0,
    job_id                           uuid                                               not null
        constraint fk_vss120_tcr1_job
            references file_processing_jobs
            on delete cascade,
    parent_vss120_record_id          uuid
        constraint fk_vss120_tcr1_parent
            references vss120_settlement_records
            on delete cascade,
    transaction_code                 varchar(2)                                         not null
        constraint vss120_tcr1_records_transaction_code_check
            check ((transaction_code)::text = '46'::text),
    transaction_code_qualifier       varchar(1)
        constraint vss120_tcr1_records_transaction_code_qualifier_check
            check ((transaction_code_qualifier)::text = '0'::text),
    transaction_component_seq_number varchar(1)                                         not null
        constraint vss120_tcr1_records_transaction_component_seq_number_check
            check ((transaction_component_seq_number)::text = '1'::text),
    destination_id                   varchar(6)                                         not null
        constraint vss120_tcr1_records_destination_id_check
            check (length((destination_id)::text) = 6),
    rate_table_id                    varchar(5),
    reserved_field                   varchar(2),
    first_count                      bigint,
    second_count                     bigint,
    first_amount                     numeric(15, 2),
    first_amount_sign                varchar(2),
    second_amount                    numeric(15, 2),
    second_amount_sign               varchar(2),
    third_amount                     numeric(15, 2),
    third_amount_sign                varchar(2),
    fourth_amount                    numeric(15, 2),
    fourth_amount_sign               varchar(2),
    fifth_amount                     numeric(15, 2),
    fifth_amount_sign                varchar(2),
    sixth_amount                     numeric(15, 2),
    sixth_amount_sign                varchar(2),
    reserved_field_2                 varchar(25),
    raw_record_line                  text,
    is_valid                         boolean                  default true,
    validation_errors                text,
    line_number                      integer
);

comment on table vss120_tcr1_records is 'TCR1 records for VSS-120 containing count and amount data';

comment on column vss120_tcr1_records.transaction_component_seq_number is 'Always "1" for TCR1 records (distinguishing from TCR0)';

comment on column vss120_tcr1_records.rate_table_id is 'Alpha and numeric values identifying the rate table';

comment on column vss120_tcr1_records.first_amount is '15-character amount with implied 2 decimal places';

comment on column vss120_tcr1_records.first_amount_sign is 'DB for debit or CR for credit';

alter table vss120_tcr1_records
    owner to dev_user;

create index idx_vss120_tcr1_job_id
    on vss120_tcr1_records (job_id);

create index idx_vss120_tcr1_transaction_code
    on vss120_tcr1_records (transaction_code);

create index idx_vss120_tcr1_destination_id
    on vss120_tcr1_records (destination_id);

create index idx_vss120_tcr1_rate_table_id
    on vss120_tcr1_records (rate_table_id);

create index idx_vss120_tcr1_parent_record
    on vss120_tcr1_records (parent_vss120_record_id);

create function uuid_nil() returns uuid
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function uuid_nil() owner to cardexis_user;

create function uuid_ns_dns() returns uuid
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function uuid_ns_dns() owner to cardexis_user;

create function uuid_ns_url() returns uuid
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function uuid_ns_url() owner to cardexis_user;

create function uuid_ns_oid() returns uuid
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function uuid_ns_oid() owner to cardexis_user;

create function uuid_ns_x500() returns uuid
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function uuid_ns_x500() owner to cardexis_user;

create function uuid_generate_v1() returns uuid
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function uuid_generate_v1() owner to cardexis_user;

create function uuid_generate_v1mc() returns uuid
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function uuid_generate_v1mc() owner to cardexis_user;

create function uuid_generate_v3(namespace uuid, name text) returns uuid
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function uuid_generate_v3(uuid, text) owner to cardexis_user;

create function uuid_generate_v4() returns uuid
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function uuid_generate_v4() owner to cardexis_user;

create function uuid_generate_v5(namespace uuid, name text) returns uuid
    immutable
    strict
    parallel safe
    language c
as
$$
begin
-- missing source code
end;
$$;

alter function uuid_generate_v5(uuid, text) owner to cardexis_user;

create function update_vss110_updated_at() returns trigger
    language plpgsql
as
$$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$;

alter function update_vss110_updated_at() owner to dev_user;

create trigger trigger_vss110_updated_at
    before update
    on vss110_settlement_records
    for each row
    execute procedure update_vss110_updated_at();

