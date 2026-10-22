ALTER TABLE incidents ADD COLUMN updated_at TIMESTAMP(6) WITH TIME ZONE;
UPDATE incidents SET updated_at = created_at;
ALTER TABLE incidents ALTER COLUMN updated_at SET NOT NULL;
