import "server-only";

const backendUrl = process.env.INCIDENT_API_URL ?? "http://localhost:8080";

// Reads should answer quickly; requests with a body may be uploading up to 200 MB of photos.
const READ_TIMEOUT_MS = 10_000;
const WRITE_TIMEOUT_MS = 120_000;

/** Codes for requests the backend never answered; other codes are the backend's Problem Details codes. */
export const BACKEND_UNREACHABLE = "BACKEND_UNREACHABLE";
export const BACKEND_TIMEOUT = "BACKEND_TIMEOUT";

/** `code` is stable for clients to map to their own messages; `message` is the backend's detail. */
export class BackendError extends Error {
  constructor(readonly code: string | undefined, message: string, readonly status: number) {
    super(message);
  }
}

/** The timeout covers the wait for response headers, so a long photo stream is not cut off. */
async function fetchBackend(path: string, init?: RequestInit): Promise<Response> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), init?.body ? WRITE_TIMEOUT_MS : READ_TIMEOUT_MS);
  try {
    return await fetch(new URL(path, backendUrl), { ...init, cache: "no-store", signal: controller.signal });
  } catch {
    throw controller.signal.aborted
      ? new BackendError(BACKEND_TIMEOUT, "The backend did not respond in time", 504)
      : new BackendError(BACKEND_UNREACHABLE, "Cannot connect to the backend", 502);
  } finally {
    clearTimeout(timer);
  }
}

export async function requestJson<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetchBackend(path, init);
  const data = await response.json().catch(() => null);
  if (!response.ok) {
    throw new BackendError(data?.code, data?.detail || data?.title || `Request failed (${response.status})`, response.status);
  }
  return data as T;
}

/** Streams a backend response to the browser, keeping only its content type and caching policy. */
export async function proxyBackend(path: string): Promise<Response> {
  try {
    const response = await fetchBackend(path);
    const headers = new Headers({ "cache-control": response.headers.get("cache-control") ?? "no-store" });
    const contentType = response.headers.get("content-type");
    if (contentType) headers.set("content-type", contentType);
    return new Response(response.body, { status: response.status, headers });
  } catch (error) {
    const { code, message, status } = error as BackendError;
    return Response.json({ code, detail: message }, { status });
  }
}
