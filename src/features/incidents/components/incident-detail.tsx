import { Icon } from "@/components/ui/icon";
import { Spinner } from "@/components/ui/state-panel";
import { getIncident } from "../queries";
import { IncidentDetailView } from "./incident-detail-view";

export function IncidentDetailLoading() {
  return <div className="detail-loading"><Spinner />Loading incident details…</div>;
}

export async function IncidentDetail({ id }: { id: string }) {
  const outcome = await getIncident(id);
  if (!outcome.ok) return <div className="detail-error"><Icon name="alert" /><p>{outcome.error}</p></div>;
  return <IncidentDetailView key={outcome.data.id} incident={outcome.data} />;
}
