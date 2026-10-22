import { Suspense, type ReactNode } from "react";
import { LoadingPanel } from "@/components/ui/state-panel";
import { getIncidentPage } from "../queries";
import type { IncidentQuery } from "../search-params";
import { IncidentResults } from "./incident-results";
import { IncidentToolbar } from "./incident-toolbar";
import { IncidentOverview, OverviewCards } from "./overview-cards";

const listLoading = <LoadingPanel label="Loading incident reports…" />;

function RecordsSection({ children }: { children: ReactNode }) {
  return (
    <section className="records-section">
      <div className="section-heading"><span className="section-kicker">REPORTS</span><h2>Incident reports</h2><p>Browse, search, and manage reports from the field.</p></div>
      {children}
    </section>
  );
}

export function IncidentDashboard({ query }: { query: IncidentQuery }) {
  // One request feeds both the overview counts and the list.
  const result = getIncidentPage(query);
  return (
    <>
      <Suspense fallback={<OverviewCards />}><IncidentOverview result={result} /></Suspense>
      <RecordsSection>
        <IncidentToolbar query={query} />
        <Suspense fallback={listLoading}><IncidentResults result={result} query={query} /></Suspense>
      </RecordsSection>
    </>
  );
}

/** Shown until the request's search params are available. */
export function IncidentDashboardFallback() {
  return <><OverviewCards /><RecordsSection>{listLoading}</RecordsSection></>;
}
