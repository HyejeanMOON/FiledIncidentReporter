# Filed Incident Reporter Server

A server project built with Spring Boot, Kotlin, and Maven.

## Requirements

- JDK 17 or later
- Maven 3.6.3 or later

## Run

```bash
mvn spring-boot:run
```

The server listens on `http://localhost:8080` by default. This is an initial project scaffold; no business endpoints have been defined yet.

Incident data is stored in a local H2 database file (`incidents-db.mv.db`). Flyway applies database migrations when the server starts. The database file is ignored by Git.

## Test and package

```bash
mvn test
mvn package
```
