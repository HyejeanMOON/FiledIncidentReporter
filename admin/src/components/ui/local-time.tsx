"use client";

import { useSyncExternalStore } from "react";

const dateOptions: Intl.DateTimeFormatOptions = {
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
  hour: "2-digit",
  minute: "2-digit",
  timeZoneName: "short",
};

const subscribe = () => () => {};

/**
 * Shows a timestamp in the viewer's time zone. The server cannot know that zone, so it renders UTC,
 * and the browser re-renders in local time right after hydration.
 */
export function LocalTime({ value }: { value: string }) {
  const inBrowser = useSyncExternalStore(subscribe, () => true, () => false);
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return <>—</>;
  const format = new Intl.DateTimeFormat("en-US", { ...dateOptions, timeZone: inBrowser ? undefined : "UTC" });
  return <time dateTime={value}>{format.format(date)}</time>;
}
