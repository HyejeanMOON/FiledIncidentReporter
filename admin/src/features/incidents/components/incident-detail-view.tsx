"use client";

import { useState } from "react";
import { Icon } from "@/components/ui/icon";
import { formatDate, incidentNumber } from "../format";
import type { Incident } from "../types";
import { DeleteIncidentButton } from "./delete-incident-button";
import { useDetailContext } from "./detail-context";
import { IncidentForm } from "./incident-form";
import { PhotoGallery } from "./photo-gallery";
import { ResolutionButton } from "./resolution-button";
import { StatusBadge } from "./status-badge";

/** Shows an incident, or edits it in place; a save re-renders the detail and the list from the server. */
export function IncidentDetailView({ incident }: { incident: Incident }) {
  const { setBusy } = useDetailContext();
  const [editing, setEditing] = useState(false);

  if (editing) {
    const done = () => setEditing(false);
    return <IncidentForm incident={incident} heading="Edit incident" onCancel={done} onSaved={done} onSavingChange={setBusy} />;
  }

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
      <div className="detail-footer">
        <div className="detail-actions">
          <button type="button" className="detail-action" onClick={() => setEditing(true)}><Icon name="edit" size={16} />Edit</button>
          <DeleteIncidentButton id={incident.id} />
        </div>
        <ResolutionButton key={String(incident.resolved)} id={incident.id} resolved={incident.resolved} />
        <p>Status changes are saved to the server.</p>
      </div>
    </>
  );
}
