# PsicoGest Frontend v0.9

Frontend React + TypeScript para a operação clínica do PsicoGest.

## Rodar

```bash
pnpm install
pnpm dev
```

Scripts de qualidade:

```bash
pnpm lint
pnpm typecheck
pnpm test
pnpm build
```

O site público fica em `/`, o pedido de demonstração em `/demo` e o produto autenticado em `/dashboard`. Por padrão, o frontend usa o adapter HTTP real (`src/shared/lib/real-api.ts`) e exige o backend autenticado. O cenário local fictício só é habilitado explicitamente com `VITE_DEMO_MODE=true`, por meio do adapter `src/shared/lib/mock-api.ts`.

O roteiro de apresentação para a clínica está em [`DEMO-ROTEIRO.md`](./DEMO-ROTEIRO.md).

No shell autenticado, use `Ctrl/Cmd + K` para abrir a busca rápida entre os módulos permitidos para o usuário. As telas são carregadas sob demanda para reduzir o tempo inicial de entrada.

Em modo demo, ações como criar consulta, emitir NFS-e, lançar financeiro e enviar convite são simulações locais. Em produção, ações sem endpoint/provider configurado são bloqueadas com erro explícito; o backend repete autorização, isolamento por organização, idempotência, rate limit e bloqueio de integrações.

Credenciais de demonstração: `demo@psicogest.com` / `demo123`, seguidas de qualquer código MFA de 6 dígitos. Elas só existem quando `VITE_DEMO_MODE=true`; sem essa flag, login, sessão e MFA dependem exclusivamente do backend.

## Organização

- `src/app`: providers e roteamento protegido.
- `src/features`: telas por domínio: clínica, financeiro, fiscal, operação, comunicação e governança.
- `src/shared/components`: design system, shell, estados e proteção de rotas.
- `src/shared/components/CommandPalette.tsx`: navegação rápida acessível por teclado.
- `src/shared/lib`: cliente HTTP, fonte mock, permissões, validações e formatação segura.

Enquanto `VITE_DEMO_MODE=true`, o `dataSource` mantém um cenário totalmente fictício em `localStorage` para que a demonstração sobreviva à navegação e ao reload. Nunca habilite esse modo com dados reais. A sessão persistida no mock contém somente um marcador de demonstração; em produção, usar cookie `HttpOnly`/Secure, autorização repetida no backend, isolamento por `FinancialEntity` e nunca registrar corpos de mensagens, dados clínicos ou contatos completos em logs/telemetria.
