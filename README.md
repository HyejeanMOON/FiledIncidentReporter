# Field Incident Reporter

An Android-native app for reporting and managing facility maintenance incidents. It uses Jetpack Compose, Hilt-injected ViewModels, Retrofit, and a Spring Boot API.

## 🚀 Quick start

**Requirements:** JDK 17+, Node.js 20.9+, and Android Studio with an emulator (API 26+). Maven and Gradle are not needed; the repository includes their wrappers.

```sh
./start.sh
```

This one command builds and starts the local API and the admin dashboard. The first run downloads dependencies and takes a few minutes; later runs start in seconds.

| Service | Address |
| --- | --- |
| Incident API (Spring Boot) | http://localhost:8080/incidents |
| Admin dashboard (Next.js) | http://localhost:3000 |

Then open this folder in Android Studio and run the `app` configuration on an emulator, or run `./gradlew installDebug` with an emulator open. Press Ctrl+C in the terminal to stop both services. Run `./start.sh --init` to rebuild after changing server code; set `SERVER_PORT` or `ADMIN_PORT` if a default port is taken. Logs are written to `logs/`.

## Repository layout

| Folder | Contents |
| --- | --- |
| `app/` | Android app (Kotlin, Jetpack Compose) |
| `server/` | Local API server (Spring Boot, Kotlin, H2); see [server/README.md](server/README.md) |
| `admin/` | Bonus operator dashboard for triaging, filtering, and resolving incidents (Next.js); see [admin/README.md](admin/README.md) |
| `start.sh` | Sets up and starts `server/` and `admin/` together |

The local API is a small Spring Boot server rather than json-server because the API contract needs things json-server cannot provide: multipart photo uploads and thumbnails, partial updates that delete individual photos, stable cursor pagination, and RFC 9457 error responses. It still runs entirely on your machine and stores data in a local H2 file.

## API

The server listens on port 8080 and stores incidents in a local H2 database. The API contract is:

| Method | Route | Purpose |
| --- | --- | --- |
| GET | `/incidents?size=20&cursor=…` | List reports in keyset pages, newest first |
| GET | `/incidents/{id}` | Read one report for detail and edit screens |
| POST | `/incidents` | Create a report with multipart photo files |
| PATCH | `/incidents/{id}` | Update a report, delete photos by ID, and upload new photo files |
| DELETE | `/incidents/{id}` | Remove a report |

Each report has string `id`, `title`, `location`, `description`, and `photos` fields, plus server-generated `createdAt` and `updatedAt` timestamps in ISO 8601 UTC, and `resolved` with `resolvedAt` (null while open), which the admin dashboard sets. The trimmed title and location are 1 to 200 characters and the description 1 to 4000; the app stops input at these limits and shows a character count. Each photo has a server-generated ID, an original file URL, and a thumbnail URL. The list API returns photo metadata instead of embedded Base64 previews. An update sends `deletePhotoIds` for removed old photos and multipart files for new photos; unchanged photos are not uploaded again. The list is ordered by `createdAt` descending, then `id` descending. A list response has `items` and a `nextCursor`, which is null on the last page; passing it back as `cursor` returns the reports right after the page's last report, so reports added or deleted meanwhile do not shift later pages. The cursor is opaque to the app. The app displays creation time in the device's local time zone and displays update time only after the report has been edited. The list and the detail view show whether a report is open or resolved, and when it was resolved. Lists and form previews load the server's thumbnails; the detail gallery and the full-screen viewer load the original photos.

## Run the Android app

Open this folder in Android Studio and run the `app` configuration on an Android emulator (API 26 or later). The default API address is `http://10.0.2.2:8080/`, which routes from the Android emulator to the development machine. Start the server with `./start.sh` before opening the app.

For a physical device on the same network, set `incidentApiBaseUrl` to the development machine's reachable LAN address (including a trailing slash), for example in your local `gradle.properties` or with `-PincidentApiBaseUrl=http://192.168.1.10:8080/`. The server binds to all interfaces so that a device can reach it; use a trusted local network.

The project uses Android Gradle Plugin 9.4.0, Gradle 9.6.0 (through `./gradlew`), JDK 17, and Android SDK Platform 37.

The list reads reports from Room, so cached reports appear immediately and offline. A Paging 3 `RemoteMediator` keeps Room in sync with the server in pages of 20: it refreshes the first page on first entry, on Refresh or pull-to-refresh, when the app returns to the foreground while the list is shown, and after returning from a successful create, update, or delete, and it fetches later pages as the user nears the end of the list. Each fetched page is compared with the cached reports from just after the page before through its own last report, so unchanged reports are skipped, changed or new reports are written, and reports the server no longer returns are removed; the cache is never cleared as a whole. Confirmed saves and deletes also update Room right away. Opening a report's details or edit form fetches it from the server and replaces its cached copy, so changes made elsewhere, such as in the web admin, reach the list; a report the server no longer has is removed from Room. A report that is not cached is not added this way. If a refresh fails, the cached reports stay visible with the error. Photos selected in the picker or taken with the camera are uploaded as files. Camera photos are written to app-private storage and deleted when the form closes, whether the report was saved or cancelled; a form restored after process death keeps them.

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
│   └── usecase/    business rules beyond plain repository calls (save, validate)
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

Features do not call each other. After a successful create, update, or delete, the repository updates Room, which the list reads. A create or delete then returns to the list and requests one server refresh; an update returns to the report's details, which reload from the server while the previous copy stays on screen. The form saves the user's edits to `SavedStateHandle` as JSON so that they survive process death.

Run the unit tests with `./gradlew :app:testDebugUnitTest`.

## Next steps

The prototype covers reporting, viewing, editing, and removing incidents end to end. These are the changes it would need next, roughly in order of value.

**API contract**

- Publish an OpenAPI document as the single source for both clients. Field limits, photo rules, and error codes are currently mirrored by hand in the server, the app, and the dashboard.
- Add optimistic concurrency: a version or ETag on each report, sent back as `If-Match` on `PATCH` and `DELETE`, with `412 Precondition Failed` on a conflict. Today a technician's edit and an operator's edit can silently overwrite each other.
- Make creates idempotent with an `Idempotency-Key` header (or a client-generated ID), so retrying a create after a timeout on a weak connection cannot create a duplicate report.
- Separate numbered paging (dashboard) from cursor paging (app) in the repository and response types, and add a `field` parameter to search so the dashboard no longer scans result pages itself.

**Android app**

- Report offline: save the draft to Room and upload it with WorkManager once a connection is available. Technicians often work in basements or plant rooms; today saving needs a connection.
- Resize and compress photos before upload to save data and stay under the 10 MB limit.
- Ask before discarding unsaved changes when leaving the form.
- Allow cleartext traffic only to the emulator host in debug builds, through a network security config.
- Test the Room queries against real SQLite with Robolectric, and add Compose UI tests for the form and list states.

**Server**

- Generate thumbnails at upload instead of on every request. ImageIO cannot decode HEIC or WebP, so those previews are currently the original files. Photos never change, so they can also be served with long-lived cache headers.
- Extend the triage model with a status workflow (open, in progress, resolved), priority, assignee, reporter, and coordinates.
- Add seed data so a fresh setup has reports to demo.

**Dashboard and delivery**

- Add Vitest tests for URL query parsing, form validation, and paging.
- Run all three test suites in GitHub Actions on every push.
- Add authentication and roles (technician, operator), which this local prototype leaves out.
