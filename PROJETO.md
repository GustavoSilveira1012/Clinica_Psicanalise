# PROJETO.md — PsicoGest / Clínica

## 1. Documento mestre

Este arquivo é o documento técnico mestre do projeto.

Ele reúne arquitetura, regras de domínio, decisões técnicas, módulos, roadmap, segurança, financeiro, fiscal, clinical, packages, notifications, LGPD, frontend, SaaS, production readiness e estratégia comercial.

> O que está descrito aqui representa a arquitetura e o comportamento definidos para o produto. A existência real de cada item no repositório deve ser confirmada pelo `v1.0 Production Audit`.

---

# 2. Objetivo

Construir uma plataforma SaaS profissional para gestão de profissionais e clínicas, começando por psicanalistas e clínicas de psicologia/psicanálise.

Objetivos principais:

1. agenda e pacientes;
2. prontuário clínico seguro;
3. financeiro completo;
4. pagamentos;
5. fiscal / NFS-e;
6. pacotes e assinaturas;
7. notificações;
8. segurança e auditoria;
9. LGPD;
10. multi-organização;
11. comercialização SaaS.

---

# 3. Stack

## Backend

```text
Java
Spring Boot
Spring Security
Spring Data JPA
PostgreSQL
Redis
Flyway
Testcontainers
JWT / OAuth2 Resource Server
```

## Frontend

```text
React
TypeScript
Tailwind CSS
React Router
TanStack Query
React Hook Form
Zod
```

---

# 4. Estrutura backend esperada

```text
controller/
service/
repository/
model/
model/enums/
dto/
config/
exception/
security/

domain/
├── appointment/
├── relationship/
├── clinic/
├── lifecycle/
├── finance/
├── fiscal/
├── package/
├── notification/
├── privacy/
└── saas/
```

---

# 5. Estrutura frontend esperada

```text
src/
├── app/
├── routes/
├── features/
│   ├── auth/
│   ├── dashboard/
│   ├── calendar/
│   ├── patients/
│   ├── clinical/
│   ├── finance/
│   ├── fiscal/
│   ├── packages/
│   ├── subscriptions/
│   ├── notifications/
│   ├── privacy/
│   ├── settings/
│   └── billing/
│
├── shared/
│   ├── api/
│   ├── components/
│   ├── hooks/
│   ├── forms/
│   ├── guards/
│   ├── utils/
│   └── types/
```

---

# 6. Persistência

Flyway é dono do schema.

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Migrações aplicadas nunca devem ser alteradas retroativamente.

```text
nova mudança
→ nova Vxx
```

---

# 7. Princípios fundamentais

## 7.1 Clinic não possui dados clínicos

`Clinic` é contexto administrativo. O prontuário pertence ao contexto clínico `Patient ↔ Psychoanalyst`, regulado por autorização contextual e `TherapeuticRelationship`.

`clinic_id` nunca deve ser usado como conceito de propriedade clínica.

## 7.2 Organization não é Clinic

Na v1.0:

```text
Organization = Tenant técnico
Clinic       = Unidade/entidade operacional
```

`organization_id` pode existir em tabelas clínicas para isolamento de tenant, porém não implica ownership clínico.

## 7.3 Payment != ServiceInvoice

```text
Receivable = obrigação
Payment = dinheiro
ServiceInvoice = documento fiscal
```

São eventos distintos.

## 7.4 Ledgers imutáveis

Históricos importantes não são sobrescritos.

Exemplos:

```text
PaymentAllocation
RefundAllocation
CreditEntry
SessionCreditEntry
AuditLog
MedicalRecordRevision
MedicalRecordAddendum
FiscalEvent
```

Correções são novos eventos.

---

# 8. v0.2 — Core Domain

## Entidades

```text
User
Patient
Psychoanalyst
Clinic
ClinicMembership
ClinicMembershipPeriod
Availability
AvailabilityException
Appointment
AppointmentSeries
TherapeuticRelationship
```

