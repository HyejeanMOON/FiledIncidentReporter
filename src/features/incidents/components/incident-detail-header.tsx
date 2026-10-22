import type { ReactNode } from "react";

/** `action` is the control that leaves the detail view: a close button or a back link. */
export function IncidentDetailHeader({ action }: { action: ReactNode }) {
  return <div className="detail-header"><div><span className="eyebrow">INCIDENT DETAIL</span><h2>Incident details</h2></div>{action}</div>;
}
