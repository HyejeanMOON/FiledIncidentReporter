# Admin Dashboard

A Next.js 16 dashboard for operators to triage, edit, and resolve incidents from the [incident API](../server/README.md).

## Run

1. Start the API server (port `8080` by default).
2. If it runs elsewhere, copy `.env.example` to `.env.local` and set `INCIDENT_API_URL`.
3. Run `npm ci` and `npm run dev`, then open http://localhost:3000.

Or run `./start.sh` from the repository root to start both.

Check the project with `npm run lint` and `npm run build`.

## Features

- **Overview:** total, open, and resolved counts over all incidents.
- **List:** pagination, search (all fields or only title, location, or description), and open/resolved filter. The state is kept in the URL, for example `?status=open&field=title&q=pump&page=1`.
- **Detail:** opens in a side drawer over the list; the same URL opened directly shows a full page. Includes a photo viewer and a Resolve / Reopen button.
- **Create, edit, delete:** same rules as the Android app — required title, location, and description, and 1–20 photos (JPEG, PNG, WebP, HEIC/HEIF, or GIF, up to 10 MB each). Delete asks for confirmation.
- Times are shown in the viewer's time zone; error messages match the Android app's.

## Structure

| Path | Contents |
| --- | --- |
| `src/app` | Routes: dashboard, detail drawer (`@modal`), standalone detail page, photo proxy |
| `src/features/incidents` | Queries, Server Functions (`actions.ts`), user-facing messages and error mapping, form validation, URL parsing, components |
| `src/components` | App shell and shared UI |
| `src/lib/backend.ts` | Server-only API client: timeouts, error codes, photo proxy |

All API calls run on the Next.js server, so the browser never talks to the API directly and no CORS setup is needed.
