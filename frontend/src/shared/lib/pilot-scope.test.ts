import { describe, expect, it } from "vitest";
import { pilotUnavailableRoutes } from "./pilot-scope";

describe("clinical-only pilot route scope", () => {
  it("marks external billing and non-pilot modules unavailable", () => {
    expect(pilotUnavailableRoutes.has("/billing")).toBe(true);
    expect(pilotUnavailableRoutes.has("/fiscal")).toBe(true);
    expect(pilotUnavailableRoutes.has("/notifications")).toBe(true);
    expect(pilotUnavailableRoutes.has("/packages")).toBe(true);
    expect(pilotUnavailableRoutes.has("/subscriptions")).toBe(true);
  });
});
