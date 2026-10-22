const backendUrl = process.env.INCIDENT_API_URL ?? "http://localhost:8080";

export async function requestBackend(path: string, init?: RequestInit): Promise<Response> {
  try {
    const response = await fetch(new URL(path, backendUrl), {
      ...init,
      cache: "no-store",
    });
    const headers = new Headers();
    const contentType = response.headers.get("content-type");
    if (contentType) headers.set("content-type", contentType);
    headers.set("cache-control", "no-store");
    return new Response(response.body, { status: response.status, headers });
  } catch {
    return Response.json(
      { detail: "Cannot connect to the incident service. Make sure the backend is running." },
      { status: 502 },
    );
  }
}
