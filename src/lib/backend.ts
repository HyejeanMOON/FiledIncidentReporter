import "server-only";

const backendUrl = process.env.INCIDENT_API_URL ?? "http://localhost:8080";

export class BackendError extends Error {
  constructor(message: string, readonly status: number) {
    super(message);
  }
}

async function fetchBackend(path: string, init?: RequestInit): Promise<Response> {
  try {
    return await fetch(new URL(path, backendUrl), { ...init, cache: "no-store" });
  } catch {
    throw new BackendError("Cannot connect to the incident service. Make sure the backend is running.", 502);
  }
}

export async function requestJson<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetchBackend(path, init);
  const data = await response.json().catch(() => null);
  if (!response.ok) {
    throw new BackendError(data?.detail || data?.title || `Request failed (${response.status})`, response.status);
  }
  return data as T;
}

/** Streams a backend response to the browser, keeping only the content type. */
export async function proxyBackend(path: string): Promise<Response> {
  try {
    const response = await fetchBackend(path);
    const headers = new Headers({ "cache-control": "no-store" });
    const contentType = response.headers.get("content-type");
    if (contentType) headers.set("content-type", contentType);
    return new Response(response.body, { status: response.status, headers });
  } catch (error) {
    const { message, status } = error as BackendError;
    return Response.json({ detail: message }, { status });
  }
}

export function errorMessage(error: unknown, fallback: string) {
  return error instanceof BackendError ? error.message : fallback;
}
