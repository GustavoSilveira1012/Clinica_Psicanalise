# PsicoGest / Clínica

Plataforma SaaS de gestão para profissionais e clínicas, com foco inicial em psicanalistas e clínicas de psicologia/psicanálise.

> **Status atual:** arquitetura definida até a v1.0, frontend informado como concluído e fase de **Production Audit / Hardening** em andamento.  
> Este README descreve a arquitetura planejada e o estado informado do projeto; produção ainda depende de validação real do repositório, integrações, infraestrutura, testes, homologações e restore.

## Visão do produto

O PsicoGest reúne:

- agenda e recorrência de atendimentos;
- pacientes e relacionamento terapêutico;
- prontuário clínico seguro e auditável;
- financeiro, pagamentos, estornos e saldo credor;
- conciliação bancária;
- emissão fiscal / NFS-e;
- pacotes e assinaturas de sessões;
- notificações;
- LGPD e compliance;
- multiusuário e multi-organização;
- camada SaaS com planos, trial, billing e onboarding.

A arquitetura separa cuidadosamente os domínios **clínico, administrativo, financeiro, fiscal e SaaS**.

## Stack principal

### Backend
- Java
- Spring Boot
- Spring Security
- Spring Data JPA
- PostgreSQL
- Redis
- Flyway
- Testcontainers
- JWT / OAuth2 Resource Server

### Frontend
- React
- TypeScript
- Tailwind CSS
- React Router
- TanStack Query
- React Hook Form
- Zod

### Produção planejada
- PostgreSQL privado
- Redis privado
- Object Storage privado
- KMS / Secret Manager
- WAF / CDN / proteção DDoS
- TLS
- backups com PITR
- observabilidade e alertas
- staging + production
- CI/CD com validações de segurança

## Princípios arquiteturais

### Clínica não é dona do prontuário
O prontuário pertence ao contexto clínico do paciente e do profissional. `clinic_id` pode existir como contexto administrativo/auditável, mas não como conceito de propriedade clínica.

### Payment != ServiceInvoice
`Receivable` representa obrigação, `Payment` representa dinheiro e `ServiceInvoice` representa documento fiscal. São eventos distintos.

### Organization != Clinic
Na camada SaaS:
- `Organization` = fronteira técnica de tenant;
- `Clinic` = entidade/unidade operacional.

Uma organização pode representar profissional autônomo, clínica ou rede.

### Ledgers são append-only
Fluxos financeiros, créditos de sessões, auditoria e históricos sensíveis evitam reescrita retroativa. Correções são novos eventos: refund, credit entry, adjustment, addendum, revision, reversal ou substitution.

### Segurança por contexto
Role sozinho não concede acesso. A autorização considera usuário, organização, clínica, paciente, profissional, relacionamento terapêutico, contexto financeiro e recurso.

## Roadmap funcional

### v0.2 — Core Domain
- User
- Patient
- Psychoanalyst
- Clinic
- ClinicMembership / ClinicMembershipPeriod
- Availability / AvailabilityException
- Appointment
- AppointmentSeries
- recorrência e rescheduling com histórico
- TherapeuticRelationship
- lifecycle / soft delete
- Testcontainers

### v0.3 — Security & Cyber Defense
- JWT RSA
- refresh token opaco e rotativo
- session management
- MFA/TOTP
- recovery codes
- rate limiting
- brute-force protection
- SecurityEvent / SecurityAlert
- autorização contextual
- AuditLog imutável com HMAC chain
- AES-256-GCM / envelope encryption
- KMS-ready
- incident response
- backup/restore architecture

Regra crítica:

```text
SYSTEM_ADMIN != acesso automático a dados clínicos
```

### v0.4 — Clinical
- MedicalRecord
- DRAFT / FINALIZED
- MedicalRecordRevision
- MedicalRecordAddendum
- autoria imutável
- criptografia por registro
- AuditLog em leitura/escrita sensível
- Clinical Timeline
- Clinical Export seguro

### v0.5 — Finance & Fiscal

Finance:
- FinancialEntity
- Receivable
- Payment / PaymentAllocation
- Refund / RefundAllocation
- CreditAccount / CreditEntry
- ReceivableAdjustment
- PaymentProvider abstraction
- webhook inbox / replay protection
- BankAccount / BankTransaction
- Bank Reconciliation
- ProviderSettlement / gateway fees / chargebacks

Fiscal:
- FiscalIssuer
- FiscalConfiguration versionada
- ServiceInvoice
- TaxSnapshot
- DPS
- FiscalProvider
- arquitetura NFS-e Nacional
- idempotência fiscal
- cancelamento / substituição
- XML / DANFSE
- storage privado e integridade por hash

