-- Add Plaid account metadata
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS plaid_account_id VARCHAR(255);
UPDATE accounts SET plaid_account_id = account_id WHERE plaid_account_id IS NULL;
ALTER TABLE accounts ALTER COLUMN plaid_account_id SET NOT NULL;
ALTER TABLE accounts ADD CONSTRAINT uq_accounts_plaid_account_id UNIQUE (plaid_account_id);

-- Add Plaid transaction metadata
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS plaid_transaction_id VARCHAR(255);
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS pending_transaction_id VARCHAR(255);
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS pending BOOLEAN DEFAULT FALSE;
UPDATE transactions SET plaid_transaction_id = external_id WHERE plaid_transaction_id IS NULL;
UPDATE transactions SET pending = COALESCE(pending, FALSE);
ALTER TABLE transactions ALTER COLUMN pending SET NOT NULL;
ALTER TABLE transactions ADD CONSTRAINT uq_transactions_plaid_transaction_id UNIQUE (plaid_transaction_id);
CREATE INDEX IF NOT EXISTS idx_transactions_pending_transaction_id ON transactions(pending_transaction_id);
