-- Add OAuth2 fields to users table
ALTER TABLE users
ADD COLUMN IF NOT EXISTS provider VARCHAR(32) DEFAULT 'local',
ADD COLUMN IF NOT EXISTS provider_user_id VARCHAR(255);

-- Make password_hash nullable for OAuth users
ALTER TABLE users
ALTER COLUMN password_hash DROP NOT NULL;

-- Add unique constraint for OAuth lookups (ensure no duplicate provider + sub)
CREATE UNIQUE INDEX IF NOT EXISTS users_provider_uid_uq
ON users (provider, provider_user_id)
WHERE provider_user_id IS NOT NULL;

-- Add regular index for performance
CREATE INDEX IF NOT EXISTS idx_users_provider_user_id ON users(provider, provider_user_id);

-- Update existing users to have 'local' provider
UPDATE users SET provider = 'local' WHERE provider IS NULL;

-- Make provider NOT NULL after setting defaults
ALTER TABLE users ALTER COLUMN provider SET NOT NULL;