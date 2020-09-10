-- Wallet
CREATE TABLE wallet (
    uuid UUID PRIMARY KEY,
    owner UUID NOT NULL,
    activation_data VARCHAR(128) NOT NULL,
    activated_at TIMESTAMP,
    hash VARCHAR(128) UNIQUE,
    type VARCHAR(8) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    coop VARCHAR(64) NOT NULL,
    alias VARCHAR(128),
    CONSTRAINT uc_wallet_in_coop UNIQUE(activation_data, coop)
);
CREATE TABLE pair_wallet_code(
    id SERIAL PRIMARY KEY,
    public_key VARCHAR(128) UNIQUE NOT NULL,
    code VARCHAR(6) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL
);

-- File
CREATE TABLE file (
    id SERIAL PRIMARY KEY,
    link VARCHAR NOT NULL,
    name VARCHAR NOT NULL,
    type VARCHAR(16) NOT NULL,
    size INT NOT NULL,
    created_by_user_uuid UUID NOT NULL,
    created_at TIMESTAMP NOT NULL
);

-- Transaction
CREATE TABLE transaction_info (
  id SERIAL PRIMARY KEY,
  type VARCHAR(16) NOT NULL,
  description VARCHAR NOT NULL,
  user_uuid UUID NOT NULL,
  companion_data VARCHAR(128),
  coop VARCHAR(64) NOT NULL
);

-- Deposit
CREATE TABLE declined(
    id SERIAL PRIMARY KEY,
    comment VARCHAR NOT NULL,
    created_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL
);
CREATE TABLE deposit(
    id SERIAL PRIMARY KEY,
    owner_uuid UUID NOT NULL,
    reference VARCHAR(16) NOT NULL,
    amount BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    coop VARCHAR(64) NOT NULL,
    type VARCHAR(8) NOT NULL,
    created_by UUID NOT NULL,
    approved BOOLEAN NOT NULL,
    approved_by_user_uuid UUID,
    approved_at TIMESTAMP,
    declined_id INT REFERENCES declined(id),
    file_id INT REFERENCES file(id),
    tx_hash VARCHAR
);

-- Withdraw
CREATE TABLE withdraw(
    id SERIAL PRIMARY KEY,
    owner_uuid UUID NOT NULL,
    amount BIGINT NOT NULL,
    bank_account VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    coop VARCHAR(64) NOT NULL,
    type VARCHAR(8) NOT NULL,
    created_by UUID NOT NULL,
    approved_tx_hash VARCHAR,
    approved_at TIMESTAMP,
    burned_tx_hash VARCHAR,
    burned_at TIMESTAMP,
    burned_by UUID,
    file_id INT REFERENCES file(id)
);

-- Revenue payout
CREATE TABLE revenue_payout(
    id SERIAL PRIMARY KEY,
    project_uuid UUID NOT NULL,
    amount BIGINT NOT NULL,
    created_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    coop VARCHAR(64) NOT NULL,
    tx_hash VARCHAR,
    completed_at TIMESTAMP
);

-- Bank account
CREATE TABLE bank_account(
    id SERIAL PRIMARY KEY,
    iban VARCHAR(64) NOT NULL,
    bank_code VARCHAR(16) NOT NULL,
    created_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    coop VARCHAR(64) NOT NULL,
    alias VARCHAR(128)
);
