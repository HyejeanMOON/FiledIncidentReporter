import { describe, expect, test } from "vitest";
import { MAX_PHOTOS, missingFields } from "./validation";

const filled = { title: "Broken pump", location: "Plant A", description: "Leaking since morning" };

describe("missingFields", () => {
  test("accepts a complete report", () => {
    expect(missingFields(filled, 1)).toEqual([]);
    expect(missingFields(filled, MAX_PHOTOS)).toEqual([]);
  });

  test("treats whitespace-only text as missing", () => {
    expect(missingFields({ ...filled, title: "   ", description: "" }, 1)).toEqual(["title", "description"]);
  });

  test("needs 1 to 20 photos", () => {
    expect(missingFields(filled, 0)).toEqual(["photos"]);
    expect(missingFields(filled, MAX_PHOTOS + 1)).toEqual(["photos"]);
  });
});
