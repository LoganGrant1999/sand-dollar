-- Add cursor field to plaid_items table for transaction sync
ALTER TABLE plaid_items ADD COLUMN cursor VARCHAR(255);