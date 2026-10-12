ALTER TABLE completion_activity ADD COLUMN IF NOT EXISTS estimated_minutes INTEGER;
UPDATE completion_activity SET estimated_minutes = COALESCE((SELECT tasks.estimated_minutes FROM tasks WHERE tasks.id = completion_activity.task_id), 0) WHERE estimated_minutes IS NULL;
ALTER TABLE user_settings ADD COLUMN IF NOT EXISTS focus_minutes INTEGER DEFAULT 25 NOT NULL;
ALTER TABLE user_settings ADD COLUMN IF NOT EXISTS break_minutes INTEGER DEFAULT 5 NOT NULL;
ALTER TABLE user_settings ALTER COLUMN focus_minutes SET DEFAULT 25;
ALTER TABLE user_settings ALTER COLUMN break_minutes SET DEFAULT 5;
