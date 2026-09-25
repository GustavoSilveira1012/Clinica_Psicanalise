# Registro de implantação e evidências — modelo

Copiar para local privado de operação. Este arquivo é um modelo vazio: nenhuma etapa está aprovada. Não inserir credenciais, chaves, dumps, prontuários ou dados identificáveis no Git. Evidências sensíveis ficam em local restrito; registrar aqui apenas referência não sensível, se apropriado.

## Identificação

- Ambiente e finalidade: pendente.
- Responsável técnico principal e substituto: pendente.
- Responsável da clínica e aprovadores: pendente.
- Base original identificada e uso confirmado: pendente.
- Dados utilizados no teste: pendente; primeira implantação deve usar dados fictícios.
- Versão/commit da aplicação: pendente.
- Provedores, planos, regiões e custo aprovado: pendente.
- Data, início e término com fuso: pendente.

## Controle das etapas

Estados: PENDENTE, EM EXECUÇÃO, REPROVADO ou APROVADO. Aprovação exige evidência, responsável, data e escopo.

| Item | Estado | Executor | Aprovador/data | Referência da evidência |
| --- | --- | --- | --- | --- |
| Base original e autorização para cópia identificadas | PENDENTE | — | — | — |
| Storage privado, criptografia, retenção e recuperação | PENDENTE | — | — | — |
| Banco/Redis, TLS, rede, proteção de entrada e segredos | PENDENTE | — | — | — |
| Roles de runtime/migrations e isolamento entre organizações | PENDENTE | — | — | — |
| Backup, recuperação das chaves e restore drill | PENDENTE | — | — | — |
| PITR e RPO/RTO medidos | PENDENTE | — | — | — |
| Histórico Flyway e upgrade ensaiado no clone | PENDENTE | — | — | — |
| Validação dos timestamps V86 | PENDENTE | — | — | — |
| DNS/HTTPS, alertas, logs e suporte | PENDENTE | — | — | — |
| NFS-e, certificado e homologação contábil | PENDENTE | — | — | — |
| Pagamentos e callbacks | PENDENTE | — | — | — |
| E-mail e WhatsApp | PENDENTE | — | — | — |
| Privacidade, retenção, DPA e incidentes | PENDENTE | — | — | — |
| Revisão independente, correções e reteste | PENDENTE | — | — | — |
| Escopo e aceite do piloto | PENDENTE | — | — | — |

## Relatório do restore drill

- Meta de RPO e aprovador: pendente.
- Meta de RTO e aprovador: pendente.
- Conta/servidor/banco de origem e destino conferidos em inventário privado: pendente.
- Destino isolado, autorização e bloqueio de chamadas reais comprovados: pendente.
- Versões do PostgreSQL e ferramentas: pendente.
- Método: dump / snapshot / PITR — pendente.
- Backup escolhido, data, checksum e referência segura: pendente.
- Chaves necessárias disponíveis e recuperação testada: pendente.
- Instante do incidente simulado: pendente.
- Último dado confirmado recuperado: pendente.
- RPO observado (diferença entre os dois instantes): pendente.
- Início/fim de provisionamento, restore e validação: pendente.
- Serviço validado/liberado às: pendente.
- RTO observado (desde declaração do incidente até liberação): pendente.
- Para PITR: horário-alvo, marcador anterior presente/posterior ausente: pendente.
- Roles, owners, grants e RLS após restore: pendente.
- Objetos/arquivos recuperados e integridade validada: pendente.
- Leitura cifrada, MFA, agenda, prontuário, financeiro e isolamento: pendente.
- Falhas, correções e reteste: pendente.
- Cumpriu as metas? Quem aprovou e quando? Pendente.
- Destino das evidências e descarte controlado do clone: pendente.

## Relatório do upgrade

- Histórico e checksums antes/depois: pendente.
- Migrations divergentes e plano específico: pendente.
- V11/V18/V25/V38/V76 e V07.1/V13.1/V75.1 revisadas: pendente.
- V20/V26: presença de registros legados, preservação e reconciliação: pendente.
- V85: reconciliação de moedas e valores: pendente.
- V86: prova do fuso histórico, datas/amostras desidentificadas e resultado esperado: pendente.
- Contagens, vínculos, valores e instantes antes/depois: pendente.
- `flyway validate` e testes funcionais: pendente.
- Janela e tempo de indisponibilidade medido: pendente.
- Versão anterior, compatibilidade e plano de recuperação/reconciliação: pendente.
- Autorização explícita para futura mudança na base original: pendente.

## Aceite do piloto

- Finalidade, participantes, duração e funcionalidades autorizadas: pendente.
- Funcionalidades fora de escopo e bloqueio efetivamente testado: pendente.
- Horário/canal de suporte, titular e substituto: pendente.
- Contingência e critérios de interrupção: pendente.
- Pendências bloqueadoras: pendente.
- Aprovações técnica, clínica, jurídica e contábil aplicáveis: pendente.
- Resultado final: **NÃO LIBERADO — modelo sem evidências preenchidas**.
