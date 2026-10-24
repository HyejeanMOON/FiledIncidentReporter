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

Create, view, edit, and delete incident reports, each with 1–20 photos from the gallery or camera. Each report shows whether it is open or resolved.

### Connecting to the server

| File | Role |
| --- | --- |
| [`gradle.properties`](gradle.properties) (project root) | Set `incidentApiBaseUrl` here to change the address |
| [`app/build.gradle.kts`](app/build.gradle.kts) | Reads `incidentApiBaseUrl`, defaulting to `http://10.0.2.2:8080/` |

- **Emulator:** works as is. The app connects to `http://10.0.2.2:8080/`, which is your computer as seen from the emulator.
- **Physical device:** connect it to the same Wi-Fi as your computer, then add this line to [`gradle.properties`](gradle.properties) with your computer's IP address and reinstall the app:

  ```properties
  incidentApiBaseUrl=http://YOUR_COMPUTER_IP:8080/
  ```

  Or skip the file and pass the address when you build and install from the command line:

  ```sh
  ./gradlew installDebug -PincidentApiBaseUrl=http://YOUR_COMPUTER_IP:8080/
  ```

  Replace `YOUR_COMPUTER_IP` with your computer's IP address. `./start.sh` prints the full line with it filled in, or run `ipconfig getifaddr en0` on macOS. Keep the trailing `/`, and change the port too if you set `SERVER_PORT`.

### How it works

- **Offline first:** reports are cached in a local database (Room), so the list loads instantly and works offline. It syncs with the server when you open the app, pull to refresh, or save a change.
- **Layers:** `feature` (screens) → `domain` (models and use cases) ← `data` (database and network).

```
com.hyejeanmoon.fieldincidentreporter
├── navigation/  screen navigation
├── domain/      models and use cases, plain Kotlin
├── data/        Room, Retrofit, repository, Hilt setup
├── ui/          theme and shared components
└── feature/     list, detail, and form screens
```

### Tests and toolchain

- Run the tests with `./gradlew :app:testDebugUnitTest`.
- The project uses AGP 9.4.0, Gradle 9.6.0, JDK 17, and Android SDK 37.

## Next steps

**API**
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
