CREATE INDEX idx_file_link ON file(link);

CREATE INDEX idx_wallet_activation_data ON wallet(activation_data);
CREATE INDEX idx_pair_wallet_code_public_key ON pair_wallet_code(public_key);

CREATE INDEX idx_deposit_reference ON deposit(reference);
CREATE INDEX idx_deposit_owner_uuid ON deposit(owner_uuid);
CREATE INDEX idx_withdraw_owner_uuid ON withdraw(owner_uuid);

CREATE INDEX idx_revenue_payout_project_uuid ON revenue_payout(project_uuid);
CREATE INDEX idx_deposit_decliend_id ON deposit(declined_id);
