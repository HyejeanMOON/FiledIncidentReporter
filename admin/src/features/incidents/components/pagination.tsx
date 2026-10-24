"use client";

import { Icon } from "@/components/ui/icon";
import type { IncidentQuery } from "../search-params";
import { useIncidentNavigation } from "../use-incident-navigation";

export function Pagination({ query: committed, count, hasNext }: { query: IncidentQuery; count: number; hasNext: boolean }) {
  const { navigate, isPending } = useIncidentNavigation(committed);
  const { page } = committed;
  return (
    <div className="pagination" data-pending={isPending ? "" : undefined}>
      <span>Page {page + 1} · Showing {count} incidents</span>
      <div>
        <button type="button" disabled={isPending || page === 0} onClick={() => navigate({ page: page - 1 })}>Previous</button>
        <button type="button" disabled={isPending || !hasNext} onClick={() => navigate({ page: page + 1 })}>Next <Icon name="chevron" size={15} /></button>
      </div>
    </div>
  );
}
