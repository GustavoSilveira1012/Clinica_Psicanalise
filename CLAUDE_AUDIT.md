# CLAUDE CODE — AUDITORIA COMPLETA DO PROJETO CLÍNICA

Você está atuando como arquiteto de software sênior, engenheiro de segurança, especialista em backend Java/Spring Boot, frontend React/TypeScript, PostgreSQL, DevOps, SaaS e sistemas para área de saúde.

Sua missão é realizar uma AUDITORIA PROFUNDA deste repositório.

NÃO comece alterando arquivos.

Primeiro:

1. entenda o projeto;
2. leia a documentação;
3. inspecione backend e frontend;
4. rode validações possíveis;
5. encontre inconsistências;
6. encontre riscos;
7. encontre melhorias;
8. produza um relatório.

Somente depois de apresentar o relatório devem ser propostas alterações.

---

# 1. DOCUMENTAÇÃO OBRIGATÓRIA

Comece lendo completamente, caso existam:

- README.md
- PROJETO.md
- pom.xml
- package.json
- application.properties
- application.yml
- docker-compose.yml
- Dockerfile
- arquivos de CI/CD
- migrations Flyway
- documentação adicional encontrada no repositório

Use README.md e principalmente PROJETO.md como referência da arquitetura planejada.

IMPORTANTE:

Não considere uma funcionalidade implementada somente porque ela aparece no PROJETO.md.

Confirme no código.

---

# 2. NÃO INVENTE RESULTADOS

Nunca diga:

"testes passaram"

se você não executou os testes.

Nunca diga:

"funcionalidade implementada"

sem localizar o código correspondente.

Nunca diga:

"seguro"

somente porque existe Spring Security.

Se alguma coisa não puder ser comprovada:

marque como:

NÃO COMPROVADO 🟡

---

# 3. CLASSIFICAÇÃO

Para todos os componentes importantes, use obrigatoriamente:

IMPLEMENTADO ✅

PARCIAL 🟡

FALTANDO ❌

BLOQUEADOR DE VENDA 🚨

NÃO COMPROVADO 🔍

Também classifique prioridade:

P0 = bloqueador de produção / segurança / perda de dados

P1 = deve ser resolvido antes dos primeiros clientes

P2 = melhoria importante

P3 = melhoria futura / nice-to-have

---

# 4. PRIMEIRO: MAPEIE O REPOSITÓRIO

Analise a estrutura inteira.

Identifique:

## Backend

- controllers
- services
- repositories
- entities
- DTOs
- enums
- security
- authorization
- exception handlers
- configuration
- Flyway
- tests
- integrations externas
- scheduled jobs
- workers
- webhooks

## Frontend

- routes
- pages
- layouts
- components
- design system
- API client
- hooks
- queries
- mutations
- forms
- schemas Zod
- autenticação
- guards
- permissions
- state management
- mocks
- testes
- error boundaries

## Infraestrutura

- Docker
- CI/CD
- cloud
- secrets
- observabilidade
- banco
- Redis
- storage
- backups
- deploy

Crie um pequeno mapa arquitetural do que REALMENTE existe.

---

# 5. RODE VALIDAÇÕES

Descubra os comandos corretos pelo próprio projeto.

Não invente comandos.

Backend, quando possível:

- compile
- unit tests
- integration tests
- Flyway validation
- Spring context tests

Frontend, quando possível:

- install/verificação de dependencies
- lint
- typecheck
- tests
- production build

Também procure:

- warnings
- deprecated dependencies
- TODO
- FIXME
- XXX
- hacks temporários
- mocks
- hardcoded secrets
- hardcoded URLs
- hardcoded IDs
- código morto

Mostre exatamente os comandos executados e os resultados relevantes.

---

# 6. CORE DOMAIN — v0.2

Compare código real com o planejado.

Audite:

- User
- Patient
- Psychoanalyst
- Clinic
- ClinicMembership
- ClinicMembershipPeriod
- Availability
- AvailabilityException
- Appointment
- AppointmentSeries
- TherapeuticRelationship
- lifecycle / soft delete

Verifique Appointment:

- state machine
- validações temporais
- overlap
- concorrência
- reschedule preservando histórico
- séries recorrentes
- THIS_AND_FUTURE
- constraints PostgreSQL
- transações

Procure possíveis condições de corrida.

---

# 7. SECURITY — v0.3

Faça uma auditoria agressiva de segurança.

Analise:

## Authentication

- login
- BCrypt
- JWT
- assinatura RSA
- issuer
- audience
- expiration
- jti
- sid
- securityVersion

## Refresh Token

- token opaco
- hash
- rotation
- reuse detection
- session revocation
- logout
- logout-all

## MFA

