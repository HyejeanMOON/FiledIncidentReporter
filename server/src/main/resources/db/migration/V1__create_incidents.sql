CREATE TABLE incidents (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    location VARCHAR(200) NOT NULL,
    description VARCHAR(4000) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_incidents_created_id ON incidents (created_at DESC, id DESC);

CREATE TABLE incident_photos (
    incident_id VARCHAR(36) NOT NULL REFERENCES incidents (id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    photo_uri VARCHAR(2048) NOT NULL,
    PRIMARY KEY (incident_id, position)
);
