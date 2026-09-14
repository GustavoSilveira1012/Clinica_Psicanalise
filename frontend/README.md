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

O site público fica em `/`, o pedido de demonstração em `/demo` e o produto autenticado em `/dashboard`. O ambiente inicia com `dataSource` mockado para permitir validar os fluxos sem dependência do backend. O cliente HTTP tipado está em `src/shared/lib/api-client.ts`; quando os endpoints de dashboard, notificações, preferências e compliance estiverem expostos, o adapter pode ser trocado sem alterar as telas.

O roteiro de apresentação para a clínica está em [`DEMO-ROTEIRO.md`](./DEMO-ROTEIRO.md).

No shell autenticado, use `Ctrl/Cmd + K` para abrir a busca rápida entre os módulos permitidos para o usuário. As telas são carregadas sob demanda para reduzir o tempo inicial de entrada.

Na demonstração, ações como criar consulta, emitir NFS-e, lançar financeiro e enviar convite são simulações locais. O backend deverá repetir autorização, isolamento por clínica, idempotência, rate limit e bloqueio de integrações antes de qualquer uso com dados reais.

Credenciais de demonstração: `demo@psicogest.com` / `demo123`, seguidas de qualquer código MFA de 6 dígitos. O modo demo é habilitado automaticamente no desenvolvimento; em um build de produção, `VITE_DEMO_MODE=true` deve ser usado somente para uma apresentação isolada. Sem essa flag, o frontend bloqueia o login local até a integração com o provedor de autenticação.

## Organização

- `src/app`: providers e roteamento protegido.
- `src/features`: telas por domínio: clínica, financeiro, fiscal, operação, comunicação e governança.
- `src/shared/components`: design system, shell, estados e proteção de rotas.
- `src/shared/components/CommandPalette.tsx`: navegação rápida acessível por teclado.
- `src/shared/lib`: cliente HTTP, fonte mock, permissões, validações e formatação segura.

Enquanto `VITE_DEMO_MODE=true`, o `dataSource` mantém um cenário totalmente fictício em `localStorage` para que a demonstração sobreviva à navegação e ao reload. Nunca habilite esse modo com dados reais. A sessão persistida no mock contém somente um marcador de demonstração; em produção, usar cookie `HttpOnly`/Secure, autorização repetida no backend, isolamento por `FinancialEntity` e nunca registrar corpos de mensagens, dados clínicos ou contatos completos em logs/telemetria.
