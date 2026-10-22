import { Icon } from "@/components/ui/icon";
import { Spinner } from "@/components/ui/state-panel";
import { formatDate, incidentNumber } from "../format";
import { getIncident } from "../queries";
import { PhotoGallery } from "./photo-gallery";
import { ResolutionButton } from "./resolution-button";
import { StatusBadge } from "./status-badge";

export function IncidentDetailLoading() {
  return <div className="detail-loading"><Spinner />Loading incident details…</div>;
}

export async function IncidentDetail({ id }: { id: string }) {
  const outcome = await getIncident(id);
  if (!outcome.ok) return <div className="detail-error"><Icon name="alert" /><p>{outcome.error}</p></div>;

  const incident = outcome.data;
  const timeline = [
    { label: "Reported", value: incident.createdAt },
    { label: "Last updated", value: incident.updatedAt },
    ...(incident.resolvedAt ? [{ label: "Resolved", value: incident.resolvedAt }] : []),
  ];

  return (
    <>
      <div className="detail-scroll">
        <div className="detail-status-row"><StatusBadge resolved={incident.resolved} /><span className="detail-id">{incidentNumber(incident.id)}</span></div>
        <h3 className="detail-title">{incident.title}</h3>
        <div className="detail-location"><Icon name="pin" size={17} />{incident.location}</div>
        <div className="detail-divider" />
        <section className="detail-section"><h4>Description</h4><p className="description-text">{incident.description}</p></section>
        <section className="detail-section"><h4>Site photos <span>({incident.photos.length})</span></h4><PhotoGallery photos={incident.photos} /></section>
        <section className="detail-section"><h4>Timeline</h4><div className="timeline">
          {timeline.map(({ label, value }) => <div key={label}><span className="timeline-dot" /><span>{label}</span><strong>{formatDate(value)}</strong></div>)}
        </div></section>
      </div>
      <ResolutionButton key={`${incident.id}:${incident.resolved}`} id={incident.id} resolved={incident.resolved} />
    </>
  );
}
