-- Align schema with JPA entity definitions (bigint IDs, varchar month)

DROP TABLE IF EXISTS budget_targets;

CREATE TABLE budget_targets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    month VARCHAR(7) NOT NULL,
    category TEXT NOT NULL,
    target_cents INTEGER NOT NULL,
    reason TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_budget_targets_user_month_category
    ON budget_targets (user_id, month, category);

CREATE INDEX idx_budget_targets_user_month
    ON budget_targets (user_id, month);
