# Roteiro de demonstração — PsicoGest v0.9

Este roteiro apresenta o produto como uma operação clínica integrada, com separação entre contexto clínico, administrativo, financeiro e governança.

## Antes da apresentação

```bash
cd frontend
pnpm install
pnpm dev
```

Abra `http://localhost:5173/`.

Credenciais do ambiente demonstrativo:

- E-mail: `demo@psicogest.com`
- Senha: `demo123`
- MFA: qualquer código com 6 dígitos

Use somente dados fictícios. O modo demonstrativo não envia mensagens, não emite documentos reais e não realiza cobranças.

## Roteiro recomendado — 15 a 20 minutos

1. **Apresentação pública (`/`)**
   - Mostre a proposta: agenda, pacientes, prontuários e financeiro em uma operação única.
   - Destaque LGPD desde a base, acesso por contexto e trilha de auditoria.

2. **Entrada segura (`/login` → `/mfa`)**
   - Mostre as credenciais de demonstração e o segundo fator.
   - Explique que a autenticação real será conectada ao backend antes do uso em produção.

3. **Dashboard (`/dashboard`)**
   - Comece pelo pulso da clínica: consultas do dia, pacientes ativos, valores a receber e registros para revisar.
   - Clique em **Simular consulta** para iniciar o fluxo de agenda.

4. **Agenda (`/agenda`)**
   - Mostre filtros por paciente/status, bloqueios e prevenção de conflitos.
   - Use **Semana anterior**, **Hoje** e **Próxima semana** para demonstrar navegação real.
   - No modal, explique que a consulta fica pendente de confirmação e nenhum lembrete é enviado.

5. **Paciente (`/patients`)**
   - Abra Marina Duarte e mostre contatos mascarados.
   - Clique em **Simular agendamento** para demonstrar o salto direto para a agenda com o paciente pré-selecionado.
   - Mostre que o prontuário exige contexto clínico autorizado.

6. **Prontuário (`/patients/pat-1/clinical-record`)**
   - Mostre estado, versões e histórico.
   - Use **Simular finalização** e confirme que o estado muda para **Finalizado** e uma nova revisão é registrada.
   - Reforce que conteúdo clínico não deve ir para logs, URLs ou telemetria.

7. **Financeiro e fiscal (`/finance`, `/fiscal`)**
   - Mostre recebíveis, pagamentos, estornos/crédito, conciliação e repasses.
   - Em fiscal, mostre o fluxo de NFS-e em fila local e deixe claro que não há autorização na prefeitura.

8. **Operação e governança**
   - Em `/packages` e `/subscriptions`, explique saldo de sessões e ciclos; a assinatura exibida é fictícia e não gera cobrança.
   - Em `/notifications`, mostre status, reenvio simulado e canais sem provedor conectado.
   - Em `/compliance`, destaque solicitações LGPD, eventos de segurança e auditoria sem corpo de mensagem ou contato completo.
   - Em `/profile`, alterne os perfis para demonstrar a experiência contextual. A troca é apenas uma prévia visual.

9. **Encerramento**
   - Em `/settings`, use **Restaurar cenário demonstrativo** se quiser repetir a apresentação do zero.
   - Pergunte à clínica quais fluxos devem ser priorizados na integração real: autenticação, agenda, provedor de mensagens, pagamentos e fiscal.

## Mensagem de posicionamento

> “Esta versão valida a experiência e as regras de operação da clínica com dados fictícios. Os fluxos críticos já estão representados com estados explícitos, autorização contextual e auditoria. Na próxima etapa conectamos o backend, banco e provedores reais sem alterar a experiência aprovada.”

## O que ainda não prometer como produção

- autenticação, autorização e isolamento multi-tenant ainda precisam ser aplicados pelo backend real;
- e-mail, WhatsApp, pagamentos e NFS-e estão em modo simulado;
- `localStorage` é usado somente para persistir o cenário fictício durante a demonstração;
- antes de dados reais, executar migrations/SQL, configurar segredos, backups, retenção, monitoramento e testes de segurança;
- validar contrato de API e homologação com a clínica antes do go-live.