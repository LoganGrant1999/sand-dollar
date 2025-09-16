-- V5__Create_budget_targets_table.sql
-- Table to store AI-generated budget targets accepted by users

CREATE TABLE budget_targets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    month CHAR(7) NOT NULL, -- YYYY-MM format
    category TEXT NOT NULL,
    target_cents INTEGER NOT NULL,
    reason TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Ensure one target per user/month/category combination
CREATE UNIQUE INDEX idx_budget_targets_user_month_category 
ON budget_targets (user_id, month, category);

-- Index for efficient queries by user and month
CREATE INDEX idx_budget_targets_user_month 
ON budget_targets (user_id, month);

-- Add comments for documentation
COMMENT ON TABLE budget_targets IS 'Stores AI-generated budget targets accepted by users';
COMMENT ON COLUMN budget_targets.month IS 'Month in YYYY-MM format (e.g., 2025-09)';
COMMENT ON COLUMN budget_targets.target_cents IS 'Target amount in cents for consistent precision';
COMMENT ON COLUMN budget_targets.reason IS 'AI-generated explanation for this target';