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

The list reads reports from Room, so cached reports appear immediately and offline. A Paging 3 `RemoteMediator` keeps Room in sync with the server in pages of 20: it refreshes the first page on first entry, on Refresh or pull-to-refresh, and after returning from a successful create, update, or delete, and it fetches later pages as the user nears the end of the list. Each fetched page is compared with the cached reports in its span, so unchanged reports are skipped, changed or new reports are written, and reports the server no longer returns are removed; the cache is never cleared as a whole. Confirmed saves and deletes also update Room right away. If a refresh fails, the cached reports stay visible with the error. Photos selected in the picker or taken with the camera are uploaded as files.

The API sends RFC 9457 Problem Details with a stable `code`. The data layer maps known codes to app failures. The list shows failed refreshes and page loads inline with Retry while keeping cached reports; report lookups, saves, and deletes use localized error dialogs. Unknown or malformed error bodies fall back to the HTTP status.

## Code organization

The app follows Google's recommended layered architecture with unidirectional data flow. Dependencies point inward: `feature` → `domain` ← `data`. The `domain` package is plain Kotlin with no Android framework dependencies; it uses Paging's platform-independent `PagingData` for the report list.

```
com.hyejeanmoon.fieldincidentreporter
├── MainActivity, IncidentReporterApplication
├── navigation/     IncidentNavHost: the type-safe navigation graph and screen transitions
├── domain/
│   ├── model/      Incident, IncidentPhoto, IncidentDraft
│   ├── error/      IncidentFailure, IncidentException, runSuspendCatching
│   ├── repository/ IncidentRepository contract
│   └── usecase/    one class per business action (get, save, delete, validate)
├── data/
│   ├── local/      Room database, report entity, DAO, and the page comparison
│   ├── remote/     Retrofit API, kotlinx.serialization DTOs, mappers, error mapping, photo upload
│   ├── repository/ OfflineFirstIncidentRepository and IncidentRemoteMediator
│   └── di/         Hilt bindings and the Retrofit and Room setup
├── ui/
│   ├── theme/      Material theme
│   ├── component/  shared composables: top bar, loading and error states, dialogs, photos, labeled text
│   └── util/       failure messages and time formatting
└── feature/
    ├── list/       paged report list, refresh, and pull-to-refresh
    ├── detail/     report details, photo gallery, and delete
    └── form/       create and edit form, photo picker and camera
```

Each feature contains a `*Navigation.kt` file (a `@Serializable` route, a `NavController.navigateTo…` extension, and a `NavGraphBuilder` extension), a Hilt ViewModel that exposes one `StateFlow` UI state and receives UI events, a stateful `*Screen` composable, and a stateless `*Content` composable that previews use. ViewModels read their route arguments from `SavedStateHandle` and load their own data.

Features do not call each other. After a successful create, update, or delete, the repository updates Room, which the list reads; navigation then returns to the list and requests one server refresh. The form saves the user's edits to `SavedStateHandle` as JSON so that they survive process death.

Run the unit tests with `gradle :app:testDebugUnitTest`.
