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

export type Result<T> = { ok: true; data: T } | { ok: false; error: string };
