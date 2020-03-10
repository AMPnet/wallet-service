CREATE TABLE declined(
    id SERIAL PRIMARY KEY,
    comment VARCHAR NOT NULL,
    created_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL
);

ALTER TABLE deposit ADD COLUMN declined_id INT REFERENCES declined(id);
CREATE INDEX idx_deposit_decliend_id ON deposit(declined_id);
