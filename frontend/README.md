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

O ambiente inicia com `dataSource` mockado para permitir validar os fluxos sem dependência do backend. O cliente HTTP tipado está em `src/shared/lib/api-client.ts`; quando os endpoints de dashboard, notificações, preferências e compliance estiverem expostos, o adapter pode ser trocado sem alterar as telas.

No shell autenticado, use `Ctrl/Cmd + K` para abrir a busca rápida entre os módulos permitidos para o usuário. As telas são carregadas sob demanda para reduzir o tempo inicial de entrada.

Credenciais de demonstração: `demo@psicogest.com` / `demo123`, seguidas de qualquer código MFA de 6 dígitos.

## Organização

- `src/app`: providers e roteamento protegido.
- `src/features`: telas por domínio: clínica, financeiro, fiscal, operação, comunicação e governança.
- `src/shared/components`: design system, shell, estados e proteção de rotas.
- `src/shared/components/CommandPalette.tsx`: navegação rápida acessível por teclado.
- `src/shared/lib`: cliente HTTP, fonte mock, permissões, validações e formatação segura.

O frontend não armazena PHI em `localStorage`/`sessionStorage`. A sessão persistida no mock contém somente um marcador de demonstração; em produção, usar cookie `HttpOnly`/Secure e autorização repetida no backend.
