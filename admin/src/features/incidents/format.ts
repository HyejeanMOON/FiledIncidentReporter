export function incidentNumber(id: string) {
  return `#${id.slice(0, 8).toUpperCase()}`;
}

/** Backend photo paths are served through the same-origin `/api` proxy. */
export function photoPath(path: string) {
  return `/api${path}`;
}
