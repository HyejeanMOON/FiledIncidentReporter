"use client";

import { createContext, useContext } from "react";
import { useRouter } from "next/navigation";

type DetailContextValue = {
  /** Leaves the detail view; `refresh` reloads the list it returns to. */
  exit: (options?: { refresh?: boolean }) => void;
  /** Blocks closing the view while a save or delete is in flight. */
  setBusy: (busy: boolean) => void;
};

export const DetailContext = createContext<DetailContextValue | null>(null);

const noop = () => {};

/** The drawer provides its own behaviour; the standalone page navigates to the list. */
export function useDetailContext(): DetailContextValue {
  const router = useRouter();
  return useContext(DetailContext) ?? { exit: () => router.replace("/"), setBusy: noop };
}
