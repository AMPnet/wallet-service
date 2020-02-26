CREATE TABLE revenue_payout(
    id SERIAL PRIMARY KEY,
    project_uuid UUID NOT NULL,
    amount BIGINT NOT NULL,
    created_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    tx_hash VARCHAR,
    completed_at TIMESTAMP
);
CREATE INDEX idx_revenue_payout_project_uuid ON revenue_payout(project_uuid);
