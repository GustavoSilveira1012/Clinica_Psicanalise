# 🧠 PsicoGest — Documento do Projeto

**Tipo:** SaaS de gestão clínica/consultório  
**Domínio:** Psicanálise  
**Público inicial:** psicanalistas e equipes administrativas  
**Visão de longo prazo:** plataforma multi-tenant para clínicas de médio porte e redes de atendimento  
**Escopo do MVP:** uso interno pelos profissionais e equipe, sem portal do paciente

---

## 1. Visão do produto

O PsicoGest será uma plataforma de gestão para profissionais e organizações de psicanálise. O produto deve resolver o problema operacional de administrar agenda, pacientes, sessões, registros privados e financeiro sem depender de múltiplas ferramentas desconectadas.

A visão de produto considera desde o começo que uma clínica pode sair de **1 profissional / 1 unidade** para **dezenas de profissionais / múltiplas unidades**, sem precisar reescrever todo o núcleo do sistema.

### Proposta de valor

> **Centralizar a operação da clínica em um ambiente seguro, simples e escalável, mantendo a separação entre dados administrativos e informações clínicas privadas.**

### Não objetivos do MVP

- portal do paciente;
- marketplace de profissionais;
- diagnóstico automático;
- recomendação automatizada de tratamento;
- IA para interpretar sessões;
- prescrição ou gestão de medicamentos;
- integração com convênios;
- telemedicina/teleatendimento completo;
- cobrança online obrigatória.

A exclusão desses itens mantém o MVP pequeno e evita transformar o primeiro release em um monstro de 47 cabeças.

---

# 2. Personas

## 2.1 Psicanalista autônomo

Precisa visualizar a semana, cadastrar pacientes, registrar sessões, controlar pagamentos e consultar seu histórico de atendimento.

**Objetivo:** reduzir tarefas administrativas e recuperar rapidamente a informação necessária para o trabalho.

## 2.2 Gestor da clínica

Administra profissionais, permissões, agenda da organização, indicadores e financeiro.

**Objetivo:** enxergar a operação sem acessar indiscriminadamente conteúdo clínico.

## 2.3 Financeiro

Cuida de cobranças, recebimentos, repasses e relatórios financeiros.

**Objetivo:** trabalhar com dados financeiros sem acessar registros clínicos.

## 2.4 Assistente/recepção

Gerencia agenda e dados operacionais permitidos.

**Objetivo:** organizar horários e cadastros sem ter acesso aos registros clínicos privados.

---

# 3. Requisitos funcionais

## RF01 — Autenticação

O sistema deve permitir login seguro para usuários autorizados.

**Critérios de aceite**

- login válido cria sessão autenticada;
- credencial inválida não autentica;
- sessão expirada exige nova autenticação;
- recuperação de senha não revela se um e-mail existe;
- ações sensíveis exigem usuário autenticado.

## RF02 — Organização/Clínica

O sistema deve associar os dados operacionais a uma organização.

**Aceite**

- usuário pertence a uma organização;
- recursos são filtrados por organização;
- usuários não atravessam o limite de tenant;
- uma organização poderá ter múltiplas unidades futuramente.

## RF03 — Usuários e papéis

Administradores devem conseguir ativar/desativar usuários e atribuir papéis permitidos.

## RF04 — Pacientes

Permitir criar, visualizar, editar e desativar pacientes.

### Dados sugeridos

- nome;
- nome social, quando aplicável;
- data de nascimento;
- e-mail;
- telefone;
- observações administrativas;
- status;
- profissional responsável;
- datas de criação/atualização.

> Evitar coletar informação que não tenha finalidade definida.

## RF05 — Agenda

Permitir criar, editar, cancelar e listar compromissos.

### Regras

- compromisso possui profissional;
- possui data/hora de início e fim;
- possui status;
- pode estar associado a paciente;
- conflito de horário é rejeitado;
- cancelamento mantém histórico.

## RF06 — Sessão

Uma sessão representa o atendimento realizado ou planejado.

Campos de domínio:

- compromisso associado;
- paciente;
- profissional;
- status;
- data de realização;
- observação administrativa mínima;
- referência para registro clínico privado.

## RF07 — Registro clínico

O sistema deve permitir que usuários autorizados criem e consultem registros privados.

### Princípio

O conteúdo clínico é uma zona de alta sensibilidade e deve possuir autorização mais restritiva do que dados operacionais.