- TOTP
- replay protection
- recovery codes
- secret encryption
- challenge TTL
- attempts

## Authorization

Procure:

- IDOR
- BOLA
- privilege escalation
- acesso apenas baseado em role
- endpoints sem @PreAuthorize
- repositories que retornam dados fora do escopo
- findAll perigosos

Regra fundamental:

SYSTEM_ADMIN não deve automaticamente possuir acesso clínico.

CLINIC_ADMIN não deve automaticamente possuir acesso clínico.

## CSRF

Verifique especialmente:

- refresh cookie
- logout
- cookies HttpOnly
- Secure
- SameSite
- endpoints webhook

## CORS

Procure wildcard perigoso.

## Headers

- HSTS
- CSP
- nosniff
- frame protection
- cache-control

## Rate limiting

Analise:

- login
- refresh
- MFA
- webhooks
- exports

## Secrets

Procure:

- API keys
- passwords
- private keys
- tokens
- certificates
- `.env` commitado

Use busca no repositório.

---

# 8. MULTI-TENANCY

Esta é uma das áreas mais críticas.

Verifique se existe realmente:

- Organization
- OrganizationMembership
- TenantContext
- organization_id
- tenant authorization
- PostgreSQL RLS

Procure possibilidade de:

Organization A acessar recursos da Organization B.

Analise:

- controllers
- services
- repositories
- native queries
- scheduled jobs
- workers
- webhooks
- exports

Qualquer tenant crossover deve ser:

BLOQUEADOR DE VENDA 🚨
P0

Se PostgreSQL RLS estiver planejado mas não implementado:

marque claramente.

---

# 9. CLINICAL — v0.4

Audite:

- MedicalRecord
- MedicalRecordRevision
- MedicalRecordAddendum
- Clinical Timeline
- Clinical Export

Confirme:

MedicalRecord NÃO pertence clinicamente à Clinic.

Verifique:

- DRAFT
- FINALIZED
- imutabilidade
- autoria
- authorization
- historical author access
- relationship ACTIVE/SUSPENDED/ENDED
- optimistic locking
- revisão concorrente
- addendum

Procure qualquer endpoint que exponha prontuário para:

- Clinic Admin
- System Admin
- usuário não relacionado

Isso deve ser tratado como P0.

---

# 10. CRIPTOGRAFIA

Confirme no código:

- AES-256-GCM
- IV aleatório
- autenticação
- envelope encryption
- DEK
- KEK/KMS abstraction
- AAD
- keyId
- cryptoVersion

Procure plaintext clínico no banco.

Procure plaintext clínico em:

- logs
- exceptions
- AuditLog
- SecurityEvent
- notification
- frontend console

Confirme que nenhuma key real está hardcoded.

---

# 11. AUDIT LOG

Audite:

- append-only
- HMAC chain
- sequence
- previousMac
- entryMac
- keyId
- external checkpoint previsto/implementado

Confirme que leituras clínicas sensíveis são auditadas.

Confirme que falha de audit em operação crítica causa comportamento fail-secure quando planejado.

Procure UPDATE/DELETE possível em audit_logs.

---

# 12. FINANCE — v0.5

Audite:

- FinancialEntity
- Receivable
- Payment
- PaymentAllocation
- Refund
- RefundAllocation
- CreditAccount
- CreditEntry
- ReceivableAdjustment

Verifique:

- BigDecimal
- NUMERIC
- rounding
- partial payment
- multi-allocation
- overpayment
- refund
- credit
- cancellation
- concurrency
- pessimistic locking

Procure possibilidade de:

- pagamento duplicado
- saldo credor duplicado
- refund acima do pagamento
- allocation acima do receivable
- saldo negativo

---

# 13. PAYMENT PROVIDERS / WEBHOOKS

Audite:

- provider abstraction
- raw request body
- signature validation
- timestamp
- replay protection
- provider event ID
- unique constraints
- idempotency
- inbox
- retries
- dead-letter

Webhook NÃO deve alterar entities diretamente.

Procure eventos duplicados que possam gerar:

- Payment duplicado
- Refund duplicado
- sessão duplicada
- cobrança duplicada

---

# 14. BANK RECONCILIATION

Audite:

- BankAccount
- BankStatement
- OFX
- BankTransaction
- ReconciliationAllocation
- FinancialEntity isolation
- duplicate statement detection
- transaction fingerprint
- concurrency

---

# 15. PROVIDER SETTLEMENT

Audite:

- gross payments
- fees
- refunds
- chargebacks
- adjustments
- net settlement
- bank reconciliation

Confirme que:

Payment.amount

não é alterado para representar valor líquido recebido do gateway.

---

# 16. FISCAL

Audite:

