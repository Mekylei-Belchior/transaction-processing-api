# Variáveis de Ambiente

Este documento consolida as variáveis de ambiente usadas pela `transaction-processing-api` em execução local, Docker Compose e produção.

Os valores sensíveis, como senhas, chaves criptográficas, credenciais Kafka e senhas de truststore, devem ser fornecidos por variáveis de ambiente, secret manager ou mecanismo equivalente. Não versione arquivos `.env` com valores reais.

## Resumo

| Variável | Finalidade | Default base | Default `dev` | Obrigatória em `prod` |
| --- | --- | --- | --- | --- |
| `PORT` | Porta HTTP da aplicação no perfil `prod`. | - | `8080` via `application.yml` | Não; default `8081` no `prod` |
| `SPRING_PROFILES_ACTIVE` | Perfil ativo da aplicação Spring. | - | `dev` no `docker-compose.yml` | Sim, para definir explicitamente o perfil esperado |
| `POSTGRES_DB` | Nome do banco usado pelo `docker-compose.yml` para montar `DB_URL`. | - | - | Somente quando usar Compose |
| `DB_URL` | URL JDBC do PostgreSQL. | - | `jdbc:postgresql://localhost:5432/transacaodb` | Sim |
| `DB_USERNAME` | Usuário do PostgreSQL. | - | `postgres` | Sim |
| `DB_PASSWORD` | Senha do PostgreSQL. | - | `password` | Sim |
| `OAUTH2_JWKS_URI` | Endpoint JWKS com as chaves públicas do provedor OAuth2/OIDC. | Sem default | `https://keycloak.lab.home/realms/bancario/protocol/openid-connect/certs` | Sim |
| `OAUTH2_ISSUER_URI` | Issuer esperado no JWT do realm `bancario`. | Sem default | `https://keycloak.lab.home/realms/bancario` | Sim |
| `APP_CRIPTOGRAFIA_CHAVE` | Chave AES-256-GCM em Base64, com 32 bytes após decodificação. | Sem default | Chave local de desenvolvimento | Sim |
| `APP_HMAC_CHAVE` | Chave usada para assinaturas determinísticas HMAC-SHA256. | Sem default | Chave local de desenvolvimento | Sim |
| `RATE_LIMIT_REQUISICOES_POR_MINUTO` | Limite de requisições por IP por minuto. | `60` | `300` | Não; default `60` herdado da base |
| `ANTIFRAUDE_LIMITE_APROVACAO_AUTOMATICA` | Valor máximo para aprovação automática no antifraude stub. | `10000.00` no código | `10000.00` | Não |
| `EVENTOS_KAFKA_ENABLED` | Habilita produtor, consumidores e configuração Kafka. | `false` | `true` | Não; default `true` no `prod` |
| `KAFKA_BOOTSTRAP_SERVERS` | Lista de brokers Kafka. | `kafka.lab.home:9094` | `kafka.lab.home:9094` | Não, mas deve ser revisada por ambiente |
| `KAFKA_USERNAME` | Usuário SASL/SCRAM-SHA-256 do Kafka. | Sem default | Sem default | Sim, quando Kafka estiver habilitado |
| `KAFKA_PASSWORD` | Senha SASL/SCRAM-SHA-256 do Kafka. | Sem default | Sem default | Sim, quando Kafka estiver habilitado |
| `KAFKA_SSL_TRUSTSTORE_LOCATION` | Caminho da truststore usada para validar o broker Kafka. | Sem default | Sem default | Sim, quando Kafka estiver habilitado |
| `KAFKA_SSL_TRUSTSTORE_PASSWORD` | Senha da truststore Kafka. | Sem default | Sem default | Sim, quando Kafka estiver habilitado |
| `KAFKA_SSL_TRUSTSTORE_TYPE` | Tipo da truststore Kafka. | `PKCS12` | `PKCS12` | Não; default `JKS` no `prod` |
| `EVENTOS_KAFKA_DLQ_MAX_TENTATIVAS` | Quantidade máxima de tentativas antes de enviar para DLQ. | `3` | `3` | Não |
| `EVENTOS_KAFKA_DLQ_INTERVALO_REPROCESSAMENTO` | Intervalo entre tentativas do consumidor antes da DLQ. | `10s` | `10s` | Não |
| `EVENTOS_OUTBOX_LOTE_PUBLICACAO` | Tamanho máximo do lote publicado pelo job de outbox. | `50` | `50` | Não |
| `EVENTOS_OUTBOX_INTERVALO_PUBLICACAO_MS` | Intervalo do scheduler de publicação da outbox, em milissegundos. | `5000` | `5000` | Não |
| `EVENTOS_OUTBOX_INTERVALO_REPROCESSAMENTO` | Intervalo para nova tentativa de eventos com falha na outbox. | `30s` | `30s` | Não |
| `EVENTOS_OUTBOX_TIMEOUT_ENVIO_MS` | Timeout de envio para Kafka, em milissegundos. | `5000` | `5000` | Não |
| `OTLP_TRACING_ENDPOINT` | Endpoint OTLP HTTP para exportação de traces. | Sem default | `https://otlp-jaeger.lab.home/v1/traces` | Sim |

