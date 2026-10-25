import "server-only";
import { BACKEND_TIMEOUT, BACKEND_UNREACHABLE, BackendError } from "@/lib/backend";
import { messages } from "./messages";

const messagesByCode: Record<string, string> = {
  INCIDENT_NOT_FOUND: messages.notFound,
  RESOURCE_NOT_FOUND: messages.notFound,
  INVALID_INCIDENT: messages.invalidIncident,
  INVALID_REQUEST: messages.invalidRequest,
  PAYLOAD_TOO_LARGE: messages.photoTooLarge,
  INTERNAL_ERROR: messages.serverError,
  [BACKEND_UNREACHABLE]: messages.unreachable,
  [BACKEND_TIMEOUT]: messages.timeout,
};

/** The message for a failed backend request; unknown codes fall back to the backend's own detail. */
export function errorMessage(error: unknown, fallback: string) {
  if (!(error instanceof BackendError)) return fallback;
  return (error.code && messagesByCode[error.code]) || error.message;
}
