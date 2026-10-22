# Fieldnote incident management

A Next.js 16 admin interface for the `Filed Incident Reporter Server` incident API.

## Run locally

1. Start the backend server (port `8080` by default).
2. If the backend uses a different address, copy `.env.example` to `.env.local` and update `INCIDENT_API_URL`.
3. Run `npm ci` and `npm run dev` in this project, then open `http://localhost:3000`.

The dashboard supports pagination, case-insensitive partial-text search across all text fields or only title, location, or description, and status filters. Field-specific search scans backend search results before pagination so page boundaries remain accurate; broad searches on large datasets may take longer. The dashboard also provides incident details and an in-page photo dialog. The detail panel sends `PATCH /incidents/{id}/resolution` to resolve or reopen an incident, then refreshes the list. Next.js Route Handlers forward browser requests through same-origin `/api/incidents` endpoints, so backend CORS configuration is not needed.

Run `npm run lint` and `npm run build` to check the project.