## AppointmentStatus

```text
SCHEDULED
CONFIRMED
COMPLETED
CANCELLED
NO_SHOW
RESCHEDULED
```

## AppointmentType

```text
IN_PERSON
ONLINE
```

## State machine

```text
SCHEDULED
 ├→ CONFIRMED
 ├→ COMPLETED
 ├→ NO_SHOW
 ├→ CANCELLED
 └→ RESCHEDULED

CONFIRMED
 ├→ COMPLETED
 ├→ NO_SHOW
 ├→ CANCELLED
 └→ RESCHEDULED
```

Estados terminais não regressam.

## Conflict prevention

PostgreSQL:

```text
btree_gist
+
EXCLUDE USING gist
```

para impedir overlap de `SCHEDULED` / `CONFIRMED` por profissional.

## AppointmentSeries

```text
RecurrenceFrequency: WEEKLY
RecurrenceScope: SINGLE | THIS_AND_FUTURE | ENTIRE_SERIES
```

Split:

```text
old series → SUPERSEDED
successor  → previousSeries
```

Histórico nunca é reescrito.

---

# 9. TherapeuticRelationship

Status:

```text
ACTIVE
SUSPENDED
ENDED
```

Regras:

```text
ACTIVE → SUSPENDED / ENDED
SUSPENDED → ACTIVE / ENDED
ENDED → terminal
```

Relação é `Patient ↔ Psychoanalyst`, independente de clínica.

---

# 10. ClinicMembershipPeriod

`ClinicMembership` representa o par estável. Os períodos representam participação real ao longo do tempo.

```text
ClinicMembership
   └── ClinicMembershipPeriod[]
```

Uma pessoa que sai e retorna recebe novo período.

---

# 11. Lifecycle

Entidades com lifecycle administrativo:

```text
User
Patient
Psychoanalyst
Clinic
Availability
```

Campos:

```text
active
deactivatedAt
deactivationReason
reactivatedAt
```

Sem hard delete para histórico relevante.

---

# 12. v0.3 — Security

## Objetivos

Proteção contra:

```text
credential stuffing
brute force
session theft
token replay
IDOR/BOLA
privilege escalation
SQLi
XSS
CSRF
exfiltration
ransomware
DDoS
insider threat
backup theft
secret theft
```

## Authentication

JWT RSA/RS256 com claims mínimos:

```text
iss
aud
sub
jti
roles
sv
sid
token_type
iat
exp
```

Sem CPF, dados de paciente ou conteúdo clínico.

## Refresh token

```text
opaque
256-bit random
```

Persistência:

```text
SHA-256 hash
```

Rotação:

```text
A → B → C
```

Reuse:

```text
revoke family
SecurityEvent CRITICAL
```

## Session security

`UserSession` representa sessão física. JWT contém `sid`.

```text
session revoked → um device
securityVersion++ → todos os devices
```

## MFA

Política definida:

```text
PATIENT → opcional/recomendado
PSYCHOANALYST → obrigatório
CLINIC_ADMIN → obrigatório
SYSTEM_ADMIN → obrigatório
```

TOTP:

```text
RFC 6238
HMAC-SHA1
30s
6 digits
±1 step
```

Recovery codes: hash-only e single-use.

---

# 13. Authorization

Role sozinho não basta.

Exemplo:

```text
CLINIC_ADMIN
```

não lê `MedicalRecord` só por ser admin.

Política:

```text
Patient → próprio perfil
Psychoanalyst → pacientes sob relationship válido
Clinic Admin → administrativo
System Admin → sem acesso clínico por padrão
```

---

# 14. SecurityEvent / SecurityAlert

Eventos de segurança registram login, rate limit, refresh reuse, access denied, MFA, session revoke, mass access e atividade suspeita.

Alertas incluem:

