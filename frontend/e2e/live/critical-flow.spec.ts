import { test, expect, type Page } from "@playwright/test";
import { createHmac } from "node:crypto";
import { execFileSync } from "node:child_process";
import { readFileSync, writeFileSync } from "node:fs";
import { resolve } from "node:path";

const apiUrl = "http://127.0.0.1:18080";
const organizationId = "10000000-0000-0000-0000-000000009001";
const syntheticTotpSecrets: Record<string, string> = {};

test.beforeAll(async ({ request }) => {
  test.setTimeout(150_000);
  await expect.poll(async () => {
    try { return (await request.get(`${apiUrl}/actuator/health/readiness`, { timeout: 2_000 })).status(); }
    catch { return 0; }
  }, { timeout: 120_000, intervals: [1_000,2_000,3_000] }).toBe(200);
});

function totp(secret: string) {
  const alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
  const bits = [...secret.replace(/=+$/, "")].map((char) => alphabet.indexOf(char).toString(2).padStart(5, "0")).join("");
  const key = Buffer.from(bits.match(/.{8}/g)!.map((byte) => parseInt(byte, 2)));
  const counter = Buffer.alloc(8);
  counter.writeBigUInt64BE(BigInt(Math.floor(Date.now() / 30_000)));
  const digest = createHmac("sha1", key).update(counter).digest();
  const offset = digest[digest.length - 1] & 15;
  return String((digest.readUInt32BE(offset) & 0x7fffffff) % 1_000_000).padStart(6, "0");
}

async function login(page: Page, email: string) {
  await page.goto("/login");
  await page.getByLabel("E-mail profissional").fill(email);
  await page.getByLabel("Senha", { exact: true }).fill("Incorrect-Synthetic-Password!");
  const rejected = page.waitForResponse((r) => r.url().endsWith("/auth/login") && r.request().method() === "POST");
  await page.getByRole("button", { name: "Continuar", exact: true }).click();
  expect((await rejected).status()).toBe(401);
  await page.getByLabel("Senha", { exact: true }).fill("Synthetic-E2E-2026!");
  await page.getByRole("button", { name: "Continuar", exact: true }).click();
  await expect(page.getByRole("heading", { name: /Proteja sua conta|Confirme sua identidade/ })).toBeVisible();
  const enrollment = await page.getByRole("heading", { name: "Proteja sua conta" }).isVisible();
  if (enrollment) {
    const secret = page.locator("code");
    await expect(secret).toHaveText(/^[A-Z2-7]{32}$/);
    syntheticTotpSecrets[email] = (await secret.textContent())!;
  } else {
    const fixture = JSON.parse(readFileSync(resolve("../ops/secrets/local-e2e/smoke.json"), "utf8"));
    syntheticTotpSecrets[email] = fixture.syntheticTotpSecrets[email];
    expect(syntheticTotpSecrets[email]).toMatch(/^[A-Z2-7]{32}$/);
    // Never replay a TOTP consumed by an earlier run. Read only the synthetic counter.
    const userId = email.startsWith("clinical") ? 9001 : 9002;
    const lastStep = Number(execFileSync("docker", ["exec", "psicogest-local-e2e-postgres", "psql", "-At", "-U", "postgres", "-d", "psicogest_e2e", "-c", `SELECT last_accepted_time_step FROM mfa_methods WHERE user_id=${userId} AND status='ACTIVE'`]).toString().trim());
    if (Math.floor(Date.now()/30_000) <= lastStep) await page.waitForTimeout((lastStep+1)*30_000-Date.now()+250);
  }
  await page.getByLabel("Código MFA").fill(totp(syntheticTotpSecrets[email]));
  const confirmed = page.waitForResponse((r) => r.url().endsWith(enrollment ? "/auth/mfa/totp/confirm" : "/auth/mfa/totp/verify") && r.request().method() === "POST");
  const profile = page.waitForResponse((r) => r.url().endsWith("/auth/me"));
  await page.getByRole("button", { name: enrollment ? "Ativar proteção" : "Entrar com segurança" }).click();
  const confirmedResponse = await confirmed;
  expect(confirmedResponse.status()).toBe(200);
  const authentication = await confirmedResponse.json();
  const token = (enrollment ? authentication.authentication.accessToken : authentication.accessToken) as string;
  const identity = await (await profile).json();
  expect(identity.organizations[0].id).toBe(organizationId);
  if (email.startsWith("clinical")) expect(identity.psychoanalystId).toBe(9001);
  await expect(page).toHaveURL(/\/dashboard$/);
  return { Authorization: `Bearer ${token}`, "X-Organization-Id": organizationId };
}

