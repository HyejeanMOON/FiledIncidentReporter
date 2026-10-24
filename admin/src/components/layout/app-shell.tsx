import type { ReactNode } from "react";
import { Icon } from "@/components/ui/icon";

function Sidebar() {
  return (
    <aside className="sidebar">
      <div className="brand"><div className="brand-mark"><span /><span /><span /><span /></div><div><strong>FIELDNOTE</strong><small>INCIDENT CONTROL</small></div></div>
      <nav className="sidebar-nav" aria-label="Main navigation"><span className="nav-label">Workspace</span><div className="nav-item active"><Icon name="grid" size={19} />Incident overview<span className="nav-indicator" /></div></nav>
      <div className="sidebar-bottom">
        <div className="sidebar-help"><span className="help-icon"><Icon name="alert" size={18} /></span><strong>Field incident management</strong><p>Track every field report and keep progress visible.</p></div>
        <div className="sidebar-caption">FIELDNOTE · ADMIN CONSOLE</div>
      </div>
    </aside>
  );
}

export function AppShell({ children }: { children: ReactNode }) {
  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-content"><div className="content-wrap">
        <header className="topbar"><div className="breadcrumb">Workspace <Icon name="chevron" size={13} /> <strong>Incident overview</strong></div><div className="topbar-right"><span className="live-dot" />Admin console</div></header>
        {children}
        <footer className="main-footer">FIELDNOTE <span>© 2026</span><span>Field incident management platform</span></footer>
      </div></main>
    </div>
  );
}
