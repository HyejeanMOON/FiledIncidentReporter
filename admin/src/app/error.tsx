"use client";

import { Icon } from "@/components/ui/icon";
import { StatePanel } from "@/components/ui/state-panel";

export default function Error({ retry }: { error: unknown; retry: () => void }) {
  return (
    <StatePanel className="error-state page-error">
      <Icon name="alert" size={28} /><h3>Something went wrong</h3><p>The page could not be displayed.</p>
      <button type="button" onClick={() => retry()}>Try again <Icon name="arrow" size={16} /></button>
    </StatePanel>
  );
}
