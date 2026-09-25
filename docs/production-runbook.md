# Runbook operacional de produção

Este runbook complementa o código e não substitui a aprovação do responsável de infraestrutura, segurança e LGPD. Os comandos abaixo devem ser executados somente em um ambiente identificado, com janela de manutenção e backup verificado.

Para organizar contas, fornecedores, responsáveis e homologações, use o
[roteiro de implantação inicial](implantacao-inicial.md) e o
[modelo de evidências](implantacao-evidencias-modelo.md). O roteiro distingue os
testes gratuitos com dados fictícios dos requisitos para uso clínico real.

## Variáveis de ambiente

Defina temporariamente, usando o secret manager do ambiente:

```powershell
$env:DATABASE_URL = "jdbc:postgresql://host:5432/psicogest"
$env:DATABASE_USERNAME = "psicogest_runtime"
$env:DATABASE_PASSWORD = "<secret manager>"
$env:BACKUP_AGE_RECIPIENT = "age1..." # chave pública X25519 para criptografia
# No restore drill, informe o caminho protegido da chave privada correspondente:
$env:BACKUP_AGE_IDENTITY_FILE = "D:\secrets\psicogest-backup-identity.txt"
```

Não grave esses valores no Git, no histórico do shell ou em arquivos `.env` compartilhados.
Instale PowerShell 7+, cliente PostgreSQL compatível e `age`. Guarde a identidade privada
fora do host de backup, com acesso restrito e cópia de recuperação independente; a chave
pública pode ser usada pelo job de backup. Perder a identidade privada torna os backups
irrecuperáveis.

## Backup

O script aceita a URL JDBC da API, converte para o formato do cliente PostgreSQL,
transmite o dump customizado diretamente a `age` (sem arquivo plaintext temporário)
e cria checksum SHA-256 do ciphertext. A exclusão por retenção é opt-in (`-PruneExpired`), protegida por confirmação `ShouldProcess`; use `-WhatIf` para simular. O manifesto SHA-256 é removido somente junto do backup correspondente.

```powershell
.\ops\backup-postgres.ps1 -OutputDirectory "D:\backups\psicogest\postgres" -RetentionDays 30
```

Para ensaiar o expurgo sem apagar arquivos, acrescente `-PruneExpired -WhatIf`. Ao executar de verdade, revise cada alvo da confirmação antes de aprovar.

Copie o `.dump.age` e o `.dump.age.sha256` para armazenamento separado com retenção
imutável/versionada. O backup local não é suficiente para RPO/RTO de produção.

### Exportação automatizada do projeto Supabase sintético

O workflow `Encrypted synthetic Supabase backup` é opcional e fica desligado até
a variável **do repositório** `ENABLE_SYNTHETIC_BACKUPS=true` (o agendamento é limitado
ao branch `main`). Ele usa o ambiente `synthetic-automation`, exige classificação
`SYNTHETIC_ONLY`, valida o project ref/host, faz `pg_dump` com TLS
verificado e transmite o dump diretamente para criptografia `age`. O artefato do
GitHub contém somente ciphertext e checksum e expira em sete dias. Para disparo manual,
digite `BACKUP-SYNTHETIC` como confirmação. Configure as credenciais como secrets e
o project ref, nome do banco (`postgres`), classificação e destinatário público `age`
como variables protegidas desse ambiente. Restrinja o ambiente ao branch `main`; não
configure aprovação manual nele se desejar execução realmente agendada. O ambiente
`synthetic-pilot` continua reservado ao rollback com aprovação.

Este workflow é apenas uma conveniência temporária para testes fictícios: artefatos do
GitHub não são backup imutável nem estratégia de recuperação de produção. O plano
Supabase Free não fornece backup gerenciado; mantenha exportações cifradas fora do
GitHub para qualquer retenção necessária e não use este workflow com dados reais. A
restauração permanece manual para um projeto Supabase de teste separado, após conferir
o checksum e a identidade do destino; nunca automatize restore sobre a origem.

## Restore drill

O restore deve ser ensaiado primeiro em uma base isolada, com o mesmo major version do PostgreSQL:

