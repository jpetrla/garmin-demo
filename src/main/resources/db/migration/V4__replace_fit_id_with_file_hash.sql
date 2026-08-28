ALTER TABLE activity
    ADD COLUMN file_hash varchar(64);

UPDATE activity
SET file_hash = 'legacy-' || id;

ALTER TABLE activity
    ALTER COLUMN file_hash SET NOT NULL,
    ADD CONSTRAINT uq_activity_file_hash UNIQUE (file_hash),
    DROP COLUMN fit_id;
