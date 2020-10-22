CREATE INDEX idx_wallet_activation_data_coop ON wallet(activation_data, coop);
CREATE INDEX idx_wallet_owner ON wallet(owner);

CREATE INDEX idx_deposit_reference_coop ON deposit(reference, coop);
CREATE INDEX idx_deposit_owner_uuid ON deposit(owner_uuid);
CREATE INDEX idx_withdraw_owner_uuid ON withdraw(owner_uuid);

CREATE INDEX idx_revenue_payout_project_uuid ON revenue_payout(project_uuid);
CREATE INDEX idx_deposit_decliend_id ON deposit(declined_id);
