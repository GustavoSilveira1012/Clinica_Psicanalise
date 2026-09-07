# MFA e sessões por dispositivo

Escopo: TOTP, códigos de recuperação, desafios de autenticação e sessões.
Passkeys/WebAuthn, SMS e SecurityEvent ficam para etapas futuras.

## Configuração

- Definir `MFA_ENCRYPTION_KEY` no ambiente: Base64 de **32 bytes aleatórios** para AES-256-GCM.
  Não salvar a chave no Git, nos logs ou no banco. Manter backup seguro: trocar/perder
  essa chave sem recifrar os segredos torna os fatores existentes inutilizáveis.
- Continuar fornecendo as chaves RSA e as configurações de cookie/CORS do JWT.
  Em produção, usar HTTPS e `JWT_COOKIE_SECURE=true`.
- Configurações padrão: desafio de cinco minutos, cinco tentativas, emissor PsicoGest.
- V14 cria desafios, métodos e recovery codes. V15 cria sessões, migra as famílias
  de refresh existentes e preserva as famílias já revogadas. `family_id = session.id`.
- O starter Flyway habilita a integração com o Spring Boot 4. O repositório ainda
  não possui V1–V3 para reconstruir o esquema clínico em um banco vazio; esta tarefa
  não inventa nem modifica esse esquema. Aplicar as migrações sobre a base existente
  com histórico Flyway consistente.
- JWTs antigos sem `sid` deixam de ser aceitos. Profissionais precisam concluir
  o MFA; refresh legado não permite contornar a matrícula obrigatória.

## Contratos da API

Primeiro, chamar `GET /auth/csrf`, preservar o cookie e enviar o token retornado
no cabeçalho `X-CSRF-TOKEN` nas operações de autenticação. Respostas com segredos
não devem ser armazenadas em cache, registradas em logs nem persistidas pelo cliente.
Refresh continua exclusivamente no cookie HttpOnly, nunca no JSON.

| Operação | Corpo / retorno |
| --- | --- |
| POST /auth/login | `{email,password}` → `{status,authentication,challenge}` |
| POST /auth/mfa/enrollment | Bearer + `{password}` → desafio de matrícula opcional |
| POST /auth/mfa/totp/setup | `{challenge}` → `{secret,otpauthUri}` |
| POST /auth/mfa/totp/confirm | `{challenge,code}` → `{authentication,recoveryCodes}` + cookie refresh |
| POST /auth/mfa/totp/verify | `{challenge,code}` → AuthResponse + cookie refresh |
| POST /auth/mfa/recovery | `{challenge,code}` → AuthResponse + cookie refresh |
| GET /auth/sessions | Bearer → sessões próprias ativas |
| DELETE /auth/sessions/{id} | Bearer → 204; revoga apenas a sessão indicada |
| DELETE /auth/mfa/totp | Bearer + `{challenge,code}` de reautenticação → 204 |

`status` é `AUTHENTICATED`, `MFA_REQUIRED` ou `MFA_ENROLLMENT_REQUIRED`.
Nos dois últimos casos, `authentication` é nulo e nenhum token é emitido.
PSYCHOANALYST, CLINIC_ADMIN e SYSTEM_ADMIN exigem MFA; PATIENT pode optar,
mas passa a precisar do segundo fator quando o ativa.

A matrícula gera um segredo Base32 de 160 bits. `setup` o mostra uma única vez por
desafio; se a resposta for perdida, iniciar outro login/matrícula. A confirmação
retorna dez recovery codes aleatórios de 256 bits, mostrados somente nessa resposta.
Cada código deve ser guardado com segurança e pode ser usado uma única vez, exatamente
como retornado. No banco, ficam apenas seus hashes SHA-256.

TOTP segue [RFC 6238](https://www.rfc-editor.org/rfc/rfc6238):
HMAC-SHA1, seis dígitos, períodos de 30 segundos e tolerância de um período.
O último período aceito é persistido: até um código correto é recusado se já
foi usado. Aguardar o próximo período para confirmar outra operação.

## Troca ou remoção do fator

Para remover, chamar novamente `POST /auth/login` com a senha e enviar o novo
desafio MFA_REQUIRED com um TOTP atual ao DELETE, usando também o Bearer da sessão.
O desafio comprova a reentrada recente da senha. Apenas Bearer nunca basta.
Troca de autenticador: remover dessa forma e realizar uma nova matrícula.
A recuperação com código permite autenticar, mas não remove ou substitui um fator
automaticamente; recuperação formal de fator perdido fica fora desta etapa.

Ativação e remoção incrementam `securityVersion`, invalidam desafios anteriores
e revogam todas as sessões/refresh anteriores. Na ativação, uma nova sessão autenticada
é emitida. Profissionais que removerem o fator precisam matriculá-lo de novo no próximo login.

Cada JWT contém `sid`; o validador confere usuário ativo/desbloqueado, versão de
segurança, dono da sessão, expiração e revogação. Refresh atualiza `lastSeenAt`,
`lastIp` e a expiração da mesma sessão. Reuso de refresh consumido revoga a família
e seu JWT. Logout revoga o dispositivo; logout-all revoga todas as sessões.

## Validação

Os testes isolados usam PostgreSQL 16 via Testcontainers e aplicam os arquivos
reais V12–V15 sobre uma tabela users mínima **exclusiva de teste**. Não acessam o
banco clínico local. Cobrem os contratos MFA, persistência de tentativas, concorrência,
criptografia, recuperação, ownership, CSRF, rotação e revogação de JWT/refresh.

Validação em 07/09/2026: 47 testes aprovados, incluindo 16 de integração MFA/sessões
e os testes unitários clínicos existentes. Na suíte completa, 54 testes executados:
47 aprovados e 7 erros de inicialização nas suítes antigas. Em banco vazio, V4 depende
da tabela appointments que não foi criada pelas migrações disponíveis; o teste antigo
de inicialização que usa a base local encontrou um esquema não vazio sem histórico
Flyway. Nenhum baseline automático foi executado. Regularizar o histórico/esquema
existente exige uma tarefa própria, antes de inicializar a aplicação nessa base.

```text
mvn -Dtest=AuthServiceSecurityTest,RefreshTokenSecurityTest,JwtServiceSecurityTest,JwtValidatorTest,TotpServiceTest,MfaSessionIntegrationTest test
```
