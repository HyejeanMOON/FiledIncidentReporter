export type IncidentPhoto = {
  id: string;
  url: string;
  thumbnailUrl: string;
};

export type Incident = {
  id: string;
  title: string;
  location: string;
  description: string;
  photos: IncidentPhoto[];
  createdAt: string;
  updatedAt: string;
  resolved: boolean;
  resolvedAt: string | null;
};

export type IncidentPage = {
  items: Incident[];
  page: number;
  size: number;
  hasNext: boolean;
};

export function formatDate(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? "—"
    : new Intl.DateTimeFormat("en-US", {
        timeZone: "Asia/Tokyo",
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
        timeZoneName: "short",
      }).format(date);
}

export function photoPath(path: string) {
  return `/api${path}`;
}
