# Fieldnote incident management

A Next.js 16 admin interface for the `Filed Incident Reporter Server` incident API.

## Run locally

1. Start the backend server (port `8080` by default).
2. If the backend uses a different address, copy `.env.example` to `.env.local` and update `INCIDENT_API_URL`.
3. Run `npm ci` and `npm run dev` in this project, then open `http://localhost:3000`.

The dashboard supports server-side pagination and search across title, description, and location; status filters; incident details; and an in-page photo dialog. The detail panel sends `PATCH /incidents/{id}/resolution` to resolve or reopen an incident, then refreshes the list. Next.js Route Handlers forward browser requests through same-origin `/api/incidents` endpoints, so backend CORS configuration is not needed.

Run `npm run lint` and `npm run build` to check the project.
