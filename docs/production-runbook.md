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
```

Não grave esses valores no Git, no histórico do shell ou em arquivos `.env` compartilhados.

## Backup

O script aceita a URL JDBC da API, converte para o formato do cliente PostgreSQL,
gera dump customizado e checksum SHA-256. A exclusão por retenção é opt-in (`-PruneExpired`).

```powershell
.\ops\backup-postgres.ps1 -OutputDirectory "D:\backups\psicogest\postgres" -RetentionDays 30
```

Copie o `.dump` e o `.sha256` para armazenamento imutável separado. O backup local não é suficiente para RPO/RTO de produção.

## Restore drill

O restore deve ser ensaiado primeiro em uma base isolada, com o mesmo major version do PostgreSQL:

```powershell
.\ops\restore-postgres.ps1 -BackupPath "D:\backups\psicogest\postgres\psicogest-YYYYMMDD-HHMMSS.dump" -ExpectedDatabaseName "psicogest_restore_drill" -Force
Invoke-WebRequest http://localhost:8080/health/ready
```

Depois valide login/MFA, isolamento entre duas organizações, agenda, prontuário, financeiro e consulta de migrations. Registre duração, RPO, RTO, falhas e responsável pelo exercício.

Antes do comando, configure `DATABASE_URL` para a base **isolada** explicitamente
indicada por `ExpectedDatabaseName`. O restore exige manifesto SHA-256 e usa uma
única transação: erro no restore não deve deixar substituição parcial do banco.

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

- Faça rollback da imagem da API/frontend para o artefato anterior pelo pipeline.
- Não edite nem remova migration já aplicada.
- Para schema, prefira migration forward compatível; restaure backup somente com decisão formal e janela de manutenção.
- Após qualquer rollback, execute `/health/ready`, `flyway validate` e os testes smoke do fluxo clínico e financeiro.

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
