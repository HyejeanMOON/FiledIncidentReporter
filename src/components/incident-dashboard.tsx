"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import Image from "next/image";
import type { Incident, IncidentPage, IncidentPhoto } from "@/lib/incidents";
import { formatDate, photoPath } from "@/lib/incidents";

type StatusFilter = "all" | "open" | "resolved";
type IconName = "grid" | "search" | "pin" | "clock" | "check" | "arrow" | "close" | "refresh" | "image" | "chevron" | "alert";

function Icon({ name, size = 20 }: { name: IconName; size?: number }) {
  const paths: Record<IconName, React.ReactNode> = {
    grid: <><rect x="3" y="3" width="7" height="7" rx="1.5" /><rect x="14" y="3" width="7" height="7" rx="1.5" /><rect x="3" y="14" width="7" height="7" rx="1.5" /><rect x="14" y="14" width="7" height="7" rx="1.5" /></>,
    search: <><circle cx="11" cy="11" r="7" /><path d="m20 20-4-4" /></>,
    pin: <><path d="M20 10c0 5-8 11-8 11S4 15 4 10a8 8 0 1 1 16 0Z" /><circle cx="12" cy="10" r="2.5" /></>,
    clock: <><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></>,
    check: <path d="m5 12 4 4L19 6" />,
    arrow: <><path d="M5 12h14" /><path d="m13 6 6 6-6 6" /></>,
    close: <path d="M5 5 19 19M19 5 5 19" />,
    refresh: <><path d="M20 7v5h-5M4 17v-5h5" /><path d="M5.5 9a7 7 0 0 1 12-2L20 12M4 12l2.5 5a7 7 0 0 0 12-2" /></>,
    image: <><rect x="3" y="3" width="18" height="18" rx="2" /><circle cx="8.5" cy="8.5" r="1.5" /><path d="m3 17 5-5 4 4 3-3 6 6" /></>,
    chevron: <path d="m9 5 7 7-7 7" />,
    alert: <><circle cx="12" cy="12" r="9" /><path d="M12 7v6M12 17h.01" /></>,
  };
  return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{paths[name]}</svg>;
}

async function readJson<T>(response: Response): Promise<T> {
  const data = await response.json().catch(() => null);
  if (!response.ok) {
    throw new Error(data?.detail || data?.title || `Request failed (${response.status})`);
  }
  return data as T;
}

function StatusBadge({ resolved }: { resolved: boolean }) {
  return <span className={`status-badge ${resolved ? "is-resolved" : "is-open"}`}><span className="status-dot" />{resolved ? "Resolved" : "Open"}</span>;
}

function IncidentCard({ incident, onOpen }: { incident: Incident; onOpen: (id: string) => void }) {
  return (
    <button className="incident-card" onClick={() => onOpen(incident.id)} aria-label={`View details for ${incident.title}`}>
      <div className="card-top"><span className="card-number">#{incident.id.slice(0, 8).toUpperCase()}</span><StatusBadge resolved={incident.resolved} /></div>
      <div className="card-main">
        {incident.photos[0] ? <div className="card-thumbnail"><Image src={photoPath(incident.photos[0].thumbnailUrl)} alt="Incident photo preview" width={130} height={118} unoptimized /></div> : <div className="card-thumbnail no-photo"><Icon name="image" size={26} /></div>}
        <div className="card-copy"><h3>{incident.title}</h3><p>{incident.description}</p></div>
      </div>
      <div className="card-bottom"><span><Icon name="pin" size={16} />{incident.location}</span><span><Icon name="clock" size={16} />{formatDate(incident.createdAt)}</span><span className="card-arrow"><Icon name="arrow" size={18} /></span></div>
    </button>
  );
}

