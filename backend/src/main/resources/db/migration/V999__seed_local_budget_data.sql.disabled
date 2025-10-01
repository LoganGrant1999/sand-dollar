-- Local demo dataset for comprehensive AI budgeting flows
-- Loads for local profile testing only

-- Create additional test accounts for comprehensive testing
INSERT INTO accounts (id, user_id, plaid_item_id, account_id, mask, name, institution_name, type, subtype, created_at, updated_at)
VALUES (9002, 9001, 9001, 'local-account-credit', '9876', 'Main Credit Card', 'Local Bank', 'credit', 'credit_card', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Generate realistic current month transactions with proper categories
-- Use current date to ensure data is for this month
WITH categories(category, weekly_amount_cents) AS (
    VALUES
        ('Rent', 0), -- Fixed monthly on 1st
        ('Groceries', 10500), -- ~$105/week
        ('Dining', 9000), -- ~$90/week
        ('Transport', 3000), -- ~$30/week
        ('Utilities', 0), -- Fixed monthly
        ('Gym', 0), -- Fixed monthly
        ('Subscriptions', 0), -- Various dates
        ('Misc', 3250) -- ~$32.50/week
), weeks AS (
    SELECT generate_series(1, 4) AS week_num
), days_in_week AS (
    SELECT generate_series(1, 7) AS day_of_week
)
-- Weekly recurring expenses
INSERT INTO transactions (account_id, external_id, date, name, merchant_name, amount_cents, currency, category_top, category_sub, is_transfer, created_at, updated_at)
SELECT
    9001,
    CONCAT('local-txn-', category, '-week-', week_num, '-', day_of_week),
    DATE_TRUNC('month', CURRENT_DATE) + (week_num - 1) * INTERVAL '7 days' + (day_of_week - 1) * INTERVAL '1 day',
    CASE
        WHEN category = 'Groceries' THEN CONCAT('Grocery Store #', week_num)
        WHEN category = 'Dining' THEN CONCAT(CASE WHEN day_of_week < 6 THEN 'Lunch at' ELSE 'Dinner at' END, ' Restaurant #', week_num)
        WHEN category = 'Transport' THEN CONCAT('Gas Station #', week_num)
        WHEN category = 'Misc' THEN CONCAT('Misc Purchase #', week_num, '-', day_of_week)
        ELSE category
    END,
    category,
    -(weekly_amount_cents + (RANDOM() * 1000)::int - 500), -- Add some variance
    'USD',
    category,
    category,
    FALSE,
    NOW(),
    NOW()
FROM categories c
CROSS JOIN weeks w
CROSS JOIN days_in_week d
WHERE c.weekly_amount_cents > 0
    AND day_of_week <= CASE
        WHEN category = 'Groceries' THEN 2 -- 2 grocery trips per week
        WHEN category = 'Dining' THEN 5 -- 5 dining occasions per week
        WHEN category = 'Transport' THEN 2 -- 2 gas fill-ups per week
        WHEN category = 'Misc' THEN 2 -- 2 misc purchases per week
        ELSE 7
    END
    AND DATE_TRUNC('month', CURRENT_DATE) + (w.week_num - 1) * INTERVAL '7 days' + (d.day_of_week - 1) * INTERVAL '1 day' <= CURRENT_DATE
ON CONFLICT (external_id) DO NOTHING;

-- Fixed monthly expenses
INSERT INTO transactions (account_id, external_id, date, name, merchant_name, amount_cents, currency, category_top, category_sub, is_transfer, created_at, updated_at)
VALUES
    -- Rent on 1st of month
    (9001, CONCAT('local-txn-rent-', TO_CHAR(CURRENT_DATE, 'YYYY-MM')), DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 day', 'Monthly Rent Payment', 'Property Management Co', -150000, 'USD', 'Rent', 'Rent', FALSE, NOW(), NOW()),
    -- Utilities on 15th
    (9001, CONCAT('local-txn-utilities-', TO_CHAR(CURRENT_DATE, 'YYYY-MM')), DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '14 days', 'Electric & Water Bill', 'Local Utilities', -16000, 'USD', 'Utilities', 'Utilities', FALSE, NOW(), NOW()),
    -- Gym on 5th
    (9001, CONCAT('local-txn-gym-', TO_CHAR(CURRENT_DATE, 'YYYY-MM')), DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '4 days', 'Gym Membership', 'Fitness Club', -4000, 'USD', 'Gym', 'Gym', FALSE, NOW(), NOW()),
    -- Subscriptions spread throughout month
    (9001, CONCAT('local-txn-netflix-', TO_CHAR(CURRENT_DATE, 'YYYY-MM')), DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '3 days', 'Netflix Subscription', 'Netflix', -1599, 'USD', 'Subscriptions', 'Entertainment', FALSE, NOW(), NOW()),
    (9001, CONCAT('local-txn-spotify-', TO_CHAR(CURRENT_DATE, 'YYYY-MM')), DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '7 days', 'Spotify Premium', 'Spotify', -999, 'USD', 'Subscriptions', 'Entertainment', FALSE, NOW(), NOW()),
    (9001, CONCAT('local-txn-amazon-', TO_CHAR(CURRENT_DATE, 'YYYY-MM')), DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '12 days', 'Amazon Prime', 'Amazon', -1399, 'USD', 'Subscriptions', 'Shopping', FALSE, NOW(), NOW()),
    (9001, CONCAT('local-txn-cloud-', TO_CHAR(CURRENT_DATE, 'YYYY-MM')), DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '20 days', 'Cloud Storage', 'Google', -299, 'USD', 'Subscriptions', 'Technology', FALSE, NOW(), NOW())
ON CONFLICT (external_id) DO NOTHING;

-- Sample income for the month
INSERT INTO transactions (account_id, external_id, date, name, merchant_name, amount_cents, currency, category_top, category_sub, is_transfer, created_at, updated_at)
VALUES
    (9001, CONCAT('local-txn-salary1-', TO_CHAR(CURRENT_DATE, 'YYYY-MM')), DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '15 days', 'Bi-weekly Salary', 'Employer Direct Deposit', 310000, 'USD', 'Income', 'Salary', FALSE, NOW(), NOW()),
    (9001, CONCAT('local-txn-salary2-', TO_CHAR(CURRENT_DATE, 'YYYY-MM')), DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '29 days', 'Bi-weekly Salary', 'Employer Direct Deposit', 310000, 'USD', 'Income', 'Salary', FALSE, NOW(), NOW())
ON CONFLICT (external_id) DO NOTHING;