## Banco de Dados

A aplicação usa PostgreSQL via Spring Data JPA e Flyway.

| Variável | Descrição | Observações |
| --- | --- | --- |
| `DB_URL` | URL JDBC do banco PostgreSQL. | No perfil `dev`, o default é `jdbc:postgresql://localhost:5432/transacaodb`. No Compose, a aplicação recebe `jdbc:postgresql://postgres-transacao:5432/${POSTGRES_DB}`, apontando para o serviço `postgres-transacao` na porta interna `5432`. |
| `DB_USERNAME` | Usuário de conexão com o banco. | Default `dev`: `postgres`. No Compose também preenche `POSTGRES_USER`. |
| `DB_PASSWORD` | Senha de conexão com o banco. | Default `dev`: `password`. No Compose também preenche `POSTGRES_PASSWORD`. |
| `POSTGRES_DB` | Nome do banco criado pelo container PostgreSQL. | Variável do `docker-compose.yml`, usada para montar `DB_URL` no container da aplicação. |

Em produção, `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` não possuem default no perfil `prod` e devem ser informados explicitamente.

## Perfil e Porta

| Variável | Descrição | Observações |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | Define o perfil Spring ativo. | O `docker-compose.yml` define `dev`. Para produção, use `prod`. |
| `PORT` | Porta HTTP usada pelo perfil `prod`. | Default `prod`: `8081`. O perfil `dev` usa `8080` pela configuração base. |

## OAuth2 / Keycloak

A API atua como OAuth2 Resource Server e valida JWTs por JWKS e issuer.

| Variável | Descrição | Default `dev` | Produção |
| --- | --- | --- | --- |
| `OAUTH2_JWKS_URI` | Endpoint de chaves públicas do Keycloak ou provedor OIDC. | `https://keycloak.lab.home/realms/bancario/protocol/openid-connect/certs` | Obrigatória, sem default no perfil `prod`. |
| `OAUTH2_ISSUER_URI` | Issuer esperado no token JWT, referente ao realm `bancario`. | `https://keycloak.lab.home/realms/bancario` | Obrigatória, sem default no perfil `prod`. |

O `issuer` configurado deve bater exatamente com o claim `iss` presente no token.

## Criptografia AES-256-GCM

| Variável | Descrição | Requisito |
| --- | --- | --- |
| `APP_CRIPTOGRAFIA_CHAVE` | Chave usada pelo conversor JPA de criptografia em repouso. | Deve ser Base64 de 32 bytes após decodificação, compatível com AES-256-GCM. |

O perfil `dev` possui uma chave default apenas para desenvolvimento local:

```text
MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=
```

Em produção, não há default seguro. Gere a chave com fonte criptograficamente segura, armazene em mecanismo de secrets e planeje rotação com cuidado, pois dados já persistidos dependem da chave usada para descriptografia.

## HMAC-SHA256

| Variável | Descrição | Observações |
| --- | --- | --- |
| `APP_HMAC_CHAVE` | Chave usada para HMAC-SHA256 determinístico de dados sensíveis pesquisáveis. | O perfil `dev` possui default somente para execução local. Em produção, defina uma chave forte e protegida por secret manager. |

O default do perfil `dev` é:

```text
xK9mP2vL8nQ5rT1wY4zB7cF3hJ6kU8aR0dG2hN5pS7vL9m
```

Como o HMAC é determinístico, trocar a chave altera as assinaturas geradas para o mesmo valor de entrada. Faça rotação somente com estratégia de migração dos dados afetados.

## Rate Limiting

