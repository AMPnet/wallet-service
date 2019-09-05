CREATE INDEX idx_file_link ON file(link);

CREATE INDEX idx_wallet_activation_data ON wallet(activation_data);
CREATE INDEX idx_pair_wallet_code_public_key ON pair_wallet_code(public_key);

CREATE INDEX idx_deposit_reference ON deposit(reference);
CREATE INDEX idx_deposit_user_uuid ON deposit(user_uuid);
CREATE INDEX idx_withdraw_user_uuid ON withdraw(user_uuid);
