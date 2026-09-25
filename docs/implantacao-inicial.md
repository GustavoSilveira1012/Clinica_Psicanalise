# Implantação inicial — roteiro para os dois responsáveis técnicos

Atualizado em 25/09/2026. Complementa o [runbook de produção](production-runbook.md).

## Decisões já informadas

- Existe uma base, mas ainda não foi confirmado se é usada em produção. Tratá-la como contendo dados reais até identificar seu responsável e sua finalidade.
- Não há provedor contratado. O início deve ser gratuito.
- O usuário e seu amigo cuidarão da parte técnica. Registrar quem assume cada tarefa e quem substitui o titular.
- R$299/mês é o preço que será cobrado da clínica. O orçamento inicial de hospedagem é R$0; essa receita não é autorização para contratar serviços pagos.

**Estado deste documento:** preparação e escolha de serviços. Nenhuma conta, contratação, infraestrutura, migration, restauração ou homologação foi executada por este roteiro. Os limites dos planos abaixo foram consultados na data acima.

## 1. Começar gratuitamente

Proposta para um ambiente de testes com dados inteiramente fictícios, condicionada à validação da integração e da capacidade da API:

| Componente | Opção inicial | Limite ou pendência relevante |
| --- | --- | --- |
| Site React | Render Static Site gratuito | Publica a interface; sozinho não entrega login e operações do sistema. |
| API Java | Render Web Service gratuito, usando Docker | Suspende por inatividade após 15 minutos; precisa testar memória, inicialização e resposta. |
| PostgreSQL | Supabase Free, em projeto novo | 500 MB; pausa após uma semana inativo; sem backups automáticos ou PITR no plano gratuito. |
| Arquivos de teste | Bucket privado do Supabase Storage | 1 GB incluído. A integração de storage no backend ainda precisa ser implementada e validada. |
| Redis | Upstash Redis Free | 256 MB e 500 mil comandos/mês. Validar os comandos usados pelo sistema e o consumo dos testes. |
| Endereço e HTTPS | Subdomínio fornecido pela hospedagem | Evita comprar domínio durante os testes; exige validar a topologia de autenticação. |

