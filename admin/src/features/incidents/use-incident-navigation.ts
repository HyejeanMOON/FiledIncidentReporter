"use client";

import { useOptimistic, useTransition } from "react";
import { useRouter } from "next/navigation";
import { incidentListHref, type IncidentQuery } from "./search-params";

/**
 * Updates the list query in the URL. The previous list stays on screen while the
 * server renders the next one; controls read the optimistic query meanwhile.
 */
export function useIncidentNavigation(query: IncidentQuery) {
  const router = useRouter();
  const [isPending, startTransition] = useTransition();
  const [optimisticQuery, setOptimisticQuery] = useOptimistic(query);

  function navigate(patch: Partial<IncidentQuery>) {
    startTransition(() => {
      setOptimisticQuery((current) => ({ ...current, ...patch }));
      router.replace(incidentListHref({ ...optimisticQuery, ...patch }), { scroll: false });
    });
  }

  return { query: optimisticQuery, navigate, isPending };
}
