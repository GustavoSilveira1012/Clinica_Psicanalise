# PsicoGest v1.0 — SaaS e prontidão de produção

## O que foi implementado

- `Organization` é o tenant SaaS e pode conter múltiplas clínicas/contextos.
- Membership separada de `ClinicMembership`, com `OWNER`, `ADMIN`, `BILLING` e `MEMBER`.
- Convites com token opaco de uso único: somente o SHA-256 é persistido; o e-mail é armazenado em envelope criptografado e hash para deduplicação.
- Tenant context validado a partir do JWT, membership ativa e organização selecionada. `X-Organization-Id` nunca é tratado como autorização.
- PostgreSQL RLS criado para o núcleo SaaS, com função `app.current_organization_id()` e contexto transacional.
- Migração legada em EXPAND/BACKFILL/VALIDATE-preparation; tabelas antigas ainda não são `NOT NULL`/RLS até todos os write paths definirem o tenant.
- Catálogo independente de `PackagePlan`: SaaS plans, versions, features, entitlements, subscriptions, invoices e usage.
- Entitlement backend com `hasFeature`, `getLimit`, `requireFeature` e `requireCapacity`. Downgrade bloqueia novas criações, sem esconder histórico.
- Onboarding, ownership com proteção do último owner, billing SaaS separado, suporte temporário auditável e termos preparados no banco.
- Frontend com seletor de organização, `/app/:organizationSlug/*`, `/onboarding`, `/invite/:token` e `/billing`.
- Health probes nativos (`/health/live`, `/health/ready` e aliases `/actuator/health/liveness`, `/actuator/health/readiness`) verificando banco, Redis e tabela do Flyway, além do quality gate GitHub Actions.
- Billing provider SaaS, usage mensal idempotente/atômico, feature flags com rollout determinístico e rate limit outbound por canal/entidade financeira.

## Decisões de migração

- O histórico tinha versões Flyway e definições de tabela repetidas em funcionalidades que foram desenvolvidas em paralelo. A sequência foi normalizada com versões fracionadas (`V34_1`, `V36_1`, `V41_1`, `V42_1`) e marcadores de compatibilidade em criações antigas; não existem mais duas migrations com a mesma versão nem colisões não intencionais para os objetos SaaS/financeiros canônicos.
- As tabelas fiscais e de repasse canônicas continuam compatíveis com as entidades atuais. Um banco já existente deve ser validado com `flyway validate` antes do deploy; migrations aplicadas nunca devem ser editadas em produção.
- Os marcadores legados de V19, V21, V32 e V42 pressupõem uma base ainda em preparação para a primeira linha de produção. Se algum desses scripts já tiver sido aplicado em um banco real, o rollout deve usar uma baseline/repair explícita e aprovação de DBA antes de executar a sequência normalizada.

## Sequência de rollout do tenant

1. Aplicar V64–V66 e revisar o mapeamento do tenant bootstrap `organizacao-legada`.
2. Migrar cada write path para resolver `TenantContext` e chamar `TenantDatabaseContext` na mesma transação.
3. Executar as validações de constraints da V67 em janela controlada.
4. Habilitar RLS nas tabelas legadas depois da validação, com usuário de runtime sem `BYPASSRLS`.
5. Remover o tenant bootstrap e tornar `organization_id` `NOT NULL` apenas quando nenhum dado legítimo estiver nullable.

## Produção ainda exige configuração operacional

- PostgreSQL gerenciado com PITR/WAL, cópia imutável e teste periódico de restore.
- Redis separado por ambiente, workers idempotentes e armazenamento de objetos com KMS.
- Segredos fora do Git (`JWT_PRIVATE_KEY`, chaves de dados, Redis, billing/provider secrets).
- Usuários de banco separados para migrations, API e workers; somente migrations pode alterar schema.
- WAF/LB/CDN, TLS, alertas de 5xx/latência/fila/outbound volume e logs sem dados clínicos, corpo de mensagem ou contatos completos.
- Billing provider real, e-mail verification/password reset, webhooks com assinatura/replay protection e pentest antes do go-live.
- A habilitação final de RLS/`NOT NULL` nas tabelas legadas continua sendo um gate obrigatório: só deve ocorrer depois que todos os serviços antigos passarem a gravar `organization_id` dentro da mesma transação.

## Perfis de ambiente

- `development`: valores locais via variáveis de ambiente, sem credenciais versionadas.
- `staging`: banco, Redis, secrets e topologia equivalentes à produção, com dados sintéticos.
- `production`: ativar `SPRING_PROFILES_ACTIVE=production`; os placeholders sem valor fazem a aplicação falhar no startup em vez de iniciar insegura.

## Verificação local

Frontend:

```text
pnpm install --frozen-lockfile
pnpm lint
pnpm typecheck
pnpm test -- --run
pnpm build
```

Backend (Java 25):

```text
./mvnw test
```

O ambiente de demonstração do frontend usa dados fictícios e não envia mensagens, cobra cartão ou chama prefeitura/provider.
