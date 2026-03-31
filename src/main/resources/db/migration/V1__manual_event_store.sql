-- V1__manual_event_store.sql
-- ES Manual: tabela de eventos de domínio com payload JSONB
CREATE TABLE domain_events (
    id               BIGSERIAL     PRIMARY KEY,
    aggregate_id     UUID          NOT NULL,
    aggregate_type   VARCHAR(100)  NOT NULL,
    sequence_number  BIGINT        NOT NULL,
    event_type       VARCHAR(200)  NOT NULL,
    payload          JSONB         NOT NULL,
    occurred_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_aggregate_sequence UNIQUE (aggregate_id, sequence_number)
);

CREATE INDEX idx_domain_events_aggregate_id
    ON domain_events(aggregate_id);

CREATE INDEX idx_domain_events_aggregate_seq
    ON domain_events(aggregate_id, sequence_number);