### v0.6 — Packages & Subscriptions
- PackagePlan / PackagePlanVersion / PackagePlanItem
- PatientPackage / PatientPackageItem
- SessionCredit ledger
- PackageConsumption
- FEFO
- expiration / reversal
- NO_SHOW / late cancellation policies
- ativação via pagamento
- cancelamento com Refund ou CreditBalance
- SubscriptionPlan / SubscriptionPlanVersion
- PatientSubscription / SubscriptionCycle
- recurring payment abstraction
- billing anchor
- rollover
- pause/resume
- cancel at period end

Saldo de sessões é derivado do ledger, não salvo como contador mutável.

### v0.7 — Notifications
- Notification
- Recipient
- Delivery
- Templates versionados
- EMAIL / WHATSAPP / SMS-ready
- provider abstraction
- retries / backoff / dead-letter
- idempotência
- webhook tracking
- quiet hours
- preferences
- consent-ready eligibility
- outbound rate limiting
- mass-send detection

### v0.8 — LGPD & Compliance
- ProcessingActivity
- LegalBasis
- PrivacyNotice versionado
- ConsentRecord
- DataSubjectRequest
- export / correction / deletion / blocking / anonymization
- RetentionPolicy
- DataDisposalJob
- LegalHold
- DataRecipient
- registro de transferências internacionais
- privacy incident assessment
- privacy contacts
- RIPD/DPIA registry

Código ajuda a operar LGPD, mas **não substitui revisão jurídica**.

### v0.9 — Frontend
Áreas previstas/concluídas no projeto:
- login / MFA / sessão
- dashboard
- agenda
- pacientes
- prontuário
- financeiro
- fiscal
- pacotes / assinaturas
- notificações
- LGPD
- configurações
- permissões
- responsividade
- estados de loading/error/empty
- integração tipada com API

A integração real com backend precisa ser confirmada pelo Production Audit.

### v1.0 — SaaS & Production Readiness
- Organization
- OrganizationMembership
- tenant isolation
- PostgreSQL RLS
- SaaSPlan / SaaSPlanVersion
- SaasFeature / Entitlements / Limits
- trial
- SaasSubscription
- SaaS Billing
- Usage tracking
- Feature Flags
- onboarding
- support access auditado
- CI/CD
- observabilidade
- backup / restore
- disaster recovery

## Tenant isolation

Arquitetura alvo:

```text
Application Authorization
+
Tenant Context
+
PostgreSQL Row Level Security
```

O usuário da aplicação não deve possuir `BYPASSRLS`.

**Tenant crossover é bloqueador de release.**

## Billing SaaS

O billing do próprio PsicoGest é separado do financeiro das clínicas:

```text
Paciente → Clínica
Receivable / Payment
```

é diferente de:

```text
Clínica → PsicoGest
SaasInvoice / SaasPayment
```

## Production Audit

Antes da venda, o repositório deve ser confrontado com esta arquitetura e cada item classificado como:

```text
IMPLEMENTADO ✅
PARCIAL 🟡
FALTANDO ❌
BLOQUEADOR DE VENDA 🚨
```

O audit deve cobrir backend, frontend, migrations, auth, MFA, tenant isolation, RLS, clinical authorization, finance, fiscal, providers, notifications, LGPD, SaaS, CI/CD, backups, restore, observability, security tests e E2E.

## GO LIVE mínimo

- [ ] frontend/backend integrados;
- [ ] mocks removidos dos fluxos críticos;
- [ ] migrations validadas;
- [ ] tenant isolation comprovado;
- [ ] MFA funcionando;
- [ ] password reset;
- [ ] email verification;
- [ ] rate limiting;
- [ ] providers externos homologados;
- [ ] NFS-e homologada ou desativada;
- [ ] KMS/Secret Manager real;
- [ ] PostgreSQL privado;
- [ ] Redis privado;
- [ ] storage privado;
- [ ] WAF/TLS;
- [ ] backups;
- [ ] PITR;
- [ ] restore comprovado;
- [ ] observabilidade;
- [ ] alertas;
- [ ] pentest;
- [ ] termos de uso;
- [ ] política de privacidade;
- [ ] revisão LGPD;
- [ ] billing SaaS;
- [ ] onboarding;
- [ ] ambiente demo;
- [ ] suporte;
- [ ] piloto controlado.

## Estratégia de lançamento

```text
Internal
↓
Demo
↓
1 cliente piloto
↓
3–5 pilotos
↓
10 clientes
↓
50 clientes
↓
escala
```

## Status

```text
✅ v0.2 Core Domain — arquitetura definida
✅ v0.3 Security — arquitetura definida
✅ v0.4 Clinical — arquitetura definida
✅ v0.5 Finance & Fiscal — arquitetura definida
✅ v0.6 Packages & Subscriptions — arquitetura definida
✅ v0.7 Notifications — arquitetura definida
✅ v0.8 LGPD & Compliance — arquitetura definida
✅ v0.9 Frontend — informado como concluído

🟡 v1.0 Production Readiness
🟡 SaaS Commercial Layer
🟡 Production Audit

🚨 Venda em produção depende do fechamento dos bloqueadores encontrados no audit.
```
