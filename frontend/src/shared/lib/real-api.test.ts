import { afterEach, describe, expect, it, vi } from "vitest";
import { realApi } from "./real-api";
import { apiClient } from "./api-client";

describe("realApi clinical records", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    apiClient.clearSession();
  });

  it("creates a draft under the selected patient's medical-record endpoint", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({
      id: "20000000-0000-0000-0000-000000000001",
      authorPsychoanalystId: 7,
      authorName: "Profissional Sintética",
      status: "DRAFT",
      createdAt: "2026-09-28T12:00:00Z",
    }), { status: 201, headers: { "Content-Type": "application/json" } }));
    vi.stubGlobal("fetch", fetchMock);

    const record = await realApi.createRecord("42", "Conteúdo sintético para teste do prontuário.");

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toContain("/patients/42/medical-records");
    expect(init.method).toBe("POST");
    expect(JSON.parse(String(init.body))).toEqual({ content: "Conteúdo sintético para teste do prontuário." });
    expect(record).toMatchObject({ patientId: "42", state: "DRAFT", author: "Profissional Sintética" });
  });

  it("registers and allocates a payment with the same idempotency key", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({
      payment: {
        id: "30000000-0000-0000-0000-000000000001",
        patientId: 42,
        clinicId: 7,
        amount: 125,
        allocatedAmount: 125,
        availableAmount: 0,
        currency: "BRL",
        paymentMethod: "CREDIT_CARD",
        status: "CONFIRMED",
        receivedAt: "2026-09-28T12:00:00Z",
        createdAt: "2026-09-28T12:00:00Z",
      },
      allocation: {
        id: "40000000-0000-0000-0000-000000000001",
        paymentId: "30000000-0000-0000-0000-000000000001",
        receivableId: "50000000-0000-0000-0000-000000000001",
        amount: 125,
        createdAt: "2026-09-28T12:00:00Z",
      },
    }), { status: 201, headers: { "Content-Type": "application/json" } }));
    vi.stubGlobal("fetch", fetchMock);

    const payment = await realApi.collectPayment({
      idempotencyKey: "payment-collection-attempt",
      patientId: "42",
      patientName: "Paciente sintético",
      receivableId: "50000000-0000-0000-0000-000000000001",
      amount: 125,
      method: "CARD",
    });

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toContain("/api/v1/payments/collect");
    expect(init.method).toBe("POST");
    expect(new Headers(init.headers).get("Idempotency-Key")).toBe("payment-collection-attempt");
    expect(JSON.parse(String(init.body))).toEqual({
      patientId: 42,
      receivableId: "50000000-0000-0000-0000-000000000001",
      amount: 125,
      paymentMethod: "CREDIT_CARD",
      description: "Baixa de recebível",
    });
    expect(payment).toMatchObject({ patientName: "Paciente sintético", amount: 125, method: "CARD", status: "CONFIRMED" });
  });

  it("preserves the selected Sao Paulo appointment time when posting UTC instants", async () => {
    apiClient.setProfessionalId(7);
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ id: 1, patientId: 42, patientName: "Synthetic patient", psychoanalystName: "Synthetic professional", scheduledStart: "2026-09-29T09:00:00", scheduledEnd: "2026-09-29T09:50:00", appointmentType: "ONLINE", status: "SCHEDULED" }), { status: 201 }));
    vi.stubGlobal("fetch", fetchMock);
    await realApi.createAppointment({ patientId: "42", patientName: "Synthetic patient", professionalName: "Synthetic professional", startAt: "2026-09-29T12:00:00.000Z", durationMinutes: 50, type: "ONLINE" });
    expect(JSON.parse(String(fetchMock.mock.calls[0][1].body))).toMatchObject({ scheduledStart: "2026-09-29T09:00:00", scheduledEnd: "2026-09-29T09:50:00" });
  });

  it("preserves block start and end on the selected local date across UTC midnight", async () => {
    apiClient.setProfessionalId(7);
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ id: 1, date: "2026-09-29", startTime: "20:30", endTime: "21:30" }), { status: 201 }));
    vi.stubGlobal("fetch", fetchMock);
    await realApi.createCalendarBlock({ startAt: "2026-09-29T23:30:00.000Z", durationMinutes: 60, professionalName: "Synthetic professional", reason: "Synthetic block" });
    expect(JSON.parse(String(fetchMock.mock.calls[0][1].body))).toMatchObject({ date: "2026-09-29", startTime: "20:30", endTime: "21:30" });
  });

  it("includes addendum metadata in the history without fetching clinical content", async () => {
    const fetchMock = vi.fn().mockImplementation((url: string) => Promise.resolve(new Response(JSON.stringify(url.endsWith("/addendums")
      ? [{ id: "addendum-1", authorName: "Synthetic professional", reason: "COMPLEMENT", createdAt: "2026-09-29T12:00:00Z" }]
      : [{ id: "revision-1", revisionNumber: 1, authorName: "Synthetic professional", createdAt: "2026-09-28T12:00:00Z" }]), { status: 200 })));
    vi.stubGlobal("fetch", fetchMock);
    const history = await realApi.getRevisions("record-1");
    expect(history.map((item) => item.label)).toEqual(["Adendo", "Revisão 1"]);
    expect(history.map((item) => item.kind)).toEqual(["ADDENDUM", "REVISION"]);
    expect(history.every((item) => item.state === undefined)).toBe(true);
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(fetchMock.mock.calls.map(([url]) => url)).toEqual(expect.arrayContaining([expect.stringContaining("/record-1/revisions"), expect.stringContaining("/record-1/addendums")]));
  });

  it("does not invent revision or addendum counts absent from summary metadata", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(JSON.stringify([
      { id: "record-1", status: "FINALIZED", authorName: "Synthetic professional", createdAt: "2026-09-29T12:00:00Z" },
    ]), { status: 200 })));
    const [record] = await realApi.getRecords("42");
    expect(record.revisionCount).toBeUndefined();
    expect(record.addendumCount).toBeUndefined();
  });
});
