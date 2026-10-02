import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { apiClient } from "../../shared/lib/api-client";
import { AsyncDemoPage } from "./AsyncDemoPage";
import { PublicPlansPage } from "./PublicPlansPage";

describe("public funnel", () => {
  beforeEach(() => vi.restoreAllMocks());

  it("lets a visitor follow the self-guided demo without submitting personal data", () => {
    render(<MemoryRouter><AsyncDemoPage /></MemoryRouter>);
    expect(screen.getByRole("heading", { name: "Uma agenda clara para a equipe" })).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: /Próxima etapa/ }));
    expect(screen.getByRole("heading", { name: "Contexto do paciente organizado" })).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: /Próxima etapa/ }));
    expect(screen.getByRole("link", { name: /Ver disponibilidade dos planos/ })).toHaveAttribute("href", "/planos");
    expect(screen.queryByRole("textbox")).not.toBeInTheDocument();
  });

  it("does not show prices or a checkout when the catalog is gated", async () => {
    vi.spyOn(apiClient, "request").mockResolvedValue({ available: false, plans: [] });
    render(<MemoryRouter><PublicPlansPage /></MemoryRouter>);
    expect(await screen.findByText(/planos e preços ainda não foram publicados/i)).toBeInTheDocument();
    expect(screen.queryByText(/R\$/)).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: /contratar|assinar/i })).not.toBeInTheDocument();
  });
});
