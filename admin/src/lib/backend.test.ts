import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import { BACKEND_TIMEOUT, BACKEND_UNREACHABLE, BackendError, proxyBackend, requestJson } from "./backend";

const fetchMock = vi.fn<typeof fetch>();

beforeEach(() => vi.stubGlobal("fetch", fetchMock));
afterEach(() => {
  vi.unstubAllGlobals();
  vi.useRealTimers();
  fetchMock.mockReset();
});

/** A fetch that only settles when its signal aborts, like a backend that never answers. */
function hangUntilAborted(_input: RequestInfo | URL, init?: RequestInit) {
  return new Promise<Response>((_resolve, reject) => {
    init?.signal?.addEventListener("abort", () => reject(new DOMException("Aborted", "AbortError")));
  });
}

async function rejection(promise: Promise<unknown>) {
  return promise.then(() => { throw new Error("expected a rejection"); }, (error: unknown) => error);
}

describe("requestJson", () => {
  test("returns the parsed body and never caches", async () => {
    fetchMock.mockResolvedValue(Response.json({ id: "1" }));

    await expect(requestJson("/incidents/1")).resolves.toEqual({ id: "1" });
    const [url, init] = fetchMock.mock.calls[0];
    expect(new URL(String(url)).pathname).toBe("/incidents/1");
    expect(init?.cache).toBe("no-store");
  });

  test("keeps the Problem Details code and detail", async () => {
    fetchMock.mockResolvedValue(Response.json({ code: "INVALID_INCIDENT", detail: "title is too long" }, { status: 400 }));

    expect(await rejection(requestJson("/incidents"))).toEqual(new BackendError("INVALID_INCIDENT", "title is too long", 400));
  });

  test("describes an error response without a JSON body by its status", async () => {
    fetchMock.mockResolvedValue(new Response("Bad gateway", { status: 502 }));

    const error = await rejection(requestJson("/incidents"));
    expect(error).toMatchObject({ code: undefined, message: "Request failed (502)", status: 502 });
  });

  test("reports an unreachable backend", async () => {
    fetchMock.mockRejectedValue(new TypeError("fetch failed"));

    expect(await rejection(requestJson("/incidents"))).toMatchObject({ code: BACKEND_UNREACHABLE, status: 502 });
  });

  test("gives up on reads after 10 seconds", async () => {
    vi.useFakeTimers();
    fetchMock.mockImplementation(hangUntilAborted);

    const result = rejection(requestJson("/incidents"));
    await vi.advanceTimersByTimeAsync(10_000);
    expect(await result).toMatchObject({ code: BACKEND_TIMEOUT, status: 504 });
  });

  test("gives uploads 120 seconds", async () => {
    vi.useFakeTimers();
    fetchMock.mockImplementation(hangUntilAborted);
    let settled = false;

    const result = rejection(requestJson("/incidents", { method: "POST", body: new FormData() })).finally(() => { settled = true; });
    await vi.advanceTimersByTimeAsync(10_000);
    expect(settled).toBe(false);
    await vi.advanceTimersByTimeAsync(110_000);
    expect(await result).toMatchObject({ code: BACKEND_TIMEOUT });
  });
});

describe("proxyBackend", () => {
  test("passes on the content type and caching policy", async () => {
    fetchMock.mockResolvedValue(new Response("jpeg", {
      headers: { "content-type": "image/jpeg", "cache-control": "max-age=31536000, private, immutable", "set-cookie": "a=b" },
    }));

    const response = await proxyBackend("/incidents/1/photos/2/thumbnail");
    expect(response.status).toBe(200);
    expect(response.headers.get("content-type")).toBe("image/jpeg");
    expect(response.headers.get("cache-control")).toBe("max-age=31536000, private, immutable");
    expect(response.headers.get("set-cookie")).toBeNull();
    expect(await response.text()).toBe("jpeg");
  });

  test("does not let responses without a caching policy be cached", async () => {
    fetchMock.mockResolvedValue(Response.json({ code: "RESOURCE_NOT_FOUND" }, { status: 404 }));

    const response = await proxyBackend("/incidents/1/photos/missing");
    expect(response.status).toBe(404);
    expect(response.headers.get("cache-control")).toBe("no-store");
  });

  test("answers with the error code when the backend is unreachable", async () => {
    fetchMock.mockRejectedValue(new TypeError("fetch failed"));

    const response = await proxyBackend("/incidents/1/photos/2");
    expect(response.status).toBe(502);
    expect(await response.json()).toMatchObject({ code: BACKEND_UNREACHABLE });
  });
});