### MVP

- criar registro;
- visualizar registros próprios/autorizados;
- editar conforme regra definida;
- registrar autor e timestamps;
- auditar acesso/alteração.

### Evolução

Versionamento imutável, anexos, templates, assinatura digital e política de retenção definida pela organização.

## RF08 — Financeiro

Permitir criar lançamentos financeiros vinculados à operação.

### MVP

- lançamento previsto;
- vencimento;
- valor;
- status;
- data de pagamento;
- método de pagamento;
- observação financeira;
- vínculo opcional com sessão/cliente.

O módulo financeiro não deve receber o texto de registros clínicos.

## RF09 — Dashboard

Mostrar indicadores operacionais e financeiros conforme permissão do usuário.

## RF10 — Auditoria

Registrar ações críticas.

### Eventos iniciais

- login bem-sucedido/fracassado;
- alteração de permissão;
- leitura de registro clínico;
- criação/alteração de registro clínico;
- exclusão/desativação de paciente;
- alterações financeiras críticas;
- mudanças de configuração.

---

# 4. Requisitos não funcionais

## RNF01 — Segurança

- HTTPS em produção;
- cookies seguros e HttpOnly quando aplicável;
- proteção contra CSRF conforme mecanismo adotado;
- validação server-side;
- autorização server-side;
- rate limiting em endpoints sensíveis;
- secrets somente em secret manager/env seguro;
- headers de segurança;
- logs sem conteúdo clínico;
- backups e recuperação testados.

## RNF02 — Privacidade

A LGPD define dados de saúde como dados pessoais sensíveis e estabelece proteção reforçada para essa categoria. A arquitetura deve, portanto, adotar minimização, controle de acesso, finalidade e segurança desde o desenho. citehttps://www.gov.br/anpd/pt-br/acesso-a-informacao/perguntas-frequentes/perguntas-frequentes

## RNF03 — Escalabilidade

O MVP deve operar de forma simples, mas as fronteiras de domínio precisam permitir evolução para:

- múltiplas clínicas;
- múltiplas unidades;
- dezenas/centenas de usuários;
- jobs assíncronos;
- cache;
- storage externo;
- observabilidade centralizada.

## RNF04 — Disponibilidade

Para a evolução do produto, definir SLOs por serviço e estabelecer objetivos explícitos de RTO/RPO.

## RNF05 — Performance

Metas iniciais sugeridas para UX:

- navegação comum perceptivelmente rápida;
- consultas de lista paginadas;
- dashboard evitando queries pesadas sem necessidade;
- índices nas colunas usadas para tenant, profissional, paciente e datas.

---

# 5. Regras de negócio

## RB01 — Isolamento de organização

Toda entidade de negócio deve pertencer direta ou indiretamente a uma organização.

**Exemplo:**

`Organization → User → Patient/Appointment/Session/FinancialEntry`

Nenhuma consulta de aplicação deve buscar recurso multi-tenant sem filtro de organização quando esse filtro fizer parte da autorização.

## RB02 — Acesso clínico por menor privilégio

Um usuário financeiro pode acessar lançamento financeiro sem poder consultar texto clínico.

## RB03 — Profissional e paciente

Um profissional só consulta registros de pacientes aos quais possui autorização vigente.

## RB04 — Conflito de agenda

Um profissional não pode possuir dois compromissos ativos sobrepostos.

## RB05 — Cancelamento

Cancelar um compromisso não deve apagá-lo fisicamente; o status permanece para auditoria e métricas.

## RB06 — Financeiro

Valor monetário deve ser armazenado em tipo apropriado e com precisão definida pelo banco; não usar `float` para dinheiro.

## RB07 — Soft delete

Entidades importantes devem preferir desativação/arquivamento quando exclusão física prejudicar auditoria ou integridade histórica.

## RB08 — Auditoria

Alterações críticas precisam manter quem executou, quando, qual ação e, quando apropriado, qual recurso foi afetado.

---

# 6. Arquitetura detalhada

