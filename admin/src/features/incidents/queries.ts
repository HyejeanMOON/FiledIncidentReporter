import "server-only";
import { requestJson } from "@/lib/backend";
import { errorMessage } from "./errors";
import { messages } from "./messages";
import type { IncidentQuery } from "./search-params";
import type { Incident, IncidentPage, IncidentSummary, Result } from "./types";

const PAGE_SIZE = 12;

function settle<T>(promise: Promise<T>): Promise<Result<T>> {
  return promise.then(
    (data) => ({ ok: true, data }),
    (error: unknown) => ({ ok: false, error: errorMessage(error, messages.loadFailed) }),
  );
}

/** Never rejects, so the result can be awaited by a Suspense boundary without an error boundary. */
export function getIncidentPage({ status, field, q, page }: IncidentQuery) {
  const params = new URLSearchParams({ page: String(page), size: String(PAGE_SIZE) });
  if (q) params.set("q", q);
  if (q && field !== "all") params.set("field", field);
  if (status !== "all") params.set("resolved", String(status === "resolved"));
  return settle(requestJson<IncidentPage>(`/incidents?${params}`));
}

export function getIncidentSummary() {
  return settle(requestJson<IncidentSummary>("/incidents/summary"));
}

export function getIncident(id: string) {
  return settle(requestJson<Incident>(`/incidents/${encodeURIComponent(id)}`));
}
