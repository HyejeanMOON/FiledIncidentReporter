import { proxyBackend } from "@/lib/backend";

// Serves `/photos/{photoId}` and `/photos/{photoId}/thumbnail` from the backend.
export async function GET(_request: Request, { params }: RouteContext<"/api/incidents/[id]/photos/[...path]">) {
  const { id, path } = await params;
  const [photoId, variant, ...rest] = path;
  if (rest.length || (variant !== undefined && variant !== "thumbnail")) {
    return Response.json({ detail: "Not found." }, { status: 404 });
  }
  const suffix = variant ? `/${variant}` : "";
  return proxyBackend(`/incidents/${encodeURIComponent(id)}/photos/${encodeURIComponent(photoId)}${suffix}`);
}