```text
                           ┌─────────────────────┐
                           │      Browser        │
                           └──────────┬──────────┘
                                      │ HTTPS
                           ┌──────────▼──────────┐
                           │      Next.js        │
                           │ App Router          │
                           │ Server Components   │
                           │ Route Handlers      │
                           │ Server Actions      │
                           └───────┬────┬────────┘
                                   │    │
                    ┌──────────────┘    └───────────────┐
                    ▼                                   ▼
          ┌──────────────────┐               ┌─────────────────┐
          │ Application Core │               │ External        │
          │ Auth / RBAC      │               │ Providers       │
          │ Validation       │               │ E-mail etc.     │
          │ Domain Services  │               └─────────────────┘
          └─────────┬────────┘
                    │ Prisma
                    ▼
          ┌──────────────────┐
          │   PostgreSQL     │
          │  multi-tenant    │
          └──────────────────┘
```

### Camadas

**UI** — componentes, telas, formulários e feedback.

**Application** — casos de uso, permissões e orquestração.

**Domain** — regras de negócio.

**Infrastructure** — Prisma, e-mail, storage e integrações.

Evitar concentrar tudo dentro de `page.tsx` ou em um único arquivo de API.

---

# 7. Modelo de dados

## Entidades principais

```text
Organization
 ├── OrganizationUser ── User
 ├── ClinicUnit
 ├── Patient
 ├── Appointment
 │      └── Session
 │             └── ClinicalRecord
 ├── FinancialEntry
 └── AuditLog
```

### Tabelas

#### `users`

- `id`
- `name`
- `email`
- `password_hash` ou vínculo de identidade do provedor
- `status`
- `created_at`
- `updated_at`

#### `organizations`

- `id`
- `name`
- `slug`
- `status`
- `created_at`
- `updated_at`

#### `organization_users`

- `id`
- `organization_id`
- `user_id`
- `role`
- `status`
- `created_at`

#### `clinic_units`

- `id`
- `organization_id`
- `name`
- `timezone`
- `status`

#### `patients`

- `id`
- `organization_id`
- `name`
- `preferred_name`
- `birth_date`
- `email`
- `phone`
- `status`
- `created_at`
- `updated_at`

#### `appointments`

- `id`
- `organization_id`
- `unit_id`
- `patient_id`
- `professional_id`
- `starts_at`
- `ends_at`
- `status`
- `notes_admin`
- `created_at`
- `updated_at`

#### `sessions`

- `id`
- `organization_id`
- `appointment_id`
- `patient_id`
- `professional_id`
- `status`
- `occurred_at`
- `created_at`

#### `clinical_records`

- `id`
- `organization_id`
- `session_id`
- `author_id`
- `content`
- `version`
- `created_at`
- `updated_at`

#### `financial_entries`

- `id`
- `organization_id`
- `patient_id`
- `appointment_id` nullable
- `type`
- `status`
- `amount`
- `due_date`
- `paid_at`
- `payment_method`
- `description`
- `created_at`

#### `audit_logs`

- `id`
- `organization_id`
- `actor_id`
- `action`
- `resource_type`
- `resource_id`
- `metadata`
- `ip_hash/technical_context` conforme política
- `created_at`

> O campo `metadata` deve ser usado com cuidado para não armazenar conteúdo clínico ou segredos.

---

# 8. DER conceitual

```text
USER 1────N ORGANIZATION_USER N────1 ORGANIZATION
                                      │
                    ┌─────────────────┼─────────────────┐
                    │                 │                 │
                    ▼                 ▼                 ▼
              CLINIC_UNIT         PATIENT          AUDIT_LOG
                    │                 │
                    │           ┌─────┴────────┐
                    │           │              │
                    ▼           ▼              ▼
                 APPOINTMENT ───────> SESSION ────> CLINICAL_RECORD
                    │                 │
                    └─────────────────┘
                                      │
                                      ▼
                              FINANCIAL_ENTRY
```

---

# 9. API / casos de uso

O projeto pode usar Server Actions para mutações internas e Route Handlers para endpoints que precisem de contrato HTTP explícito.

### Autenticação

```text
POST /api/auth/...
```

### Usuários

```text
GET    /api/users
POST   /api/users
PATCH  /api/users/:id
```

### Pacientes

```text
GET    /api/patients
POST   /api/patients
GET    /api/patients/:id
PATCH  /api/patients/:id
DELETE /api/patients/:id   # desativação lógica
```

### Agenda

```text
GET    /api/appointments
POST   /api/appointments
GET    /api/appointments/:id
PATCH  /api/appointments/:id
DELETE /api/appointments/:id
```

### Sessões

