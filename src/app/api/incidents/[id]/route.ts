import { requestBackend } from "@/lib/backend";

type Context = { params: Promise<{ id: string }> };

export async function GET(_request: Request, { params }: Context) {
  const { id } = await params;
  return requestBackend(`/incidents/${encodeURIComponent(id)}`);
}
