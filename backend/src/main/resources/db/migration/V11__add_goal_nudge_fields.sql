-- Add nudge fields to goals table
ALTER TABLE goals
ADD COLUMN IF NOT EXISTS nudge_suggested BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS nudge_amount NUMERIC(12,2);