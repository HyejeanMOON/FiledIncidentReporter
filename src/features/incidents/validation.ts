/** Report rules shared by the form and the Server Functions; they mirror the backend and the Android client. */
export const MAX_PHOTOS = 20;

export const fieldLimits = { title: 200, location: 200, description: 4000 } as const;

export const photoTypes = ["image/jpeg", "image/png", "image/webp", "image/heic", "image/heif", "image/gif"];

export type TextField = keyof typeof fieldLimits;
export type RequiredField = TextField | "photos";

export type IncidentFields = Record<TextField, string>;

export function missingFields(fields: IncidentFields, photoCount: number): RequiredField[] {
  const missing: RequiredField[] = (Object.keys(fieldLimits) as TextField[]).filter((field) => !fields[field].trim());
  if (photoCount < 1 || photoCount > MAX_PHOTOS) missing.push("photos");
  return missing;
}
