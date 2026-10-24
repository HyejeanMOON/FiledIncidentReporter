export function StatusBadge({ resolved }: { resolved: boolean }) {
  return <span className={`status-badge ${resolved ? "is-resolved" : "is-open"}`}><span className="status-dot" />{resolved ? "Resolved" : "Open"}</span>;
}
