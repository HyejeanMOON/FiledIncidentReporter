ALTER TABLE incidents ADD COLUMN resolved_at TIMESTAMP(6) WITH TIME ZONE;

CREATE INDEX idx_incidents_resolved_created_id ON incidents (resolved_at, created_at DESC, id DESC);
