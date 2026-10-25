import { Icon, type IconName } from "@/components/ui/icon";
import type { IncidentSummary, Result } from "../types";

const cards: { label: string; caption: string; icon: IconName; className: string; count: keyof IncidentSummary }[] = [
  { label: "All incidents", caption: "Total reports", icon: "grid", className: "all-icon", count: "total" },
  { label: "Open", caption: "Needs attention", icon: "clock", className: "open-icon", count: "open" },
  { label: "Resolved", caption: "Completed", icon: "check", className: "resolved-icon", count: "resolved" },
];

/** Without `summary` (loading or failed), every count shows a dash. */
export function OverviewCards({ summary }: { summary?: IncidentSummary }) {
  return (
    <section className="overview-grid" aria-label="Incident totals">
      {cards.map((card) => (
        <div key={card.label} className="overview-card">
          <div className={`overview-icon ${card.className}`}><Icon name={card.icon} /></div>
          <div><span>{card.label}</span><strong>{summary ? String(summary[card.count]).padStart(2, "0") : "—"}</strong></div>
          <small>{card.caption}</small>
        </div>
      ))}
    </section>
  );
}

export async function IncidentOverview({ result }: { result: Promise<Result<IncidentSummary>> }) {
  const outcome = await result;
  return <OverviewCards summary={outcome.ok ? outcome.data : undefined} />;
}
