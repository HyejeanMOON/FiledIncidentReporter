import "server-only";
import { errorMessage, requestJson } from "@/lib/backend";
import type { IncidentQuery } from "./search-params";
import type { Incident, IncidentPage, Result } from "./types";

const PAGE_SIZE = 12;
const BACKEND_PAGE_SIZE = 100;

function settle<T>(promise: Promise<T>, fallback: string): Promise<Result<T>> {
  return promise.then(
    (data) => ({ ok: true, data }),
    (error: unknown) => ({ ok: false, error: errorMessage(error, fallback) }),
  );
}

async function fetchIncidentPage({ status, field, q, page }: IncidentQuery): Promise<IncidentPage> {
  const params = new URLSearchParams({ page: String(page), size: String(PAGE_SIZE) });
  if (q) params.set("q", q);
  if (status !== "all") params.set("resolved", String(status === "resolved"));
  if (!q || field === "all") return requestJson<IncidentPage>(`/incidents?${params}`);

  // The backend searches all text fields. Scan its matching pages before slicing so
  // field-specific results and hasNext stay correct across admin pages.
  const matches: Incident[] = [];
  const needed = (page + 1) * PAGE_SIZE + 1;
  const needle = q.toLowerCase();
  params.set("size", String(BACKEND_PAGE_SIZE));
  for (let backendPage = 0, hasMore = true; hasMore && matches.length < needed; backendPage++) {
    params.set("page", String(backendPage));
    const batch = await requestJson<IncidentPage>(`/incidents?${params}`);
    matches.push(...batch.items.filter((incident) => incident[field].toLowerCase().includes(needle)));
    hasMore = batch.hasNext;
  }

  const offset = page * PAGE_SIZE;
  return {
    items: matches.slice(offset, offset + PAGE_SIZE),
    page,
    size: PAGE_SIZE,
    hasNext: matches.length > offset + PAGE_SIZE,
  };
}

/** Never rejects, so the same promise can be awaited by several Suspense boundaries. */
export function getIncidentPage(query: IncidentQuery) {
  return settle(fetchIncidentPage(query), "Unable to load data");
}

export function getIncident(id: string) {
  return settle(requestJson<Incident>(`/incidents/${encodeURIComponent(id)}`), "Unable to load data");
}
