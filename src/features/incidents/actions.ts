"use server";

import { refresh } from "next/cache";
import { errorMessage, requestJson } from "@/lib/backend";

export async function setIncidentResolution(id: string, resolved: boolean): Promise<string | null> {
  if (typeof id !== "string" || typeof resolved !== "boolean") return "resolved must be a boolean.";
  try {
    await requestJson(`/incidents/${encodeURIComponent(id)}/resolution`, {
      method: "PATCH",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ resolved }),
    });
  } catch (error) {
    return errorMessage(error, "Update failed. Please try again.");
  }
  // Re-render the open detail and the list behind it with the new status.
  refresh();
  return null;
}