function PhotoDialog({ photos, initialIndex, onClose }: {
  photos: IncidentPhoto[];
  initialIndex: number;
  onClose: () => void;
}) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const [index, setIndex] = useState(initialIndex);

  useEffect(() => {
    const dialog = dialogRef.current;
    dialog?.showModal();
    return () => { if (dialog?.open) dialog.close(); };
  }, []);

  const photo = photos[index];
  if (!photo) return null;
  const closeViewer = () => {
    dialogRef.current?.close();
    onClose();
  };

  return (
    <dialog
      ref={dialogRef}
      className="photo-viewer"
      aria-label={`Photo ${index + 1} of ${photos.length}`}
      onCancel={(event) => { event.preventDefault(); closeViewer(); }}
      onClick={(event) => { if (event.target === event.currentTarget) closeViewer(); }}
      onKeyDown={(event) => {
        event.stopPropagation();
        if (event.key === "ArrowLeft") setIndex((current) => (current - 1 + photos.length) % photos.length);
        if (event.key === "ArrowRight") setIndex((current) => (current + 1) % photos.length);
      }}
    >
      <div className="photo-viewer-header">
        <span>Photo {index + 1} of {photos.length}</span>
        <button type="button" className="photo-viewer-close" onClick={closeViewer} aria-label="Close photo">
          <Icon name="close" size={21} />
        </button>
      </div>
      <div className="photo-viewer-image">
        <Image key={photo.id} src={photoPath(photo.url)} alt={`Incident photo ${index + 1}`} fill sizes="94vw" unoptimized />
      </div>
      {photos.length > 1 && <div className="photo-viewer-controls">
        <button type="button" onClick={() => setIndex((current) => (current - 1 + photos.length) % photos.length)} aria-label="Previous photo">Previous</button>
        <button type="button" onClick={() => setIndex((current) => (current + 1) % photos.length)} aria-label="Next photo">Next</button>
      </div>}
    </dialog>
  );
}

function IncidentDetail({ incident, loading, error, saving, actionError, onClose, onToggle }: {
  incident: Incident | null;
  loading: boolean;
  error: string | null;
  saving: boolean;
  actionError: string | null;
  onClose: () => void;
  onToggle: () => void;
}) {
  const [photoIndex, setPhotoIndex] = useState<number | null>(null);

  return (
    <div className="detail-overlay" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}>
      <aside className="detail-panel" role="dialog" aria-modal="true" aria-label="Incident details">
        <div className="detail-header"><div><span className="eyebrow">INCIDENT DETAIL</span><h2>Incident details</h2></div><button className="icon-button" onClick={onClose} aria-label="Close details"><Icon name="close" /></button></div>
        {loading ? <div className="detail-loading"><div className="spinner" />Loading incident details…</div> : error ? <div className="detail-error"><Icon name="alert" /><p>{error}</p></div> : incident ? <>
          <div className="detail-scroll">
            <div className="detail-status-row"><StatusBadge resolved={incident.resolved} /><span className="detail-id">#{incident.id.slice(0, 8).toUpperCase()}</span></div>
            <h3 className="detail-title">{incident.title}</h3>
            <div className="detail-location"><Icon name="pin" size={17} />{incident.location}</div>
            <div className="detail-divider" />
            <section className="detail-section"><h4>Description</h4><p className="description-text">{incident.description}</p></section>
            <section className="detail-section"><h4>Site photos <span>({incident.photos.length})</span></h4>{incident.photos.length ? <div className="photo-grid">{incident.photos.map((photo, index) => <button type="button" key={photo.id} onClick={() => setPhotoIndex(index)} aria-label={`Enlarge photo ${index + 1}`}><Image src={photoPath(photo.url)} alt={`Site photo ${index + 1}`} fill sizes="(max-width: 650px) 100vw, 450px" unoptimized /></button>)}</div> : <p className="empty-photos">No photos available</p>}</section>
            <section className="detail-section"><h4>Timeline</h4><div className="timeline"><div><span className="timeline-dot" /><span>Reported</span><strong>{formatDate(incident.createdAt)}</strong></div><div><span className="timeline-dot" /><span>Last updated</span><strong>{formatDate(incident.updatedAt)}</strong></div>{incident.resolvedAt && <div><span className="timeline-dot green" /><span>Resolved</span><strong>{formatDate(incident.resolvedAt)}</strong></div>}</div></section>
          </div>
          <div className="detail-footer">{actionError && <p className="action-error" role="alert">{actionError}</p>}<button className={`action-button ${incident.resolved ? "secondary-action" : "primary-action"}`} disabled={saving} onClick={onToggle}>{saving ? <span className="button-spinner" /> : <Icon name={incident.resolved ? "refresh" : "check"} size={18} />}{saving ? "Updating…" : incident.resolved ? "Mark as unresolved" : "Mark as resolved"}</button><p>Status changes are saved to the server.</p></div>
        </> : null}
      </aside>
      {incident && photoIndex !== null && <PhotoDialog photos={incident.photos} initialIndex={photoIndex} onClose={() => setPhotoIndex(null)} />}
    </div>
  );
}

