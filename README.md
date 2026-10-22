# Fieldnote incident management

A Next.js 16 admin interface for the `Filed Incident Reporter Server` incident API.

## Run locally

1. Start the backend server (port `8080` by default).
2. If the backend uses a different address, copy `.env.example` to `.env.local` and update `INCIDENT_API_URL`.
3. Run `npm ci` and `npm run dev` in this project, then open `http://localhost:3000`.

The dashboard supports pagination, case-insensitive partial-text search across all text fields or only title, location, or description, and status filters. The list state lives in the URL (`?status=open&field=title&q=pump&page=1`), so it survives refreshes. Field-specific search scans backend search results before pagination so page boundaries remain accurate; broad searches on large datasets may take longer.

Opening an incident shows its details in a side drawer over the list (an intercepted `/incidents/{id}` route); visiting that URL directly renders a standalone detail page. The detail view has an in-page photo viewer and a button that sends `PATCH /incidents/{id}/resolution` to resolve or reopen the incident, then refreshes the detail and the list.

## Architecture

- `src/app` — routes only: the dashboard page, the `@modal` slot for the detail drawer, the standalone detail page, and a Route Handler that proxies incident photos.
- `src/features/incidents` — the incident feature: server-only `queries.ts`, the `actions.ts` Server Function, URL query parsing, and its components.
- `src/components` — the app shell and shared UI primitives (icon, dialog, state panels, refresh button).
- `src/lib/backend.ts` — server-only client for the backend API.

Server Components read from the backend on the server, so the browser only talks to Next.js and backend CORS configuration is not needed.

Run `npm run lint` and `npm run build` to check the project.