- FiscalIssuer
- FiscalConfiguration
- ServiceInvoice
- TaxSnapshot
- DPS
- FiscalProvider
- NFS-e integration
- cancellation
- substitution
- XML
- DANFSE
- storage

Confirme:

Payment != ServiceInvoice

Confirme:

NFS-e autorizada é imutável.

Confirme:

timeout externo não gera retry cego.

Procure:

RECONCILIATION_REQUIRED

ou mecanismo equivalente.

Verifique se credenciais fiscais ficam fora do banco.

---

# 17. PACKAGES — v0.6

Audite:

- PackagePlan
- PackagePlanVersion
- PackagePlanItem
- PatientPackage
- PatientPackageItem
- SessionCreditEntry
- PackageConsumption

Confirme:

saldo de sessões NÃO é contador mutável.

Deve ser derivado de ledger.

Teste mental e/ou automatizado:

saldo = 1

duas consultas consomem simultaneamente

resultado nunca pode ser -1.

---

# 18. PACKAGE CANCELLATION

Verifique:

- consumed value
- remaining service value
- ReceivableAdjustment
- Refund
- CreditBalance
- partial payment
- cancellation pending
- failed refund

Procure inconsistências entre ledger de sessões e financeiro.

---

# 19. SUBSCRIPTIONS

Audite:

- SubscriptionPlan
- SubscriptionPlanVersion
- PatientSubscription
- SubscriptionCycle
- recurring payment
- cycle Receivable
- cycle PatientPackage
- billing anchor
- rollover
- pause/resume
- cancel at period end

Confirme idempotência.

Webhook duplicado nunca pode conceder:

4 + 4 sessões

para o mesmo ciclo.

---

# 20. NOTIFICATIONS — v0.7

Audite:

- Notification
- Recipient
- Delivery
- Templates
- Providers
- Preferences

Confirme:

- destination encryption
- template versioning
- retries
- idempotency
- quiet hours
- webhooks
- opt-out
- rate limits

Procure dados clínicos em mensagens.

Notificação não deve conter conteúdo clínico sensível por padrão.

---

# 21. LGPD — v0.8

Audite:

- ProcessingActivity
- LegalBasis
- PrivacyNotice
- ConsentRecord
- DataSubjectRequest
- RetentionPolicy
- DataDisposalJob
- LegalHold
- PrivacyIncidentAssessment
- international transfers
- privacy contacts

Confirme que:

"Excluir conta"

não executa simplesmente:

DELETE CASCADE

em dados históricos obrigatórios.

Confirme que correção LGPD não altera indevidamente:

- MedicalRecord finalizado
- NFS-e autorizada
- AuditLog
- Payment histórico

---

# 22. FRONTEND — v0.9

Faça auditoria completa da UI.

Analise:

- estrutura
- componentização
- tipagem
- duplicação
- acessibilidade
- responsividade
- loading
- empty
- errors
- modals
- tables
- pagination
- forms

Procure:

- `any`
- type assertions perigosas
- fetch direto espalhado
- URLs hardcoded
- tokens em localStorage
- secrets
- console.log sensível
- HTML inseguro
- dangerouslySetInnerHTML
- XSS

---

# 23. FRONTEND ↔ BACKEND

Esta parte é CRÍTICA.

Faça uma tabela:

| Frontend | API usada | Backend existe? | DTO compatível? | Status |
| -------- | --------- | --------------- | --------------- | ------ |

Procure:

- páginas ainda mockadas;
- endpoint inexistente;
- campos divergentes;
- enums diferentes;
- datas incompatíveis;
- status incompatíveis;
- erros não tratados;
- paginação divergente.

Identifique tudo que quebraria em produção.

---

# 24. AUTENTICAÇÃO NO FRONTEND

Confirme:

- access token storage
- refresh handling
- CSRF
- 401
- 403
- session expiration
- logout
- MFA
- route guards

Evite token sensível persistido inseguramente quando a arquitetura definir cookie seguro.

---

# 25. SaaS — v1.0

Audite:

- Organization
- OrganizationMembership
- OrganizationInvite
- SaasPlan
- SaasPlanVersion
- SaasFeature
- SaasEntitlement
- SaasSubscription
- trial
- SaaS billing
- usage
- feature flags
- onboarding

Confirme:

billing SaaS

é separado do financeiro de pacientes.

---

# 26. PLAN LIMITS

Procure race conditions.

Exemplo:

plano permite 5 profissionais.

Já existem 5.

Duas requisições simultâneas tentam adicionar profissional.

Resultado nunca pode ser 7.

---

# 27. DOWNGRADE

Confirme:

downgrade NÃO apaga dados.

Deve bloquear criação futura quando limite estiver excedido, preservando histórico.

---

