-- Create goals table
CREATE TABLE goal (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    goal_type VARCHAR(32) NOT NULL CHECK (goal_type IN ('TRIP','PURCHASE')),
    name VARCHAR(120) NOT NULL,
    target_amount NUMERIC(12,2) NOT NULL,
    target_date DATE NOT NULL,
    plan_monthly_contribution NUMERIC(12,2),
    status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    trip_metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Create index on user_id for efficient queries
CREATE INDEX idx_goal_user_id ON goal(user_id);

-- Create index on status for filtering active goals
CREATE INDEX idx_goal_status ON goal(status);