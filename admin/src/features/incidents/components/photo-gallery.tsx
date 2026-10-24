"use client";

import { useState } from "react";
import Image from "next/image";
import { Dialog } from "@/components/ui/dialog";
import { Icon } from "@/components/ui/icon";
import { photoPath } from "../format";
import type { IncidentPhoto } from "../types";

function PhotoViewer({ photos, initialIndex, onClose }: { photos: IncidentPhoto[]; initialIndex: number; onClose: () => void }) {
  const [index, setIndex] = useState(initialIndex);
  const step = (delta: number) => setIndex((current) => (current + delta + photos.length) % photos.length);
  const photo = photos[index];
  if (!photo) return null;

  return (
    <Dialog
      className="photo-viewer"
      label={`Photo ${index + 1} of ${photos.length}`}
      onClose={onClose}
      onKeyDown={(event) => {
        if (event.key === "ArrowLeft") step(-1);
        if (event.key === "ArrowRight") step(1);
      }}
    >
      <div className="photo-viewer-header">
        <span>Photo {index + 1} of {photos.length}</span>
        <button type="button" className="photo-viewer-close" onClick={onClose} aria-label="Close photo"><Icon name="close" size={21} /></button>
      </div>
      <div className="photo-viewer-image">
        <Image key={photo.id} src={photoPath(photo.url)} alt={`Incident photo ${index + 1}`} fill sizes="94vw" unoptimized />
      </div>
      {photos.length > 1 && <div className="photo-viewer-controls">
        <button type="button" onClick={() => step(-1)} aria-label="Previous photo">Previous</button>
        <button type="button" onClick={() => step(1)} aria-label="Next photo">Next</button>
      </div>}
    </Dialog>
  );
}

export function PhotoGallery({ photos }: { photos: IncidentPhoto[] }) {
  const [openIndex, setOpenIndex] = useState<number | null>(null);
  if (!photos.length) return <p className="empty-photos">No photos available</p>;

  return (
    <>
      <div className="photo-grid">
        {photos.map((photo, index) => (
          <button type="button" key={photo.id} onClick={() => setOpenIndex(index)} aria-label={`Enlarge photo ${index + 1}`}>
            <Image src={photoPath(photo.url)} alt={`Site photo ${index + 1}`} fill sizes="(max-width: 650px) 100vw, 450px" unoptimized />
          </button>
        ))}
      </div>
      {openIndex !== null && <PhotoViewer photos={photos} initialIndex={openIndex} onClose={() => setOpenIndex(null)} />}
    </>
  );
}
