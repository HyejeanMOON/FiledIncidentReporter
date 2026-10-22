import { requestBackend } from "@/lib/backend";

type Context = { params: Promise<{ id: string }> };

export async function PATCH(request: Request, { params }: Context) {
  const { id } = await params;
  let body: unknown;
  try {
    body = await request.json();
  } catch {
    return Response.json({ detail: "Invalid request body." }, { status: 400 });
  }
  if (!body || typeof body !== "object" || typeof (body as { resolved?: unknown }).resolved !== "boolean") {
    return Response.json({ detail: "resolved must be a boolean." }, { status: 400 });
  }
  return requestBackend(`/incidents/${encodeURIComponent(id)}/resolution`, {
    method: "PATCH",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ resolved: (body as { resolved: boolean }).resolved }),
  });
}
