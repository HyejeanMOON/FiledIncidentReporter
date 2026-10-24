import { Icon } from "@/components/ui/icon";
import { RefreshButton } from "@/components/ui/refresh-button";
import { StatePanel } from "@/components/ui/state-panel";
import type { IncidentQuery } from "../search-params";
import type { IncidentPage, Result } from "../types";
import { IncidentCard } from "./incident-card";
import { Pagination } from "./pagination";

export async function IncidentResults({ result, query }: { result: Promise<Result<IncidentPage>>; query: IncidentQuery }) {
  const outcome = await result;
  if (!outcome.ok) {
    return (
      <StatePanel className="error-state">
        <Icon name="alert" size={28} /><h3>Could not load incidents</h3><p>{outcome.error}</p>
        <RefreshButton>Try again <Icon name="arrow" size={16} /></RefreshButton>
      </StatePanel>
    );
  }

  const { items, page, hasNext } = outcome.data;
  if (!items.length) {
    const filtered = Boolean(query.q) || query.status !== "all";
    return (
      <StatePanel className="empty-state">
        <div className="empty-icon"><Icon name={filtered ? "search" : "grid"} size={28} /></div>
        <h3>{filtered ? "No matching incidents" : "No incidents yet"}</h3>
        <p>{filtered ? "Try another search term or filter." : "New field reports will appear here."}</p>
      </StatePanel>
    );
  }

  return (
    <>
      <div className="incident-list">{items.map((incident) => <IncidentCard key={incident.id} incident={incident} />)}</div>
      {(page > 0 || hasNext) && <Pagination query={query} count={items.length} hasNext={hasNext} />}
    </>
  );
}
