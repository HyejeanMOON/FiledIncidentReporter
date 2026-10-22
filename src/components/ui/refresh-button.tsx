"use client";

import { useTransition, type ReactNode } from "react";
import { useRouter } from "next/navigation";

/** Re-renders the current route on the server while keeping the current UI visible. */
export function RefreshButton({ className, children }: { className?: string; children: ReactNode }) {
  const router = useRouter();
  const [isPending, startTransition] = useTransition();
  return (
    <button
      type="button"
      className={className}
      data-pending={isPending ? "" : undefined}
      onClick={() => startTransition(() => router.refresh())}
    >
      {children}
    </button>
  );
}
