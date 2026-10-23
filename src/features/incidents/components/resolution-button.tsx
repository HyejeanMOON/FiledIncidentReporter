"use client";

import { useActionState } from "react";
import { Icon } from "@/components/ui/icon";
import { setIncidentResolution } from "../actions";

export function ResolutionButton({ id, resolved }: { id: string; resolved: boolean }) {
  const [error, toggleAction, isPending] = useActionState(() => setIncidentResolution(id, !resolved), null);

  return (
    <form action={toggleAction}>
      {error && <p className="action-error" role="alert">{error}</p>}
      <button type="submit" className={`action-button ${resolved ? "secondary-action" : "primary-action"}`} disabled={isPending}>
        {isPending ? <span className="button-spinner" /> : <Icon name={resolved ? "refresh" : "check"} size={18} />}
        {isPending ? "Updating…" : resolved ? "Mark as unresolved" : "Mark as resolved"}
      </button>
    </form>
  );
}