async function checkWidths(page: Page, label: string) {
  for (const width of [320, 375, 768, 1024, 1440]) {
    await page.setViewportSize({ width, height: 900 });
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth), `${label} width ${width}`).toBe(true);
  }
}

test("patient, appointment, immutable record and payment work with real API and RLS", async ({ page, browser }, testInfo) => {
  test.setTimeout(240_000);
  const pageErrors: string[] = [];
  page.on("pageerror", (error) => pageErrors.push(error.message));
  const clinicalHeaders = await login(page, "clinical-e2e@example.invalid");
  const foreignPatient = await page.request.get(`${apiUrl}/patients/9003`, { headers: clinicalHeaders });
  expect([403,404]).toContain(foreignPatient.status());
  await page.getByRole("link", { name: "Pacientes", exact: true }).first().click();
  await page.getByRole("button", { name: "Novo paciente", exact: true }).click();
  const name = `Paciente Sintetico E2E ${Date.now()}`;
  await page.getByLabel("Nome completo").fill(name);
  await page.getByLabel("E-mail", { exact: true }).fill(`patient-${Date.now()}@example.invalid`);
  await page.getByLabel("Telefone").fill("11900000000");
  const created = page.waitForResponse((r) => r.url().endsWith("/patients") && r.request().method() === "POST");
  await page.getByRole("button", { name: "Criar paciente", exact: true }).click();
  const response = await created;
  expect(response.status(), await response.text()).toBe(201);
  const patient = await response.json();
  await expect(page.getByRole("link", { name, exact: true })).toBeVisible();
  await checkWidths(page, "patients");
  await page.screenshot({ path: testInfo.outputPath("patients-real-api.png"), fullPage: true });
  expect(patient.id).toBeGreaterThan(0);

  await page.getByRole("link", { name, exact: true }).click();
  await page.getByRole("button", { name: "Agendar consulta" }).click();
  const tomorrow = new Date(); tomorrow.setDate(tomorrow.getDate() + 1);
  const date = new Intl.DateTimeFormat("sv-SE", { timeZone: "America/Sao_Paulo" }).format(tomorrow);
  const time = `09:${String(Math.floor(Date.now() / 1000) % 60).padStart(2, "0")}`;
  await page.getByLabel("Data", { exact: true }).fill(date);
  await page.getByLabel("Horário", { exact: true }).fill(time);
  const appointmentCreated = page.waitForResponse((r) => r.url().endsWith("/appointments") && r.request().method() === "POST");
  await page.getByRole("button", { name: "Criar consulta", exact: true }).click();
  const appointmentResponse = await appointmentCreated;
  expect(appointmentResponse.status()).toBe(201);
  const appointment = await appointmentResponse.json();
  expect(appointment.scheduledStart).toBe(`${date}T${time}:00`);
  const appointmentRow = page.locator("div.grid").filter({ has: page.getByText(name, { exact: true }) }).filter({ has: page.getByRole("button", { name: "Detalhes", exact: true }) });
  await appointmentRow.getByRole("button", { name: "Detalhes", exact: true }).click();
  await page.getByRole("button", { name: "Confirmar", exact: true }).click();
  await expect(page.getByText("Consulta marcada como confirmada.")).toBeVisible();

  // Simulate elapsed consultation time in the disposable DB. No production clock or data is changed.
  const elapsedSql = `DO $$ BEGIN IF current_database() <> 'psicogest_e2e' THEN RAISE EXCEPTION 'Synthetic DB required'; END IF; END $$;
    UPDATE appointments SET scheduled_start=timezone('America/Sao_Paulo',now())-interval '51 minutes', scheduled_end=timezone('America/Sao_Paulo',now())-interval '1 minute'
    WHERE id=${Number(appointment.id)} AND psychoanalyst_id=9001 AND organization_id='${organizationId}' AND status='CONFIRMED';`;
  execFileSync("docker", ["exec", "-i", "psicogest-local-e2e-postgres", "psql", "-v", "ON_ERROR_STOP=1", "-U", "postgres", "-d", "psicogest_e2e"], { input: elapsedSql });
  await appointmentRow.getByRole("button", { name: "Detalhes", exact: true }).click();
  const completed = page.waitForResponse((r) => r.url().endsWith(`/${appointment.id}/complete`));
  await page.getByRole("button", { name: "Marcar atendida", exact: true }).click();
  expect((await completed).status()).toBe(200);
  await checkWidths(page, "agenda");
  await page.screenshot({ path: testInfo.outputPath("agenda-real-api.png"), fullPage: true });

  await page.goto(`/patients/${patient.id}/clinical-record`);
  const content = "Registro estritamente sintetico E2E para validar criptografia, persistencia e imutabilidade.";
  await page.getByLabel("Anotação da sessão").fill(content);
  const draftCreated = page.waitForResponse((r) => r.url().endsWith("/medical-records") && r.request().method() === "POST");
  await page.getByRole("button", { name: "Salvar rascunho", exact: true }).click();
  const draftResponse = await draftCreated;
  expect(draftResponse.status(), await draftResponse.text()).toBe(201);
  const record = await draftResponse.json();
  const encryptedCheck = execFileSync("docker", ["exec", "-i", "psicogest-local-e2e-postgres", "psql", "-v", "ON_ERROR_STOP=1", "-At", "-U", "postgres", "-d", "psicogest_e2e"], {
    input: `SELECT octet_length(content_iv)=12 AND octet_length(encrypted_dek)>32 AND strpos(encode(encrypted_content,'hex'),'${Buffer.from(content).toString("hex")}')=0 FROM medical_records WHERE id='${record.id}';`,
  }).toString().trim();
  expect(encryptedCheck).toBe("t");
  await expect(page.getByRole("heading", { name: "Registro clínico", exact: true })).toBeVisible();
  await expect(page.getByLabel("Anotação da sessão")).toHaveValue(content);
  await page.getByRole("button", { name: "Finalizar registro", exact: true }).click();
  const finalized = page.waitForResponse((r) => r.url().endsWith(`/${record.id}/finalize`));
  await page.getByRole("button", { name: "Finalizar registro", exact: true }).last().click();
  expect((await finalized).status()).toBe(200);
  await expect(page.getByLabel("Anotação da sessão")).toHaveAttribute("readonly");
  const rewrite = await page.request.put(`${apiUrl}/medical-records/${record.id}`, { headers: clinicalHeaders, data: { content: "Synthetic forbidden overwrite of finalized record" } });
  expect([403, 409]).toContain(rewrite.status());
  await page.getByRole("button", { name: "Novo adendo" }).click();
  await page.getByLabel("Conteúdo do adendo").fill("Informacao complementar estritamente sintetica para o teste E2E.");
  const addendumCreated = page.waitForResponse((r) => r.url().endsWith(`/${record.id}/addendums`) && r.request().method() === "POST");
  await page.getByRole("button", { name: "Adicionar adendo" }).click();
  const addendumResponse = await addendumCreated;
  expect(addendumResponse.status(), await addendumResponse.text()).toBe(201);
  await expect(page.getByText("Adendo", { exact: true })).toBeVisible();
  await expect(page.getByText("2 versões", { exact: true })).toBeVisible();
  await checkWidths(page, "clinical");
  await page.screenshot({ path: testInfo.outputPath("record-finalized-real-api.png"), fullPage: true });

  const finance = await browser.newPage();
  finance.on("pageerror", (error) => pageErrors.push(error.message));
  const financeHeaders = await login(finance, "finance-e2e@example.invalid");
  await finance.getByRole("link", { name: "Financeiro", exact: true }).first().click();
  await finance.getByRole("button", { name: "Novo lançamento", exact: true }).click();
  await finance.getByLabel("Paciente ou responsável").fill(name);
  await finance.getByLabel("Descrição", { exact: true }).fill("Sessao sintetica E2E");
  await finance.getByLabel("Valor", { exact: true }).fill("125.50");
  const receivableCreated = finance.waitForResponse((r) => r.url().endsWith("/api/v1/receivables") && r.request().method() === "POST");
  await finance.getByRole("button", { name: "Criar lançamento" }).click();
  const receivableResponse = await receivableCreated;
  expect(receivableResponse.status(), await receivableResponse.text()).toBe(201);
  const receivable = await receivableResponse.json();
  await finance.locator("div.grid").filter({ has: finance.getByText(name, { exact: true }) }).filter({ has: finance.getByRole("button", { name: "Receber", exact: true }) }).getByRole("button", { name: "Receber", exact: true }).click();
  const collected = finance.waitForResponse((r) => r.url().endsWith("/payments/collect") && r.request().method() === "POST");
  await finance.getByRole("button", { name: "Confirmar pagamento" }).click();
  const collectedResponse = await collected;
  expect(collectedResponse.status(), await collectedResponse.text()).toBe(201);
  await expect(finance.getByText("Pagamento confirmado e alocado à cobrança.")).toBeVisible();
  const payment = await collectedResponse.json();
  expect(payment.payment.allocatedAmount).toBe(125.50);
  expect(payment.payment.availableAmount).toBe(0);
  const originalRequest = collectedResponse.request();
  const requestHeaders = await originalRequest.allHeaders();
  const replay = await finance.request.post(`${apiUrl}/api/v1/payments/collect`, { headers: { ...financeHeaders, "Idempotency-Key": requestHeaders["idempotency-key"] }, data: originalRequest.postDataJSON() });
  expect(replay.status(), await replay.text()).toBe(201);
  expect((await replay.json()).allocation.id).toBe(payment.allocation.id);
  const balance = await finance.request.get(`${apiUrl}/api/v1/receivables/${receivable.id}`, { headers: financeHeaders });
  expect((await balance.json()).outstandingAmount).toBe(0);

  const newReceivable = async (amount: number) => {
    const response = await finance.request.post(`${apiUrl}/api/v1/receivables`, { headers: financeHeaders,
      data: { patientId: patient.id, description: "Concorrencia sintetica E2E", grossAmount: amount, discountAmount: 0, dueDate: date } });
    expect(response.status(), await response.text()).toBe(201);
    return (await response.json()).id as string;
  };
  const concurrentReceivable = await newReceivable(37.75);
  const sameKey = `synthetic-concurrent-${Date.now()}`;
  const concurrentRequest = { patientId: patient.id, receivableId: concurrentReceivable, amount: 37.75, paymentMethod: "PIX", description: "Concorrencia sintetica E2E" };
  const sameKeyResponses = await Promise.all([0, 1].map(() => finance.request.post(`${apiUrl}/api/v1/payments/collect`, {
    headers: { ...financeHeaders, "Idempotency-Key": sameKey }, data: concurrentRequest,
  })));
  expect(sameKeyResponses.map((response) => response.status())).toEqual([201, 201]);
  const sameKeyPayments = await Promise.all(sameKeyResponses.map((response) => response.json()));
  expect(sameKeyPayments[0].payment.id).toBe(sameKeyPayments[1].payment.id);
  expect(sameKeyPayments[0].allocation.id).toBe(sameKeyPayments[1].allocation.id);
  const differentTarget = await finance.request.post(`${apiUrl}/api/v1/payments/collect`, {
    headers: { ...financeHeaders, "Idempotency-Key": sameKey }, data: { ...concurrentRequest, receivableId: receivable.id },
  });
  expect(differentTarget.status()).toBe(409);

  const conflictingReceivable = await newReceivable(62.25);
  const beforePayments = await (await finance.request.get(`${apiUrl}/api/v1/payments`, { headers: financeHeaders })).json() as Array<{ id: string }>;
  const competing = await Promise.all([0, 1].map((index) => finance.request.post(`${apiUrl}/api/v1/payments/collect`, {
    headers: { ...financeHeaders, "Idempotency-Key": `synthetic-distinct-${Date.now()}-${index}` },
    data: { ...concurrentRequest, receivableId: conflictingReceivable, amount: 62.25 },
  })));
  expect(competing.filter((response) => response.status() === 201)).toHaveLength(1);
  expect(competing.filter((response) => [409, 422].includes(response.status()))).toHaveLength(1);
  const afterPayments = await (await finance.request.get(`${apiUrl}/api/v1/payments`, { headers: financeHeaders })).json() as Array<{ id: string }>;
  expect(afterPayments.filter((payment) => !beforePayments.some((previous) => previous.id === payment.id))).toHaveLength(1);

  const partialReceivable = await newReceivable(120);
  for (const [index, amount] of [50,70].entries()) {
    const part = await finance.request.post(`${apiUrl}/api/v1/payments/collect`, {
      headers: { ...financeHeaders, "Idempotency-Key": `synthetic-partial-${Date.now()}-${index}` },
      data: { ...concurrentRequest, receivableId: partialReceivable, amount },
    });
    expect(part.status(), await part.text()).toBe(201);
    const partialBalance = await finance.request.get(`${apiUrl}/api/v1/receivables/${partialReceivable}`, { headers: financeHeaders });
    expect((await partialBalance.json()).outstandingAmount).toBe(index === 0 ? 70 : 0);
  }
  const overpayReceivable = await newReceivable(20);
  const beforeOverpay = await (await finance.request.get(`${apiUrl}/api/v1/payments`, { headers: financeHeaders })).json();
  const overpay = await finance.request.post(`${apiUrl}/api/v1/payments/collect`, {
    headers: { ...financeHeaders, "Idempotency-Key": `synthetic-overpayment-${Date.now()}` },
    data: { ...concurrentRequest, receivableId: overpayReceivable, amount: 21 },
  });
  expect([409,422]).toContain(overpay.status());
  const afterOverpay = await (await finance.request.get(`${apiUrl}/api/v1/payments`, { headers: financeHeaders })).json();
  expect(afterOverpay).toHaveLength(beforeOverpay.length);

  // Existing refund API: real persistence, replay, concurrency and balance reversal.
  const refundsUrl = `${apiUrl}/api/v1/payments/${payment.payment.id}/refunds`;
  for (const [index, amount] of [25.50,100].entries()) {
    const refundKey = `synthetic-refund-${Date.now()}-${index}`;
    const refundBody = { amount, reason: "CUSTOMER_REQUEST", allocations: [{ paymentAllocationId: payment.allocation.id, amount }] };
    const refunds = await Promise.all([0,1].map(() => finance.request.post(refundsUrl, {
      headers: { ...financeHeaders, "Idempotency-Key": refundKey }, data: refundBody,
    })));
    expect(refunds.map((result) => result.status())).toEqual([201,201]);
    const refund = await refunds[0].json();
    expect((await refunds[1].json()).id).toBe(refund.id);
    const mismatch = await finance.request.post(refundsUrl, { headers: { ...financeHeaders, "Idempotency-Key": refundKey }, data: { ...refundBody, reason: "PAYMENT_ERROR" } });
    expect(mismatch.status()).toBe(409);
    const confirmedRefund = await finance.request.post(`${refundsUrl}/${refund.id}/confirm`, { headers: financeHeaders });
    expect(confirmedRefund.status(), await confirmedRefund.text()).toBe(200);
    expect((await confirmedRefund.json()).status).toBe("CONFIRMED");
    const refundedBalance = await finance.request.get(`${apiUrl}/api/v1/receivables/${receivable.id}`, { headers: financeHeaders });
    expect((await refundedBalance.json()).outstandingAmount).toBe(index === 0 ? 25.50 : 125.50);
    const replayRefund = await finance.request.post(refundsUrl, { headers: { ...financeHeaders, "Idempotency-Key": refundKey }, data: refundBody });
    expect(replayRefund.status(), await replayRefund.text()).toBe(201);
    expect((await replayRefund.json()).id).toBe(refund.id);
  }
  const forbiddenClinical = await finance.request.get(`${apiUrl}/medical-records/${record.id}`, { headers: financeHeaders });
  expect(forbiddenClinical.status()).toBe(403);
  const tenantCrossover = await finance.request.get(`${apiUrl}/api/v1/payments`, { headers: { ...financeHeaders, "X-Organization-Id": "10000000-0000-0000-0000-000000009999" } });
  expect(tenantCrossover.status()).toBe(403);
  await checkWidths(finance, "finance");
  await finance.screenshot({ path: testInfo.outputPath("payment-real-api.png"), fullPage: true });

  // Independent authenticated API checks against the same real runtime/RLS role.
  const appointmentData = appointmentResponse.request().postDataJSON();
  const raceDate = new Date(tomorrow); raceDate.setDate(raceDate.getDate()+1);
  const raceDay = new Intl.DateTimeFormat("sv-SE", { timeZone: "America/Sao_Paulo" }).format(raceDate);
  const raceBody = { ...appointmentData, scheduledStart: `${raceDay}T10:00:00`, scheduledEnd: `${raceDay}T10:50:00` };
  const appointmentsUrl = `${apiUrl}/psychoanalysts/9001/appointments`;
  const appointmentRace = await Promise.all([0,1].map(() => page.request.post(appointmentsUrl, { headers: clinicalHeaders, data: raceBody })));
  expect(appointmentRace.filter((result) => result.status() === 201)).toHaveLength(1);
  expect(appointmentRace.filter((result) => result.status() === 409)).toHaveLength(1);
  const raceAppointment = await appointmentRace.find((result) => result.status() === 201)!.json();
  const rescheduled = await page.request.post(`${appointmentsUrl}/${raceAppointment.id}/reschedule`, { headers: clinicalHeaders,
    data: { scheduledStart: `${raceDay}T11:00:00`, scheduledEnd: `${raceDay}T11:50:00` } });
  expect(rescheduled.status(), await rescheduled.text()).toBe(201);
  const replacement = await rescheduled.json();
  const cancelled = await page.request.patch(`${appointmentsUrl}/${replacement.id}/cancel`, { headers: clinicalHeaders, data: { reason: "Synthetic test cancellation" } });
  expect(cancelled.status(), await cancelled.text()).toBe(200);
  expect((await cancelled.json()).status).toBe("CANCELLED");
  const edited = await page.request.patch(`${apiUrl}/patients/${patient.id}`, { headers: clinicalHeaders, data: { phone: "11900000001" } });
  expect(edited.status()).toBe(200);
  expect((await edited.json()).phone).toBe("11900000001");
  const deactivated = await page.request.patch(`${apiUrl}/patients/${patient.id}/deactivate`, { headers: clinicalHeaders, data: { reason: "Synthetic test lifecycle" } });
  expect(deactivated.status(), await deactivated.text()).toBe(200);
  expect((await deactivated.json()).active).toBe(false);
  const reactivated = await finance.request.patch(`${apiUrl}/patients/${patient.id}/reactivate`, { headers: financeHeaders });
  expect(reactivated.status(), await reactivated.text()).toBe(200);
  expect((await reactivated.json()).active).toBe(true);
  const search = await page.request.get(`${apiUrl}/patients/search`, { headers: clinicalHeaders, params: { query: name, active: true } });
  expect(search.status()).toBe(200);
  expect((await search.json()).content.some((entry: { id: number }) => entry.id === patient.id)).toBe(true);
  await finance.close();
  expect(pageErrors).toEqual([]);
  // Only synthetic JWTs/content, stored under the repository's ignored secrets directory.
  writeFileSync(resolve("../ops/secrets/local-e2e/smoke.json"), JSON.stringify({ recordId: record.id, patientId: patient.id, paymentId: payment.payment.id, content, clinicalHeaders, financeHeaders, syntheticTotpSecrets }));
});
