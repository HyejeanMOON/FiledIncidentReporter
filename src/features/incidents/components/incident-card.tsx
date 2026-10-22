import Image from "next/image";
import Link from "next/link";
import { Icon } from "@/components/ui/icon";
import { formatDate, incidentNumber, photoPath } from "../format";
import type { Incident } from "../types";
import { StatusBadge } from "./status-badge";

export function IncidentCard({ incident }: { incident: Incident }) {
  const thumbnail = incident.photos[0];
  return (
    <Link href={`/incidents/${encodeURIComponent(incident.id)}`} scroll={false} className="incident-card" aria-label={`View details for ${incident.title}`}>
      <div className="card-top"><span className="card-number">{incidentNumber(incident.id)}</span><StatusBadge resolved={incident.resolved} /></div>
      <div className="card-main">
        {thumbnail ? <div className="card-thumbnail"><Image src={photoPath(thumbnail.thumbnailUrl)} alt="Incident photo preview" width={130} height={118} unoptimized /></div> : <div className="card-thumbnail no-photo"><Icon name="image" size={26} /></div>}
        <div className="card-copy"><h3>{incident.title}</h3><p>{incident.description}</p></div>
      </div>
      <div className="card-bottom"><span><Icon name="pin" size={16} />{incident.location}</span><span><Icon name="clock" size={16} />{formatDate(incident.createdAt)}</span><span className="card-arrow"><Icon name="arrow" size={18} /></span></div>
    </Link>
  );
}
