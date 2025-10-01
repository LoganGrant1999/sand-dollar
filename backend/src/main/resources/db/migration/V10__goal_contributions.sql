-- Create goal contributions table
CREATE TABLE goal_contribution (
    id BIGSERIAL PRIMARY KEY,
    goal_id BIGINT NOT NULL REFERENCES goal(id) ON DELETE CASCADE,
    amount NUMERIC(12,2) NOT NULL,
    source VARCHAR(16) NOT NULL CHECK (source IN ('MANUAL','AUTO')),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Create index on goal_id for efficient queries
CREATE INDEX idx_goal_contribution_goal_id ON goal_contribution(goal_id);

-- Create index on created_at for sorting contributions chronologically
CREATE INDEX idx_goal_contribution_created_at ON goal_contribution(created_at);