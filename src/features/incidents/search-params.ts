export const statusFilters = [
  { value: "all", label: "All incidents" },
  { value: "open", label: "Open" },
  { value: "resolved", label: "Resolved" },
] as const;

export const searchFields = [
  { value: "all", label: "All text" },
  { value: "title", label: "Title" },
  { value: "location", label: "Location" },
  { value: "description", label: "Description" },
] as const;

export type StatusFilter = (typeof statusFilters)[number]["value"];
export type SearchField = (typeof searchFields)[number]["value"];

/** List state kept in the URL so refreshes and the detail route preserve it. */
export type IncidentQuery = {
  status: StatusFilter;
  field: SearchField;
  q: string;
  page: number;
};

type SearchParams = Record<string, string | string[] | undefined>;

function pick<T extends string>(options: readonly { value: T }[], value: unknown): T {
  return options.find((option) => option.value === value)?.value ?? options[0].value;
}

export function parseIncidentQuery(params: SearchParams): IncidentQuery {
  const read = (key: string) => {
    const value = params[key];
    return Array.isArray(value) ? value[0] : value;
  };
  return {
    status: pick(statusFilters, read("status")),
    field: pick(searchFields, read("field")),
    q: (read("q") ?? "").trim().slice(0, 200),
    page: Math.max(0, Number.parseInt(read("page") ?? "0", 10) || 0),
  };
}

export function incidentListHref({ status, field, q, page }: IncidentQuery) {
  const params = new URLSearchParams();
  if (status !== "all") params.set("status", status);
  if (field !== "all") params.set("field", field);
  if (q) params.set("q", q);
  if (page > 0) params.set("page", String(page));
  const search = params.toString();
  return search ? `/?${search}` : "/";
}
