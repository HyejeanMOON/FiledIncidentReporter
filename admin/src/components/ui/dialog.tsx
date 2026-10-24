"use client";

import { useEffect, useRef, useSyncExternalStore, type KeyboardEventHandler, type ReactNode } from "react";
import { createPortal } from "react-dom";

const subscribeNever = () => () => {};

/**
 * A native modal `<dialog>`: focus trapping, Escape and stacking come from the browser.
 * Escape and backdrop clicks call `onClose`; the dialog closes when the parent unmounts it.
 */
export function Dialog({ className, label, onClose, onKeyDown, children }: {
  className: string;
  label: string;
  onClose: () => void;
  onKeyDown?: KeyboardEventHandler<HTMLDialogElement>;
  children: ReactNode;
}) {
  const ref = useRef<HTMLDialogElement>(null);
  // The portal target only exists in the browser; prerendered shells render nothing here.
  const isClient = useSyncExternalStore(subscribeNever, () => true, () => false);

  useEffect(() => {
    const dialog = ref.current;
    dialog?.showModal();
    return () => dialog?.close();
  }, [isClient]);

  if (!isClient) return null;
  // Portal to <body> so styles of the opener's ancestors don't leak into the dialog.
  return createPortal(
    <dialog
      ref={ref}
      className={className}
      aria-label={label}
      // React propagates these events through parent components, so a nested dialog's
      // Escape or backdrop click must not also close this one.
      onCancel={(event) => { if (event.target !== event.currentTarget) return; event.preventDefault(); onClose(); }}
      onClick={(event) => { if (event.target === event.currentTarget) onClose(); }}
      onKeyDown={onKeyDown}
    >
      {children}
    </dialog>,
    document.body,
  );
}
