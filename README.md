# Field Incident Reporter

An Android-native prototype for reporting and managing facility maintenance incidents. It uses Jetpack Compose, a ViewModel, Retrofit, and a local json-server API.

## Run the mock API

Install Node.js 22.12 or later. From `mock-api/`, run:

```sh
npm install
npm start
```

The server listens on port 3000 and persists reports in `mock-api/db.json`. The API contract is:

| Method | Route | Purpose |
| --- | --- | --- |
| GET | `/incidents` | List reports |
| POST | `/incidents` | Create a report |
| PUT | `/incidents/{id}` | Replace a report |
| DELETE | `/incidents/{id}` | Remove a report |

Each report has string `id`, `title`, `location`, `description`, and `photoUris` fields. The server generates the ID on POST. Photo URIs are local references; json-server stores their strings but does not upload image data, so another device cannot display those images.

## Run the Android app

Open this folder in Android Studio and run the `app` configuration on an Android emulator (API 26 or later). The default API address is `http://10.0.2.2:3000/`, which routes from the Android emulator to the development machine. Start the mock API before opening the app.

For a physical device on the same network, set `incidentApiBaseUrl` to the development machine's reachable LAN address (including a trailing slash), for example in your local `gradle.properties` or with `-PincidentApiBaseUrl=http://192.168.1.10:3000/`. The server binds to all interfaces so that a device can reach it; use a trusted local network.

The project uses Android Gradle Plugin 9.4.0, Gradle 9.6.0, JDK 17, and Android SDK Platform 37. This checkout lacks Gradle wrapper launch scripts and the wrapper JAR; use a locally installed Gradle 9.6.0 or generate the wrapper with `gradle wrapper --gradle-version 9.6.0`.

The app loads reports on launch, shows loading and request errors, and updates the list only after server writes succeed. The Refresh button reloads reports from the server.

## Code organization

The top-level packages separate responsibilities: `domain/model`, `domain/repository`, and `domain/error` contain the app model, repository contract, and failure types; `data` contains API payloads, Retrofit endpoints, mappers, and the repository implementation; `feature` contains the UI. Within `feature`, `list` and `form` each have their own screen, ViewModel, state, and event; `detail` contains the detail screen. `MainActivity` creates the concrete repository and injects it into `IncidentApp`, which coordinates navigation and passes a saved report from the form ViewModel to the list ViewModel.
