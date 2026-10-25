"use client";

import { useEffect, useEffectEvent, useRef, useState, useTransition, type FormEvent } from "react";
import Image from "next/image";
import { Icon } from "@/components/ui/icon";
import { createIncident, updateIncident } from "../actions";
import { photoPath } from "../format";
import { messages } from "../messages";
import type { Incident } from "../types";
import {
  fieldLimits, MAX_PHOTO_BYTES, missingFields, photoTypes, type IncidentFields, type RequiredField, type TextField,
} from "../validation";

/** A photo already on the server (`storedId`) or a file picked in this form. */
type FormPhoto = { key: string; src: string; storedId?: string; file?: File };

const textFields: { field: TextField; label: string; multiline?: boolean }[] = [
  { field: "title", label: "Incident title" },
  { field: "location", label: "Location" },
  { field: "description", label: "Description", multiline: true },
];

const fileKey = (file: File) => `${file.name}:${file.size}:${file.lastModified}`;

/** Creates a report, or edits `incident` when given. Mirrors the Android report form. */
export function IncidentForm({ incident, heading, onCancel, onSaved, onSavingChange }: {
  incident?: Incident;
  heading?: string;
  onCancel: () => void;
  onSaved: () => void;
  onSavingChange?: (saving: boolean) => void;
}) {
  const [fields, setFields] = useState<IncidentFields>({
    title: incident?.title ?? "",
    location: incident?.location ?? "",
    description: incident?.description ?? "",
  });
  const [photos, setPhotos] = useState<FormPhoto[]>(() =>
    incident?.photos.map((photo) => ({ key: photo.id, src: photoPath(photo.thumbnailUrl), storedId: photo.id })) ?? [],
  );
  // Only fields the user has visited show a "required" error.
  const [visited, setVisited] = useState<ReadonlySet<RequiredField>>(new Set());
  const [error, setError] = useState<string | null>(null);
  // Set when the last pick skipped files the backend would reject for their size.
  const [photoError, setPhotoError] = useState<string | null>(null);
  const [isSaving, startTransition] = useTransition();
  const pickerRef = useRef<HTMLInputElement>(null);
  const previewUrls = useRef(new Set<string>());

  useEffect(() => onSavingChange?.(isSaving), [isSaving, onSavingChange]);
  useEffect(() => {
    const urls = previewUrls.current;
    return () => urls.forEach((url) => URL.revokeObjectURL(url));
  }, []);

  const missing = missingFields(fields, photos.length);
  const showError = (field: RequiredField) => visited.has(field) && missing.includes(field);
  const visit = (field: RequiredField) => setVisited((current) => new Set(current).add(field));

  // Closing the picker without a photo reveals the photo error, as on Android.
  // React does not type `onCancel` for inputs, so listen to the native event.
  const onPickerCancel = useEffectEvent(() => { if (!photos.length) visit("photos"); });
  useEffect(() => {
    const input = pickerRef.current;
    const listener = () => onPickerCancel();
    input?.addEventListener("cancel", listener);
    return () => input?.removeEventListener("cancel", listener);
  }, []);

  const addFiles = (files: FileList | null) => {
    if (!files?.length) return;
    const picked = [...files];
    const accepted = picked.filter((file) => file.size <= MAX_PHOTO_BYTES);
    setPhotoError(accepted.length < picked.length ? messages.photoTooLarge : null);
    const known = new Set(photos.map((photo) => photo.key));
    const added = accepted.filter((file) => !known.has(fileKey(file))).map((file) => {
      const src = URL.createObjectURL(file);
      previewUrls.current.add(src);
      return { key: fileKey(file), src, file };
    });
    setPhotos([...photos, ...added]);
  };

  const removePhotos = (removed: FormPhoto[]) => {
    removed.forEach(({ src }) => {
      if (previewUrls.current.delete(src)) URL.revokeObjectURL(src);
    });
    setPhotos((current) => current.filter((photo) => !removed.includes(photo)));
    visit("photos");
  };

  const submit = (event: FormEvent) => {
    event.preventDefault();
    if (missing.length || isSaving) return;
    const formData = new FormData();
    for (const { field } of textFields) formData.set(field, fields[field]);
    photos.forEach(({ file }) => file && formData.append("photos", file));
    const retained = new Set(photos.map((photo) => photo.storedId));
    incident?.photos.forEach(({ id }) => !retained.has(id) && formData.append("deletePhotoIds", id));

    setError(null);
    startTransition(async () => {
      const result = incident ? await updateIncident(incident.id, formData) : await createIncident(formData);
      startTransition(() => (result ? setError(result) : onSaved()));
    });
  };

  const photoInput = (
    <input
      ref={pickerRef}
      type="file"
      accept={photoTypes.join(",")}
      hidden
      multiple
      onChange={(event) => { addFiles(event.target.files); event.target.value = ""; }}
    />
  );

  return (
    <form className="incident-form" onSubmit={submit} noValidate>
      <fieldset className="detail-scroll" disabled={isSaving}>
        {heading && <h3 className="form-heading">{heading}</h3>}
        <p className="form-intro">Add the details a maintenance team needs to understand and locate the issue.</p>
        {textFields.map(({ field, label, multiline }) => {
          const invalid = showError(field);
          const props = {
            id: `incident-${field}`,
            value: fields[field],
            maxLength: fieldLimits[field],
            "aria-invalid": invalid,
            "aria-describedby": invalid ? `incident-${field}-error` : undefined,
            onChange: (event: { target: { value: string } }) => setFields((current) => ({ ...current, [field]: event.target.value })),
            onBlur: () => visit(field),
          };
          return (
            <div key={field} className="form-field">
              <label htmlFor={props.id}>{label}</label>
              {multiline ? <textarea rows={5} {...props} /> : <input type="text" {...props} />}
              {invalid && <p id={`incident-${field}-error`} className="field-error">This field is required.</p>}
            </div>
          );
        })}

        <section className={`photo-field${showError("photos") ? " is-invalid" : ""}`} aria-labelledby="incident-photos-label">
          <h4 id="incident-photos-label">Photos</h4>
          <p className="form-help">Add pictures that help identify the issue.</p>
          {showError("photos") && <p className="field-error" role="alert">Select 1 to 20 photos.</p>}
          {photoError && <p className="field-error" role="alert">{photoError}</p>}
          <div className="photo-field-buttons">
            <button type="button" className="outline-button" onClick={() => pickerRef.current?.click()} onBlur={() => visit("photos")}><Icon name="image" size={16} />Choose photos</button>
            {photoInput}
          </div>
          {photos.length > 0 && <>
            <p className="photo-count">{photos.length} photos selected</p>
            <ul className="photo-picks">
              {photos.map((photo, index) => (
                <li key={photo.key}>
                  <div className="photo-pick-image"><Image src={photo.src} alt={`Incident photo ${index + 1}`} fill sizes="96px" unoptimized /></div>
                  <button type="button" className="text-button" onClick={() => removePhotos([photo])}>Remove</button>
                </li>
              ))}
            </ul>
            <button type="button" className="text-button" onClick={() => removePhotos(photos)}>Remove all photos</button>
          </>}
        </section>
      </fieldset>

      <div className="detail-footer">
        {error && <p className="action-error" role="alert">{error}</p>}
        <button type="submit" className="action-button primary-action" disabled={missing.length > 0 || isSaving}>
          {isSaving && <span className="button-spinner" />}
          {isSaving ? (incident ? "Saving changes…" : "Submitting incident…") : incident ? "Save changes" : "Submit incident"}
        </button>
        <button type="button" className="text-button form-cancel" onClick={onCancel} disabled={isSaving}>Cancel</button>
      </div>
    </form>
  );
}
