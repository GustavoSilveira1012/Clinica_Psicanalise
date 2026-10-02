import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { Input, Select, Textarea } from "./ui";

describe("form field associations", () => {
  it("assigns unique IDs even when two forms share field names", () => {
    render(<>
      <Input name="value" label="First input" error="First error" />
      <Input name="value" label="Second input" hint="Second hint" />
      <Select name="value" label="First select"><option>One</option></Select>
      <Select name="value" label="Second select"><option>Two</option></Select>
      <Textarea name="content" label="Finalized note" readOnly />
      <Textarea name="content" label="Editable addendum" />
    </>);
    const labels = ["First input", "Second input", "First select", "Second select", "Finalized note", "Editable addendum"];
    const fields = labels.map((label) => screen.getByLabelText(label, { exact: false }));
    expect(new Set(fields.map((field) => field.id)).size).toBe(6);
    expect(fields[4]).toHaveAttribute("readonly");
    expect(fields[5]).not.toHaveAttribute("readonly");
    expect(document.getElementById(fields[0].getAttribute("aria-describedby")!)).toHaveTextContent("First error");
    expect(document.getElementById(fields[1].getAttribute("aria-describedby")!)).toHaveTextContent("Second hint");
  });

  it("preserves an explicit ID and its label association", () => {
    render(<Textarea id="explicit-synthetic-note" name="content" label="Explicit note" />);
    expect(screen.getByLabelText("Explicit note")).toHaveAttribute("id", "explicit-synthetic-note");
  });
});
