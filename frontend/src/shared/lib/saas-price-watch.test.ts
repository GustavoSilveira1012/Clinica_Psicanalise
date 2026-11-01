import { describe, expect, it, vi } from "vitest";
import { loadSaasPriceFeed, parseSaasPriceFeed } from "./saas-price-watch";

const sample = {
  updated: "2026-10-19",
  license: "CC BY 4.0",
  vendors: [{
    vendor: "example.com",
    name: "Example",
    category: "scheduling",
    pricing_url: "https://example.com/pricing",
    checked: "2026-10-19",
    prices: [
      { amount: 20, unit: "mo", period: "per month", kind: "plan", plan: "Start", quote: "$20/mo" },
      { amount: 1, unit: "mo", period: "first month", kind: "trial", quote: "$1 trial" },
    ],
  }],
};

describe("SaaS Price Watch", () => {
  it("accepts only HTTPS vendor links and paid plan references", () => {
    const result = parseSaasPriceFeed({ ...sample, vendors: [
      ...sample.vendors,
      { ...sample.vendors[0], vendor: "unsafe.example", pricing_url: "javascript:alert(1)" },
    ] });
    expect(result.vendors).toHaveLength(1);
    expect(result.vendors[0].prices).toEqual([expect.objectContaining({ amount: 20, plan: "Start" })]);
  });

  it("rejects an unexpected license or malformed feed", () => {
    expect(() => parseSaasPriceFeed({ ...sample, license: "unknown" })).toThrow();
    expect(() => parseSaasPriceFeed({ ...sample, updated: "not-a-date" })).toThrow();
  });

  it("requests only the public feed without sending app credentials or a referrer", async () => {
    const fetchMock = vi.fn().mockResolvedValue({ ok: true, text: async () => JSON.stringify(sample) });
    vi.stubGlobal("fetch", fetchMock);
    try {
      await expect(loadSaasPriceFeed()).resolves.toMatchObject({ updated: "2026-10-19" });
      expect(fetchMock).toHaveBeenCalledWith("https://saaspricealerts.com/api/vendors.json", { signal: undefined, referrerPolicy: "no-referrer" });
    } finally {
      vi.unstubAllGlobals();
    }
  });
});
