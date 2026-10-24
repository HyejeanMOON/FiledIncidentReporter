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

// Messages for the backend's stable Problem Details codes, shared with the Android client.
const problemMessages: Record<string, string> = {
  INCIDENT_NOT_FOUND: "This report no longer exists on the server. Refresh the list.",
  RESOURCE_NOT_FOUND: "This report no longer exists on the server. Refresh the list.",
  INVALID_INCIDENT: "The report does not meet the server requirements. Check its fields and photos, then try again.",
  INVALID_REQUEST: "The server could not understand this request. Check that the app and server versions match.",
  PAYLOAD_TOO_LARGE: "A photo is too large to upload. Choose photos under 10 MB and try again.",
  INTERNAL_ERROR: "The server could not complete the request. Please try again.",
};

export async function requestJson<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetchBackend(path, init);
  const data = await response.json().catch(() => null);
  if (!response.ok) {
    const message = problemMessages[data?.code] || data?.detail || data?.title || `Request failed (${response.status})`;
    throw new BackendError(message, response.status);
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
