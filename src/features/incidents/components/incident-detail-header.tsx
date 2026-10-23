import type { ReactNode } from "react";

/** Panel header for the detail and form views; `action` closes the panel or links back. */
export function IncidentDetailHeader({ eyebrow = "INCIDENT DETAIL", title = "Incident details", action }: {
  eyebrow?: string;
  title?: string;
  action: ReactNode;
}) {
  return <div className="detail-header"><div><span className="eyebrow">{eyebrow}</span><h2>{title}</h2></div>{action}</div>;
}
