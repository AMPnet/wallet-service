-- Wallet
CREATE TABLE wallet (
    uuid UUID PRIMARY KEY,
    owner UUID NOT NULL,
    activation_data VARCHAR(128) UNIQUE NOT NULL,
    activated_at TIMESTAMP,
    hash VARCHAR(128) UNIQUE,
    type VARCHAR(8) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMP NOT NULL
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
  title VARCHAR NOT NULL,
  description VARCHAR NOT NULL,
  user_uuid UUID NOT NULL,
  companion_data VARCHAR(128)
);

-- Deposit
CREATE TABLE deposit(
    id SERIAL PRIMARY KEY,
    user_uuid UUID NOT NULL,
    reference VARCHAR(16) NOT NULL,
    amount BIGINT NOT NULL,
    approved BOOLEAN NOT NULL,
    approved_by_user_uuid UUID,
    approved_at TIMESTAMP,
    file_id INT REFERENCES file(id),
    tx_hash VARCHAR,
    created_at TIMESTAMP NOT NULL
);

-- Withdraw
CREATE TABLE withdraw(
    id SERIAL PRIMARY KEY,
    user_uuid UUID NOT NULL,
    amount BIGINT NOT NULL,
    bank_account VARCHAR(64) NOT NULL,
    approved_tx_hash VARCHAR,
    approved_at TIMESTAMP,
    burned_tx_hash VARCHAR,
    burned_at TIMESTAMP,
    burned_by UUID,
    file_id INT REFERENCES file(id),
    created_at TIMESTAMP NOT NULL
);
