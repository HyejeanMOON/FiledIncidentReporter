# Filed Incident Reporter Server

A server project built with Spring Boot, Kotlin, and Maven.

## Requirements

- JDK 17 or later
- Maven 3.6.3 or later

## Run

```bash
mvn spring-boot:run
```

The server listens on `http://localhost:8080` by default.

## API

`POST /incidents` accepts `multipart/form-data` with an `incident` JSON part (`title`, `location`, `description`) and 1 to 20 `photos` file parts. Each photo must be an image of at most 10 MB. The response contains a generated string ID and server photo paths (`201 Created`). Invalid fields return `400 Bad Request`.

`GET /photos/{filename}` streams a stored photo. Incident responses contain these relative paths so clients can resolve them against the API base URL.

`GET /incidents?page=0&size=20` returns `{ "items": [...], "page": 0, "size": 20, "hasNext": false }`. Pages start at zero, and `size` must be between 1 and 100. Results are ordered newest first.

`PUT /incidents/{id}` accepts `multipart/form-data` with an `incident` JSON part (`id`, `title`, `location`, `description`, `retainedPhotoUris`) and optional new `photos` file parts. The `id` must match the path. Existing photos omitted from `retainedPhotoUris` are removed. A missing ID returns `404 Not Found`.

`DELETE /incidents/{id}` removes an incident and its stored photo files, then returns `204 No Content`. A missing ID returns `404 Not Found`.

Incident data is stored in a local H2 database file (`incidents-db.mv.db`). Flyway applies database migrations when the server starts. The database file is ignored by Git.

## Test and package

```bash
mvn test
mvn package
```
