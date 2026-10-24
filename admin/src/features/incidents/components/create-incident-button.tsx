"use client";

import { useState } from "react";
import { Dialog } from "@/components/ui/dialog";
import { Icon } from "@/components/ui/icon";
import { IncidentDetailHeader } from "./incident-detail-header";
import { IncidentForm } from "./incident-form";

/** Opens the new report form in a side panel; a successful submit refreshes the list. */
export function CreateIncidentButton() {
  const [open, setOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const close = () => { if (!saving) setOpen(false); };

  return (
    <>
      <button type="button" className="report-button" onClick={() => setOpen(true)}><Icon name="plus" size={17} />Report incident</button>
      {open && (
        <Dialog className="detail-panel" label="New incident" onClose={close}>
          <IncidentDetailHeader eyebrow="NEW REPORT" title="New incident" action={<button type="button" className="icon-button" onClick={close} disabled={saving} aria-label="Close form"><Icon name="close" /></button>} />
          <IncidentForm onCancel={close} onSaved={() => setOpen(false)} onSavingChange={setSaving} />
        </Dialog>
      )}
    </>
  );
}
