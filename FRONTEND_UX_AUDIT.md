# FRONTEND_UX_AUDIT.md — PsicoGest

> Auditoria de UX/UI do frontend anterior a qualquer alteração (ETAPA 1).
> Stack: React 19 · TypeScript 5.8 · Tailwind 3.4 · React Router 6 · TanStack Query 5 · React Hook Form 7 + Zod 4 · Vite 7 · Vitest.
> Método: leitura completa do app (shell, providers, design system, todas as páginas de features, camada de auth/API, superfície pública). Nenhum arquivo foi alterado nesta etapa.

## 1. Resumo executivo

O frontend **já está construído, funcional e endurecido em segurança**. A profissionalização pedida é **refino cirúrgico** — identidade visual, consistência de sistema, UX de confirmação e qualidade — e **não** uma reescrita.

- **Nenhum bloqueador P0 encontrado.** Token só em memória, refresh via cookie httpOnly, CSRF em mutações `/auth/`, cache isolado por tenant (`userId:tenantId`), guarda de versão de sessão (409), e comportamento *fail-closed* fora do modo demo. Não há token em `localStorage`, nem `dangerouslySetInnerHTML`, nem log de conteúdo clínico.
- **Maior lacuna é de identidade e consistência (P1):** o visual carrega vários *tells* genéricos e o mapeamento de status/labels está fragmentado em 4+ lugares.
- **Segunda lacuna é UX de segurança (P1):** ações irreversíveis (cancelar consulta, finalizar prontuário, revogar sessão) não pedem confirmação.

## 2. Pontos fortes a preservar (NÃO regredir)

- **Segurança de sessão:** access token em memória; refresh por cookie httpOnly; CSRF; header `X-Organization-Id`; guarda `SESSION_CONTEXT_CHANGED` (409); `QueryClient` remontado por `key={userId:tenantId}` (isolamento de cache entre tenants).
- **Fail-closed:** fora de `demoMode`, telas sem integração real exibem `Notice` em vez de dados falsos.
- **Privacidade/LGPD:** auditoria sem corpo de mensagem, minimização de dados sensíveis, `maskContact`, prontuário com revisão/addendum.
- **Acessibilidade (base):** `Modal` com focus-trap/ESC/scroll-lock; toasts com `aria-live`; `CommandPalette` com `role=listbox`/`aria-activedescendant`; `Input` com `aria-invalid`/`aria-describedby`.
- **Formulários:** RHF + Zod centralizado em `validators.ts` com mensagens pt-BR.
- **Separação de domínios:** Clínico / Administrativo / Financeiro / Fiscal / Billing SaaS / Privacidade nunca se misturam. Billing SaaS explicitamente separado do financeiro dos pacientes.

## 3. Achados priorizados

### P0 — Bloqueadores
Nenhum. (Ver §2 para a postura de segurança que sustenta esta conclusão.)

### P1 — Alto impacto

**P1.1 — Identidade visual genérica (*AI-slop tells*).** O design é competente, mas usa marcas de template que enfraquecem a percepção de produto próprio:
- *Eyebrows* em CAIXA ALTA com `tracking` largo repetidos em quase toda página/seção (`PageHeader`, Landing, Login, Billing, Demo…).
- Paleta base `cream (#f8f7f2)` + acento `coral (#dc725a)` — combinação hoje associada a páginas geradas por IA.
- Cards uniformes `rounded-2xl` + `shadow-soft` único, sem hierarquia de elevação.
- Separadores com ponto-médio (`·`) em metadados (Landing: `demonstração · dados fictícios`, `Hoje · agenda`, `online · 50 min`).
- *Impacto:* percepção de genérico contraria o objetivo (calma, confiança, precisão, profissionalismo). *Ação:* direção visual própria + tokens semânticos (ETAPA 2/3).

**P1.2 — Mapeamento de status/labels fragmentado.** A mesma responsabilidade vive em 4+ lugares: `StatusBadge` (mapa inline em `ui.tsx`), `labels.ts`, e mapas por página (`AgendaPage.statusLabel`, `FinanceTable` origem/método, `SettingsPage.roleLabel`, `ProfilePage`). Não há fonte única (label/cor/ícone/descrição por domínio).
- *Impacto:* divergência de rótulo/cor entre telas; risco de status sem tradução. *Ação:* módulo central de status por domínio, consumido por um único `StatusBadge`.

**P1.3 — `StatusBadge` sem *fallback* seguro.** Valores fora do mapa renderizam o texto cru (ex.: `"EXAMPLE"` em Notifications; valores fiscais). *Ação:* *fallback* neutro (rótulo formatado + tom neutro) e log de aviso em dev.

