import { Suspense } from "react";
import type { Metadata } from "next";
import Link from "next/link";
import { Icon } from "@/components/ui/icon";
import { IncidentDetail, IncidentDetailLoading } from "@/features/incidents/components/incident-detail";
import { IncidentDetailHeader } from "@/features/incidents/components/incident-detail-header";

export const metadata: Metadata = { title: "Incident details | Fieldnote" };

// Direct visits and reloads of a detail URL render it as a standalone page.
export default function IncidentPage({ params }: PageProps<"/incidents/[id]">) {
  return (
    <article className="detail-page">
      <IncidentDetailHeader action={<Link href="/" className="icon-button" aria-label="Back to incident overview"><Icon name="back" /></Link>} />
      <Suspense fallback={<IncidentDetailLoading />}>
        {params.then(({ id }) => <IncidentDetail id={id} />)}
      </Suspense>
    </article>
  );
}
