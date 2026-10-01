import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { ClinicalRecordPage } from "./ClinicalRecordPage";
import userEvent from "@testing-library/user-event";

const api = vi.hoisted(() => ({
  getPatient: vi.fn(),
  getRecords: vi.fn(),
  getRecordContent: vi.fn(),
  getRevisions: vi.fn(),
  createRecord: vi.fn(),
  saveRecord: vi.fn(),
  createAddendum: vi.fn(),
}));

vi.mock("../../shared/lib/app-api", () => ({ dataSource: api }));
vi.mock("../../shared/providers/ToastProvider", () => ({ useToast: () => vi.fn() }));

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: Infinity } },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
        initialEntries={["/patients/patient-1/clinical-record"]}
      >
        <Routes>
          <Route path="/patients/:patientId/clinical-record" element={<ClinicalRecordPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("ClinicalRecordPage fail-closed loading", () => {
  beforeEach(() => {
    api.getPatient.mockResolvedValue({ id: "patient-1", name: "Paciente sintético" });
    api.getRecords.mockResolvedValue([{
      id: "record-1",
      patientId: "patient-1",
      title: "Sessão",
      state: "DRAFT",
      updatedAt: "2026-09-28T12:00:00Z",
      author: "Profissional sintético",
      contentPreview: "",
      revisionCount: 1,
      addendumCount: 0,
    }]);
    api.getRecordContent.mockResolvedValue("Conteúdo clínico sintético");
    api.getRevisions.mockResolvedValue([]);
  });

  afterEach(() => vi.clearAllMocks());

  it("counts persisted revisions separately from addenda without fabricating historical states", async () => {
    api.getRevisions.mockResolvedValue([
      { id: "addendum-1", kind: "ADDENDUM", label: "Adendo", createdAt: "2026-09-29T12:00:00Z", author: "Synthetic professional", reason: "Complemento" },
      { id: "revision-2", kind: "REVISION", label: "Revisão 2", createdAt: "2026-09-28T12:01:00Z", author: "Synthetic professional", reason: "Revisão clínica" },
      { id: "revision-1", kind: "REVISION", label: "Revisão 1", createdAt: "2026-09-28T12:00:00Z", author: "Synthetic professional", reason: "Revisão clínica" },
    ]);
    renderPage();
    expect(await screen.findByText("2 versões")).toBeInTheDocument();
    expect(screen.queryByText("Finalizado")).not.toBeInTheDocument();
  });

  it("does not offer editing or finalization when record content fails to load", async () => {
    api.getRecordContent.mockRejectedValue(new Error("synthetic transport failure"));

    renderPage();

    expect(await screen.findByText("Não foi possível carregar o conteúdo clínico")).toBeInTheDocument();
    expect(screen.getByText("O registro permanece protegido. Tente novamente antes de editar ou finalizar.")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Finalizar registro" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Salvar rascunho" })).not.toBeInTheDocument();
  });

  it("blocks access to the editor when the audit history fails to load", async () => {
    api.getRevisions.mockRejectedValue(new Error("synthetic history failure"));

    renderPage();

    expect(await screen.findByText("Não foi possível carregar o histórico")).toBeInTheDocument();
    expect(screen.getByText("Edição e finalização ficam bloqueadas até que o histórico auditável esteja disponível.")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Finalizar registro" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Salvar rascunho" })).not.toBeInTheDocument();
  });

  it("shows an unavailable state when the patient request fails instead of waiting on a disabled query", async () => {
    api.getPatient.mockRejectedValue(new Error("synthetic authorization failure"));
    renderPage();
    expect(await screen.findByText("Prontuário indisponível")).toBeInTheDocument();
    expect(api.getRecords).not.toHaveBeenCalled();
  });

  it("makes a finalized record read-only and enables only addenda", async () => {
    api.getRecords.mockResolvedValue([{ id: "record-1", patientId: "patient-1", state: "FINALIZED", updatedAt: "2026-09-28T12:00:00Z", revisionCount: 1 }]);
    renderPage();
    expect(await screen.findByLabelText("Anotação da sessão")).toHaveAttribute("readonly");
    expect(screen.getByRole("button", { name: "Salvar rascunho" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Finalizar registro" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Novo adendo" })).toBeEnabled();
  });

  it("associates the addendum label with its editable field while the record remains immutable", async () => {
    api.getRecords.mockResolvedValue([{ id: "record-1", patientId: "patient-1", state: "FINALIZED", updatedAt: "2026-09-28T12:00:00Z", revisionCount: 1 }]);
    api.createAddendum.mockResolvedValue({ id: "addendum-1" });
    const user = userEvent.setup();
    renderPage();
    const note = await screen.findByLabelText("Anotação da sessão");
    await user.click(screen.getByRole("button", { name: "Novo adendo" }));
    const addendum = screen.getByLabelText("Conteúdo do adendo");
    expect(addendum).not.toBe(note);
    expect(addendum).not.toHaveAttribute("readonly");
    await user.type(addendum, "Complemento clínico estritamente sintético.");
    await user.click(screen.getByRole("button", { name: "Adicionar adendo" }));
    expect(api.createAddendum).toHaveBeenCalledWith("record-1", "Complemento clínico estritamente sintético.");
    expect(api.saveRecord).not.toHaveBeenCalled();
    expect(note).toHaveValue("Conteúdo clínico sintético");
  });
});
