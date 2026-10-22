import { requestBackend } from "@/lib/backend";

type Context = { params: Promise<{ id: string; photoId: string }> };

export async function GET(_request: Request, { params }: Context) {
  const { id, photoId } = await params;
  return requestBackend(`/incidents/${encodeURIComponent(id)}/photos/${encodeURIComponent(photoId)}/thumbnail`);
}