export function IncidentDashboard() {
  const [searchInput, setSearchInput] = useState("");
  const [query, setQuery] = useState("");
  const [filter, setFilter] = useState<StatusFilter>("all");
  const [page, setPage] = useState(0);
  const [version, setVersion] = useState(0);
  const [data, setData] = useState<IncidentPage | null>(null);
  const [loading, setLoading] = useState(true);
  const [listError, setListError] = useState<string | null>(null);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [selected, setSelected] = useState<Incident | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    const timer = window.setTimeout(() => { setPage(0); setQuery(searchInput.trim()); }, 350);
    return () => window.clearTimeout(timer);
  }, [searchInput]);

  useEffect(() => {
    const controller = new AbortController();
    const params = new URLSearchParams({ page: String(page) });
    if (query) params.set("q", query);
    if (filter !== "all") params.set("resolved", filter === "resolved" ? "true" : "false");
    Promise.resolve().then(() => {
      if (controller.signal.aborted) return null;
      setLoading(true);
      setListError(null);
      setData(null);
      return fetch(`/api/incidents?${params}`, { signal: controller.signal });
    })
      .then((response) => response && readJson<IncidentPage>(response))
      .then((pageData) => { if (pageData) setData(pageData); })
      .catch((error) => { if (!controller.signal.aborted) setListError(error instanceof Error ? error.message : "Unable to load data"); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [query, filter, page, version]);

  useEffect(() => {
    if (!selectedId) return;
    const controller = new AbortController();
    Promise.resolve().then(() => {
      if (controller.signal.aborted) return null;
      setSelected(null);
      setDetailError(null);
      setActionError(null);
      setDetailLoading(true);
      return fetch(`/api/incidents/${encodeURIComponent(selectedId)}`, { signal: controller.signal });
    })
      .then((response) => response && readJson<Incident>(response))
      .then((incident) => { if (incident) setSelected(incident); })
      .catch((error) => { if (!controller.signal.aborted) setDetailError(error instanceof Error ? error.message : "Unable to load data"); })
      .finally(() => { if (!controller.signal.aborted) setDetailLoading(false); });
    return () => controller.abort();
  }, [selectedId]);

  useEffect(() => {
    if (!selectedId) return;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape" && !document.querySelector(".photo-viewer[open]")) setSelectedId(null);
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [selectedId]);

  const changeFilter = useCallback((next: StatusFilter) => { setFilter(next); setPage(0); }, []);
  const toggleResolution = async () => {
    if (!selected || saving) return;
    setSaving(true);
    setActionError(null);
    try {
      const response = await fetch(`/api/incidents/${encodeURIComponent(selected.id)}/resolution`, {
        method: "PATCH",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ resolved: !selected.resolved }),
      });
      const updated = await readJson<Incident>(response);
      setSelected(updated);
      setVersion((value) => value + 1);
    } catch (error) {
      setActionError(error instanceof Error ? error.message : "Update failed. Please try again.");
    } finally {
      setSaving(false);
    }
  };
  const items = data?.items ?? [];
  const openCount = items.filter((item) => !item.resolved).length;
  const resolvedCount = items.length - openCount;

  return <div className="app-shell">
    <aside className="sidebar"><div className="brand"><div className="brand-mark"><span /><span /><span /><span /></div><div><strong>FIELDNOTE</strong><small>INCIDENT CONTROL</small></div></div><nav className="sidebar-nav" aria-label="Main navigation"><span className="nav-label">Workspace</span><div className="nav-item active"><Icon name="grid" size={19} />Incident overview<span className="nav-indicator" /></div></nav><div className="sidebar-bottom"><div className="sidebar-help"><span className="help-icon"><Icon name="alert" size={18} /></span><strong>Field incident management</strong><p>Track every field report and keep progress visible.</p></div><div className="sidebar-caption">FIELDNOTE · ADMIN CONSOLE</div></div></aside>
    <main className="main-content"><div className="content-wrap">
      <header className="topbar"><div className="breadcrumb">Workspace <Icon name="chevron" size={13} /> <strong>Incident overview</strong></div><div className="topbar-right"><span className="live-dot" />Admin console</div></header>
      <div className="page-heading"><div><div className="eyebrow heading-eyebrow"><span /> INCIDENT MANAGEMENT</div><h1>Incident overview<span className="heading-period">.</span></h1><p>Stay on top of field reports and follow through on every incident.</p></div><button className="refresh-button" onClick={() => setVersion((value) => value + 1)}><Icon name="refresh" size={17} />Refresh</button></div>
      <section className="overview-grid" aria-label="Incidents on the current page"><div className="overview-card"><div className="overview-icon all-icon"><Icon name="grid" /></div><div><span>On this page</span><strong>{loading && !data ? "—" : items.length.toString().padStart(2, "0")}</strong></div><small>Current page</small></div><div className="overview-card"><div className="overview-icon open-icon"><Icon name="clock" /></div><div><span>Open</span><strong>{loading && !data ? "—" : openCount.toString().padStart(2, "0")}</strong></div><small>Needs attention</small></div><div className="overview-card"><div className="overview-icon resolved-icon"><Icon name="check" /></div><div><span>Resolved</span><strong>{loading && !data ? "—" : resolvedCount.toString().padStart(2, "0")}</strong></div><small>Completed</small></div></section>
      <section className="records-section"><div className="section-heading"><div><span className="section-kicker">REPORTS</span><h2>Incident reports</h2><p>Browse, search, and manage reports from the field.</p></div></div><div className="toolbar"><div className="filter-tabs" role="group" aria-label="Filter by status"><button className={filter === "all" ? "selected" : ""} onClick={() => changeFilter("all")}>All incidents</button><button className={filter === "open" ? "selected" : ""} onClick={() => changeFilter("open")}>Open</button><button className={filter === "resolved" ? "selected" : ""} onClick={() => changeFilter("resolved")}>Resolved</button></div><label className="search-field"><Icon name="search" size={18} /><input type="search" placeholder="Search title, description, or location..." value={searchInput} onChange={(event) => setSearchInput(event.target.value)} maxLength={200} aria-label="Search incident title, description, or location" /></label></div>
      {listError ? <div className="state-panel error-state"><Icon name="alert" size={28} /><h3>Could not load incidents</h3><p>{listError}</p><button onClick={() => setVersion((value) => value + 1)}>Try again <Icon name="arrow" size={16} /></button></div> : loading ? <div className="loading-list"><div className="spinner" /><span>Loading incident reports…</span></div> : items.length ? <div className="incident-list">{items.map((incident) => <IncidentCard key={incident.id} incident={incident} onOpen={setSelectedId} />)}</div> : <div className="state-panel empty-state"><div className="empty-icon"><Icon name={query ? "search" : "grid"} size={28} /></div><h3>{query ? "No matching incidents" : "No incidents yet"}</h3><p>{query ? "Try another search term or status filter." : "New field reports will appear here."}</p></div>}
      {!listError && !loading && data && (page > 0 || data.hasNext) && <div className="pagination"><span>Page {page + 1} · Showing {items.length} incidents</span><div><button disabled={page === 0} onClick={() => setPage((value) => value - 1)}>Previous</button><button disabled={!data.hasNext} onClick={() => setPage((value) => value + 1)}>Next <Icon name="chevron" size={15} /></button></div></div>}
      </section><footer className="main-footer">FIELDNOTE <span>© 2026</span><span>Field incident management platform</span></footer>
    </div></main>
    {selectedId && <IncidentDetail incident={selected} loading={detailLoading} error={detailError} saving={saving} actionError={actionError} onClose={() => setSelectedId(null)} onToggle={toggleResolution} />}
  </div>;
}
