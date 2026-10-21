# Filed Incident Reporter Server

A server project built with Spring Boot, Kotlin, and Maven.

## Requirements

- JDK 17 or later
- Maven 3.6.3 or later

## Initialize and run

```bash
./init.sh
./start.sh
```

Run `./init.sh` once after cloning the project. It builds the application, creates the photo directory and local H2 database, and applies the Flyway migrations without starting the HTTP server. Run `./start.sh` for normal use; it listens on `http://localhost:8080` by default. Arguments to `start.sh` are passed to Spring Boot, for example `./start.sh --server.port=8081`.

Re-run `./init.sh` after changing application code or dependencies to rebuild the JAR. Existing incidents and photos are preserved.

## API

Errors use RFC 9457 Problem Details (`application/problem+json`). The `code` field is stable for clients; `detail` is descriptive text and may change. Codes are `INCIDENT_NOT_FOUND` (404), `RESOURCE_NOT_FOUND` (404), `INVALID_INCIDENT` (400), `INVALID_REQUEST` (other malformed requests), `PAYLOAD_TOO_LARGE` (413), and `INTERNAL_ERROR` (500). Unexpected server failures are logged on the server and return a generic detail instead of internal exception text.

`POST /incidents` accepts `multipart/form-data` with an `incident` JSON part (`title`, `location`, `description`) and 1 to 20 `photos` file parts. Each photo must be an image of at most 10 MB. The response contains a generated string ID and server photo paths (`201 Created`). Invalid fields return `400 Bad Request`.

`GET /photos/{filename}` streams a stored photo. Incident responses contain these relative paths so clients can resolve them against the API base URL.

`GET /incidents?page=0&size=20` returns `{ "items": [...], "page": 0, "size": 20, "hasNext": false }`. Pages start at zero, and `size` must be between 1 and 100. Results are ordered newest first. Each item includes `photos`, an array of displayable base64 data URIs. Decodable images are resized to at most 512 pixels on either side and encoded as JPEG at 70% quality. Formats unavailable to the server's image decoder are returned as their original data URI.

`GET /incidents/{id}` returns one incident, including its photo paths. A missing ID returns `404 Not Found`.

`PUT /incidents/{id}` accepts `multipart/form-data` with an `incident` JSON part (`id`, `title`, `location`, `description`, `retainedPhotoUris`) and optional new `photos` file parts. The `id` must match the path. Existing photos omitted from `retainedPhotoUris` are removed. A missing ID returns `404 Not Found`.

`DELETE /incidents/{id}` removes an incident and its stored photo files, then returns `204 No Content`. A missing ID returns `404 Not Found`.

Incident data is stored in a local H2 database file (`incidents-db.mv.db`). Flyway applies database migrations when the server starts. The database file is ignored by Git.

## Test and package

```bash
mvn test
mvn package
```
