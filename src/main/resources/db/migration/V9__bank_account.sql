CREATE TABLE bank_account(
    id SERIAL PRIMARY KEY,
    iban VARCHAR(64) NOT NULL,
    bank_code VARCHAR(16) NOT NULL,
    created_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    alias VARCHAR(128)
);
