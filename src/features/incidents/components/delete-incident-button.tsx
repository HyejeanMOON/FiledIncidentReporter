"use client";

import { useEffect, useState, useTransition } from "react";
import { Dialog } from "@/components/ui/dialog";
import { Icon } from "@/components/ui/icon";
import { deleteIncident } from "../actions";
import { useDetailContext } from "./detail-context";

export function DeleteIncidentButton({ id }: { id: string }) {
  const { exit, setBusy } = useDetailContext();
  const [confirming, setConfirming] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isDeleting, startTransition] = useTransition();

  useEffect(() => setBusy(isDeleting), [isDeleting, setBusy]);

  const close = () => { if (!isDeleting) { setConfirming(false); setError(null); } };
  const confirm = () => startTransition(async () => {
    const result = await deleteIncident(id);
    if (result) startTransition(() => setError(result));
    else exit({ refresh: true });
  });

  return (
    <>
      <button type="button" className="detail-action danger" onClick={() => setConfirming(true)}><Icon name="trash" size={16} />Delete</button>
      {confirming && (
        <Dialog className="confirm-dialog" label="Delete this incident?" onClose={close}>
          <h3>Delete this incident?</h3>
          <p>This permanently removes the incident and its photos from the server.</p>
          {error && <p className="action-error" role="alert">{error}</p>}
          <div className="confirm-actions">
            <button type="button" className="text-button" onClick={close} disabled={isDeleting}>Cancel</button>
            <button type="button" className="danger-button" onClick={confirm} disabled={isDeleting}>
              {isDeleting && <span className="button-spinner" />}{isDeleting ? "Deleting…" : "Delete"}
            </button>
          </div>
        </Dialog>
      )}
    </>
  );
}