```text
ID enumeration
mass patient access
mass clinical access
mass export
credential stuffing
brute force
session anomaly
refresh compromise
authorization abuse
```

Nunca logar password, JWT completo, refresh token, TOTP, recovery code, clinical plaintext ou chaves criptográficas.

---

# 15. AuditLog

AuditLog é separado de SecurityEvent.

Integridade:

```text
HMAC-SHA-256 chain
```

Campos principais:

```text
sequence
previousMac
entryMac
keyId
actor
session
action
resource
patient
clinic context
outcome
timestamp
correlationId
metadata
```

Key fora do banco.

---

# 16. Clinical Encryption

```text
AES-256-GCM
96-bit IV
128-bit auth tag
```

Envelope encryption:

```text
plaintext
↓
DEK random
↓
AES-GCM
↓
DEK wrapped by KEK/KMS
```

AAD liga ciphertext ao recurso correto.

---

# 17. v0.4 — Clinical

## MedicalRecord

Status:

```text
DRAFT
FINALIZED
```

Regras:

```text
DRAFT → editable
FINALIZED → immutable
```

Correções após finalização: `MedicalRecordAddendum`.

## MedicalRecordRevision

Cada save explícito cria novo snapshot cifrado e imutável.

Restore futuro copia conteúdo antigo para **nova** revision.

## Addendum

Somente em `FINALIZED`. Não edita prontuário original.

## Clinical Timeline

Metadados de:

```text
Appointment completed
Record created
Record finalized
Addendum
Relationship changes
```

Draft revisions não entram normalmente.

## Clinical Export

High-risk:

```text
1 patient
date range
finalized content
private storage
short TTL
hash
audit
rate limit
```

---

# 18. v0.5 — Finance

## Core

```text
FinancialEntity
Receivable
Payment
PaymentAllocation
Refund
RefundAllocation
CreditAccount
CreditEntry
ReceivableAdjustment
```

## FinancialEntity

Representa o recebedor econômico.

```text
CLINIC
PSYCHOANALYST
```

## Receivable

Status:

```text
OPEN
PARTIALLY_PAID
PAID
CANCELLATION_PENDING
CANCELLED
```

`OVERDUE` é derivado.

## Payment

Status:

```text
PENDING
CONFIRMED
FAILED
CANCELLED
PARTIALLY_REFUNDED
REFUNDED
```

Pagamento não implica automaticamente quitação de Receivable.

## PaymentAllocation

Permite:

```text
1 Payment → múltiplos Receivables
múltiplos Payments → 1 Receivable
```

## Refund

Refund não altera o Payment original.

## CreditAccount

Saldo credor é ledger `CREDIT/DEBIT`, não campo mutável no paciente.

## Cancellation

Cobrança com pagamento exige settlement:

```text
REFUND
ou
CREDIT_BALANCE
```

---

# 19. PaymentProvider e webhooks

```text
PaymentProvider
├── AsaasPaymentProvider
├── MercadoPagoPaymentProvider
├── PagarmePaymentProvider
└── ...
```

Webhook:

```text
raw body
↓
signature
↓
timestamp/replay
↓
provider event id
↓
inbox
↓
idempotent processor
↓
domain services
```

Provider callback não altera entity diretamente.

---

# 20. Bank Reconciliation

```text
BankAccount
BankStatementImport
BankTransaction
BankReconciliationAllocation
```

Descrição bancária pode ser cifrada.

---

# 21. ProviderSettlement

Resolve:

```text
gross payments
- refunds
- provider fees
- chargebacks
± adjustments
=
net payout
```

Depois `ProviderSettlement ↔ BankTransaction`.

---

# 22. Fiscal

## FiscalIssuer

Ligado a `FinancialEntity`. Tax identifiers são strings, não números.

## FiscalConfiguration

Versionada e efetiva por período. Credenciais são referências ao Secret Manager.

## ServiceInvoice

Status:

