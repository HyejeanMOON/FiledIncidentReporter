"use client";

import type { ReactNode } from "react";
import { useRouter } from "next/navigation";
import { Dialog } from "@/components/ui/dialog";
import { Icon } from "@/components/ui/icon";
import { IncidentDetailHeader } from "./incident-detail-header";

/** Side panel for the intercepted detail route; closing returns to the list underneath. */
export function DetailDrawer({ children }: { children: ReactNode }) {
  const router = useRouter();
  const close = () => router.back();
  return (
    <Dialog className="detail-panel" label="Incident details" onClose={close}>
      <IncidentDetailHeader action={<button type="button" className="icon-button" onClick={close} aria-label="Close details"><Icon name="close" /></button>} />
      {children}
    </Dialog>
  );
}
