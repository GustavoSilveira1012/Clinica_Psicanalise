# PsicoGest

Plataforma SaaS para operação clínica, agenda, pacientes, prontuário, financeiro, fiscal e governança de clínicas de psicanálise.

O checkout atual é separado em:

- `backend/psicogest/psicogest`: API Spring Boot 4, Java 25, PostgreSQL, Flyway, Redis e autorização contextual;
- `frontend`: React, TypeScript, Vite, Tailwind, React Router, TanStack Query, React Hook Form e Zod.

O produto foi estruturado para multi-tenant: cada organização possui membros, papel, plano, limites, onboarding e contexto de acesso. Dados clínicos, financeiros e de governança são autorizados no servidor e protegidos por isolamento de organização e RLS no PostgreSQL.

## Módulos entregues

- autenticação, MFA TOTP, refresh token, sessões e revogação;
- organizações, membros, convites, onboarding, planos, entitlements e uso;
- pacientes, agenda, séries, bloqueios e prevenção de conflitos;
- prontuário DRAFT/FINALIZED, revisões, adendos, autorização clínica e auditoria;
- recebíveis, pagamentos, estornos, crédito, conciliação bancária e repasses;
- NFS-e com fluxo fail-closed, documentos fiscais e origem financeira;
- notificações, preferências, templates seguros, quiet hours e rate limit outbound;
- LGPD, solicitações de titulares, retenção, incidentes e trilha de auditoria sem payload sensível;
- health/readiness, CI, migrations versionadas e configuração de produção.

## Segurança e limites importantes

O frontend usa o backend real por padrão. O adapter demonstrativo só é habilitado explicitamente com `VITE_DEMO_MODE=true`; ele deve ser usado apenas com dados fictícios e nunca em uma organização real.

Em produção, injete por secret manager: senha do banco, chaves JWT, chave de criptografia MFA, Redis, credenciais de providers e endpoints de alertas. Não coloque `.env`, certificados ou chaves no Git. O projeto não é uma certificação legal ou de conformidade LGPD; a clínica ainda precisa validar contratos, bases legais, retenção e operação com seus responsáveis.

## Execução local

Pré-requisitos: Node.js 22+, pnpm 10+, Java 25, Maven Wrapper e PostgreSQL/Redis. Docker Compose está disponível para subir apenas as dependências:

```powershell
Copy-Item .env.example .env
docker compose up -d postgres redis
```

Configure chaves JWT e `MFA_ENCRYPTION_KEY` antes de iniciar a API. Os caminhos padrão são `backend/psicogest/psicogest/secrets/jwt-public.pem` e `jwt-private.pem`; essa pasta é ignorada pelo Git.

API:

```powershell
cd backend/psicogest/psicogest
./mvnw spring-boot:run
```

Frontend:

```powershell
cd frontend
pnpm install --frozen-lockfile
Copy-Item .env.example .env
pnpm dev
```

Abra `http://localhost:5173`. O readiness da API fica em `http://localhost:8080/health/ready` e a liveness em `http://localhost:8080/health/live`.

## Qualidade

```powershell
cd frontend
pnpm lint
pnpm typecheck
pnpm test
pnpm build

cd ..\backend\psicogest\psicogest
./mvnw -DskipTests compile
./mvnw test
```

Os testes de integração do backend usam Testcontainers e exigem Docker ativo. O CI executa install congelado, lint, typecheck, testes, build e validação da sequência de migrations.

## Banco e migrations

O backend executa Flyway automaticamente no startup. Para validar ou aplicar manualmente em um banco já existente:

```powershell
cd backend/psicogest/psicogest
./mvnw flyway:validate
./mvnw flyway:migrate
```

Migrations antigas baselined não devem ser reaplicadas em bancos existentes. Em caso de adoção de uma base criada fora do histórico do Flyway, faça backup, valide os objetos e reconcilie o histórico com revisão técnica; nunca use `clean` em dados de cliente.

## Demonstração para a clínica

Para uma demonstração sem integrações externas, use um ambiente isolado com `VITE_DEMO_MODE=true`, dados fictícios e a conta exibida na tela de login. Mostre, nesta ordem:

1. dashboard e seletor de organização;
2. agenda, criação de consulta e bloqueio de horário;
3. paciente e prontuário com autorização, rascunho, finalização e adendo;
4. financeiro com recebível, status e separação de contexto;
5. notificações, preferências e estados de supressão;
6. LGPD, auditoria e sessões;
7. onboarding, equipe, convite, plano e limites SaaS.

Deixe claro durante a apresentação quais telas são demonstração local e quais dependem de backend/provider homologado. Não use dados pessoais, telefones, e-mails ou prontuários reais.

## Release e produção

Há Dockerfiles para API e frontend, `docker-compose.yml` para dependências locais e workflow em `.github/workflows/quality.yml`. O procedimento de operação está em [`docs/production-runbook.md`](docs/production-runbook.md), com scripts de backup/restore em `ops/`. Antes de inserir dados reais, ainda devem ser executados pela equipe responsável: restore drill de backup, teste de rollback, homologação de pagamento/NFS-e/e-mail/WhatsApp, revisão independente de segurança, configuração de observabilidade e validação jurídica/operacional da LGPD.
