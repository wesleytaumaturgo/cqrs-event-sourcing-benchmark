-- V2__account_balance_view.sql
-- Projeção de saldo da conta (ES Manual)
CREATE TABLE account_balance_view (
    account_id   UUID          PRIMARY KEY,
    owner_id     VARCHAR(255)  NOT NULL,
    balance      NUMERIC(19,2) NOT NULL DEFAULT 0,
    last_updated TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    version      BIGINT        NOT NULL DEFAULT 0
);
