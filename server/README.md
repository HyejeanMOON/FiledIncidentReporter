# Incident API Server

The incident API for the Android app and admin dashboard. Built with Spring Boot, Kotlin, H2, and Flyway.

## Run

Requires JDK 17+. Maven is downloaded by `./mvnw` on first use.

```bash
./init.sh    # once after cloning, and after code changes: builds the JAR and creates the database
./start.sh   # starts the server on http://localhost:8080
```

Arguments to `start.sh` are passed to Spring Boot, for example `./start.sh --server.port=8081`. Re-running `init.sh` keeps existing data.

```bash
./mvnw test      # run tests
./mvnw package   # build the JAR
```

## API

| Method | Route | Purpose |
| --- | --- | --- |
| GET | `/incidents` | List incidents, newest first |
| GET | `/incidents/summary` | Open, resolved, and total counts over all incidents |
| GET | `/incidents/{id}` | Get one incident |
| POST | `/incidents` | Create an incident with photos |
| PATCH | `/incidents/{id}` | Update text, remove photos, add photos |
| PATCH | `/incidents/{id}/resolution` | Resolve or reopen |
| DELETE | `/incidents/{id}` | Delete an incident and its photo files |
| GET | `/incidents/{id}/photos/{photoId}` | Original photo |
| GET | `/incidents/{id}/photos/{photoId}/thumbnail` | Preview, at most 512 px (JPEG when decodable) |

Photo responses are immutable and carry `Cache-Control: private, max-age=31536000, immutable`.

### Incident

```json
{
  "id": "…",
  "title": "Leaking pipe",
  "location": "B2 plant room",
  "description": "…",
  "photos": [{ "id": "…", "url": "/incidents/…/photos/…", "thumbnailUrl": "…/thumbnail" }],
  "createdAt": "2026-10-24T08:00:00Z",
  "updatedAt": "2026-10-24T08:00:00Z",
  "resolved": false,
  "resolvedAt": null
}
```

- `title` and `location`: 1–200 characters; `description`: 1–4000 characters (trimmed).
- 1–20 photos per incident, each an image of at most 10 MB.
- Timestamps are ISO 8601 UTC and set by the server.

### Create and update

`POST` and `PATCH /incidents/{id}` take `multipart/form-data`:

- an `incident` JSON part: `title`, `location`, `description`, and for `PATCH` an optional `deletePhotoIds` array;
- `photos` file parts for new photos (at least one on create).

Unchanged photos are not uploaded again. `PATCH /incidents/{id}/resolution` takes JSON `{"resolved": true}` or `{"resolved": false}`.

### List and paging

`GET /incidents` returns `{ items, page, size, hasNext, nextCursor }`, ordered by `createdAt` then `id`, both descending.

| Parameter | Meaning |
| --- | --- |
| `size` | Page size, 1–100 (default 20) |
| `page` | Numbered page from 0 (used by the dashboard) |
| `cursor` | The previous response's `nextCursor` (used by the app); stable when incidents are added or deleted |
| `q` | Case-insensitive search in title, location, and description |
| `field` | `title`, `location`, or `description` to search only that field; `all` (default) searches every field |
| `resolved` | `true` or `false` to filter by status |

Pass either `page` or `cursor`, not both.

### Errors

Errors use RFC 9457 Problem Details (`application/problem+json`). Clients should rely on the stable `code` field, not `detail`.

| Code | Status |
| --- | --- |
| `INVALID_INCIDENT` | 400 |
| `INVALID_REQUEST` | Other 4xx (malformed request) |
| `INCIDENT_NOT_FOUND` | 404 |
| `RESOURCE_NOT_FOUND` | 404 |
| `PAYLOAD_TOO_LARGE` | 413 |
| `INTERNAL_ERROR` | 500 |

## Database

Data is stored in a local H2 file, `incidents-db.mv.db`, and photo files in `incident-photos/`. Both are ignored by Git. Flyway creates the schema from [`V1__create_incidents.sql`](src/main/resources/db/migration/V1__create_incidents.sql).

```
incidents 1 ──< incident_photos
```

### `incidents`

| Column | Type | Notes |
| --- | --- | --- |
| `id` | `VARCHAR(36)` | Primary key (UUID) |
| `title` | `VARCHAR(200)` | Not null |
| `location` | `VARCHAR(200)` | Not null |
| `description` | `VARCHAR(4000)` | Not null |
| `created_at` | `TIMESTAMP(6) WITH TIME ZONE` | Not null, defaults to now |
| `updated_at` | `TIMESTAMP(6) WITH TIME ZONE` | Not null |
| `resolved_at` | `TIMESTAMP(6) WITH TIME ZONE` | Null while open; the API's `resolved` is `resolved_at IS NOT NULL` |

Indexes:
- `idx_incidents_created_id (created_at DESC, id DESC)` for list ordering and cursor paging.
- `idx_incidents_resolved_created_id (resolved_at, created_at DESC, id DESC)` for the status filter.

### `incident_photos`

| Column | Type | Notes |
| --- | --- | --- |
| `incident_id` | `VARCHAR(36)` | Foreign key to `incidents.id`, `ON DELETE CASCADE` |
| `position` | `INTEGER` | Display order within the incident |
| `photo_id` | `VARCHAR(36)` | Photo ID exposed by the API, unique |
| `photo_uri` | `VARCHAR(2048)` | Stored file, for example `/photos/<uuid>.jpg` in `incident-photos/` |

Primary key: `(incident_id, position)`. Unique index: `idx_incident_photos_photo_id (photo_id)`.

The schema has no upgrade path yet. After changing the migration script, delete `incidents-db.mv.db` and run `./init.sh` again.
