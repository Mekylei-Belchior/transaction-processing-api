# Execução Local

Este documento descreve como executar a `transaction-processing-api` em ambiente local, tanto diretamente via Maven Wrapper quanto via Docker Compose.

## Pré-requisitos

- **Java 21 (JDK)**: necessário para executar a aplicação localmente via `./mvnw`.
- **Maven Wrapper (`mvnw`)**: já incluído no repositório; não é necessário instalar Maven localmente para os comandos documentados aqui.
- **Docker e Docker Compose v2**: necessários para executar a aplicação via `docker compose`.
- **PostgreSQL 16+**: necessário para execução direta sem Compose. O `docker-compose.yml` já sobe PostgreSQL 16 para o cenário com containers.
- **Kafka 3.9+**: necessário somente quando `EVENTOS_KAFKA_ENABLED=true`; nesse caso, a aplicação deve apontar para um broker externo.
- **Servidor OAuth2/OIDC compatível**: por exemplo, Keycloak, com endpoint JWKS exposto para validação dos tokens JWT.

## Configuração do ambiente

Copie o arquivo `.env.example` para `.env` na raiz do projeto:

```bash
cd transaction-processing-api
cp .env.example .env
```

O arquivo `.env` é usado automaticamente pelo `docker compose`. Para execução direta via `./mvnw`, exporte as variáveis no shell ou configure-as na IDE, pois o Spring Boot não carrega `.env` automaticamente por padrão.

O arquivo `.env` nunca deve ser versionado. O repositório já mantém `.env` e `.env.*` no `.gitignore`.

### Variáveis mínimas para desenvolvimento

| Variável | Obrigatória para | Descrição |
| --- | --- | --- |
| `POSTGRES_DB` | Compose | Nome do banco criado no container PostgreSQL. |
| `DB_USERNAME` | Maven e Compose | Usuário de conexão com o PostgreSQL. |
| `DB_PASSWORD` | Maven e Compose | Senha de conexão com o PostgreSQL. |
| `OAUTH2_JWKS_URI` | Maven e Compose | Endpoint JWKS do provedor OAuth2/OIDC. |
| `OAUTH2_ISSUER_URI` | Maven e Compose | Issuer esperado nos tokens JWT. |
| `APP_CRIPTOGRAFIA_CHAVE` | Maven e Compose | Chave Base64 usada pela criptografia AES-256-GCM. |
| `APP_HMAC_CHAVE` | Maven e Compose | Chave usada para HMAC-SHA256 de dados sensíveis pesquisáveis. |

Exemplo seguro com placeholders:

```env
POSTGRES_DB=transacaodb
DB_USERNAME=<usuario-local>
DB_PASSWORD=<senha-local>

OAUTH2_JWKS_URI=https://<host-keycloak>/realms/<realm>/protocol/openid-connect/certs
OAUTH2_ISSUER_URI=https://<host-keycloak>/realms/<realm>

APP_CRIPTOGRAFIA_CHAVE=<chave-base64-32-bytes>
APP_HMAC_CHAVE=<chave-hmac-forte>
```

No perfil `dev`, as variáveis de banco, criptografia e HMAC possuem valores default para facilitar a inicialização local. Mesmo assim, recomenda-se sobrescrevê-las em ambientes compartilhados. As configurações de OAuth2 devem apontar para um provedor real e acessível, com `issuer` e JWKS compatíveis com os tokens usados nas chamadas da API.

## Execução direta via Maven Wrapper

Clone o repositório e entre no diretório do projeto:

```bash
git clone <url-do-repositorio>
cd transaction-processing-api
```

Para execução direta, tenha um PostgreSQL 16+ acessível. Com o perfil `dev`, a URL default é `jdbc:postgresql://localhost:5432/transacaodb`. Se o seu banco estiver em outro host, porta ou nome, exporte `DB_URL` antes de iniciar.

Execute com o perfil `dev`:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Build

Build sem execução de testes:

```bash
./mvnw clean package -DskipTests
```

Build com execução de testes:

```bash
./mvnw clean package
```

O artefato gerado fica em:

```text
target/transaction-processing-*.jar
```

### Testes

