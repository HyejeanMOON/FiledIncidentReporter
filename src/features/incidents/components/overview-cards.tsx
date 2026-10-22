import { Icon, type IconName } from "@/components/ui/icon";
import type { Incident, IncidentPage, Result } from "../types";

const cards: { label: string; caption: string; icon: IconName; className: string; count: (items: Incident[]) => number }[] = [
  { label: "On this page", caption: "Current page", icon: "grid", className: "all-icon", count: (items) => items.length },
  { label: "Open", caption: "Needs attention", icon: "clock", className: "open-icon", count: (items) => items.filter((item) => !item.resolved).length },
  { label: "Resolved", caption: "Completed", icon: "check", className: "resolved-icon", count: (items) => items.filter((item) => item.resolved).length },
];

/** Without `items` (loading or failed), every count shows a dash. */
export function OverviewCards({ items }: { items?: Incident[] }) {
  return (
    <section className="overview-grid" aria-label="Incidents on the current page">
      {cards.map((card) => (
        <div key={card.label} className="overview-card">
          <div className={`overview-icon ${card.className}`}><Icon name={card.icon} /></div>
          <div><span>{card.label}</span><strong>{items ? String(card.count(items)).padStart(2, "0") : "—"}</strong></div>
          <small>{card.caption}</small>
        </div>
      ))}
    </section>
  );
}

export async function IncidentOverview({ result }: { result: Promise<Result<IncidentPage>> }) {
  const outcome = await result;
  return <OverviewCards items={outcome.ok ? outcome.data.items : undefined} />;
}
