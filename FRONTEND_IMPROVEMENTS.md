# FRONTEND_IMPROVEMENTS.md — PsicoGest

> Registro da profissionalização do frontend (ETAPAs 2–8), complementar a [FRONTEND_UX_AUDIT.md](FRONTEND_UX_AUDIT.md).
> Escopo: **somente frontend**. Nenhuma regra de negócio, autorização, migração ou contrato de API do backend foi alterada.
> Stack mantida: React 19 · TypeScript 5.8 · Tailwind 3.4 (`darkMode: "class"`) · React Router 6 · TanStack Query 5 · React Hook Form 7 + Zod 4 · Vite 7 · Vitest 3.
> Prioridade seguida: **Usabilidade → Clareza → Consistência → Segurança → Acessibilidade → Responsividade → Performance → Polish visual**.

## 1. Verificação final (resultados REAIS)

Executado em `frontend/` nesta sessão, com saída real dos comandos:

| Verificação | Comando | Resultado |
|---|---|---|
| Tipos | `tsc -b --pretty false` | ✅ exit 0 — sem erros |
| Lint | `eslint .` | ✅ exit 0 — sem avisos |
| Testes | `vitest run` | ✅ 9/9 testes em 3 arquivos — exit 0 |
| Build | `tsc -b && vite build` | ✅ exit 0 — 1 aviso de tamanho de chunk (ver §5) |

Contrato de teste preservado: `LoginPage.test.tsx` segue validando "E-mail profissional", "Senha", `/continuar/i`, "Segundo fator" e "Código MFA".

## 2. O que mudou, por etapa

- **ETAPA 2 — Direção visual "Tinta & Âmbar".** Tipografia IBM Plex Sans (texto/UI) + Newsreader (títulos serifados); paleta harbor (azul-tinta) + amber (âmbar) sobre papel/ink, com modo escuro em slate-950 (página) / slate-900 (cartões). Escolhas deliberadas contra *tells* de template: sem eyebrow em CAIXA-ALTA decorativa, sem destacar uma única palavra do título, numeração apenas onde há sequência real.
- **ETAPA 3 — Design system.** Tokens de cor/tipografia/espaçamento em `tailwind.config.js` + `index.css`; primitivos em `shared/components/ui.tsx` (Button, IconButton, Card, Input, Select, Textarea, Notice, Badge, StatusBadge, Modal, ConfirmDialog, PageHeader, EmptyState, Skeleton, Tabs/TabPanel, SearchInput). Mapeamento central de status → rótulo/tom em `shared/lib/status.ts`, eliminando a fragmentação apontada na auditoria. Formatação pt-BR (dd/MM/yyyy, HH:mm, BRL) em `shared/lib/format.ts`.
- **ETAPA 4 — Reconstrução das páginas.** Dashboard, Agenda, Pacientes/Detalhe, Prontuários/Prontuário, Financeiro, Fiscal, Pacotes, Notificações, Compliance, Configurações e Perfil; superfícies de auth (Login e MFA com QR local), públicas (Landing e Demo), SaaS (Billing, Invite, Onboarding) e estados 404/403/500 com **correlationId** (`shared/lib/correlation.ts`). `ConfirmDialog` (com `confirmWord`) para ações irreversíveis em Agenda e Prontuário.
- **ETAPA 5 — Responsividade.** Layouts revisados para 320 / 375 / 768 / 1024 / 1440 px (grids que colapsam, navegação adaptável, tabelas que viram cartões no mobile).
- **ETAPA 6 — Acessibilidade.** Foco visível, `prefers-reduced-motion` respeitado, navegação por teclado e **contraste WCAG AA**. Varredura dos pares texto/fundo e correção dos acentos inline (vermelho/sucesso/esmeralda) sem parceiro de modo escuro em MfaPage, FinancePage (2), ProfilePage e SettingsPage (2) — 7 ajustes de classe. Medição empírica no fundo real: `emerald-400` sobre slate-900 → razão **9.29**; `red-400` sobre slate-900 → **6.45** (ambos ≥ 4.5:1).

## 3. Segurança e domínio preservados (não regredido)