```powershell
.\ops\restore-postgres.ps1 -BackupPath "D:\backups\psicogest\postgres\psicogest-YYYYMMDD-HHMMSS.dump.age" -ExpectedDatabaseName "psicogest_restore_drill" -ExpectedHost "db-clone.example.invalid" -ExpectedPort 5432 -Force
Invoke-WebRequest http://localhost:8080/health/ready
```

Depois valide login/MFA, isolamento entre duas organizações, agenda, prontuário, financeiro e consulta de migrations. Registre duração, RPO, RTO, falhas e responsável pelo exercício.

Antes do comando, configure `DATABASE_URL` para a base **isolada** explicitamente
indicada por `ExpectedDatabaseName`, `ExpectedHost` e `ExpectedPort`. O restore exige manifesto SHA-256 e usa uma
única transação: erro no restore não deve deixar substituição parcial do banco. A
descriptografia de `age` é transmitida diretamente ao `pg_restore`; não é criado dump
plaintext intermediário. O script recusa entradas sem extensão criptografada e só toca
no banco dentro do `ShouldProcess` após a confirmação destrutiva explícita.

## Chaves e credenciais de produção

Use `production`, cookies HTTPS e origens HTTPS explícitas. Defina chaves distintas
de 32 bytes em Base64 para `MFA_ENCRYPTION_KEY`, `CLINICAL_KEK` e `AUDIT_HMAC_KEY`.
As duas últimas usam o identificador inicial `primary`. Para rotação, injete o
keyring completo por configuração externa, mantendo as chaves antigas para leitura
e alterando `current-key-id`; nunca substitua uma chave mantendo o mesmo identificador.
Faça backup separado das chaves: o dump sozinho não permite recuperar conteúdo cifrado.

`DATABASE_USERNAME` deve ser uma role de runtime `NOSUPERUSER NOBYPASSRLS`, com
privilégios mínimos. `MIGRATION_DATABASE_USERNAME` e `MIGRATION_DATABASE_PASSWORD`
são credenciais separadas, exclusivas da aplicação de migrations. O backend recusa
iniciar em produção com runtime capaz de ignorar RLS.

Os schedulers de ciclos de assinatura, inadimplência e retry de webhook ficam
desativados por padrão. Só defina `SCHEDULING_ENABLED=true` depois de homologar
os providers envolvidos, confirmar idempotência/retry e ativar alertas para filas.

Recuperação de senha e confirmação de e-mail usam SMTP somente quando
`AUTH_ACTION_MAIL_ENABLED=true`; em produção, `AUTH_ACTION_MAIL_REQUIRED_FOR_READINESS`
fica verdadeiro por padrão e a prontidão falha enquanto o adapter não estiver
disponível. Configure `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`,
`AUTH_ACTION_MAIL_FROM`, `AUTH_ACTION_PUBLIC_BASE_URL` (HTTPS) e uma chave Base64
de pelo menos 32 bytes em `AUTH_ACTION_RATE_LIMIT_KEY`. O rate limit usa HMAC para
não guardar IP/e-mail em claro nas chaves Redis. Faça sandbox com endereços
controlados antes de habilitar; os links usam fragmentos para que o token não vá
em access logs nem no cabeçalho Referer. O piloto sintético mantém a entrega desligada.

## Compatibilidade do histórico pré-release

A execução em banco vazio detectou SQL histórico inexequível (V11/V18/V25/V38/V76)
e pré-requisitos ausentes (V07.1/V13.1/V75.1). Esta revisão corrige a linha pré-release.
**Não aplique sobre uma instalação existente nem rode `flyway repair` automaticamente.**
Exporte primeiro `flyway_schema_history`, compare checksums e valide uma cópia do
banco existente. Migrations já aplicadas em produção devem permanecer imutáveis;
uma instalação com outro histórico precisa de um plano de upgrade específico.
`baseline-on-migrate` e `out-of-order` não são atalhos para essa validação.

