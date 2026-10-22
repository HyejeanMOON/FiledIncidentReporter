import type { ReactNode } from "react";

export function Spinner() {
  return <div className="spinner" />;
}

/** Bordered placeholder for loading, empty and error states of a list. */
export function StatePanel({ className = "", children }: { className?: string; children: ReactNode }) {
  return <div className={`state-panel ${className}`}>{children}</div>;
}

export function LoadingPanel({ label }: { label: string }) {
  return <StatePanel><Spinner /><span>{label}</span></StatePanel>;
}