# 28. SUPPORT ACCESS

Procure implementação de:

- support grants
- scope
- TTL
- audit
- break-glass

Suporte nunca deve precisar da senha do cliente.

Support/System Admin não deve possuir acesso invisível a dados clínicos.

---

# 29. PRODUCTION INFRASTRUCTURE

Procure configuração para:

- production
- staging
- Docker
- reverse proxy
- TLS
- WAF
- Redis
- Postgres
- Object Storage
- KMS
- Secret Manager

Classifique o que existe.

---

# 30. DATABASE SECURITY

Verifique se há estratégia para usuários separados:

- app_runtime
- migration_service
- backup_service
- restore_service

Procure privilégios excessivos.

Runtime não deve ter:

- SUPERUSER
- BYPASSRLS
- DROP indiscriminado
- acesso aos backups

---

# 31. BACKUPS

Procure:

- backup automation
- PITR
- WAL
- retention
- immutable backups

Mais importante:

procure prova/teste de restore.

Backup sem restore validado deve ser classificado como:

PARCIAL 🟡

ou, dependendo do estágio:

BLOQUEADOR DE VENDA 🚨

---

# 32. OBSERVABILITY

Procure:

- structured logs
- correlation ID
- metrics
- tracing
- health checks
- alerting

Métricas desejáveis:

- p95/p99
- 5xx
- DB pool
- Redis
- queue backlog
- webhook backlog
- provider failure
- KMS failure

Procure PII/PHI em logs e traces.

---

# 33. CI/CD

Analise pipeline.

Verifique se possui:

- compile
- unit tests
- integration tests
- frontend build
- lint
- typecheck
- SAST
- dependency scan
- secret scan
- SBOM
- container scan
- staging
- E2E
- production approval

Mostre o que existe e o que falta.

---

# 34. DEPENDÊNCIAS

Procure:

- outdated packages
- known vulnerable dependencies
- deprecated APIs
- duplicate dependencies
- dependency conflicts

NÃO atualize automaticamente major versions nesta primeira análise.

Apenas reporte.

---

# 35. PERFORMANCE

Procure:

- N+1 queries
- queries sem índice
- paginação ausente
- carregamento completo de grandes tabelas
- `findAll()`
- JSON excessivo
- eager relationships perigosos
- loops fazendo query
- frontend waterfalls
- queries TanStack duplicadas

Analise principalmente:

- calendar
- patients
- clinical timeline
- finance
- dashboard

---

# 36. CONSISTÊNCIA TRANSACIONAL

Procure:

- operações multi-entidade sem `@Transactional`;
- chamada externa dentro de transaction/DB lock;
- eventos publicados antes do commit;
- inconsistência entre ledger e aggregate.

HTTP externo nunca deve ficar segurando pessimistic lock.

---

# 37. ERROR HANDLING

Audite:

- global exception handler
- error model
- correlation ID
- validation errors
- 400
- 401
- 403
- 404
- 409
- 422
- 429
- 500
- 503

Frontend deve tratar esses estados.

Erros internos não devem vazar stack trace ou SQL.

---

# 38. MELHORIAS DE CÓDIGO

Além de bugs, procure melhorias em:

- arquitetura
- legibilidade
- SOLID
- boundaries
- coupling
- package structure
- naming
- DTO design
- testability
- duplicated logic
- abstractions excessivas
- abstractions ausentes

Não proponha pattern apenas porque é "bonito".

Explique o benefício concreto.

---

# 39. NÃO FAÇA OVERENGINEERING

Questione também o projeto.

Se algo planejado estiver complexo demais para nosso primeiro lançamento:

marque como:

SIMPLIFICAÇÃO RECOMENDADA 💡

Explique:

- complexidade atual;
- risco;
- versão mais simples;
- quando evoluir depois.

Queremos um sistema profissional, mas também vendável.

---

# 40. ANALISE O PRODUTO

Além de código, avalie a experiência.

Pergunte:

"Uma clínica consegue começar a usar isso sem o desenvolvedor acessar o banco?"

Analise:

- signup
- onboarding
- criação da organização
- primeira clínica
- primeiro profissional
- primeiro paciente
- primeira consulta
- financeiro
- suporte
- billing SaaS
- upgrade/downgrade

---

# 41. IDENTIFIQUE O QUE FALTA PARA VENDER

Crie seção específica:

# BLOQUEADORES DE VENDA

Liste SOMENTE coisas que realmente impedem:

- piloto;
- cliente pagante;
- escala.

Separe:

## Bloqueia piloto

## Bloqueia cobrança

## Bloqueia escala

---

# 42. RELATÓRIO FINAL

Crie na raiz:

```text
PRODUCTION_AUDIT.md
```