**P1.4 — UX de confirmação ausente em ações irreversíveis.** Sem diálogo de confirmação para cancelar consulta (`AgendaPage`), finalizar prontuário (`ClinicalRecordPage`), revogar sessão (`ProfilePage`) e cancelar assinatura. *Impacto:* erro destrutivo com um clique em contexto clínico. *Ação:* primitivo `ConfirmDialog` (título, consequência, confirmação tipada quando crítico).

**P1.5 — Inputs de busca crus.** Agenda/Patients/Finance usam `<input>` nativo em vez do `Input` do design system. *Impacto:* estilo, foco e a11y inconsistentes. *Ação:* primitivo `SearchInput` compartilhado.

### P2 — Médio impacto

- **P2.1 — Datas inconsistentes.** `format.ts` produz "25 set" por padrão, mas o brief exige `dd/MM/yyyy` + `HH:mm`. `CompliancePage` (`request.receivedAt`) e `ProfilePage` renderizam datas cruas. *Ação:* padronizar em `formatDate`/`formatDateTime` e eliminar renders crus.
- **P2.2 — Cópia com gênero fixo.** `LoginPage`: "Bem-vinda de volta" assume usuária feminina. *Ação:* cópia neutra ("Que bom ver você de novo" / "Acesse sua conta").
- **P2.3 — MFA sem QR code.** `MfaPage` mostra `secret` + `otpauthUri` como texto cru; sem QR aumenta fricção e erro de digitação. *Ação:* renderizar QR a partir do `otpauthUri` (lib leve ou SVG), mantendo o segredo copiável como fallback.
- **P2.4 — Sem `correlationId` em erros.** `ErrorBoundary` (500 de fato) e páginas 404/403 não expõem código de correlação. Brief pede isso para suporte. *Ação:* gerar/mostrar `correlationId` nas telas de erro (sem vazar conteúdo).
- **P2.5 — Padrão de abas duplicado.** `Finance`, `Notifications`, `Compliance`, `Settings`, `Profile` reimplementam abas localmente. *Ação:* primitivo `Tabs` acessível (`role=tablist`/`tab`/`tabpanel`, setas).
- **P2.6 — Empty/loading states desiguais.** Cobertura varia por página. *Ação:* padronizar `EmptyState`/`Skeleton` em todas as listas e detalhes.

### P3 — Polish / baixo risco

- **P3.1 — `FiscalPage` ternário redundante:** `value={demoMode ? String(authorized) : String(authorized)}` (ramos idênticos). *Ação:* simplificar.
- **P3.2 — Paleta `sage` incompleta** no `tailwind.config` (faltam 300/400/800/900), limitando nuances de estado/hover/dark. *Ação:* completar a escala.
- **P3.3 — `LandingPage` usa `emerald` cru** para pills de status no mockup (ilustrativo, mas diverge do sistema). *Ação:* alinhar ao token de status ou marcar como ilustração.
- **P3.4 — Dados de demonstração hardcoded** dispersos (ver §4). Quase todos protegidos por `demoMode`, porém alguns sem comentário explicativo. *Ação:* documentar/centralizar e comentar a origem.

## 4. Inventário de dados de demonstração hardcoded

Todos os itens abaixo são exibidos **apenas** sob `demoMode` (`import.meta.env.DEV && VITE_DEMO_MODE === "true"`); em produção as telas fazem *fail-closed*. Objetivo: documentar (não remover comportamento) e comentar a origem.

| Página | Dado fixo | Observação |
|---|---|---|
| Dashboard | saudação "Bom dia" fixa, `/5400`, `stats[2]`/`stats[3]`, "Sala 02" | saudação deveria variar por horário |
| PatientDetails | "Paciente desde mar 2025" | data ilustrativa |
| FinancePage | linhas de reembolso demo | apenas exibição |
| PackagesPage | stats "76" / "R$ 17.840" / "01" | métricas ilustrativas |
| ProfilePage | "Configurado em 14 ago 2026 · último uso há 2 min" | data/atividade ilustrativa |
| SettingsPage | `demoMembers` | fluxo real usa `getOrganizationMembers` |
| BillingPage | `demoPlans` / `demoBilling` | troca/cancelamento dependem de provedor de billing |
| LandingPage | métricas do mockup ("08", "148", "R$ 8.420", "04") | ilustração de marketing — aceitável |

## 5. Inventário por página (observações pontuais)

