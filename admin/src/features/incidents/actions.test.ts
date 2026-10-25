import { beforeEach, describe, expect, test, vi } from "vitest";
import { refresh } from "next/cache";
import { BackendError, requestJson } from "@/lib/backend";
import { createIncident, deleteIncident, setIncidentResolution, updateIncident } from "./actions";
import { messages } from "./messages";
import { MAX_PHOTO_BYTES, MAX_PHOTOS, type IncidentFields } from "./validation";

vi.mock("next/cache", () => ({ refresh: vi.fn() }));
vi.mock("@/lib/backend", async (importOriginal) => ({
  ...(await importOriginal<typeof import("@/lib/backend")>()),
  requestJson: vi.fn(),
}));

const request = vi.mocked(requestJson);

beforeEach(() => {
  vi.clearAllMocks();
  request.mockResolvedValue(null);
});

const photo = (type = "image/jpeg", size = 4) => new File([new Uint8Array(size)], "photo", { type });

function form({ photos = [photo()], ...fields }: Partial<IncidentFields> & { photos?: File[] } = {}) {
  const data = new FormData();
  Object.entries({ title: "Broken pump", location: "Plant A", description: "Leaking", ...fields })
    .forEach(([name, value]) => data.set(name, value));
  photos.forEach((file) => data.append("photos", file));
  return data;
}

/** The method and the parts of the multipart body sent to the backend. */
async function sent() {
  const [path, init] = request.mock.calls[0];
  const body = init?.body as FormData;
  return {
    path,
    method: init?.method,
    incident: JSON.parse(await (body.get("incident") as Blob).text()),
    photos: body.getAll("photos"),
  };
}

describe("createIncident", () => {
  test("sends trimmed fields and photos, then refreshes the page", async () => {
    await expect(createIncident(form({ title: "  Broken pump  ", photos: [photo(), photo("image/png")] }))).resolves.toBeNull();

    const { path, method, incident, photos } = await sent();
    expect([path, method]).toEqual(["/incidents", "POST"]);
    expect(incident).toEqual({ title: "Broken pump", location: "Plant A", description: "Leaking" });
    expect(photos).toHaveLength(2);
    expect(refresh).toHaveBeenCalledOnce();
  });

  test.each([
    ["a blank field", form({ location: "   " }), messages.invalidIncident],
    ["a field over its limit", form({ title: "x".repeat(201) }), messages.invalidIncident],
    ["no photos", form({ photos: [] }), messages.invalidIncident],
    ["too many photos", form({ photos: Array.from({ length: MAX_PHOTOS + 1 }, () => photo()) }), messages.invalidIncident],
    ["an unsupported photo type", form({ photos: [photo("application/pdf")] }), messages.unreadablePhoto],
    ["a photo over 10 MB", form({ photos: [photo("image/jpeg", MAX_PHOTO_BYTES + 1)] }), messages.photoTooLarge],
  ])("rejects %s without calling the backend", async (_case, data, message) => {
    await expect(createIncident(data)).resolves.toBe(message);
    expect(request).not.toHaveBeenCalled();
  });

  test("accepts a photo of exactly 10 MB", async () => {
    await expect(createIncident(form({ photos: [photo("image/jpeg", MAX_PHOTO_BYTES)] }))).resolves.toBeNull();
  });

  test("shows the backend's error and keeps the page as it is", async () => {
    request.mockRejectedValue(new BackendError("PAYLOAD_TOO_LARGE", "Maximum upload size exceeded", 413));

    await expect(createIncident(form())).resolves.toBe(messages.photoTooLarge);
    expect(refresh).not.toHaveBeenCalled();
  });

  test("hides unexpected errors behind a generic message", async () => {
    request.mockRejectedValue(new Error("socket hang up"));

    await expect(createIncident(form())).resolves.toBe(messages.saveFailed);
  });
});

describe("updateIncident", () => {
  test("sends the photos to delete and allows no new photos", async () => {
    const data = form({ photos: [] });
    data.append("deletePhotoIds", "p1");
    data.append("deletePhotoIds", "p2");

    await expect(updateIncident("a/b", data)).resolves.toBeNull();
    const { path, method, incident, photos } = await sent();
    expect([path, method]).toEqual(["/incidents/a%2Fb", "PATCH"]);
    expect(incident.deletePhotoIds).toEqual(["p1", "p2"]);
    expect(photos).toEqual([]);
    expect(refresh).toHaveBeenCalledOnce();
  });

  test("rejects a forged id", async () => {
    await expect(updateIncident(42 as unknown as string, form())).resolves.toBe(messages.invalidRequest);
    expect(request).not.toHaveBeenCalled();
  });
});

describe("deleteIncident", () => {
  test("leaves refreshing to the caller, which closes the deleted detail first", async () => {
    await expect(deleteIncident("1")).resolves.toBeNull();
    expect(request).toHaveBeenCalledWith("/incidents/1", { method: "DELETE" });
    expect(refresh).not.toHaveBeenCalled();
  });
});

describe("setIncidentResolution", () => {
  test("sends the new status as JSON", async () => {
    await expect(setIncidentResolution("1", true)).resolves.toBeNull();
    const [path, init] = request.mock.calls[0];
    expect(path).toBe("/incidents/1/resolution");
    expect(init).toMatchObject({ method: "PATCH", body: JSON.stringify({ resolved: true }) });
    expect(refresh).toHaveBeenCalledOnce();
  });

  test("rejects forged arguments", async () => {
    await expect(setIncidentResolution("1", "yes" as unknown as boolean)).resolves.toBe(messages.invalidRequest);
    expect(request).not.toHaveBeenCalled();
  });
});
