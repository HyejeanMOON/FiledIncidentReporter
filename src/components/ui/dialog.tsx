"use client";

import { useEffect, useRef, type KeyboardEventHandler, type ReactNode } from "react";

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

  useEffect(() => {
    const dialog = ref.current;
    dialog?.showModal();
    return () => dialog?.close();
  }, []);

  return (
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
    </dialog>
  );
}
