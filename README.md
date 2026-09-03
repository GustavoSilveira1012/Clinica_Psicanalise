[README.md](https://github.com/user-attachments/files/31813081/README.md)
# 🧠 PsicoGest

> Plataforma SaaS para gestão profissional de clínicas e consultórios de psicanálise, pensada para crescer de um MVP de portfólio para uma operação multiunidade de médio porte.



## 📌 Sobre o projeto

O **PsicoGest** é um sistema web de gestão para profissionais de psicanálise e para clínicas que precisam centralizar sua operação em um único ambiente.
O objetivo não é criar apenas uma agenda. A proposta é modelar um produto SaaS com visão de negócio, arquitetura escalável, controle de acesso, trilha de auditoria, gestão financeira, prontuário/fichas e indicadores.

> **Escopo inicial:** acesso restrito aos profissionais e à equipe administrativa. O MVP não possui portal externo do paciente.

## 🎯 Problema

Profissionais e clínicas podem espalhar sua operação entre agenda, planilhas, aplicativos de mensagem, documentos locais e ferramentas financeiras. Isso gera retrabalho, baixa rastreabilidade, risco de exposição de informações e dificuldade para enxergar a operação como um todo.
O PsicoGest pretende resolver isso oferecendo:

- agenda e disponibilidade;
- cadastro e histórico de pacientes;
- registros clínicos privados;
- gestão de sessões;
- controle financeiro;
- dashboards operacionais;
- permissões por função;
- auditoria de ações críticas;
- arquitetura preparada para múltiplos profissionais e unidades.

## 🧩 Módulos

| Módulo             | MVP          | Futuro                             |
| ------------------ | ------------ | ---------------------------------- |
| Autenticação       | ✅            | MFA/SSO                            |
| Usuários e perfis  | ✅            | SCIM/SSO corporativo               |
| Pacientes          | ✅            | Portal do paciente                 |
| Agenda             | ✅            | Integrações com calendários        |
| Sessões            | ✅            | Teleatendimento                    |
| Registros clínicos | ✅            | Templates e versionamento avançado |
| Financeiro         | ✅ básico     | Repasse, faturamento e conciliação |
| Dashboard          | ✅ básico     | BI avançado                        |
| Auditoria          | ✅            | SIEM/exportação                    |
| Multiunidade       | 🟡 estrutura | ✅                                  |
| Notificações       | 🟡 internas  | E-mail/SMS/WhatsApp via provedor   |
| Assinaturas SaaS   | ❌            | ✅                                  |

## 🛠️ Stack

A implementação recomendada é:

- **Next.js + App Router** — aplicação full stack em um único repositório.
- **TypeScript** — tipagem estática de ponta a ponta.
- **PostgreSQL** — banco relacional para dados transacionais.
- **Prisma ORM** — acesso tipado ao PostgreSQL.
- **Auth.js** — autenticação e sessões.
- **Tailwind CSS** — camada visual rápida e consistente.
- **Zod** — validação de entradas.
- **React Hook Form** — formulários complexos.
- **Vitest + Testing Library** — testes unitários/componentes.
- **Playwright** — testes E2E.
- **Docker Compose** — ambiente local reproducível.
- **GitHub Actions** — CI.

A escolha de PostgreSQL + Prisma é especialmente adequada para entidades relacionais como clínicas, profissionais, pacientes, sessões, cobranças e auditoria; Prisma possui suporte oficial para PostgreSQL e também para provedores gerenciados como Supabase e Neon. [Prisma PostgreSQL](https://docs.prisma.io/docs/orm/core-concepts/supported-databases/postgresql)

## 🏗️ Arquitetura

```
┌─────────────────────────────────────────────┐
│                Browser / PWA                 │
└──────────────────────┬──────────────────────┘
                       │ HTTPS
┌──────────────────────▼──────────────────────┐
│                 Next.js                      │
│  App Router · Server Components · Actions   │
│  Route Handlers · Middleware/Auth            │
└───────────┬───────────────────┬─────────────┘
            │                   │
            ▼                   ▼
      ┌───────────┐       ┌──────────────┐
      │ PostgreSQL│       │ External APIs│
      │ + Prisma  │       │ e-mail etc.  │
      └───────────┘       └──────────────┘
```

### Princípios arquiteturais

1. **Server-first:** operações sensíveis acontecem no servidor.
2. **RBAC:** permissões dependem do papel do usuário e do contexto da clínica.
3. **Tenant-aware:** todos os dados pertencem a uma organização/clínica.
4. **Auditável:** eventos críticos são registrados.
5. **Least privilege:** cada usuário recebe apenas o acesso necessário.
6. **Privacy by design:** dados privados não são enviados ao cliente sem necessidade.
7. **Observabilidade:** logs técnicos não devem conter conteúdo clínico.

## 📂 Estrutura inicial

```
psicogest/
├── .github/
│   └── workflows/
├── prisma/
│   ├── schema.prisma
│   └── seed.ts
├── public/
├── src/
│   ├── app/
│   │   ├── (auth)/
│   │   ├── (dashboard)/
│   │   └── api/
│   ├── components/
│   ├── features/
│   │   ├── patients/
│   │   ├── appointments/
│   │   ├── sessions/
│   │   ├── clinical-records/
│   │   └── finance/
│   ├── lib/
│   │   ├── auth/
│   │   ├── db/
│   │   ├── permissions/
│   │   └── validation/
│   └── types/
├── tests/
│   ├── unit/
│   └── e2e/
├── .env.example
├── docker-compose.yml
├── next.config.ts
├── package.json
└── README.md
```

## 🚀 MVP

O MVP tem como objetivo entregar uma operação funcional para uma clínica pequena ou para um profissional que trabalha sozinho.

### Fluxo principal

```
Login
  ↓
Dashboard
  ├── Agenda
  │    └── Criar/editar sessão
  ├── Pacientes
  │    └── Histórico + registros
  ├── Financeiro
  │    └── Lançamentos/cobranças
  └── Configurações
       └── Usuários/permissões
```

### Critérios de pronto do MVP

- autenticação funcionando;
- autorização por papel funcionando;
- CRUD de pacientes;
- agenda com prevenção de conflito de horário;
- registro de sessão;
- ficha/registros privados;
- lançamento financeiro;
- dashboard com indicadores básicos;
- logs de auditoria para ações críticas;
- testes automatizados dos fluxos principais;
- documentação para rodar localmente;
- seed com dados fictícios;
- projeto sem dados reais de pacientes.

## 🔐 Segurança e privacidade

O sistema foi desenhado para tratar informações potencialmente sensíveis. A LGPD classifica dados referentes à saúde como dados pessoais sensíveis e prevê hipóteses específicas para seu tratamento; por isso, o projeto deve minimizar coleta, restringir acesso, registrar ações críticas e separar dados clínicos de logs técnicos. [ANPD](https://www.gov.br/anpd/pt-br/acesso-a-informacao/perguntas-frequentes/perguntas-frequentes)
No repositório público:

- **não usar dados reais**;
- usar apenas dados fictícios no seed;
- nunca commitar `.env` ou segredos;
- não registrar conteúdo de sessão em logs;
- não colocar prontuários dentro de screenshots do README;
- aplicar autorização no servidor, não apenas esconder botões;
- considerar criptografia em repouso e em trânsito no ambiente de produção;
- documentar política de retenção e descarte antes de usar dados reais;
- aplicar backup, restauração e testes de recuperação em produção.

> Este projeto é um case técnico/educacional e não deve ser considerado, por si só, uma certificação de conformidade regulatória.

## 👥 Perfis de acesso

| Perfil                  | Exemplo de acesso                                    |
| ----------------------- | --------------------------------------------------- |
| `OWNER`                 | Toda a clínica/organização                            |
| `ADMIN`                 | Gestão operacional, usuários e financeiro             |
| `PSYCHOANALYST`         | Próprios pacientes, agenda e registros autorizados    |
| `FINANCE`               | Financeiro sem conteúdo clínico                       |
| `ASSISTANT`             | Agenda e cadastro operacional, sem registros clínicos |

## 📊 Indicadores do dashboard

**Operacionais**

- sessões de hoje;
- próximas sessões;
- pacientes ativos;
- faltas/cancelamentos;
- ocupação da agenda.

**Financeiros**

- previsto no mês;
- recebido no mês;
- em aberto;
- inadimplência;
- receita por profissional.

**Gestão**

- profissionais ativos;
- unidades;
- crescimento de pacientes;
- taxa de comparecimento.

## 🧪 Qualidade

O projeto deve nascer com três camadas de teste:

```
Unit        → regras de negócio
Integration → banco + serviços
E2E         → fluxos reais do usuário
```

Exemplos prioritários:

- usuário sem permissão não consegue ler registro clínico;
- profissional não consegue acessar paciente de outro tenant;
- duas sessões não podem ocupar o mesmo horário do profissional;
- sessão cancelada não pode gerar cobrança indevida;
- exclusão/desativação respeita regras de auditoria;
- financeiro não recebe conteúdo clínico.

## 🗺️ Roadmap

### Fase 1 — MVP

Auth, RBAC, pacientes, agenda, sessões, registros, financeiro básico, dashboard e auditoria.

### Fase 2 — Produto validável

Notificações, templates, filtros avançados, exportações, relatórios, configurações de clínica e melhorias de UX.

### Fase 3 — Empresa média

Multiunidade, repasses, centro de custos, planos/assinaturas, integrações, observabilidade, filas e cache.

### Fase 4 — Escala

Arquitetura modular, processamento assíncrono, storage dedicado, read replicas quando necessário, feature flags, SLOs e governança de dados.

## 💻 Instalação local

### Pré-requisitos

- Node.js LTS compatível com a versão definida no `package.json`;
- Docker + Docker Compose;
- Git.

### 1. Clonar

```
git clone https://github.com/SEU-USUARIO/psicogest.git
cd psicogest
```

### 2. Configurar ambiente

```
cp .env.example .env
```

Preencha as variáveis sem publicar o arquivo `.env`.

### 3. Subir PostgreSQL

```
docker compose up -d postgres
```

### 4. Instalar dependências

```
npm install
```

### 5. Preparar banco

```
npx prisma migrate dev
npm run db:seed
```

### 6. Rodar

```
npm run dev
```

Abra `http://localhost:3000`.

## 🔑 Contas de demonstração

Use somente credenciais fictícias criadas pelo seed, por exemplo:

```
admin@demo.local
psicanalista@demo.local
finance@demo.local
```

As senhas devem ser definidas por variáveis de ambiente ou pelo seed local; não publique senhas reais.

## 📜 Scripts esperados

```
{
  "dev": "next dev",
  "build": "next build",
  "start": "next start",
  "lint": "eslint .",
  "test": "vitest run",
  "test:e2e": "playwright test",
  "db:migrate": "prisma migrate dev",
  "db:seed": "tsx prisma/seed.ts",
  "db:studio": "prisma studio"
}
```

## 🌐 Deploy sugerido

Para portfólio, uma configuração simples é:

```
GitHub
  ↓
Vercel / plataforma Next.js
  ↓
PostgreSQL gerenciado
```

Para uma evolução empresarial, separar claramente aplicação, banco, storage, observabilidade e serviços externos.

## 📚 Documentação complementar

Consulte [PROJETO.md](./PROJETO.md) para requisitos, casos de uso, modelo de dados, regras de negócio, segurança, roadmap, riscos e critérios de aceite.

## 🤝 Contribuição

1. Crie uma branch `feature/nome-da-feature`.
2. Faça commits pequenos e descritivos.
3. Rode lint e testes antes do push.
4. Abra um Pull Request descrevendo problema, solução e impactos.

## 📄 Licença

MIT. Consulte `LICENSE` para o texto completo.

---

**PsicoGest** — tecnologia para tornar a operação de uma clínica mais organizada, rastreável e preparada para crescer.
