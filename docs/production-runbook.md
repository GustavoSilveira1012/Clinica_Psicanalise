# Runbook operacional de produção

Este runbook complementa o código e não substitui a aprovação do responsável de infraestrutura, segurança e LGPD. Os comandos abaixo devem ser executados somente em um ambiente identificado, com janela de manutenção e backup verificado.

## Variáveis de ambiente

Defina temporariamente, usando o secret manager do ambiente:

```powershell
$env:DATABASE_URL = "jdbc:postgresql://host:5432/psicogest"
$env:DATABASE_USERNAME = "psicogest_runtime"
$env:DATABASE_PASSWORD = "<secret manager>"
```

Não grave esses valores no Git, no histórico do shell ou em arquivos `.env` compartilhados.

## Backup

O script gera formato customizado do PostgreSQL, checksum SHA-256 e aplica retenção local:

```powershell
.\ops\backup-postgres.ps1 -OutputDirectory "D:\backups\psicogest\postgres" -RetentionDays 30
```

Copie o `.dump` e o `.sha256` para armazenamento imutável separado. O backup local não é suficiente para RPO/RTO de produção.

## Restore drill

O restore deve ser ensaiado primeiro em uma base isolada, com o mesmo major version do PostgreSQL:

```powershell
.\ops\restore-postgres.ps1 -BackupPath "D:\backups\psicogest\postgres\psicogest-YYYYMMDD-HHMMSS.dump" -Force -Confirm:$false
Invoke-WebRequest http://localhost:8080/health/ready
```

Depois valide login/MFA, isolamento entre duas organizações, agenda, prontuário, financeiro e consulta de migrations. Registre duração, RPO, RTO, falhas e responsável pelo exercício.

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
