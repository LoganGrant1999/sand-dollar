-- Make password_hash nullable for OAuth users
ALTER TABLE users
  ALTER COLUMN password_hash DROP NOT NULL;

-- Create unique index for OAuth lookups if it doesn't exist
CREATE UNIQUE INDEX IF NOT EXISTS users_provider_uid_uq ON users(provider, provider_user_id) WHERE provider IS NOT NULL AND provider_user_id IS NOT NULL;