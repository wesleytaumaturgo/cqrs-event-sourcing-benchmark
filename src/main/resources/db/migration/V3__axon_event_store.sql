-- V3__axon_event_store.sql
-- Schema padrão Axon Framework (PostgreSQL, sem Axon Server)

CREATE SEQUENCE domain_event_entry_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE domain_event_entry (
    global_index         BIGINT        NOT NULL DEFAULT nextval('domain_event_entry_seq'),
    event_identifier     VARCHAR(255)  NOT NULL UNIQUE,
    meta_data            OID,
    payload              OID           NOT NULL,
    payload_revision     VARCHAR(255),
    payload_type         VARCHAR(255)  NOT NULL,
    time_stamp           VARCHAR(255)  NOT NULL,
    aggregate_identifier VARCHAR(255)  NOT NULL,
    sequence_number      BIGINT        NOT NULL,
    type                 VARCHAR(255),
    PRIMARY KEY (global_index),
    CONSTRAINT uk_axon_aggid_seq UNIQUE (aggregate_identifier, sequence_number)
);

CREATE INDEX idx_axon_events_aggid
    ON domain_event_entry(aggregate_identifier, sequence_number);

CREATE TABLE snapshot_event_entry (
    aggregate_identifier VARCHAR(255)  NOT NULL,
    sequence_number      BIGINT        NOT NULL,
    type                 VARCHAR(255)  NOT NULL,
    event_identifier     VARCHAR(255)  NOT NULL UNIQUE,
    meta_data            OID,
    payload              OID           NOT NULL,
    payload_revision     VARCHAR(255),
    payload_type         VARCHAR(255)  NOT NULL,
    time_stamp           VARCHAR(255)  NOT NULL,
    PRIMARY KEY (aggregate_identifier, sequence_number, type)
);

CREATE TABLE token_entry (
    processor_name VARCHAR(255) NOT NULL,
    segment        INT          NOT NULL,
    token          BYTEA,
    token_type     VARCHAR(255),
    timestamp      VARCHAR(255),
    owner          VARCHAR(255),
    PRIMARY KEY (processor_name, segment)
);

-- Projeção de saldo da conta (ES Axon)
CREATE TABLE axon_account_balance_view (
    account_id   VARCHAR(255)  PRIMARY KEY,
    owner_id     VARCHAR(255)  NOT NULL,
    balance      NUMERIC(19,2) NOT NULL DEFAULT 0,
    last_updated TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    version      BIGINT        NOT NULL DEFAULT 0
);