As migrations V85 e V86 alinham os tipos de moeda e os campos `Instant` do JPA.
A V86 interpreta timestamps sem fuso já existentes como UTC. Antes de aplicá-la
a uma base populada, faça backup e confirme que os valores legados foram gravados
em UTC; se o ambiente anterior usou outro fuso, prepare e ensaie uma conversão
específica antes do deploy. Não rode a migration em produção até essa validação.

As V20/V26 agora recusam apagar prontuários/pagamentos legados. Havendo registros,
é obrigatória uma migração dedicada, com preservação dos vínculos, criptografia,
auditoria e reconciliação de contagens. Não desative essa proteção para liberar o deploy.

## Rollback de aplicação e schema

- Deploy normal do piloto: o blueprint Render publica os serviços somente após os checks do CI passarem (`autoDeployTrigger: checksPass`). Isso ainda precisa ser ensaiado no projeto sintético real.
- Rollback do piloto: configure no ambiente protegido do GitHub `synthetic-pilot` os segredos `RENDER_API_KEY`, `RENDER_API_SERVICE_ID` e `RENDER_WEB_SERVICE_ID`; proteja o ambiente com aprovador. Execute manualmente a ação `Roll back synthetic Render pilot`, selecione `api` ou `web`, informe um deploy bem-sucedido `dep-...` e digite `ROLLBACK-SYNTHETIC`. A automação desabilita o auto-deploy antes de chamar o endpoint de rollback e aguarda o deploy atingir `live`; depois, confira `/health/ready` e os smoke tests no ambiente.
- O rollback via API deixa o auto-deploy desabilitado. Reative-o manualmente só depois de corrigir a causa e revisar os checks. No plano gratuito, o Render retém somente dois deploys anteriores; confirme que o artefato desejado ainda existe.
- Não configure segredos nem execute rollback contra serviços reais nesta fase. Estes passos são somente para o projeto sintético aprovado.
- Não edite nem remova migration já aplicada.
- Para schema, prefira migration forward compatível; restaure backup somente com decisão formal e janela de manutenção.
- Após qualquer rollback, execute `/health/ready`, `flyway validate` e os testes smoke do fluxo clínico e financeiro.

## Health check do piloto sem fornecedor adicional

O workflow `Synthetic Render availability check` fica inativo até a variável **do
repositório** `ENABLE_SYNTHETIC_SMOKE=true` (execução somente no branch `main`). No
ambiente `synthetic-automation`, configure `SYNTHETIC_API_URL` e `SYNTHETIC_WEB_URL` com
os hosts HTTPS `.onrender.com` do projeto fictício. Ele consulta apenas `/health/live`,
`/health/ready` e a página inicial, uma vez por dia, e falha o workflow quando algum
endpoint não responde com HTTP 200. O GitHub deve estar configurado para notificar os
responsáveis por falhas de Actions. Restrinja `synthetic-automation` ao branch `main` e
não exija aprovação por execução agendada.
Esse check não coleta métricas, não mede SLO nem substitui monitoramento contratado;
em plano gratuito serve apenas como alarme básico de disponibilidade do ambiente de
teste. O acesso a Prometheus permanece autenticado e não deve ser exposto publicamente.

## Incidente

1. Preserve evidências e o correlation ID da requisição.
2. Restrinja o acesso da organização afetada sem apagar dados.
3. Verifique logs minimizados, eventos de segurança, fila/outbox, banco, Redis e providers.
4. Acione os sinks de alerta configurados e o responsável de plantão.
5. Documente impacto, dados potencialmente envolvidos, contenção e comunicação necessária.

## Checklist antes de inserir dados reais

- [ ] Backup imutável e restore drill aprovados.
- [ ] Monitoramento de erro, latência, banco, Redis e volume outbound ativo.
- [ ] JWT, MFA, banco, Redis e providers estão em secret manager.
- [ ] Providers de pagamento, NFS-e, e-mail e WhatsApp homologados em sandbox e produção.
- [ ] Teste cross-tenant e revisão independente de segurança concluídos.
- [ ] Política de privacidade, retenção, DPA, bases legais e responsáveis aprovados.
