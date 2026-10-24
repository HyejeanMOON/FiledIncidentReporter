ALTER TABLE incident_photos ADD COLUMN photo_id VARCHAR(36);
UPDATE incident_photos SET photo_id = SUBSTRING(photo_uri, 9, 36);
ALTER TABLE incident_photos ALTER COLUMN photo_id SET NOT NULL;
CREATE UNIQUE INDEX idx_incident_photos_photo_id ON incident_photos (photo_id);