| Variável | Descrição | Default |
| --- | --- | --- |
| `RATE_LIMIT_REQUISICOES_POR_MINUTO` | Quantidade máxima de requisições por IP por minuto. | `60` na configuração base; `300` no perfil `dev`. |

O valor efetivo em `dev` é `300` quando a variável não for informada. Em produção, a aplicação herda o default base `60`, salvo sobrescrita por ambiente.

## Antifraude

| Variável | Descrição | Default |
| --- | --- | --- |
| `ANTIFRAUDE_LIMITE_APROVACAO_AUTOMATICA` | Valor máximo para aprovação automática no antifraude stub. | `10000.00` |

O adaptador atual é um stub. Transações dentro do limite configurado são consideradas automaticamente aprovadas pelo antifraude.

## Kafka

A integração Kafka usa `SASL_SSL`, mecanismo `SCRAM-SHA-256`, produtor idempotente e consumidores com commit manual.

| Variável | Descrição | Default / Obrigatoriedade |
| --- | --- | --- |
| `EVENTOS_KAFKA_ENABLED` | Liga ou desliga produtores, consumidores, error handler e publicação via outbox. | `false` em `application.yml`; `true` nos perfis `dev` e `prod`. |
| `KAFKA_BOOTSTRAP_SERVERS` | Endereço dos brokers Kafka. | `kafka.lab.home:9094`. |
| `KAFKA_USERNAME` | Usuário SASL/SCRAM-SHA-256. | Sem default; obrigatório quando Kafka estiver habilitado. |
| `KAFKA_PASSWORD` | Senha SASL/SCRAM-SHA-256. | Sem default; obrigatória quando Kafka estiver habilitado. |
| `KAFKA_SSL_TRUSTSTORE_LOCATION` | Caminho da truststore do cliente Kafka. | Sem default; obrigatório quando Kafka estiver habilitado. Use o prefixo `file:` para caminho de arquivo, por exemplo `file:/opt/secrets/kafka-client-truststore.p12`. |
| `KAFKA_SSL_TRUSTSTORE_PASSWORD` | Senha da truststore. | Sem default; obrigatória quando Kafka estiver habilitado. |
| `KAFKA_SSL_TRUSTSTORE_TYPE` | Tipo da truststore. | `PKCS12` na base e no perfil `dev`; `JKS` como default do perfil `prod`. |

O `docker-compose.yml` atual repassa para o container da aplicação as variáveis de banco, OAuth2, criptografia, rate limit e antifraude. Se executar a aplicação pelo Compose com Kafka habilitado, inclua também as variáveis Kafka no serviço `app` ou defina `EVENTOS_KAFKA_ENABLED=false` para uma execução sem broker.

## Outbox / DLQ

| Variável | Descrição | Default |
| --- | --- | --- |
| `EVENTOS_KAFKA_DLQ_MAX_TENTATIVAS` | Quantidade máxima de tentativas do consumidor antes da DLQ. | `3` |
| `EVENTOS_KAFKA_DLQ_INTERVALO_REPROCESSAMENTO` | Intervalo fixo entre tentativas antes da DLQ. | `10s` |
| `EVENTOS_OUTBOX_LOTE_PUBLICACAO` | Quantidade máxima de eventos publicados por ciclo do job. | `50` |
| `EVENTOS_OUTBOX_INTERVALO_PUBLICACAO_MS` | Frequência do scheduler de publicação da outbox. | `5000` |
| `EVENTOS_OUTBOX_INTERVALO_REPROCESSAMENTO` | Atraso para nova tentativa de eventos `FALHOU` na outbox. | `30s` |
| `EVENTOS_OUTBOX_TIMEOUT_ENVIO_MS` | Tempo máximo aguardando confirmação de envio ao Kafka. | `5000` |

O sufixo dos tópicos de DLQ é fixo na configuração da aplicação como `.DLQ`.

## OTLP / Jaeger

| Variável | Descrição | Default / Obrigatoriedade |
| --- | --- | --- |
| `OTLP_TRACING_ENDPOINT` | Endpoint OTLP HTTP configurado em `management.opentelemetry.tracing.export.otlp.endpoint`. | No `dev`, há default para `https://otlp-jaeger.lab.home/v1/traces`. No `prod`, é obrigatório e não possui default. |

O sampling de traces é configurado por perfil:

