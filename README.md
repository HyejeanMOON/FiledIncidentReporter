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
| POST | `/incidents` | Create a report with multipart photo files |
| PUT | `/incidents/{id}` | Update a report and upload new photo files |
| DELETE | `/incidents/{id}` | Remove a report |

Each report has string `id`, `title`, `location`, `description`, and `photoUris` fields. The server generates the ID and stores uploaded image bytes. The response contains relative server photo paths.

## Run the Android app

Open this folder in Android Studio and run the `app` configuration on an Android emulator (API 26 or later). The default API address is `http://10.0.2.2:8080/`, which routes from the Android emulator to the development machine. Start the Spring Boot service before opening the app.

For a physical device on the same network, set `incidentApiBaseUrl` to the development machine's reachable LAN address (including a trailing slash), for example in your local `gradle.properties` or with `-PincidentApiBaseUrl=http://192.168.1.10:8080/`. The server binds to all interfaces so that a device can reach it; use a trusted local network.

The project uses Android Gradle Plugin 9.4.0, Gradle 9.6.0, JDK 17, and Android SDK Platform 37. This checkout lacks Gradle wrapper launch scripts and the wrapper JAR; use a locally installed Gradle 9.6.0 or generate the wrapper with `gradle wrapper --gradle-version 9.6.0`.

The app loads reports on launch, shows loading and request errors, and updates the list only after server writes succeed. Photos selected in the picker or taken with the camera are uploaded as files. The Refresh button reloads reports from the server.

## Code organization

The top-level packages separate responsibilities: `domain/model`, `domain/repository`, and `domain/error` contain the app model, repository contract, and failure types; `data` contains API payloads, Retrofit endpoints, mappers, the repository implementation, and Hilt bindings in `data/di`; `feature` contains the UI. Within `feature`, `list` and `form` each have their own screen, Hilt ViewModel, state, and event; `detail` contains the detail screen. `IncidentReporterApplication` initializes Hilt, and `IncidentApp` coordinates navigation and passes a saved report from the form ViewModel to the list ViewModel.