```text
DRAFT
PENDING
PROCESSING
AUTHORIZED
REJECTED
CANCEL_PENDING
CANCELLED
ERROR
RECONCILIATION_REQUIRED
```

## TaxSnapshot

Congela os impostos usados na emissão.

## DPS

Numeração segura e única por emissor/série.

## FiscalProvider

```text
FiscalProvider
├── NationalNfseProvider
├── MunicipalProvider
└── ThirdPartyProvider
```

## Timeout fiscal

```text
timeout != rejected
```

Resultado externo incerto → `RECONCILIATION_REQUIRED`.

## Imutabilidade fiscal

NFS-e autorizada não é editada nem deletada.

Correção:

```text
cancel
substitute
```

---

# 23. v0.6 — Packages & Subscriptions

## Packages

```text
PackagePlan
PackagePlanVersion
PackagePlanItem
PatientPackage
PatientPackageItem
SessionCreditEntry
PackageConsumption
```

Versões comerciais publicadas são imutáveis.

## SessionCredit ledger

Tipos:

```text
PACKAGE_ACTIVATION
APPOINTMENT_CONSUMPTION
CONSUMPTION_REVERSAL
EXPIRATION
PACKAGE_CANCELLATION
MANUAL_ADJUSTMENT
```

Saldo é derivado do ledger.

## FEFO

```text
First Expire, First Out
```

## Package cancellation

```text
package value
-
consumed economic value
=
remaining service value
```

Depois:

```text
paid
-
adjusted receivable
=
settlement amount
```

Settlement por Refund ou CreditBalance.

## Subscriptions

```text
SubscriptionPlan
SubscriptionPlanVersion
PatientSubscription
SubscriptionCycle
```

Cada ciclo gera `Receivable` e, quando aplicável, `PatientPackage`.

## Billing anchor

Anchor 31:

```text
31/01
28/02
31/03
30/04
```

sem drift.

---

# 24. v0.7 — Notifications

```text
Notification
Recipient
Delivery
Template
Provider
```

Canais:

```text
EMAIL
WHATSAPP
SMS
```

Regras:

- destino cifrado;
- templates versionados;
- retry controlado;
- webhooks assinados;
- quiet hours;
- outbound rate limiting;
- mass send detection;
- nenhum conteúdo clínico sensível por padrão.

---

# 25. v0.8 — LGPD

## ProcessingActivity

Registra:

```text
purpose
legal basis
role
data categories
recipients
retention
```

## PrivacyNotice

Versionado e imutável após publicação.

## ConsentRecord

Consentimento não é base universal.

## DataSubjectRequest

Tipos:

```text
CONFIRMATION
ACCESS
CORRECTION
ANONYMIZATION
BLOCKING
DELETION
PORTABILITY
SHARING_INFORMATION
CONSENT_REVOCATION
AUTOMATED_DECISION_REVIEW
```

## Retention

Configurável por domínio. Clinical começa em `MANUAL_REVIEW` até validação jurídica específica.

## LegalHold

Bloqueia disposal automático em caso de litígio, incidente, obrigação legal ou investigação.

## Privacy incidents

`SecurityIncident` pode gerar `PrivacyIncidentAssessment`. Decisão regulatória não é feita automaticamente só por score.

---

# 26. v0.9 — Frontend

Áreas esperadas:

```text
Auth
MFA
Dashboard
Calendar
Patients
Clinical
Finance
Fiscal
Packages
Subscriptions
Notifications
Privacy
Settings
Billing
```

Padrões:

```text
typed API client
TanStack Query
React Hook Form
Zod
route guards
error boundaries
loading states
empty states
accessibility
responsive layouts
```

Frontend foi informado como concluído, mas integração real com backend deve ser comprovada no Production Audit.

---

# 27. v1.0 — SaaS

## Organization

Tenant técnico.

Tipos:

```text
SOLO
CLINIC
NETWORK
```

## OrganizationMembership

```text
OWNER
ADMIN
BILLING
MEMBER
```

