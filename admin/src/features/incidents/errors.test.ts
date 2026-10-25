import { describe, expect, test } from "vitest";
import { BACKEND_TIMEOUT, BACKEND_UNREACHABLE, BackendError } from "@/lib/backend";
import { errorMessage } from "./errors";
import { messages } from "./messages";

const fallback = "fallback";

describe("errorMessage", () => {
  test.each([
    ["INCIDENT_NOT_FOUND", messages.notFound],
    ["RESOURCE_NOT_FOUND", messages.notFound],
    ["INVALID_INCIDENT", messages.invalidIncident],
    ["INVALID_REQUEST", messages.invalidRequest],
    ["PAYLOAD_TOO_LARGE", messages.photoTooLarge],
    ["INTERNAL_ERROR", messages.serverError],
    [BACKEND_UNREACHABLE, messages.unreachable],
    [BACKEND_TIMEOUT, messages.timeout],
  ])("shows the shared message for %s", (code, message) => {
    expect(errorMessage(new BackendError(code, "backend detail", 400), fallback)).toBe(message);
  });

  test("shows the backend's detail for codes it does not know", () => {
    expect(errorMessage(new BackendError("NEW_CODE", "backend detail", 409), fallback)).toBe("backend detail");
    expect(errorMessage(new BackendError(undefined, "Request failed (502)", 502), fallback)).toBe("Request failed (502)");
  });

  test("hides errors that did not come from the backend", () => {
    expect(errorMessage(new Error("internal detail"), fallback)).toBe(fallback);
    expect(errorMessage("thrown string", fallback)).toBe(fallback);
  });
});
