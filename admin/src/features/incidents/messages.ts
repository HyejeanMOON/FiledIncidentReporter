/** User-facing copy for incidents, shared with the Android client. Safe to import from client components. */
export const messages = {
  notFound: "This report no longer exists on the server. Refresh the list.",
  invalidIncident: "The report does not meet the server requirements. Check its fields and photos, then try again.",
  invalidRequest: "The server could not understand this request. Check that the app and server versions match.",
  photoTooLarge: "A photo is too large to upload. Choose photos under 10 MB and try again.",
  unreadablePhoto: "Could not read a selected photo. Select it again and retry.",
  serverError: "The server could not complete the request. Please try again.",
  unreachable: "Cannot connect to the incident service. Make sure the backend is running.",
  timeout: "The incident service did not respond in time. Please try again.",
  loadFailed: "Unable to load data",
  saveFailed: "Something went wrong. Please try again.",
  updateFailed: "Update failed. Please try again.",
};
