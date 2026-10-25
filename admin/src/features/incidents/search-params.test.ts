import { describe, expect, test } from "vitest";
import { incidentListHref, parseIncidentQuery, type IncidentQuery } from "./search-params";

describe("parseIncidentQuery", () => {
  test("starts at the first page of all incidents", () => {
    expect(parseIncidentQuery({})).toEqual({ status: "all", field: "all", q: "", page: 0 });
  });

  test("reads every list setting", () => {
    expect(parseIncidentQuery({ status: "resolved", field: "location", q: "pump", page: "3" }))
      .toEqual({ status: "resolved", field: "location", q: "pump", page: 3 });
  });

  test("falls back to the defaults for unknown values", () => {
    expect(parseIncidentQuery({ status: "closed", field: "photos", page: "next" }))
      .toEqual({ status: "all", field: "all", q: "", page: 0 });
  });

  test("uses the first of repeated parameters", () => {
    expect(parseIncidentQuery({ status: ["open", "resolved"], q: ["first", "second"] }))
      .toMatchObject({ status: "open", q: "first" });
  });

  test("trims the search and keeps at most 200 characters", () => {
    expect(parseIncidentQuery({ q: "  pump  " }).q).toBe("pump");
    expect(parseIncidentQuery({ q: "x".repeat(250) }).q).toHaveLength(200);
  });

  test("keeps the page a whole number from zero", () => {
    expect(parseIncidentQuery({ page: "-2" }).page).toBe(0);
    expect(parseIncidentQuery({ page: "2.9" }).page).toBe(2);
  });
});

describe("incidentListHref", () => {
  test("leaves the defaults out of the URL", () => {
    expect(incidentListHref({ status: "all", field: "all", q: "", page: 0 })).toBe("/");
    expect(incidentListHref({ status: "all", field: "all", q: "", page: 1 })).toBe("/?page=1");
  });

  test("round-trips through parseIncidentQuery", () => {
    const query: IncidentQuery = { status: "open", field: "title", q: "pump & valve", page: 2 };
    const href = incidentListHref(query);

    expect(href).toBe("/?status=open&field=title&q=pump+%26+valve&page=2");
    expect(parseIncidentQuery(Object.fromEntries(new URL(href, "http://admin").searchParams))).toEqual(query);
  });
});
