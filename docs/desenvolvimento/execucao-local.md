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

Crie um arquivo `.env` na raiz do projeto:

```bash
cd transaction-processing-api
touch .env
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

## Seed da base de dados

O diretório `seed-db` está no `.gitignore` e não é versionado. Use-o apenas para arquivos locais de carga manual.

Exemplo de organização local:

```bash
mkdir -p seed-db
```

Salve seu script local como:

```text
seed-db/seed.sql
```

O seed documentado no `OLD_README.md` cobre três cenários principais:

- **Alice**: conta ativa com saldo normal para fluxos positivos e validação de limites.
- **Bob**: conta ativa com saldo baixo para cenário de saldo insuficiente.
- **Carol**: conta bloqueada para cenário de conta inativa/bloqueada.

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
