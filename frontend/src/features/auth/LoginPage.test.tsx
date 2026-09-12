import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { AppProviders } from "../../app/providers";
import { AppRouter } from "../../app/router";

describe("LoginPage", () => {
  it("moves a valid demo login to the MFA step", async () => {
    const user = userEvent.setup();
    render(<MemoryRouter initialEntries={["/login"]}><AppProviders><AppRouter /></AppProviders></MemoryRouter>);
    await user.click(screen.getByRole("button", { name: /continuar/i }));
    expect(await screen.findByText("Segundo fator")).toBeInTheDocument();
    expect(screen.getByLabelText("Código MFA")).toBeInTheDocument();
  });
});
