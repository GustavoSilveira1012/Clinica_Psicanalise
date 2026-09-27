import { describe, expect, it } from "vitest";
import { clinicalDataAccessGranted, pilotUnavailableRoutes } from "./pilot-scope";

describe("clinical-only pilot route scope", () => {
  it("marks external billing and non-pilot modules unavailable", () => {
    expect(pilotUnavailableRoutes.has("/billing")).toBe(true);
    expect(pilotUnavailableRoutes.has("/fiscal")).toBe(true);
    expect(pilotUnavailableRoutes.has("/notifications")).toBe(true);
    expect(pilotUnavailableRoutes.has("/packages")).toBe(true);
    expect(pilotUnavailableRoutes.has("/subscriptions")).toBe(true);
  });
});

describe("clinical data release gate", () => {
  it("requires both explicit flags in production", () => {
    expect(clinicalDataAccessGranted(true, "true", "false")).toBe(false);
    expect(clinicalDataAccessGranted(true, "false", "true")).toBe(false);
    expect(clinicalDataAccessGranted(true, "true", "true")).toBe(true);
    expect(clinicalDataAccessGranted(true, undefined, undefined)).toBe(false);
  });

  it("keeps local development behavior independent of production approval", () => {
    expect(clinicalDataAccessGranted(false, undefined, undefined)).toBe(true);
    expect(clinicalDataAccessGranted(false, "false", "true")).toBe(false);
  });
});
