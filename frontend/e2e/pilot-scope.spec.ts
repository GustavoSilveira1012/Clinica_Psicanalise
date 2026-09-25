import { expect, test, type Page } from "@playwright/test";

const organizationId = "10000000-0000-0000-0000-000000000001";
const profile = {
  userId: 501,
  name: "Usuário Sintético",
  email: "e2e@example.invalid",
  role: "CLINIC_ADMIN",
  sessionId: "synthetic-e2e-session",
  organizations: [{
    id: organizationId,
    name: "Clínica Sintética",
    slug: "clinica-sintetica",
    type: "CLINIC",
    status: "ACTIVE",
    timezone: "America/Sao_Paulo",
    role: "OWNER",
  }],
};

async function mockAuthentication(page: Page) {
  let authenticated = false;

  await page.route("**/auth/refresh", async (route) => {
    if (authenticated) {
      await route.fulfill({ json: { accessToken: "synthetic-access-token" } });
      return;
    }
    await route.fulfill({ status: 401, json: { code: "UNAUTHENTICATED" } });
  });
  await page.route("**/auth/csrf", (route) => route.fulfill({ json: { token: "synthetic-csrf-token" } }));
  await page.route("**/auth/login", (route) => route.fulfill({
    json: { status: "MFA_REQUIRED", challenge: "synthetic-mfa-challenge" },
  }));
  await page.route("**/auth/mfa/totp/verify", async (route) => {
    authenticated = true;
    await route.fulfill({ json: { accessToken: "synthetic-access-token" } });
  });
  await page.route("**/auth/me", (route) => route.fulfill({ json: profile }));
}

async function signIn(page: Page) {
  await page.goto("/login", { waitUntil: "commit" });
  await page.getByLabel("E-mail profissional").fill(profile.email);
  await page.getByRole("textbox", { name: "Senha" }).fill("Synthetic-only-password");
  await page.getByRole("button", { name: /continuar/i }).click();
  await expect(page.getByRole("heading", { name: "Confirme sua identidade" })).toBeVisible();
  await page.getByLabel("Código MFA").fill("123456");
  await page.getByRole("button", { name: /entrar com segurança/i }).click();
}

test("keeps patient data and platform billing visibly unavailable in the pilot", async ({ page }) => {
  await mockAuthentication(page);
  let billingApiCalls = 0;
  await page.route(`**/organizations/${organizationId}/billing`, async (route) => {
    billingApiCalls += 1;
    await route.fulfill({ json: {} });
  });

  await signIn(page);
  await expect(page).toHaveURL(/\/dashboard$/);
  await expect(page.getByRole("heading", { name: "Dashboard indisponível" })).toBeVisible();
  await expect(page.getByText(/acesso a dados clínicos e financeiros está desabilitado/i)).toBeVisible();

  await page.goto("/billing", { waitUntil: "commit" });
  await expect(page.getByRole("heading", { name: "Faturamento da plataforma indisponível" })).toBeVisible();
  await expect(page.getByText(/nenhuma cobrança, nota fiscal ou notificação automática será executada/i)).toBeVisible();
  expect(billingApiCalls).toBe(0);
});

test("redirects unauthenticated direct navigation to the protected billing route", async ({ page }) => {
  await mockAuthentication(page);
  await page.goto("/billing", { waitUntil: "commit" });
  await expect(page).toHaveURL(/\/login$/);
  await expect(page.getByRole("heading", { name: "Acesse sua organização" })).toBeVisible();
});
