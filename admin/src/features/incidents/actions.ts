"use server";

import { refresh } from "next/cache";
import { requestJson } from "@/lib/backend";
import { errorMessage } from "./errors";
import { messages } from "./messages";
import { fieldLimits, MAX_PHOTO_BYTES, MAX_PHOTOS, photoTypes, type IncidentFields } from "./validation";

const incidentPath = (id: string) => `/incidents/${encodeURIComponent(id)}`;

async function send(path: string, init: RequestInit, fallback: string): Promise<string | null> {
  try {
    await requestJson(path, init);
    return null;
  } catch (error) {
    return errorMessage(error, fallback);
  }
}

/** Reads the form fields and new photos, or returns the message for invalid input. */
function readIncidentForm(formData: FormData): { fields: IncidentFields; photos: File[] } | string {
  const fields = {} as IncidentFields;
  for (const [field, max] of Object.entries(fieldLimits) as [keyof IncidentFields, number][]) {
    const value = formData.get(field);
    const trimmed = typeof value === "string" ? value.trim() : "";
    if (!trimmed || trimmed.length > max) return messages.invalidIncident;
    fields[field] = trimmed;
  }
  const photos = formData.getAll("photos").filter((value): value is File => value instanceof File);
  if (photos.length > MAX_PHOTOS) return messages.invalidIncident;
  if (photos.some((photo) => !photoTypes.includes(photo.type))) return messages.unreadablePhoto;
  if (photos.some((photo) => photo.size > MAX_PHOTO_BYTES)) return messages.photoTooLarge;
  return { fields, photos };
}

/** The backend expects an `incident` JSON part plus `photos` file parts. */
function multipart(incident: object, photos: File[]) {
  const body = new FormData();
  body.append("incident", new Blob([JSON.stringify(incident)], { type: "application/json" }));
  photos.forEach((photo, index) => body.append("photos", photo, `incident-photo-${index}`));
  return body;
}

export async function createIncident(formData: FormData): Promise<string | null> {
  const input = readIncidentForm(formData);
  if (typeof input === "string") return input;
  if (!input.photos.length) return messages.invalidIncident;
  const error = await send("/incidents", { method: "POST", body: multipart(input.fields, input.photos) }, messages.saveFailed);
  if (!error) refresh();
  return error;
}

export async function updateIncident(id: string, formData: FormData): Promise<string | null> {
  if (typeof id !== "string") return messages.invalidRequest;
  const input = readIncidentForm(formData);
  if (typeof input === "string") return input;
  const deletePhotoIds = formData.getAll("deletePhotoIds").filter((value) => typeof value === "string");
  const body = multipart({ ...input.fields, deletePhotoIds }, input.photos);
  const error = await send(incidentPath(id), { method: "PATCH", body }, messages.saveFailed);
  if (!error) refresh();
  return error;
}

/** The caller leaves the detail view and refreshes the list after a successful delete. */
export async function deleteIncident(id: string): Promise<string | null> {
  if (typeof id !== "string") return messages.invalidRequest;
  return send(incidentPath(id), { method: "DELETE" }, messages.saveFailed);
}

export async function setIncidentResolution(id: string, resolved: boolean): Promise<string | null> {
  // Server Functions are public endpoints, so check the argument types a forged request could change.
  if (typeof id !== "string" || typeof resolved !== "boolean") return messages.invalidRequest;
  const error = await send(`${incidentPath(id)}/resolution`, {
    method: "PATCH",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ resolved }),
  }, messages.updateFailed);
  // Re-render the open detail and the list behind it with the new status.
  if (!error) refresh();
  return error;
}