Não substitui permissões clínicas.

---

# 28. Tenant Isolation

Camadas:

```text
Authorization
+
TenantContext
+
PostgreSQL RLS
```

Regra:

```text
Tenant crossover = release blocker
```

O runtime user do banco não deve ter `BYPASSRLS`.

---

# 29. SaaS Plans

```text
SaasPlan
SaasPlanVersion
SaasFeature
SaasPlanEntitlement
```

Frontend esconder feature é só UX; backend valida entitlement e limite.

Downgrade nunca apaga dados históricos.

---

# 30. SaasSubscription

Estados:

```text
TRIALING
ACTIVE
PAST_DUE
RESTRICTED
CANCEL_AT_PERIOD_END
CANCELLED
EXPIRED
```

Trial expirado não apaga dados.

Inadimplência deve ter grace/restricted mode, evitando bloqueio destrutivo de dados clínicos.

---

# 31. SaaS Billing

Separado do financeiro do paciente.

```text
SaasInvoice
SaasPayment
SaasBillingProvider
```

Nunca misturar mensalidade do produto com `Payment` da clínica.

---

# 32. Usage e Feature Flags

```text
SaasUsageEvent
SaasUsageMonthly
```

Métricas de uso podem incluir equipe, mensagens, exports e storage.

Feature Flag != Plan Entitlement.

```text
Entitlement = cliente pagou?
Feature Flag = queremos liberar feature?
```

---

# 33. Support Access

Suporte nunca pede senha.

Usar `SupportAccessGrant` com:

```text
scope
approval
reason
TTL
audit
```

Acesso clínico excepcional usa break-glass.

---

# 34. Production Architecture

```text
Internet
↓
CDN / DDoS
↓
WAF
↓
Load Balancer
↓
Spring APIs
↓
PostgreSQL / Redis / Workers
↓
Storage / KMS
```

Ambientes:

```text
development
staging
production
```

---

# 35. Database users

Separar:

```text
app_runtime
migration_service
backup_service
restore_service
security_admin
```

`app_runtime`:

```text
no SUPERUSER
no BYPASSRLS
no backup privileges
```

---

# 36. CI/CD

Pipeline desejado:

```text
compile
unit
integration
tenant isolation
security integration
frontend tests
lint
typecheck
SAST
SCA
secret scan
SBOM
container scan
build
sign
staging
E2E
DAST
manual approval
production
```

---

# 37. Observability

Métricas técnicas:

```text
requests/sec
p50/p95/p99
5xx
DB pool
DB latency
Redis
queue backlog
webhook lag
provider failures
KMS errors
```

Sem conteúdo clínico em traces.

Métricas comerciais:

```text
MRR
ARR
active organizations
trial conversion
churn
ARPA
plan distribution
onboarding completion
```

---

# 38. Backup / Disaster Recovery

```text
PITR
WAL
daily backup
immutable copy
```

Aplicação não pode apagar backup.

Restore validation:

```text
restore
↓
Flyway validate
↓
DB integrity
↓
Audit chain
↓
decrypt controlled sample
↓
smoke tests
```

Backup nunca restaurado não é backup comprovado.

---

# 39. Production Audit

Cada item deve ser classificado como:

```text
IMPLEMENTADO ✅
PARCIAL 🟡
FALTANDO ❌
BLOQUEADOR DE VENDA 🚨
```

Nunca marcar algo como implementado apenas porque foi planejado.

---

# 40. Priorização

## P0 — bloqueadores típicos

```text
tenant crossover
auth broken
clinical access leak
secrets exposed
no backup
restore not validated
broken migrations
payment duplication
missing audit on clinical access
plaintext medical records
production mocks
```

## P1

```text
provider integration
notifications
onboarding
support tooling
observability
alerts
legal docs
privacy workflows
```

## P2

```text
advanced dashboards
extra automation
UX polish
analytics
additional integrations
```