```text
POST   /api/sessions
GET    /api/sessions/:id
PATCH  /api/sessions/:id
```

### Registros clínicos

```text
GET    /api/clinical-records/:id
POST   /api/sessions/:id/clinical-records
PATCH  /api/clinical-records/:id
```

Rotas clínicas devem ter autorização mais restritiva que rotas administrativas.

### Financeiro

```text
GET    /api/finance
POST   /api/finance
PATCH  /api/finance/:id
```

---

# 10. RBAC

## Matriz inicial

| Recurso | OWNER | ADMIN | PSYCHOANALYST | FINANCE | ASSISTANT |
|---|---:|---:|---:|---:|---:|
| Dashboard geral | ✅ | ✅ | limitado | financeiro | operacional |
| Usuários | ✅ | ✅ | ❌ | ❌ | ❌ |
| Pacientes | ✅ | ✅ | ✅ próprios | limitado | ✅ básico |
| Agenda | ✅ | ✅ | ✅ | leitura limitada | ✅ |
| Sessões | ✅ | ✅ | ✅ próprias | ❌ | status limitado |
| Registros clínicos | ✅* | ✅* | ✅ próprios | ❌ | ❌ |
| Financeiro | ✅ | ✅ | conforme política | ✅ | ❌ |
| Auditoria | ✅ | ✅ | limitada | limitada | ❌ |

`*` O acesso do OWNER/ADMIN ao conteúdo clínico deve ser uma decisão explícita de governança; não assumir que administrador operacional precisa ler conteúdo clínico.

---

# 11. UX / páginas

## Autenticação

- `/login`
- `/forgot-password`
- `/reset-password`

## Aplicação

- `/dashboard`
- `/agenda`
- `/pacientes`
- `/pacientes/[id]`
- `/sessoes/[id]`
- `/financeiro`
- `/relatorios`
- `/usuarios`
- `/configuracoes`
- `/auditoria`

## Navegação

```text
PsicoGest
│
├── Dashboard
├── Agenda
├── Pacientes
├── Sessões
├── Financeiro
├── Relatórios
│
└── Administração
    ├── Usuários
    ├── Unidades
    ├── Configurações
    └── Auditoria
```

### Princípios visuais

- interface limpa e profissional;
- destaque para agenda e tarefas do dia;
- poucos elementos decorativos;
- estados vazios úteis;
- feedback após operações;
- confirmação para ações destrutivas;
- informação clínica claramente identificada como privada;
- responsividade para desktop e tablet.

---

# 12. Segurança por domínio

## Autenticação

- hash seguro quando houver senha própria;
- proteção contra brute force;
- recuperação de senha por token de uso único;
- invalidação de sessões quando necessário;
- MFA na evolução do produto.

## Autorização

A autorização deve ocorrer no servidor e seguir a combinação:

```text
Identity + Organization + Role + Resource + Action
```

Exemplo conceitual:

```ts
await assertCan(user, {
  organizationId,
  resource: 'clinical_record',
  action: 'read',
  resourceOwnerId: professionalId,
});
```

## Banco

- índices por `organization_id`;
- foreign keys;
- constraints de integridade;
- transações para operações críticas;
- migrations versionadas;
- backup automático no provedor de produção.

## Observabilidade

**Pode ir para logs:**

- request id;
- duração;
- status HTTP;
- erro técnico sanitizado;
- identificador técnico de recurso.

**Não deve ir para logs:**

- conteúdo de sessão;
- texto de registro clínico;
- tokens;
- senhas;
- dados completos de pagamento;
- informações sensíveis desnecessárias.

---

# 13. LGPD e governança de dados

