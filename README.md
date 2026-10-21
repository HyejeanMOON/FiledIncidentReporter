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

Incident data is stored in a local H2 database file (`incidents-db.mv.db`). Flyway applies database migrations when the server starts. The database file is ignored by Git.

## Test and package

```bash
mvn test
mvn package
```