## P3

```text
nice-to-have
AI features
advanced reporting
secondary providers
```

---

# 41. GO LIVE

## Backend
- [ ] compile;
- [ ] integration tests;
- [ ] security tests;
- [ ] tenant isolation;
- [ ] migrations valid;
- [ ] RLS;
- [ ] KMS;
- [ ] Redis;
- [ ] PostgreSQL privado.

## Frontend
- [ ] production build;
- [ ] sem mocks críticos;
- [ ] auth integrada;
- [ ] MFA;
- [ ] permissions;
- [ ] sem sensitive console logs.

## Providers
- [ ] payment;
- [ ] webhook;
- [ ] email;
- [ ] WhatsApp;
- [ ] NFS-e ou feature desativada.

## Operations
- [ ] monitoring;
- [ ] alerting;
- [ ] backups;
- [ ] PITR;
- [ ] restore;
- [ ] runbooks;
- [ ] suporte.

## Legal
- [ ] Terms of Service;
- [ ] Privacy Notice;
- [ ] SaaS contract;
- [ ] controlador/operador revisado;
- [ ] retention review;
- [ ] incident procedure.

## Security
- [ ] pentest;
- [ ] rate limits;
- [ ] WAF;
- [ ] TLS;
- [ ] secret scan;
- [ ] dependency scan;
- [ ] tenant crossover test.

---

# 42. Commercial launch

Estratégia:

```text
internal
↓
demo
↓
1 pilot
↓
3–5 pilots
↓
10 customers
↓
50 customers
↓
scale
```

Ambiente demo separado de produção, somente com dados fictícios e resetáveis.

---

# 43. Planos SaaS

A arquitetura suporta:

```text
Solo
Start
Pro
Enterprise
```

Preços definitivos são decisão comercial e devem evoluir por `SaasPlanVersion`, nunca hardcoded no domínio.

---

# 44. Trial e onboarding

```text
landing
↓
signup
↓
email verification
↓
Organization
↓
trial
↓
onboarding
↓
dashboard
↓
checkout
↓
paid subscription
```

---

# 45. Status do projeto

Com base no planejamento confirmado:

```text
✅ v0.2 Core Domain — arquitetura definida
✅ v0.3 Security — arquitetura definida
✅ v0.4 Clinical — arquitetura definida
✅ v0.5 Finance & Fiscal — arquitetura definida
✅ v0.6 Packages — arquitetura definida
✅ v0.7 Notifications — arquitetura definida
✅ v0.8 LGPD — arquitetura definida
✅ v0.9 Frontend — informado como concluído

🟡 v1.0 Production Readiness
🟡 SaaS Commercial Layer
🟡 Production Audit

🚨 Implementação real deve ser confirmada no repositório.
```

---

# 46. Próxima fase

```text
v1.0 Production Audit
↓
v1.0.1 Hardening
↓
v1.0.2 Pilot
↓
v1.0.3 Commercial Launch
```

O foco agora passa a ser:

```text
prove
test
secure
deploy
observe
sell
```

e não adicionar features continuamente.

---

# 47. Critério de sucesso

O PsicoGest estará pronto para venda quando:

1. nenhum P0 permanecer;
2. integrações reais estiverem validadas;
3. tenant isolation estiver provado;
4. restore tiver sido executado;
5. produção estiver observável;
6. fluxos críticos tiverem E2E;
7. documentos legais estiverem revisados;
8. onboarding funcionar sem acesso manual ao banco;
9. billing SaaS estiver operacional;
10. 3–5 pilotos conseguirem usar o sistema de ponta a ponta.

---

# 48. Regra final

Não adicionar feature nova apenas porque parece interessante enquanto existirem bloqueadores de produção.

Prioridade:

```text
segurança
consistência
backup
observabilidade
onboarding
suporte
clientes reais
```

Depois disso, novas features devem ser guiadas principalmente por feedback de uso real.