A ANPD informa que dados de saúde são dados pessoais sensíveis e que o tratamento de dados pessoais depende de hipóteses legais aplicáveis. A arquitetura do PsicoGest deve ser tratada como **privacy by design**, com finalidade, necessidade, segurança e controle de acesso desde a concepção. [ANPD — Perguntas frequentes](https://www.gov.br/anpd/pt-br/acesso-a-informacao/perguntas-frequentes/perguntas-frequentes)

### Decisões de produto

1. Não criar campos clínicos genéricos só porque “pode ser útil”.
2. Separar dados operacionais de dados clínicos.
3. Restringir acesso a registros clínicos.
4. Auditar acessos críticos.
5. Utilizar dados fictícios no GitHub.
6. Não armazenar segredo no repositório.
7. Definir retenção antes do uso real.
8. Documentar incident response.

Para uma implantação real, a organização deve obter avaliação jurídica/compliance adequada ao contexto, aos profissionais envolvidos, aos fornecedores e às finalidades do tratamento.

---

# 14. Plano de implementação

## Sprint 0 — Fundação

- criar projeto Next.js;
- configurar TypeScript;
- Tailwind;
- ESLint/formatador;
- Docker Compose;
- PostgreSQL;
- Prisma;
- estrutura de pastas;
- `.env.example`;
- CI básica.

## Sprint 1 — Auth e organizações

- login;
- sessão;
- usuários;
- organização;
- roles;
- middleware/guards;
- seed.

## Sprint 2 — Pacientes

- CRUD;
- busca;
- filtros;
- paginação;
- tela de detalhes;
- permissões.

## Sprint 3 — Agenda

- calendário;
- criação de compromisso;
- edição/cancelamento;
- conflito de horários;
- filtros por profissional/unidade.

## Sprint 4 — Sessões e registros

- iniciar/finalizar sessão;
- registro privado;
- autorização clínica;
- auditoria.

## Sprint 5 — Financeiro

- lançamentos;
- status;
- filtros;
- fechamento;
- dashboard financeiro.

## Sprint 6 — Qualidade e deploy

- testes E2E;
- testes de autorização;
- acessibilidade básica;
- performance;
- documentação;
- deploy;
- dados demo.

---

# 15. Backlog do MVP

| ID | História | Prioridade | Status |
|---|---|---|---|
| US01 | Como usuário, quero fazer login | P0 | Todo |
| US02 | Como admin, quero gerenciar usuários | P0 | Todo |
| US03 | Como usuário, quero ver minha agenda | P0 | Todo |
| US04 | Como profissional, quero cadastrar paciente | P0 | Todo |
| US05 | Como profissional, quero consultar paciente | P0 | Todo |
| US06 | Como profissional, quero criar compromisso | P0 | Todo |
| US07 | Como sistema, quero impedir conflito de horário | P0 | Todo |
| US08 | Como profissional, quero registrar sessão | P0 | Todo |
| US09 | Como profissional, quero guardar registro privado | P0 | Todo |
| US10 | Como financeiro, quero lançar cobrança | P1 | Todo |
| US11 | Como gestor, quero visualizar indicadores | P1 | Todo |
| US12 | Como sistema, quero auditar ações críticas | P0 | Todo |
| US13 | Como admin, quero configurar unidade | P2 | Futuro |
| US14 | Como gestor, quero relatórios avançados | P2 | Futuro |

---

# 16. Estratégia de branches

```text
main
 │
 ├── develop
 │    ├── feature/auth
 │    ├── feature/patients
 │    ├── feature/schedule
 │    ├── feature/sessions
 │    └── feature/finance
 │
 └── hotfix/*
```

Para um projeto individual de portfólio, também é aceitável trabalhar apenas com `main` + branches de feature e Pull Requests.

### Conventional Commits

```text
feat: adiciona cadastro de pacientes
fix: corrige conflito de agenda
refactor: extrai política de autorização
test: cobre leitura de registro clínico
docs: atualiza documentação do MVP
chore: atualiza dependências
```

---

# 17. CI/CD

Pipeline inicial:

```text
Push / Pull Request
        ↓
Install
        ↓
Lint
        ↓
Typecheck
        ↓
Unit tests
        ↓
Build
        ↓
E2E
        ↓
Deploy
```

Em produção:

- migrations controladas;
- secrets separados por ambiente;
- rollback documentado;
- monitoramento de erros;
- backups;
- teste periódico de restauração.

---

# 18. Evolução para médio porte

## Multi-tenant

Desde o início, adicionar `organization_id` às entidades relevantes e centralizar a autorização. Isso permite hospedar várias clínicas sem misturar dados.

## Multiunidade

Adicionar `clinic_unit_id` em agenda e recursos relacionados.

## Cache

Introduzir cache apenas em leituras que demonstrem necessidade, como dashboards e configurações de baixa mutação.

## Jobs assíncronos

Mover para fila:

- envio de notificações;
- geração de relatórios;
- exports grandes;
- tarefas de limpeza;
- processamento de arquivos.

## Storage

Arquivos não devem ficar no banco sem uma razão clara. Quando houver anexos, usar storage apropriado com URLs assinadas e autorização.

## Observabilidade

Adicionar:

- logs estruturados;
- métricas;
- tracing;
- error tracking;
- alertas;
- dashboards operacionais.

## Feature flags

Permitir ativação gradual de funcionalidades por organização.

---

# 19. Monetização futura

O produto pode evoluir para SaaS B2B com planos por organização.

### Exemplo conceitual

**Solo**

- 1 profissional;
- agenda;
- pacientes;
- sessões;
- financeiro básico.

**Clinic**

- múltiplos profissionais;
- permissões;
- relatórios;
- unidades;
- repasses.

**Enterprise**

- multiunidade;
- SSO;
- auditoria avançada;
- políticas personalizadas;
- suporte prioritário;
- contratos empresariais.

Os preços não fazem parte do MVP porque precisam ser validados com mercado, custos e posicionamento.

---

# 20. Riscos

| Risco | Impacto | Mitigação |
|---|---|---|
| Vazamento de dados clínicos | Crítico | RBAC, minimização, auditoria, criptografia e revisão de segurança |
| IDOR/BOLA | Crítico | autorização por tenant + recurso em todas as leituras/mutações |
| Crescimento de escopo | Alto | manter MVP fechado |
| Queries lentas | Médio | índices, paginação, profiling |
| Logs com dados sensíveis | Alto | logger sanitizado |
| Dependência externa indisponível | Médio | retries, timeout, fallback |
| Migração quebrada | Alto | migrations revisadas + backup + staging |
| Uso de dados reais no GitHub | Crítico | dataset fictício e revisão antes do push |

---

# 21. Definition of Done

Uma funcionalidade só está “pronta” quando:

- funciona pelo fluxo da interface;
- possui validação;
- possui autorização;
- trata erros;
- possui teste relevante;
- não vaza dados sensíveis;
- segue padrão de código;
- está documentada quando necessário;
- foi validada com dados fictícios;
- passa CI.

---

# 22. Checklist do MVP antes de publicar no GitHub

```text
[ ] README completo
[ ] PROJETO.md completo
[ ] LICENSE
[ ] .env.example
[ ] .gitignore correto
[ ] seed apenas com dados fictícios
[ ] nenhum segredo versionado
[ ] nenhum dado real de paciente
[ ] autenticação
[ ] autorização/RBAC
[ ] isolamento por organização
[ ] pacientes
[ ] agenda
[ ] sessões
[ ] registros privados
[ ] financeiro básico
[ ] dashboard
[ ] auditoria
[ ] testes unitários
[ ] testes E2E
[ ] CI
[ ] Docker local
[ ] deploy de demonstração
[ ] screenshots sem dados pessoais
```

---

# 23. Critério de sucesso do portfólio

O projeto terá maior força como portfólio quando o recrutador conseguir enxergar, em poucos minutos:

1. **produto:** problema real e usuários claros;
2. **engenharia:** arquitetura organizada;
3. **backend:** regras de negócio e autorização reais;
4. **banco:** modelagem relacional coerente;
5. **frontend:** dashboard e fluxos usáveis;
6. **qualidade:** testes e CI;
7. **segurança:** tenant isolation, RBAC e auditoria;
8. **maturidade:** roadmap de MVP → médio porte;
9. **deploy:** aplicação demonstrável;
10. **documentação:** decisões técnicas explicadas.

Esse conjunto faz o projeto parecer menos “CRUD de curso” e mais um produto SaaS pensado por alguém que entende desenvolvimento, arquitetura e operação.

---

# 24. Referências técnicas

- Next.js — https://nextjs.org/docs
- Auth.js — https://authjs.dev/
- Prisma ORM — https://www.prisma.io/docs/
- PostgreSQL — https://www.postgresql.org/docs/
- ANPD / LGPD — https://www.gov.br/anpd/

## Nota sobre versões

Evite fixar versões de framework no documento de produto. As versões exatas devem viver no `package.json` e no lockfile, com atualização controlada. A documentação atual do Prisma, por exemplo, já descreve Prisma ORM 8 e requisitos modernos de Node.js; isso reforça a ideia de manter o README orientado à arquitetura e o `package.json` como fonte da verdade para versões. [Prisma](https://www.prisma.io/docs/prisma-orm/quickstart/postgresql)