Fontes: [Render Free](https://render.com/docs/free), [Render Static Sites](https://render.com/docs/static-sites), [Supabase: planos](https://supabase.com/pricing), [Upstash: planos](https://upstash.com/pricing/redis).

Essa combinação não conclui o checklist de produção: não entrega automaticamente rede privada entre os três fornecedores, KMS controlado pela clínica, backup imutável, PITR, suporte contratado e revisão independente. A documentação do Render também orienta não usar instâncias gratuitas em produção.

Não usar o PostgreSQL gratuito do Render para preservar a base existente: ele expira após 30 dias. Não substituir a base clínica por uma cópia em serviços gratuitos.

### Ordem para abrir e configurar as contas

1. Criar contas do projeto no [Supabase](https://supabase.com/dashboard), [Render](https://dashboard.render.com/) e [Upstash](https://console.upstash.com/). Usar acessos individuais, MFA e recuperação guardada em local seguro; evitar compartilhar senha.
2. Confirmar que a organização e cada recurso estão no plano gratuito. Não ativar trial pago, cobrança por consumo ou upgrade. A documentação do Upstash informa mudança para cobrança por consumo ao adicionar cartão; conferir o estado final do plano. [Fonte](https://upstash.com/pricing/redis).
3. Criar **um projeto vazio** no Supabase, identificado como teste. Registrar região e versão do PostgreSQL. Comparar com o PostgreSQL 16 usado no ambiente local; se forem versões diferentes, validar essa compatibilidade separadamente.
4. Antes de criar tabelas do PsicoGest, desabilitar a Data API se ela não for usada ou excluir os schemas da aplicação da exposição. O navegador continuará usando a API Java. Revisar permissões padrão para que tabelas novas não fiquem acessíveis por APIs alternativas. [Segurança da Data API](https://supabase.com/docs/guides/api/securing-your-api).
5. Criar um Redis gratuito para teste. Guardar host, porta e credencial Redis no painel seguro do backend. Usar TLS; token REST não substitui automaticamente a senha do protocolo Redis. [Segurança do Upstash](https://upstash.com/docs/redis/features/security).
6. Criar o bucket de teste como **privado**. Validar acesso negado sem autorização. O caráter privado do bucket não comprova retenção imutável nem KMS dedicado. [Buckets do Supabase](https://supabase.com/docs/guides/storage/buckets/fundamentals).
7. Preparar o serviço Java no Render com o Dockerfile existente. Conferir as dependências de código e segurança descritas abaixo antes de publicar. O Render recomenda Docker para aplicações Java. [Docker no Render](https://render.com/docs/docker).
8. Publicar a interface somente depois de definir o endereço e o encaminhamento da API. Testar login, renovação de sessão e logout no navegador.

O objetivo de custo inicial é R$0 dentro das cotas gratuitas. Isso não cobre domínio próprio, certificado fiscal, taxas de pagamentos/mensagens, assessoria, pentest ou trabalho dos responsáveis.

**Limite importante do storage candidato:** o S3 do Supabase não oferece versionamento, lifecycle de bucket nem object lock na compatibilidade atual; exclusões são permanentes. As credenciais S3 de servidor também têm acesso total e ignoram RLS. Por isso, esse bucket pode servir para ensaio com dados fictícios, mas não comprova retenção/recuperação e não deve liberar exports clínicos de produção. A prontidão do adapter permanece fechada até haver retenção/recuperação comprovadas e controles aprovados. [Compatibilidade S3](https://supabase.com/docs/guides/storage/s3/compatibility), [autenticação S3](https://supabase.com/docs/guides/storage/s3/authentication).

## 2. O que o repositório ainda exige

A contratação dos serviços precisa ser acompanhada destes trabalhos técnicos:

| Constatação no código | Ação necessária |
| --- | --- |
| A camada `EncryptedClinicalExportStorage` cifra exportações no backend e vincula chave/contexto ao tenant; `UnavailableClinicalExportStorage` continua sendo o padrão. | Ainda falta o adapter concreto do fornecedor escolhido. Ele deve provar acesso privado, cifragem em repouso, política de retenção e recuperação em health check real; adapter e teste do provedor seguem pendentes. A camada atual não habilita exports em produção sozinha. |
| `FailClosedNationalNfseClient` recusa emissão real. | Implementar/configurar o emissor escolhido e homologar emissão, consulta e eventos fiscais. |
| Existem registries de pagamento e notificações, sem adapters reais encontrados nos diretórios examinados. | Confirmar cobertura e implementar os fornecedores contratados, incluindo callbacks e tratamento de falhas. |
| `REQUIRE_*` é consultado pelo serviço de readiness. | Esses parâmetros exigem integrações na verificação de saúde; não são, por si só, bloqueios de toda operação comercial. `false` não prova que mensagens ou cobranças não serão disparadas. |
| `REQUIRE_NOTIFICATION_PROVIDERS` exige e-mail e WhatsApp juntos. | Homologar ambos antes de exigir essa prontidão, ou desenvolver uma separação explícita dos canais. |
| Cookies de renovação usam `SameSite=Strict`. | Planejar site e API na mesma origem, ou outra topologia comprovadamente compatível. Não presumir que dois subdomínios gratuitos funcionem apenas liberando CORS. Validar também CSRF e renovação de sessão. |
| A demonstração da interface exige execução em desenvolvimento. | `VITE_DEMO_MODE=true` não transforma a build normal de produção em demonstração autônoma. |
| O Flyway integra a inicialização da API. | Não iniciar uma nova versão apontando para a base desconhecida: isso pode aplicar migrations antes de qualquer teste manual. |

O perfil de produção exige storage de exportação disponível por padrão. Não remover essa exigência para declarar a implantação concluída. Qualquer escopo reduzido precisa ser descrito e ter suas operações indisponíveis comprovadas.

### Configuração técnica a preparar após criar o ambiente vazio

- PostgreSQL: conexão JDBC com TLS e verificação do certificado/hostname; credenciais separadas de runtime e migrations. Runtime deve ser `NOSUPERUSER NOBYPASSRLS`, sem privilégios administrativos indiretos. Validar políticas RLS com duas organizações.
- Supabase: usar conexão adequada a um servidor persistente; conferir conectividade direta/IPv6 ou pooler em modo de sessão. Não escolher pooler transacional sem validar Flyway e o contexto de tenant. [Conexões PostgreSQL](https://supabase.com/docs/guides/database/connecting-to-postgres).
- Redis: `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` e TLS. Testar as operações de autenticação/rate limit com o fornecedor.
- Backend: `SPRING_PROFILES_ACTIVE=production`, origens HTTPS explícitas, cookie seguro, par de chaves JWT e três chaves independentes para MFA, conteúdo clínico e auditoria.
- O perfil `production` mantém `CLINICAL_DATA_ENABLED=false` por padrão. Não habilitar essa variável nem sua equivalente `VITE_CLINICAL_DATA_ENABLED` enquanto a base, o isolamento, storage, backup/restore e demais controles não tiverem sido aprovados. Nunca apontar o ambiente gratuito para a base existente.
- Guardar chaves e senhas em configuração protegida; nenhuma credencial deve ir para `VITE_*`, Git ou conversa. Guardar cópia recuperável das chaves, com acesso restrito.
- Manter `SCHEDULING_ENABLED=false` até validar as rotinas automáticas e seus fornecedores. No clone, bloquear também saída de rede para serviços reais: esse parâmetro sozinho não impede chamadas diretas.
- Site: build a partir de `frontend/`, publicando `dist/`, com a URL/rota de API definida. O projeto utiliza pnpm no Dockerfile; usar o lockfile existente.
- API: Dockerfile em `backend/psicogest/psicogest/`. `/health/ready` precisa refletir as dependências exigidas; testar os fluxos além de verificar HTTP 200.

## 3. Identificar e preservar a base existente

Responsável: um dos técnicos, com confirmação do responsável pela base/clínica.

1. Descobrir em qual computador/serviço ela fica e qual aplicação aponta para ela.
2. Confirmar quem a usa, quando foi o último uso e se há pacientes, agenda, financeiro ou documentos reais. Não exportar conteúdo para fazer essa identificação.
3. Registrar versão do PostgreSQL, tamanho, nome do ambiente, responsável e existência de backups. Guardar inventário em local privado.
4. Confirmar a disponibilidade das chaves que cifraram os dados existentes. Chaves novas não recuperam dados antigos.
5. Definir destino isolado e acesso autorizado antes de gerar uma cópia. A cópia deve receber proteção equivalente à da origem, inclusive controle de acesso e prazo de descarte.

Não conectar a nova API nem executar restore/migrations enquanto a identidade e a função dessa base estiverem indefinidas. Arquivos `.env`, dumps e prontuários não devem ser anexados ao repositório ou enviados nesta conversa.

## 4. Checklist completo e evidência de conclusão

| Etapa | Trabalho concreto | Evidência para considerar concluída | Responsável proposto |
| --- | --- | --- | --- |
| Storage privado | Escolher serviço; configurar acesso mínimo, criptografia, recuperação e retenção por categoria; integrar o backend. | Acesso anônimo negado, acesso de outra organização negado, recuperação de objeto e chave testadas; política aprovada. | Técnico A + clínica para retenção |
| Banco, Redis e rede | Provisionar serviços, TLS, rede privada, proteção de entrada, armazenamento de segredos e contas separadas. | Diagrama e revisão de acessos; conexões seguras; negação de acesso indevido e isolamento entre organizações testados. | Técnico A; revisão do técnico B |
| Backup/PITR | Definir perda tolerável e prazo de recuperação; automatizar backups e testar recuperação isolada. | Relatório com RPO/RTO medidos, integridade, acesso às chaves e recuperação de banco e objetos. | Técnico B; validação da clínica |
| Upgrade do banco | Comparar histórico; ensaiar upgrade numa cópia protegida; validar V86 e dados legados. | Comparação antes/depois e aprovação explícita do plano de mudança. | Técnico B; revisão do técnico A |
| Domínio, HTTPS e operação | Definir DNS, renovação de certificado, logs minimizados, alertas e escala de responsáveis. | HTTPS e renovação verificados, alerta recebido/confirmado, retenção documentada e contatos testados. | Técnico A |
| NFS-e Sorocaba | Contador confirma enquadramento e canal; selecionar emissor, certificado compatível e ambiente de homologação. | Emissão/consulta/cancelamento e substituição quando aplicável; documentos preservados; validação contábil. | Clínica + contador + técnico B |
| Pagamento, e-mail, WhatsApp | Selecionar fornecedores; integrar e testar sandbox, assinaturas de callbacks, repetição e falhas. | Casos de teste aprovados, credenciais segregadas e habilitação de produção autorizada por canal. | Técnico B + clínica |
| Privacidade e contratos | Inventariar tratamentos, bases legais, retenção, fornecedores, DPA, direitos e incidentes. | Versões aprovadas pelos responsáveis da clínica e assessoria; canal de atendimento definido. | Clínica + assessoria jurídica |
| Revisão e piloto | Contratar avaliação independente; corrigir e retestar; definir suporte, limites e critérios de interrupção. | Relatório sem bloqueadores em aberto, reteste e aceite do piloto. | Avaliador independente + clínica + técnicos |

Os dois técnicos podem revisar o trabalho um do outro, mas isso não substitui a revisão independente solicitada no checklist.

## 5. Backup e ensaio de recuperação

**RPO** é o intervalo máximo de dados que a clínica aceita perder. **RTO** é o tempo máximo para recuperar o serviço. A clínica deve aprovar as metas antes da contratação; ainda não há valores aprovados.

1. Definir separadamente backup do PostgreSQL, objetos, configurações e chaves de criptografia. Backup do banco não contém automaticamente os arquivos do storage.
2. Criar banco de restauração em ambiente isolado, sem rota para o banco original e sem credenciais de emissão, cobrança ou mensagens reais.
3. Seguir os scripts e as verificações SHA-256 do [runbook](production-runbook.md). Conferir **conta, servidor e banco**, não apenas o nome do banco. O script de restore é destrutivo no destino e não comprova sozinho que o servidor escolhido é seguro.
4. Restaurar primeiro sem iniciar a nova API. Os scripts usam `--no-owner` e `--no-privileges`: recriar/revisar roles, ownership e grants do destino antes de validar RLS com a role real de runtime.
5. Validar o restore com versão compatível da aplicação. Somente depois executar o ensaio de upgrade aprovado, ainda no clone.
6. Conferir leitura de conteúdo cifrado, login/MFA, duas organizações isoladas, agenda, prontuário, financeiro e objetos.
7. Para testar PITR, escolher horário-alvo e confirmar um marcador anterior presente e um posterior ausente. Um dump restaurado não comprova PITR.
8. Medir RPO observado a partir do incidente simulado e do último dado confirmado recuperado. Medir RTO da declaração do incidente até serviço validado e liberado; guardar também os tempos de infraestrutura, restore e testes.
9. Guardar evidências em repositório privado, com acesso restrito, sem prontuários em capturas de tela. Usar o [modelo de evidências](implantacao-evidencias-modelo.md).

Backups manuais de dados fictícios permitem aprender o procedimento gratuitamente; não substituem PITR nem sustentam promessa de recuperação clínica.

## 6. Upgrade: pontos que precisam de aprovação específica

- Exportar `flyway_schema_history` e comparar versões, sucesso e checksums com o código candidato.
- Revisar alterações históricas V11/V18/V25/V38/V76 e pré-requisitos V07.1/V13.1/V75.1 descritos no runbook. Não rodar `flyway repair`, baseline ou out-of-order para esconder divergências.
- Verificar V20/V26: havendo prontuários/pagamentos legados, preparar conversão dedicada e reconciliar registros e vínculos. Não retirar as proteções.
- Validar V85 para moedas e valores financeiros.
- A **V86 interpreta os timestamps antigos como UTC**. Confirmar isso com configuração histórica e eventos cujo horário seja conhecido. A timezone atual do servidor não comprova o fuso usado na gravação antiga. Não aplicar uma correção fixa de horas sem analisar datas históricas e horário de verão.
- Comparar contagens, relacionamentos, valores e instantes antes/depois. Registrar divergências e resolver antes do aceite.
- Documentar janela, versão da aplicação, artefato anterior e compatibilidade de rollback. Voltar a imagem não desfaz mudança de schema. Se houver restauração após novas gravações, o plano deve tratar sua recuperação/reconciliação.

## 7. NFS-e de Sorocaba

Não escolher o emissor somente pelo município. O contador precisa confirmar CNPJ/inscrição municipal, regime tributário, serviço prestado e data de entrada em operação.

A Prefeitura mantém orientação de atendimento para NFS-e e acesso ao SIAC pelo portal municipal. Isso, sozinho, não comprova qual integração atende cada contribuinte. [Prefeitura de Sorocaba](https://fazenda.sorocaba.sp.gov.br/fiscalizacao/nfse/).

A Receita Federal informa obrigatoriedade do Emissor Nacional para ME/EPP optantes pelo Simples Nacional a partir de **01/11/2026**. A aplicação à clínica deve ser confirmada pelo contador, inclusive diante da transição. [Comunicado da Receita Federal](https://www.gov.br/receitafederal/pt-br/assuntos/noticias/2026/agosto/simples-nacional-nfs-e-nacional-sera-obrigatoria-para-me-e-epp-a-partir-de-1o-de-novembro-de-2026).

Antes de contratar, obter do fornecedor confirmação escrita de cobertura do regime/canal necessário, certificado aceito, sandbox, consulta após timeout, cancelamento/substituição aplicáveis, guarda de XML/PDF, custo por documento e exportação na saída. Comprar certificado somente após confirmar a compatibilidade. Guardar certificado/senha de forma protegida e definir responsável pela renovação.

## 8. Pagamentos, mensagens e documentos jurídicos

Na fase gratuita, usar dados e destinatários de teste, sem transações reais. A homologação deve verificar:

- Pagamentos: aprovação, recusa, timeout, consulta, reembolso, conciliação, callback assinado e duplicado sem dupla cobrança.
- E-mail: domínio/remetente autorizados, SPF/DKIM/DMARC conforme fornecedor, entrega, rejeição e ausência de informação clínica desnecessária.
- WhatsApp: conta/número empresariais, fluxo oficial do fornecedor, modelos aprovados quando exigidos, elegibilidade do destinatário, opt-out e callbacks.
- Todos: sandbox separado, limites, logs sem segredos, tratamento de indisponibilidade e preço/cotas documentados. Uma conta gratuita não equivale a envio comercial gratuito sem limites.

A clínica e sua assessoria devem definir controlador/operadores, bases legais por finalidade, retenção por categoria, direitos dos titulares, contratos com fornecedores/suboperadores, transferências internacionais e resposta a incidentes. Não presumir que consentimento genérico autoriza todo tratamento ou que escolher região brasileira resolve todos esses pontos.

O plano de incidentes deve prever avaliação imediata de risco e o fluxo de comunicação aplicável. A ANPD informa prazo geral de três dias úteis para comunicações cabíveis, com ressalvas legais; a assessoria deve confirmar o enquadramento. [Orientação da ANPD](https://www.gov.br/anpd/pt-br/canais_atendimento/agente-de-tratamento/comunicado-de-incidente-de-seguranca-cis).

## 9. Operação e piloto

Definir um responsável técnico principal e um substituto, horário de suporte e canal de escalonamento. Não prometer plantão 24 horas sem capacidade contratada.

Monitorar disponibilidade, erro/latência, banco, Redis, cotas gratuitas, falhas de backup, filas, expiração de certificados e falhas dos fornecedores. Cada alerta precisa ter destinatário e ação; testar a entrega. Definir retenção de logs com acesso restrito e sem conteúdo de prontuário, tokens ou senhas.

Começar com testes internos fictícios. O piloto com pacientes exige os controles e aceites da seção 4, escopo escrito, participantes limitados, treinamento, canal de suporte e procedimento de contingência. Falha de isolamento, corrupção de dados, recuperação inviável ou incidente grave impedem a liberação e exigem interrupção do piloto afetado.

### Próxima ação

Identificar a base existente e criar as contas gratuitas/projeto vazio. Depois, registrar somente nomes dos serviços, planos e regiões escolhidos; preparar a integração e testar o ambiente antes de qualquer transferência de dados reais.
