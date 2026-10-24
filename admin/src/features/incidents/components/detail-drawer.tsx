"use client";

import { useMemo, useState, type ReactNode } from "react";
import { useRouter } from "next/navigation";
import { Dialog } from "@/components/ui/dialog";
import { Icon } from "@/components/ui/icon";
import { DetailContext } from "./detail-context";
import { IncidentDetailHeader } from "./incident-detail-header";

/** Side panel for the intercepted detail route; closing returns to the list underneath. */
export function DetailDrawer({ children }: { children: ReactNode }) {
  const router = useRouter();
  const [busy, setBusy] = useState(false);
  const context = useMemo(() => ({
    exit: ({ refresh = false } = {}) => {
      // Refresh once the list entry is restored, so the list drops the deleted incident.
      if (refresh) window.addEventListener("popstate", () => router.refresh(), { once: true });
      router.back();
    },
    setBusy,
  }), [router]);
  const close = () => { if (!busy) context.exit(); };

  return (
    <DetailContext value={context}>
      <Dialog className="detail-panel" label="Incident details" onClose={close}>
        <IncidentDetailHeader action={<button type="button" className="icon-button" onClick={close} disabled={busy} aria-label="Close details"><Icon name="close" /></button>} />
        {children}
      </Dialog>
    </DetailContext>
  );
}
