import { requestBackend } from "@/lib/backend";
import type { Incident, IncidentPage } from "@/lib/incidents";

const PAGE_SIZE = 12;
const BACKEND_PAGE_SIZE = 100;
const searchFields = ["title", "location", "description"] as const;

export async function GET(request: Request) {
  const input = new URL(request.url).searchParams;
  const page = Math.max(0, Number.parseInt(input.get("page") ?? "0", 10) || 0);
  const query = (input.get("q") ?? "").trim().slice(0, 200);
  const resolved = input.get("resolved");
  const requestedField = input.get("field");
  const field = searchFields.find((value) => value === requestedField);
  const params = new URLSearchParams({ page: String(page), size: String(PAGE_SIZE) });
  if (query) params.set("q", query);
  if (resolved === "true" || resolved === "false") params.set("resolved", resolved);
  if (!query || !field) return requestBackend(`/incidents?${params}`);

  // The backend searches all text fields. Scan its matching pages before slicing so
  // field-specific results and hasNext stay correct across admin pages.
  const matches: Incident[] = [];
  const needed = (page + 1) * PAGE_SIZE + 1;
  const needle = query.toLowerCase();
  let backendPage = 0;
  let hasMore = true;
  while (hasMore && matches.length < needed) {
    params.set("page", String(backendPage++));
    params.set("size", String(BACKEND_PAGE_SIZE));
    const response = await requestBackend(`/incidents?${params}`);
    if (!response.ok) return response;
    const batch = (await response.json()) as IncidentPage;
    matches.push(...batch.items.filter((incident) => incident[field].toLowerCase().includes(needle)));
    hasMore = batch.hasNext;
  }

  const offset = page * PAGE_SIZE;
  return Response.json({
    items: matches.slice(offset, offset + PAGE_SIZE),
    page,
    size: PAGE_SIZE,
    hasNext: matches.length > offset + PAGE_SIZE,
  } satisfies IncidentPage);
}
