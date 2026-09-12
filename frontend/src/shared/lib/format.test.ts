import { describe, expect, it } from "vitest";
import { maskContact } from "./format";

describe("maskContact", () => {
  it("minimizes e-mail and phone values for operational views", () => {
    expect(maskContact("marina.duarte@example.com")).toBe("ma•••@example.com");
    expect(maskContact("+55 11 99876-2231")).toContain("••••");
  });
});
