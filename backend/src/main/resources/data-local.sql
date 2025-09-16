-- Local demo dataset for AI budgeting flows
-- Loads when spring.profiles.active=local

-- Base user
INSERT INTO users (id, email, password_hash, first_name, last_name, created_at, updated_at)
VALUES (9001, 'ai.demo@sanddollar.local', '$2a$10$N8zfV4PjF31PSU8WINFCAuqUbZOqF8P2kGRlwBKr2lNqvKwgaUE5C', 'AI', 'Demo', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Minimal Plaid + account scaffolding
INSERT INTO plaid_items (id, user_id, item_id, institution_id, institution_name, status, access_token_encrypted, created_at, updated_at)
VALUES (9001, 9001, 'local-item-demo', 'ins_000000', 'Local Bank', 'ACTIVE', 'demo-token', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO accounts (id, user_id, plaid_item_id, account_id, mask, name, institution_name, type, subtype, created_at, updated_at)
VALUES (9001, 9001, 9001, 'local-account-demo', '1234', 'Primary Checking', 'Local Bank', 'depository', 'checking', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Seed three months of expenses matching the reference scenario (amounts in cents)
WITH categories(category, amount_cents) AS (
    VALUES 
        ('Rent', 150000),
        ('Groceries', 42000),
        ('Dining', 36000),
        ('Transport', 12000),
        ('Utilities', 16000),
        ('Gym', 4000),
        ('Subscriptions', 7000),
        ('Misc', 13000)
), months AS (
    SELECT generate_series(0, 2) AS m
)
INSERT INTO transactions (account_id, external_id, date, name, merchant_name, amount_cents, currency, category_top, category_sub, is_transfer, created_at, updated_at)
SELECT 
    9001,
    CONCAT('local-txn-', category, '-', m.m),
    (date_trunc('month', CURRENT_DATE) - (m.m || ' months')::interval) + INTERVAL '5 days',
    CONCAT(category, ' spend'),
    category,
    -amount_cents,
    'USD',
    category,
    category,
    FALSE,
    NOW(),
    NOW()
FROM categories c
CROSS JOIN months m
ON CONFLICT (external_id) DO NOTHING;

-- Sample goals for UI context
INSERT INTO goals (id, user_id, name, target_cents, target_date, saved_cents, status, created_at, updated_at)
VALUES
    (9001, 9001, 'Emergency fund to $5k', 500000, CURRENT_DATE + INTERVAL '3 months', 120000, 'ACTIVE', NOW(), NOW()),
    (9002, 9001, 'Pay down credit card', 240000, CURRENT_DATE + INTERVAL '12 months', 0, 'ACTIVE', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Accepted AI budget for the current month (targets in cents)
INSERT INTO budget_targets (user_id, month, category, target_cents, reason, created_at)
VALUES
    (9001, to_char(CURRENT_DATE, 'YYYY-MM'), 'Rent', 150000, 'Fixed rent payment', NOW()),
    (9001, to_char(CURRENT_DATE, 'YYYY-MM'), 'Groceries', 38000, 'Dial groceries back slightly to boost savings', NOW()),
    (9001, to_char(CURRENT_DATE, 'YYYY-MM'), 'Dining', 30000, 'Cap dining out per your request', NOW()),
    (9001, to_char(CURRENT_DATE, 'YYYY-MM'), 'Transport', 12000, 'Average of last quarter fuel and ride share costs', NOW()),
    (9001, to_char(CURRENT_DATE, 'YYYY-MM'), 'Utilities', 17000, 'Allow a cushion for seasonal swings', NOW()),
    (9001, to_char(CURRENT_DATE, 'YYYY-MM'), 'Gym', 4000, 'Keep existing membership', NOW()),
    (9001, to_char(CURRENT_DATE, 'YYYY-MM'), 'Subscriptions', 6000, 'Trim unused services slightly', NOW()),
    (9001, to_char(CURRENT_DATE, 'YYYY-MM'), 'Misc', 15000, 'Set aside for small surprises', NOW()),
    (9001, to_char(CURRENT_DATE, 'YYYY-MM'), 'Emergency Fund', 50000, 'Put $500 toward the March goal', NOW()),
    (9001, to_char(CURRENT_DATE, 'YYYY-MM'), 'Card Paydown', 20000, 'Allocate $200 toward credit card balance', NOW())
ON CONFLICT (user_id, month, category) DO NOTHING;

-- Adjust sequences so locally inserted IDs don’t collide
SELECT setval('users_id_seq', GREATEST((SELECT MAX(id) FROM users), nextval('users_id_seq')));
SELECT setval('plaid_items_id_seq', GREATEST((SELECT MAX(id) FROM plaid_items), nextval('plaid_items_id_seq')));
SELECT setval('accounts_id_seq', GREATEST((SELECT MAX(id) FROM accounts), nextval('accounts_id_seq')));
SELECT setval('transactions_id_seq', GREATEST((SELECT MAX(id) FROM transactions), nextval('transactions_id_seq')));
SELECT setval('goals_id_seq', GREATEST((SELECT MAX(id) FROM goals), nextval('goals_id_seq')));
SELECT setval('budget_targets_id_seq', GREATEST((SELECT MAX(id) FROM budget_targets), nextval('budget_targets_id_seq')));
