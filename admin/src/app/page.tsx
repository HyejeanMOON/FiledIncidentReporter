import { Suspense } from "react";
import { Icon } from "@/components/ui/icon";
import { RefreshButton } from "@/components/ui/refresh-button";
import { CreateIncidentButton } from "@/features/incidents/components/create-incident-button";
import { IncidentDashboard, IncidentDashboardFallback } from "@/features/incidents/components/incident-dashboard";
import { parseIncidentQuery } from "@/features/incidents/search-params";

export default function IncidentsPage({ searchParams }: PageProps<"/">) {
  return (
    <>
      <div className="page-heading">
        <div><div className="eyebrow heading-eyebrow"><span /> INCIDENT MANAGEMENT</div><h1>Incident overview<span className="heading-period">.</span></h1><p>Stay on top of field reports and follow through on every incident.</p></div>
        <div className="heading-actions">
          <RefreshButton className="refresh-button"><Icon name="refresh" size={17} />Refresh</RefreshButton>
          <CreateIncidentButton />
        </div>
      </div>
      <Suspense fallback={<IncidentDashboardFallback />}>
        {searchParams.then((params) => <IncidentDashboard query={parseIncidentQuery(params)} />)}
      </Suspense>
    </>
  );
}
