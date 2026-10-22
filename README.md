# Field Incident Reporter

An Android-native app for reporting and managing facility maintenance incidents. It uses Jetpack Compose, Hilt-injected ViewModels, Retrofit, and a Spring Boot API.

## Run the backend

Start the separate `Filed Incident Reporter Server` Spring Boot project with:

```sh
mvn spring-boot:run
```

The server listens on port 8080 and stores incidents in a local H2 database. The API contract is:

| Method | Route | Purpose |
| --- | --- | --- |
| GET | `/incidents?page=0&size=20` | List reports in pages |
| GET | `/incidents/{id}` | Read one report for detail and edit screens |
| POST | `/incidents` | Create a report with multipart photo files |
| PATCH | `/incidents/{id}` | Update a report, delete photos by ID, and upload new photo files |
| DELETE | `/incidents/{id}` | Remove a report |

Each report has string `id`, `title`, `location`, `description`, and `photos` fields, plus server-generated `createdAt` and `updatedAt` timestamps in ISO 8601 UTC. Each photo has a server-generated ID, an original file URL, and a thumbnail URL. The list API returns photo metadata instead of embedded Base64 previews. An update sends `deletePhotoIds` for removed old photos and multipart files for new photos; unchanged photos are not uploaded again. The app displays creation time in the device's local time zone and displays update time only after the report has been edited.

## Run the Android app

Open this folder in Android Studio and run the `app` configuration on an Android emulator (API 26 or later). The default API address is `http://10.0.2.2:8080/`, which routes from the Android emulator to the development machine. Start the Spring Boot service before opening the app.

For a physical device on the same network, set `incidentApiBaseUrl` to the development machine's reachable LAN address (including a trailing slash), for example in your local `gradle.properties` or with `-PincidentApiBaseUrl=http://192.168.1.10:8080/`. The server binds to all interfaces so that a device can reach it; use a trusted local network.

The project uses Android Gradle Plugin 9.4.0, Gradle 9.6.0, JDK 17, and Android SDK Platform 37. This checkout lacks Gradle wrapper launch scripts and the wrapper JAR; use a locally installed Gradle 9.6.0 or generate the wrapper with `gradle wrapper --gradle-version 9.6.0`.

The app loads reports in pages of 20 using Paging 3, shows loading and request errors, and refreshes the list only after server writes succeed. Photos selected in the picker or taken with the camera are uploaded as files. The Refresh button reloads reports from the server.

The API sends RFC 9457 Problem Details with a stable `code`. The data layer maps known codes to app failures, and the UI shows a localized error dialog for failed list loads, report lookups, saves, and deletes. A failed page retains its Retry control. Unknown or malformed error bodies fall back to the HTTP status.

## Code organization

The top-level packages separate responsibilities: `domain/model`, `domain/repository`, and `domain/error` contain the app model, repository contract, and failure types; `data` contains API payloads, Retrofit endpoints, mappers, the repository implementation, and Hilt bindings in `data/di`; `feature` contains the UI. Within `feature`, `list` and `form` each have their own screen, Hilt ViewModel, state, and event; `detail` contains the detail screen. `IncidentReporterApplication` initializes Hilt, and `IncidentApp` coordinates navigation and passes a saved report from the form ViewModel to the list ViewModel.