Executar todos os testes:

```bash
./mvnw test
```

Executar verificação completa, incluindo gates configurados no Maven:

```bash
./mvnw verify
```

Gerar relatórios de cobertura sem falhar no gate local:

```bash
./mvnw test -Pcoverage
```

## Execução via Docker Compose

O `docker-compose.yml` deste repositório sobe apenas:

- `postgres-transacao`, usando PostgreSQL 16 e porta `5433` no host.
- `app`, expondo a API na porta `8080`.

O Kafka não está incluído no Compose. Se `EVENTOS_KAFKA_ENABLED=true`, configure a aplicação para apontar para um broker Kafka externo. Se não for usar Kafka localmente, desabilite-o no bloco `environment` do serviço `app`:

```yaml
EVENTOS_KAFKA_ENABLED: false
```

O PostgreSQL do Compose é acessível no host por:

```text
localhost:5433
```

Dentro da rede Docker, a aplicação usa:

```text
jdbc:postgresql://postgres-transacao:5432/${POSTGRES_DB}
```

### Certificado root CA no Dockerfile

O `Dockerfile` importa `certificados/root_ca.crt` para o truststore da JVM. Esse comportamento existe para cenários em que o Keycloak ou outro provedor OAuth2/OIDC usa HTTPS com uma autoridade certificadora local.

Caso você não use root CA local, comente ou remova a seção abaixo do `Dockerfile`, conforme indicado no `OLD_README.md`:

```dockerfile
COPY certificados/root_ca.crt /tmp/root_ca.crt

RUN keytool -importcert \
    -noprompt \
    -trustcacerts \
    -alias root-ca \
    -file /tmp/root_ca.crt \
    -keystore $JAVA_HOME/lib/security/cacerts \
    -storepass changeit
```

### Comandos

Faça o build da imagem sem cache:

```bash
docker compose build --no-cache
```

Suba os containers em background:

```bash
docker compose up -d
```

Verifique os containers:

```bash
docker compose ps
```

### Documentação interativa da API

Com a aplicação em execução, acesse:

- **Swagger UI:** http://localhost:8080/swagger-ui/index.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs

> ℹ️ O Swagger UI exige autenticação para testar endpoints protegidos. Consulte Como obter um token de acesso.

## Execução sem acesso ao homelab

O perfil `dev` aponta por padrão para os serviços do homelab do autor. Para executar a aplicação sem acesso a esse ambiente, use um Keycloak local e desabilite as integrações externas que não forem necessárias.

Adicione manualmente o serviço abaixo ao bloco `services` do `docker-compose.yml`:

```yaml
  keycloak-local:
    image: quay.io/keycloak/keycloak:24.0
    command: start-dev
    ports:
      - "8180:8080"
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin
```

Depois, suba o serviço:

```bash
docker compose up -d keycloak-local
```

Configure o Keycloak pela Admin Console:

