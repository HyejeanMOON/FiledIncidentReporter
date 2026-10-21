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

`POST /incidents` creates an incident and returns it with a generated string ID (`201 Created`). The JSON body contains `title`, `location`, `description`, and a nonempty `photoUris` array. Invalid fields return `400 Bad Request`.

`GET /incidents?page=0&size=20` returns `{ "items": [...], "page": 0, "size": 20, "hasNext": false }`. Pages start at zero, and `size` must be between 1 and 100. Results are ordered newest first.

Incident data is stored in a local H2 database file (`incidents-db.mv.db`). Flyway applies database migrations when the server starts. The database file is ignored by Git.

## Test and package

```bash
mvn test
mvn package
```
