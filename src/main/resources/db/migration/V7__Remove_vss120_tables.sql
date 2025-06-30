-- V7__Remove_vss120_tables.sql
-- Migration to remove VSS-120 related tables as part of simplification to V2110 records only

DROP TABLE IF EXISTS vss120_financial_details CASCADE;
DROP TABLE IF EXISTS vss120_settlement_records CASCADE;