1. Acesse [http://localhost:8180](http://localhost:8180) e abra a **Administration Console**.
2. Entre com o usuário `admin` e a senha `admin`, definidos no serviço acima.
3. No seletor de realms, escolha **Create realm**, informe `bancario` em **Realm name** e conclua a criação.
4. Em **Clients**, escolha **Create client**, mantenha o protocolo **OpenID Connect**, informe `transaction-api-client` em **Client ID** e salve. Configure-o como público, desabilitando **Client authentication**, ou como confidential, habilitando essa opção e usando a credencial gerada nas aplicações clientes.
5. Em **Realm roles**, crie individualmente as roles `CLIENTE`, `OPERADOR`, `GERENTE`, `ADMIN` e `SERVICO_INTERNO`.
6. Em **Users**, crie um usuário de teste. Na aba **Credentials**, defina uma senha e desabilite **Temporary** caso não queira exigir a troca no primeiro login.
7. Na aba **Role mapping** do usuário, escolha **Assign role** e atribua a role `CLIENTE`.

No `.env`, substitua as configurações de OAuth2 pelos endpoints do Keycloak local:

```env
OAUTH2_JWKS_URI=http://localhost:8180/realms/bancario/protocol/openid-connect/certs
OAUTH2_ISSUER_URI=http://localhost:8180/realms/bancario
```

Para executar sem um broker Kafka, defina também:

```env
EVENTOS_KAFKA_ENABLED=false
```

O Jaeger pode ser ignorado durante a execução local sem impacto funcional na API. Caso o coletor não esteja acessível, o envio de traces falhará silenciosamente.

Para detalhes de cada variável, consulte [docs/operacao/variaveis-ambiente.md](../operacao/variaveis-ambiente.md).

## Seed da base de dados

Use o ‘script’ abaixo para popular a base de dados, após esta ter sido criada ao executar a aplicação.

```sql

-- ============================================================
-- SEED — Transaction Processing API — Fase 1
-- Executar APÓS subir a aplicação (para que o JPA crie as tabelas)
-- ============================================================

-- IDs fixos para uso direto nos testes via curl
-- Alice  → ATIVA,    saldo R$ 10.000  (fluxos normais + limite)
-- Bob    → ATIVA,    saldo R$    100  (cenário saldo insuficiente)
-- Carol  → BLOQUEADA, saldo R$  2.000 (cenário conta inativa)

-- -------------------------------------------------------
-- CONTAS
-- agencia: 0001
-- 1 - número conta: 00001-9
-- 2 - número conta: 00002-7
-- 3 - número conta: 00003-5
-- -------------------------------------------------------
INSERT INTO public.conta (id, agencia, agencia_hmac, criado_em, id_cliente, numero_conta, numero_conta_hmac, status, tipo)
VALUES
    ('aaaaaaaa-0000-0000-0000-000000000001', 'ndmQxuQoMUN8023lOFhONmW+cTAwTG/ob04lM9i8WQk=', '4f3baf8df9c85e5a9d56f3df7f6c4a9efdb8f3c13f3d0b4c4cbfdcf05a4a9c0d',  NOW(), '11111111-0000-0000-0000-000000000001', 'Gp9UEf+hVdymVh+gxKDVI6v9dD2n6XfOPY+duIq5rTCt2Xc=', '74e38e5e3a9e1f61d36f22e6b59c1f7b33a2db18f4d0a4d876c0a1d8a2d3f2e9',  'ATIVA',      'CORRENTE'),
    ('bbbbbbbb-0000-0000-0000-000000000002', 'ndmQxuQoMUN8023lOFhONmW+cTAwTG/ob04lM9i8WQk=', '4f3baf8df9c85e5a9d56f3df7f6c4a9efdb8f3c13f3d0b4c4cbfdcf05a4a9c0d', NOW(), '22222222-0000-0000-0000-000000000002', 'cawdBLoH00mTeqODJhn9Dh3iAZfbUcbeP4VdkA7pAwATP2U=', '2dcf0d48f7a2a4bb3f59cb34d1e08c89f0cf57c65bc46a52f9b2f67a15a42a87',  'ATIVA',      'CORRENTE'),
    ('cccccccc-0000-0000-0000-000000000003', 'ndmQxuQoMUN8023lOFhONmW+cTAwTG/ob04lM9i8WQk=', '4f3baf8df9c85e5a9d56f3df7f6c4a9efdb8f3c13f3d0b4c4cbfdcf05a4a9c0d', NOW(), '33333333-0000-0000-0000-000000000003', 'GY+ApqCbh1PBcfZ8OGr+x7x33X6OREvAYV+Njd2d+PuHJ3E=', '6f81db6c2ecfd0f3d0a7a3cb8f0dfd8cb4eec0b02a9e67b09cf4f4aef16cb8a3',  'BLOQUEADA',  'CORRENTE');

-- -------------------------------------------------------
-- SALDOS
-- -------------------------------------------------------
INSERT INTO public.saldo (id, atualizado_em, bloqueado, disponivel, id_conta, versao)
VALUES
    ('a1a1a1a1-0000-0000-0000-000000000001', NOW(), 0.00, 10000.00, 'aaaaaaaa-0000-0000-0000-000000000001', 0),
    ('b2b2b2b2-0000-0000-0000-000000000002', NOW(), 0.00,   100.00, 'bbbbbbbb-0000-0000-0000-000000000002', 0),
    ('c3c3c3c3-0000-0000-0000-000000000003', NOW(), 0.00,  2000.00, 'cccccccc-0000-0000-0000-000000000003', 0);

-- -------------------------------------------------------
-- LIMITES TRANSACIONAIS
--   limite_utilizado = limite por transação (campo mal nomeado na DDL)
--   limite_diario    = teto acumulado no dia
--   utilizado_hoje   = quanto já foi consumido hoje
--
-- Alice — limites normais, zerado hoje
-- Bob   — limites normais, zerado hoje (saldo é o gargalo)
-- Carol — bloqueada, limites irrelevantes mas necessários para consistência
-- -------------------------------------------------------

-- Alice — PIX: até R$ 2.000 por transação, R$ 5.000/dia
INSERT INTO public.limite (id, data_referencia, id_conta, limite_diario, limite_utilizado, tipo, utilizado_hoje)
VALUES ('a1000001-0000-0000-0000-000000000001', CURRENT_DATE, 'aaaaaaaa-0000-0000-0000-000000000001', 5000.00,  2000.00, 'PIX', 0.00);

-- Alice — TED: até R$ 5.000 por transação, R$ 10.000/dia
INSERT INTO public.limite (id, data_referencia, id_conta, limite_diario, limite_utilizado, tipo, utilizado_hoje)
VALUES ('a1000002-0000-0000-0000-000000000001', CURRENT_DATE, 'aaaaaaaa-0000-0000-0000-000000000001', 10000.00, 5000.00, 'TED', 0.00);

-- Alice — TEF: até R$ 500 por transação, R$ 1.500/dia
INSERT INTO public.limite (id, data_referencia, id_conta, limite_diario, limite_utilizado, tipo, utilizado_hoje)
VALUES ('a1000003-0000-0000-0000-000000000001', CURRENT_DATE, 'aaaaaaaa-0000-0000-0000-000000000001', 1500.00,   500.00, 'TEF', 0.00);

-- Bob — PIX
INSERT INTO public.limite (id, data_referencia, id_conta, limite_diario, limite_utilizado, tipo, utilizado_hoje)
VALUES ('b2000001-0000-0000-0000-000000000002', CURRENT_DATE, 'bbbbbbbb-0000-0000-0000-000000000002', 5000.00,  2000.00, 'PIX', 0.00);

-- Bob — TED
INSERT INTO public.limite (id, data_referencia, id_conta, limite_diario, limite_utilizado, tipo, utilizado_hoje)
VALUES ('b2000002-0000-0000-0000-000000000002', CURRENT_DATE, 'bbbbbbbb-0000-0000-0000-000000000002', 10000.00, 5000.00, 'TED', 0.00);

-- Bob — TEF
INSERT INTO public.limite (id, data_referencia, id_conta, limite_diario, limite_utilizado, tipo, utilizado_hoje)
VALUES ('b2000003-0000-0000-0000-000000000002', CURRENT_DATE, 'bbbbbbbb-0000-0000-0000-000000000002', 1500.00,   500.00, 'TEF', 0.00);

-- Carol — PIX (bloqueada, mas inserção mantém consistência)
INSERT INTO public.limite (id, data_referencia, id_conta, limite_diario, limite_utilizado, tipo, utilizado_hoje)
VALUES ('c3000001-0000-0000-0000-000000000003', CURRENT_DATE, 'cccccccc-0000-0000-0000-000000000003', 5000.00,  2000.00, 'PIX', 0.00);

-- -------------------------------------------------------
-- VERIFICAÇÃO
-- -------------------------------------------------------
select c.id as idconta,
       c.numero_conta,
	   c.status as status_conta,
	   s.disponivel,
	   s.bloqueado,
	   l.tipo as limite_tipo,
	   l.limite_utilizado as limite_por_transacao,
	   l.limite_diario,
	   l.utilizado_hoje
  from conta c
       join saldo s on s.id_conta = c.id
       join limite l on l.id_conta = c.id
 order by c.numero_conta, l.tipo;

```

### Dados de teste disponíveis após o seed

O seed cria três contas de teste com valores pré-criptografados. Os valores em texto claro são:

| Titular | ID da Conta | Agência | Número da Conta | Saldo | Status |
| --- | --- | --- | --- | --- | --- |
| Alice | aaaaaaaa-0000-0000-0000-000000000001 | 0001 | 00001-9 | R$ 10.000,00 | ATIVA |
| Bob | aaaaaaaa-0000-0000-0000-000000000002 | 0001 | 00002-7 | R$ 100,00 | ATIVA |
| Carol | aaaaaaaa-0000-0000-0000-000000000003 | 0001 | 00003-5 | R$ 2.000,00 | BLOQUEADA |

✅ **Cenário normal:** Use `idContaOrigem: aaaaaaaa-0000-0000-0000-000000000001` (Alice) para o fluxo padrão.

⚠️ **Saldo insuficiente:** Use `idContaOrigem: aaaaaaaa-0000-0000-0000-000000000002` (Bob) para testar rejeição.

🚫 **Conta bloqueada:** Use `idContaOrigem: aaaaaaaa-0000-0000-0000-000000000003` (Carol) para testar bloqueio.

Veja exemplos de chamadas completas em docs/api/endpoints.md.

O seed de exemplo cobre três cenários principais:

- **Alice**: conta ativa com saldo normal para fluxos positivos e validação de limites.
- **Bob**: conta ativa com saldo baixo para cenário de saldo insuficiente.
- **Carol**: conta bloqueada para cenário de conta inativa/bloqueada.

Salve seu script como:

```text
seed-db/seed.sql
```

Com os containers do Compose em execução, copie o seed para o container PostgreSQL:

```bash
docker cp seed-db/seed.sql transacao_postgres:/tmp/seed.sql
```

Execute usando as credenciais do seu `.env`:

```bash
docker exec transacao_postgres psql -U <DB_USERNAME> -d <POSTGRES_DB> -f /tmp/seed.sql
```

Exemplo, caso seu `.env` use `DB_USERNAME=ozzy` e `POSTGRES_DB=transacaodb`:

```bash
docker exec transacao_postgres psql -U ozzy -d transacaodb -f /tmp/seed.sql
```

Também é possível entrar no container e executar manualmente:

```bash
docker exec -it transacao_postgres /bin/sh
psql -U <DB_USERNAME> -d <POSTGRES_DB> -f /tmp/seed.sql
```

## Swagger UI e OpenAPI

A documentação interativa fica disponível apenas com o perfil `dev`.

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI spec:

```text
http://localhost:8080/v3/api-docs
```

## Spring Boot Actuator

Os endpoints expostos pela aplicação são:

- `health`
- `info`
- `metrics`
- `prometheus`

Health check:

```text
http://localhost:8080/actuator/health
```

Métricas no formato Prometheus:

```text
http://localhost:8080/actuator/prometheus
```

O endpoint `/actuator/health` é público. O endpoint `/actuator/prometheus` pode exigir autenticação e autoridade apropriada, conforme a configuração de segurança da aplicação.

## Problemas comuns

### Erro de certificado SSL ao chamar Keycloak

Verifique se `certificados/root_ca.crt` foi gerado e se a seção de importação do certificado no `Dockerfile` está correta. Se você não usa uma root CA local, comente ou remova as linhas de cópia e importação do certificado.

### Kafka indisponível ao iniciar

Verifique `EVENTOS_KAFKA_ENABLED`. Quando o valor é `false`, Kafka não é necessário. Quando o valor é `true`, configure as variáveis do broker externo, como `KAFKA_BOOTSTRAP_SERVERS`, credenciais SASL/SCRAM e truststore, se aplicável.

### `DB_URL` não configurada

O perfil `dev` possui default para execução direta:

```text
jdbc:postgresql://localhost:5432/transacaodb
```

No Compose, o container da aplicação usa `DB_URL` explicitamente:

```text
jdbc:postgresql://postgres-transacao:5432/${POSTGRES_DB}
```

### Porta `5433` já ocupada

O PostgreSQL do Compose mapeia `5433:5432` no host. Se a porta `5433` já estiver em uso, libere a porta ou ajuste o mapeamento no `docker-compose.yml`.

[Voltar ao README principal](../../README.md)