- **Público — Landing/Demo/Invite:** semântica boa (`main/header/nav/section/article/footer`, `aria-label`); RHF+Zod no Demo; Invite com *fail-closed* real, redireciona login preservando token do convite e reforça validação no backend. *Tells* visuais (eyebrows/·) presentes.
- **Auth — Login/MFA:** RHF+Zod, `autoComplete` correto, `inputMode` numérico no MFA. Ver P2.2 (gênero) e P2.3 (QR).
- **Shell/Providers:** aninhamento `ErrorBoundary > Theme > Toast > Auth > ScopedQuery`; cache por tenant; `queryClient.clear()` no desmonte. Preservar.
- **Agenda/Patients/Finance:** busca com `<input>` cru (P1.5); mapas de status/rótulo locais (P1.2); sem confirmação de cancelamento (P1.4).
- **Clínico (Records/Record):** finalizar registro sem confirmação (P1.4); privacidade correta.
- **Notifications/Compliance/Settings/Profile:** abas locais duplicadas (P2.5); `StatusBadge "EXAMPLE"` cru (P1.3); datas cruas (P2.1).
- **Fiscal/Packages/Subscriptions:** *fail-closed* correto; ternário redundante no Fiscal (P3.1); stats demo (§4).
- **SaaS — Billing/Onboarding:** billing SaaS separado do financeiro clínico (preservar); ações dependem de provedor (documentar como ponto de integração).
- **Erros — 404/403/ErrorBoundary:** profissionais, sem `correlationId` (P2.4); sem log de conteúdo clínico (preservar).
## 6. Plano de execução (ordem das etapas)

Prioridade do brief: **Usabilidade → Clareza → Consistência → Segurança → Acessibilidade → Responsividade → Performance → Polish.**

1. **ETAPA 2 — Direção visual** (método frontend-design, dois passos): personalidade, tipografia, paleta de **tokens semânticos** (substitui cream+coral por identidade própria), espaçamento/raio/elevação, densidade, navegação, movimento contido. → resolve P1.1.
2. **ETAPA 3 — Design System:** tokens no `tailwind.config` + `index.css`; refino de `ui.tsx`; **módulo central de status por domínio** + `StatusBadge` com *fallback*; novos primitivos `ConfirmDialog`, `SearchInput`, `Tabs`. → resolve P1.2, P1.3, P1.4, P1.5, P2.5, P3.2.
3. **ETAPA 4 — Páginas** (ordem: Dashboard → Agenda → Patients → Patient Detail → Clinical → Finance → Fiscal → Packages → Subscriptions → Notifications → Privacy/LGPD → Settings → Billing SaaS → Onboarding): aplicar primitivos, confirmar ações irreversíveis, padronizar empty/loading/erro, datas `dd/MM/yyyy`, cópia neutra, QR no MFA, `correlationId` nas telas de erro. → resolve P2.1–P2.4, P2.6, P3.1, P3.3, P3.4.
4. **ETAPA 5 — Responsividade:** 320/375/768/1024/1440.
5. **ETAPA 6 — Acessibilidade:** foco visível, navegação por teclado, contraste, `prefers-reduced-motion`.
6. **ETAPA 7 — Qualidade:** `npm run lint`, `typecheck`, `test`, `build` em `frontend/`; corrigir regressões; **reportar resultados reais**.
7. **ETAPA 8 — `FRONTEND_IMPROVEMENTS.md`** com seção "Skills & Tools Used" (registrar só o que foi usado de fato).

## 7. Restrições e situação de branch

- **Não alterar** regras de negócio, autorização ou migrations do backend; guarda de frontend é UX (backend é a autoridade). Preservar `Clinic ≠ Organization`; `Clinic/System Admin ≠ acesso clínico automático`. Não misturar domínios; **Billing SaaS separado do financeiro dos pacientes**. Não renomear propriedades de API nem inventar campos/endpoints; se faltar dado, manter compilável e marcar o ponto de integração. Nunca enviar prontuário/nome/CPF/e-mail/telefone/diagnóstico a log/analytics.
- **Ferramentas:** `frontend-design` disponível (será usada). `shadcn/ui MCP`, `Figma MCP`, `animate`, `review-animations` **não disponíveis** → princípios de movimento aplicados manualmente e documentados.
- **Branch:** trabalhando no worktree `claude/agitated-jepsen-7daab1`. O pedido é "trabalhar apenas na main" — as mudanças serão integradas à `main` no momento do commit. **Sem commit automático**; commits só quando solicitado. Sem `reset --hard`/`clean -fd`/force push.

_Fim da ETAPA 1. Nenhuma alteração de código foi feita; próxima etapa é a direção visual (ETAPA 2)._
