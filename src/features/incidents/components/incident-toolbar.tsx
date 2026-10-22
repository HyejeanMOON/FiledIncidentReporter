"use client";

import { useEffect, useEffectEvent, useState } from "react";
import { Icon } from "@/components/ui/icon";
import { searchFields, statusFilters, type IncidentQuery, type SearchField } from "../search-params";
import { useIncidentNavigation } from "../use-incident-navigation";

export function IncidentToolbar({ query: committed }: { query: IncidentQuery }) {
  const { query, navigate, isPending } = useIncidentNavigation(committed);
  const [searchInput, setSearchInput] = useState(committed.q);

  const commitSearch = useEffectEvent((value: string) => {
    const q = value.trim();
    if (q !== query.q) navigate({ q, page: 0 });
  });
  useEffect(() => {
    const timer = window.setTimeout(() => commitSearch(searchInput), 350);
    return () => window.clearTimeout(timer);
  }, [searchInput]);

  // The field only affects results while there is a search term.
  const changeField = (field: SearchField) => navigate(query.q ? { field, page: 0 } : { field });

  return (
    <div className="toolbar" data-pending={isPending ? "" : undefined}>
      <div className="filter-tabs" role="group" aria-label="Filter by status">
        {statusFilters.map(({ value, label }) => (
          <button key={value} type="button" className={query.status === value ? "selected" : ""} onClick={() => navigate({ status: value, page: 0 })}>{label}</button>
        ))}
      </div>
      <div className="search-controls">
        <label className="search-scope">Search in<select value={query.field} onChange={(event) => changeField(event.target.value as SearchField)} aria-label="Search field">
          {searchFields.map(({ value, label }) => <option key={value} value={value}>{label}</option>)}
        </select></label>
        <label className="search-field"><Icon name="search" size={18} /><input type="search" placeholder="Search incidents..." value={searchInput} onChange={(event) => setSearchInput(event.target.value)} maxLength={200} aria-label="Search incidents" /></label>
      </div>
    </div>
  );
}
