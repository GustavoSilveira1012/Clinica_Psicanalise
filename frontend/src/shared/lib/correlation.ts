// Correlation id shown on error surfaces (404/403/500) so a person can quote it
// to support. It is generated in the browser and carries no personal, clinical or
// financial data — it only links what the user saw to a later diagnostic step.
export function newCorrelationId(): string {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }
  return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
}
