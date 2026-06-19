# Dependências Externas

Este documento consolida as dependências de infraestrutura usadas pela `transaction-processing-api` fora do código de domínio.

## PostgreSQL

A aplicação usa PostgreSQL como banco relacional principal.

| Item                  | Configuração                           |
| --------------------- | -------------------------------------- |
| Versão                | PostgreSQL 16                          |
| Provisionamento local | `docker-compose.yml` deste repositório |
| Serviço Compose       | `postgres-transacao`                   |
| Porta no host         | `5433`                                 |
| Porta na rede Docker  | `5432`                                 |
| Migrações             | Flyway                                 |

O estado transacional, contas, auditoria, outbox e idempotência de consumo são persistidos no PostgreSQL. As estruturas de banco devem ser evoluídas por migrations Flyway.

## Keycloak

O Keycloak atua como Authorization Server OAuth2/OIDC. A API valida tokens JWT como Resource Server usando issuer e JWKS configurados por ambiente.

| Item            | Configuração                                                              |
| --------------- | ------------------------------------------------------------------------- |
| Host no homelab | `keycloak.lab.home`                                                       |
| Realm           | `bancario`                                                                |
| Issuer esperado | `https://keycloak.lab.home/realms/bancario`                               |
| JWKS            | `https://keycloak.lab.home/realms/bancario/protocol/openid-connect/certs` |

Roles necessárias para autorização:

- `CLIENTE`
- `OPERADOR`
- `GERENTE`
- `ADMIN`
- `SERVICO_INTERNO`

## Kafka

O Kafka é usado para publicação e consumo de eventos transacionais a partir da outbox. A integração é condicional: a aplicação só deve depender do broker quando `EVENTOS_KAFKA_ENABLED=true`.

| Item           | Configuração                 |
| -------------- | ---------------------------- |
| Broker         | `kafka.lab.home:9094`        |
| Protocolo      | `SASL_SSL`                   |
| Mecanismo SASL | `SCRAM-SHA-256`              |
| Habilitação    | `EVENTOS_KAFKA_ENABLED=true` |

Tópicos de transação:

- `transacoes.iniciadas`
- `transacoes.concluidas`
- `transacoes.falhas`
- `transacoes.estornadas`

Cada tópico pode ter uma DLQ correspondente com sufixo `.DLQ`, por exemplo `transacoes.iniciadas.DLQ`.

## Prometheus

O Prometheus coleta métricas expostas pelo Spring Boot Actuator.

| Item                  | Configuração                                                   |
| --------------------- | -------------------------------------------------------------- |
| Endpoint da aplicação | `/actuator/prometheus`                                         |
| Uso                   | Saúde operacional, volume, falhas, latência e estado da outbox |

O endpoint pode exigir autenticação e autoridade apropriada conforme a configuração de segurança da aplicação.

## Grafana

O Grafana consome métricas do Prometheus para dashboards operacionais, incluindo SLOs e métricas de transação.

Referência arquitetural: [ADR-012: Prometheus, Grafana e SLOs de PIX](../adr/ADR-012-prometheus-grafana-slo.md).

## Jaeger / OTLP

A aplicação exporta traces distribuídos via OTLP HTTP para o Jaeger.

| Item               | Configuração                                                                       |
| ------------------ | ---------------------------------------------------------------------------------- |
| Variável           | `OTLP_TRACING_ENDPOINT`                                                            |
| Exemplo no homelab | `https://otlp-jaeger.lab.home/v1/traces`                                           |
| Perfil             | Habilitado somente no perfil `prod`; em `dev`, existe default local para validação |

O endpoint de produção deve ser informado explicitamente por ambiente.

## Traefik

O Traefik é o reverse proxy do homelab. Ele centraliza a terminação TLS e roteia requisições HTTPS para a aplicação e demais serviços internos.

No cenário do homelab, a rota HTTPS da API deve apontar para o container ou host em que a `transaction-processing-api` está exposta. O `docker-compose.yml` da aplicação não provisiona o Traefik.
