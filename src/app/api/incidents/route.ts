import { requestBackend } from "@/lib/backend";

export async function GET(request: Request) {
  const input = new URL(request.url).searchParams;
  const page = Math.max(0, Number.parseInt(input.get("page") ?? "0", 10) || 0);
  const query = (input.get("q") ?? "").trim().slice(0, 200);
  const resolved = input.get("resolved");
  const params = new URLSearchParams({ page: String(page), size: "12" });
  if (query) params.set("q", query);
  if (resolved === "true" || resolved === "false") params.set("resolved", resolved);
  return requestBackend(`/incidents?${params}`);
}
