# Field Incident Reporter

An Android app for reporting facility maintenance incidents with photos, backed by a local Spring Boot API and a Next.js admin dashboard.

## Quick start

**Requirements:** JDK 17+, Node.js 20.9+, Android Studio with an emulator (API 26+). The Maven and Gradle wrappers are included.

```sh
./start.sh
```

This builds and starts both backend services. The first run takes a few minutes; Ctrl+C stops both.

| Service | Address |
| --- | --- |
| Incident API | http://localhost:8080/incidents |
| Admin dashboard | http://localhost:3000 |

Then run the `app` configuration in Android Studio, or `./gradlew installDebug` with an emulator open.

- `./start.sh --init` rebuilds after server code changes.
- `SERVER_PORT` / `ADMIN_PORT` change the default ports.
- Logs are written to `logs/`.

## Repository layout

| Folder | Contents |
| --- | --- |
| `app/` | Android app (Kotlin, Jetpack Compose, Hilt, Room, Retrofit) |
| `server/` | Incident API (Spring Boot, H2). API and database schema: [server/README.md](server/README.md) |
| `admin/` | Operator dashboard for triaging and resolving incidents: [admin/README.md](admin/README.md) |
| `start.sh` | Starts `server/` and `admin/` together |

## Android app

**Features:** list, view, create, edit, and delete reports; attach 1–20 photos from the picker or camera; see whether a report is open or resolved.

**API address:** the emulator default is `http://10.0.2.2:8080/`. For a physical device on the same network, set `incidentApiBaseUrl` (with a trailing slash), for example `-PincidentApiBaseUrl=http://192.168.1.10:8080/`.

**Offline first:** the list reads from Room, so cached reports show immediately and offline. A Paging 3 `RemoteMediator` syncs pages of 20 from the server on first entry, on pull-to-refresh, on returning to the foreground, and after a save or delete. Opening a report fetches its latest copy from the server.

**Architecture:** Google's layered architecture with unidirectional data flow (`feature` → `domain` ← `data`).

```
com.hyejeanmoon.fieldincidentreporter
├── navigation/  type-safe navigation graph
├── domain/      models, failures, repository contract, use cases (plain Kotlin)
├── data/        Room (local), Retrofit (remote), offline-first repository, Hilt setup
├── ui/          theme, shared composables, formatting helpers
└── feature/     list, detail, form — each with a ViewModel, Screen, and Content
```

Each ViewModel exposes one `StateFlow` UI state. Features don't call each other; they communicate through the repository and Room.

**Tests:** `./gradlew :app:testDebugUnitTest`

**Toolchain:** AGP 9.4.0, Gradle 9.6.0, JDK 17, Android SDK 37.

## Next steps

**API**
- Publish an OpenAPI document; field limits and error codes are currently mirrored by hand in three places.
- Add optimistic concurrency (ETag + `If-Match`) so concurrent edits can't silently overwrite each other.
- Make creates idempotent (`Idempotency-Key`) so retries on a weak connection can't duplicate a report.

**Android app**
- Save drafts offline and upload them later with WorkManager.
- Compress photos before upload.
- Confirm before discarding unsaved form changes.
- Add Robolectric Room tests and Compose UI tests.

**Server**
- Generate thumbnails at upload time and serve photos with long-lived cache headers.
- Add status workflow, priority, assignee, and seed data.

**Dashboard and delivery**
- Add Vitest tests, run all test suites in GitHub Actions, and add authentication with roles.
