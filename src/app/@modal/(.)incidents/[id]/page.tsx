import { Suspense } from "react";
import { DetailDrawer } from "@/features/incidents/components/detail-drawer";
import { IncidentDetail, IncidentDetailLoading } from "@/features/incidents/components/incident-detail";

// Opening a card from the list renders the detail as a drawer over the list.
export default function IncidentDrawerPage({ params }: PageProps<"/incidents/[id]">) {
  return (
    <DetailDrawer>
      <Suspense fallback={<IncidentDetailLoading />}>
        {params.then(({ id }) => <IncidentDetail id={id} />)}
      </Suspense>
    </DetailDrawer>
  );
}
