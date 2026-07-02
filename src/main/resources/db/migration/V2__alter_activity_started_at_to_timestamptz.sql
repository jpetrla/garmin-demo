ALTER TABLE activity
ALTER COLUMN started_at TYPE timestamp with time zone
      USING started_at AT TIME ZONE 'UTC';