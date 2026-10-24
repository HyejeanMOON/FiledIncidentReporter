const dateFormat = new Intl.DateTimeFormat("en-US", {
  timeZone: "Asia/Tokyo",
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
  hour: "2-digit",
  minute: "2-digit",
  timeZoneName: "short",
});

export function formatDate(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "—" : dateFormat.format(date);
}

export function incidentNumber(id: string) {
  return `#${id.slice(0, 8).toUpperCase()}`;
}

/** Backend photo paths are served through the same-origin `/api` proxy. */
export function photoPath(path: string) {
  return `/api${path}`;
}