| Perfil | Sampling |
| --- | --- |
| `dev` | `1.0`, ou 100% das requisições. |
| `prod` | `0.10`, ou 10% das requisições. |

## Logs / Observabilidade

No perfil `dev`, a aplicação usa logs no padrão de console com campos de rastreabilidade como `idCorrelacao`, nível `DEBUG` para pacotes do projeto, Swagger ativo e sampling de tracing em 100%. Alguns frameworks têm níveis reduzidos para controlar ruído, como Kafka em `WARN`.

No perfil `prod`, a aplicação usa logs estruturados em JSON no console com mascaramento de dados sensíveis, nível `INFO` para `com.mekylei.transactionprocessing`, Swagger desabilitado e Actuator expondo apenas `health` e `prometheus`.

## Diferenças entre `dev` e `prod`

| Item | `dev` | `prod` |
| --- | --- | --- |
| Porta HTTP | `8080` | `8081` por default, sobrescritível por `PORT` |
| Banco de dados | Defaults locais para `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` | `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` obrigatórios |
| OAuth2 / Keycloak | Defaults para `keycloak.lab.home` no realm `bancario` | `OAUTH2_JWKS_URI` e `OAUTH2_ISSUER_URI` obrigatórios |
| Swagger / OpenAPI | Ativo | Desabilitado |
| Logs da aplicação | `DEBUG` para pacotes do projeto; console de desenvolvimento | `INFO` para aplicação; JSON estruturado |
| Criptografia e HMAC | Defaults presentes apenas para desenvolvimento local | Sem defaults; chaves obrigatórias |
| `RATE_LIMIT_REQUISICOES_POR_MINUTO` | Default efetivo `300` | Default efetivo `60` |
| `EVENTOS_KAFKA_ENABLED` | `true` | `true` |
| `KAFKA_SSL_TRUSTSTORE_TYPE` | `PKCS12` | `JKS` |
| `OTLP_TRACING_ENDPOINT` | Pode ficar ausente porque há default local para Jaeger | Obrigatório |
| Sampling de traces | `100%` | `10%` |
| Actuator exposto | `health`, `info`, `metrics`, `prometheus` | `health`, `prometheus` |

## Exemplo de `.env` para `dev`

O arquivo `.env` pode ser usado pela IDE ou pelo `docker compose`. Ele não deve ser versionado.

Use placeholders descritivos para qualquer segredo real:

```env
POSTGRES_DB=transacaodb
DB_USERNAME=postgres
DB_PASSWORD=<senha-local-do-postgres>

OAUTH2_JWKS_URI=https://keycloak.lab.home/realms/bancario/protocol/openid-connect/certs
OAUTH2_ISSUER_URI=https://keycloak.lab.home/realms/bancario

APP_CRIPTOGRAFIA_CHAVE=<base64-de-32-bytes-para-desenvolvimento>
APP_HMAC_CHAVE=<chave-hmac-local-para-desenvolvimento>

RATE_LIMIT_REQUISICOES_POR_MINUTO=300
ANTIFRAUDE_LIMITE_APROVACAO_AUTOMATICA=10000.00

EVENTOS_KAFKA_ENABLED=true
EVENTOS_KAFKA_DLQ_MAX_TENTATIVAS=3
EVENTOS_KAFKA_DLQ_INTERVALO_REPROCESSAMENTO=10s
EVENTOS_OUTBOX_LOTE_PUBLICACAO=50
EVENTOS_OUTBOX_INTERVALO_PUBLICACAO_MS=5000
EVENTOS_OUTBOX_INTERVALO_REPROCESSAMENTO=30s
EVENTOS_OUTBOX_TIMEOUT_ENVIO_MS=5000

KAFKA_BOOTSTRAP_SERVERS=kafka.lab.home:9094
KAFKA_USERNAME=<usuario-sasl-scram>
KAFKA_PASSWORD=<senha-sasl-scram>
KAFKA_SSL_TRUSTSTORE_LOCATION=file:/<caminho>/kafka-client-truststore.p12
KAFKA_SSL_TRUSTSTORE_PASSWORD=<senha-do-truststore>
KAFKA_SSL_TRUSTSTORE_TYPE=PKCS12

OTLP_TRACING_ENDPOINT=https://otlp-jaeger.lab.home/v1/traces
```

Para executar localmente sem Kafka, defina:

```env
EVENTOS_KAFKA_ENABLED=false
```