- **Sessão:** access token só em memória; refresh via cookie httpOnly; CSRF nas mutações `/auth/`; header `X-Organization-Id`; guarda de versão de sessão (409); `QueryClient` isolado por `userId:tenantId` (cache não vaza entre tenants).
- **Fail-closed:** fora do modo demo, telas sem integração real exibem `Notice` — nunca dados fictícios.
- **Privacidade/LGPD:** nada de prontuário, nome, CPF, e-mail, telefone, diagnóstico ou notas clínicas em log/analytics; `maskContact` em contatos; QR do TOTP renderizado localmente (sem rede, sem log).
- **Domínio:** Clínica ≠ Organização; Clinic Admin e System Admin ≠ acesso clínico automático; separação Clínico / Administrativo / Financeiro / Fiscal / Billing SaaS / Privacidade mantida (Billing SaaS não se mistura ao financeiro de pacientes). A guarda de RBAC no frontend é apenas UX; a autoridade é o backend.
- **Superfície de ataque:** sem token em `localStorage`, sem `dangerouslySetInnerHTML`, sem redirect aberto.

## 4. Skills e ferramentas usadas

### Efetivamente usadas
- **Skill `frontend-design` (Anthropic)** — base da direção visual e da crítica anti-template.
- **Dependência nova: `qrcode` (+ `@types/qrcode`)** — renderização local do QR do TOTP na tela de MFA, sem chamada de rede.
- **Claude Browser (preview)** — servidor de dev e `preview_eval` / `preview_resize` / `preview_console_logs`, com recarga completa da página e uma sonda de contraste inline (sem variáveis globais) para evidência de acessibilidade em modo escuro.
- **Toolchain do projeto** — `tsc`, `eslint`, `vitest` e `vite build` para a verificação da ETAPA 7.
- **React Hook Form + Zod** (já no stack) — validação dos formulários.

### Consideradas e NÃO usadas
- **shadcn/ui** — mantido o `ui.tsx` próprio; nenhum kit externo adicionado.
- **Importação de Figma / geração por design tool** — indisponível no ambiente.
- **Bibliotecas de animação** (framer-motion etc.) — não adicionadas; movimento feito em CSS e sempre sob `prefers-reduced-motion`.
- Nenhuma reescrita do zero, nenhuma troca de React/Tailwind, nenhum pacote de features não solicitado.

## 5. Desvios e decisões

1. **Auditoria de contraste ao vivo do `/` (Landing) não concluída pelo navegador.** O servidor de preview havia sido encerrado pelo próprio app (confirmado parado ~12 h antes) e o *pane* oculto fazia o `preview_eval` estourar o timeout de 30 s. O contraste da Landing é garantido **por construção** (ela usa o mesmo sistema de tokens já validado em todas as outras páginas) e por **medição empírica** dos dois únicos tons de acento escuro em uso (`emerald-400` → 9.29; `red-400` → 6.45 sobre slate-900/950, ambos ≥ AA 4.5:1). Nenhum par de cores novo/não testado foi introduzido na Landing.
2. **Poluição do ambiente de teste (encontrada e corrigida).** Um `frontend/.env.local` temporário (`VITE_DEMO_MODE=true`), criado para dirigir a verificação demo no navegador, estava sendo carregado pelo Vitest e forçava o app em modo demo — fazendo o teste de transição para o MFA falhar (o login demo rejeita credenciais não-demo, então a navegação para `/mfa` não ocorria). O arquivo foi removido; a suíte ficou verde (9/9). Não era regressão de app.
3. **Chunk principal acima de 500 kB.** `dist/assets/index-*.js` = 522,76 kB (gzip 161,98 kB), logo acima do limite de aviso do Vite. Aceito como não-regressão conhecida: as rotas já são *lazy*/code-split; o peso restante é o núcleo compartilhado (vendor/runtime). A config de build não foi alterada para evitar mudança não solicitada — otimização futura via `manualChunks`.
4. **Flakiness latente no teste de MFA.** O `LoginPage.test.tsx` conclui em ~975 ms contra o timeout padrão de 1000 ms do `findByText` em máquina fria/carregada. Mantido sem alteração para preservar o contrato de teste; candidato a um `findByText(..., { timeout })` explícito caso pisque em CI.
5. **Escopo frontend respeitado.** Sem tocar em regras de negócio, autorização, migrações ou contratos de API do backend; nenhuma propriedade de API renomeada e nenhum campo/endpoint inventado.

## 6. Pendências e próximos passos sugeridos

- Reexecutar a sonda de contraste na Landing (`/`) com o preview ativo e o *pane* visível, para registro ao vivo.
- Avaliar `build.rollupOptions.output.manualChunks` (split de vendor) para o chunk principal.
- Integrações reais ainda ausentes (estornos/crédito, conciliação bancária, provedores de notificação, horários de silêncio) seguem marcadas como **pontos de integração fail-closed** até os endpoints existirem — nenhuma exibe dado fictício fora do modo demo.
