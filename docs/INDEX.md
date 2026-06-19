# Índice Geral da Documentação

O `transaction-processing-api` é uma API Java/Spring Boot para processamento de transações financeiras com suporte a PIX, TED e TEF. O projeto usa Arquitetura Hexagonal, DDD, PostgreSQL, Kafka com Outbox Pattern, segurança OAuth2/JWT, auditoria e observabilidade com métricas e rastreamento distribuído.

## Quick Links

| Quero... | Documento |
| --- | --- |
| Quero executar localmente | [Execução Local](desenvolvimento/execucao-local.md) |
| Quero entender a arquitetura | [Visão Geral](arquitetura/visao-geral.md), [Arquitetura Hexagonal](arquitetura/hexagonal.md) e [DDD](arquitetura/ddd.md) |
| Quero entender a infraestrutura de suporte | [Infraestrutura](infraestrutura/visao-geral.md) |
| Quero ver as APIs | [Endpoints da API](api/endpoints.md) |
| Quero resolver um problema em produção | [Troubleshooting](operacao/troubleshooting.md) |
| Quero ver decisões arquiteturais | [Índice de ADRs](adr/README.md) |

## Documentos por Área

| Área | Documento | Descrição |
| --- | --- | --- |
| Arquitetura | [Visão Geral](arquitetura/visao-geral.md) | Apresenta o contexto do sistema, objetivos de negócio, componentes principais e integrações externas. |
| Arquitetura | [Arquitetura Hexagonal](arquitetura/hexagonal.md) | Explica a separação entre domínio, aplicação, portas, adaptadores e infraestrutura. |
| Arquitetura | [Domain-Driven Design](arquitetura/ddd.md) | Descreve bounded contexts, aggregates, value objects, eventos de domínio e glossário. |
| Arquitetura | [Fluxo de Transação](arquitetura/fluxo-transacao.md) | Sequência completa do fluxo PIX/TED/TEF e estorno. |
| API | [Endpoints da API](api/endpoints.md) | Lista endpoints REST de transações, headers, payloads, respostas e modelo de erro. |
| Segurança | [Autenticação e Autorização](seguranca/autenticacao-autorizacao.md) | Documenta OAuth2 Resource Server, JWT, RBAC, criptografia, HMAC, rate limiting e mascaramento. |
| Observabilidade | [Métricas](observabilidade/metricas.md) | Detalha métricas Prometheus/Micrometer, SLOs e consultas operacionais. |
| Observabilidade | [Rastreamento](observabilidade/rastreamento.md) | Explica traces com Micrometer Tracing, OpenTelemetry, OTLP, Jaeger e correlação. |
| Banco de dados | [Modelo de Dados](banco-de-dados/modelo-dados.md) | Descreve o modelo relacional PostgreSQL, tabelas, constraints, índices e migrations Flyway. |
| Mensageria | [Kafka e Outbox](mensageria/kafka-outbox.md) | Documenta publicação assíncrona, Outbox Pattern, tópicos Kafka, DLQ e idempotência de consumo. |
| Desenvolvimento | [Execução Local](desenvolvimento/execucao-local.md) | Centraliza orientações para executar o projeto em ambiente local. |
| Desenvolvimento | [Fluxos de Negócio](desenvolvimento/fluxos-negocio.md) | Regras de PIX, TED, TEF e estorno em linguagem de negócio. |
| Infraestrutura | [Visão Geral](infraestrutura/visao-geral.md) | Apresenta os serviços de suporte da aplicação e sua relação com o ambiente do docker-compose e o homelab. |
| Infraestrutura | [Dependências Externas](infraestrutura/dependencias-externas.md) | Descreve cada serviço externo: PostgreSQL, Keycloak, Kafka, Prometheus, Grafana, Jaeger e Traefik. |
| Infraestrutura | [Homelab](infraestrutura/homelab.md) | Explica o ambiente homelab onde rodam os serviços de suporte externos ao docker-compose da app. |
| Infraestrutura | [Certificados e Truststore](infraestrutura/certificados-truststore.md) | Descreve o uso da root CA local, importação para a JVM no Dockerfile e criação do truststore Kafka. |
| Operação | [Variáveis de Ambiente](operacao/variaveis-ambiente.md) | Lista configurações e variáveis necessárias para os ambientes da aplicação. |
| Operação | [Troubleshooting](operacao/troubleshooting.md) | Guia de diagnóstico para problemas operacionais comuns: banco, Flyway, JWT, Kafka, Outbox, DLQ, métricas e traces. |
| Roadmap | [Roadmap](roadmap/roadmap.md) | Resume funcionalidades implementadas, em andamento e planejadas. |
| ADR | [Índice de ADRs](adr/README.md) | Lista as decisões arquiteturais registradas no projeto. |

## ADRs

Os ADRs registram decisões arquiteturais relevantes, seu contexto e suas consequências. Para uma visão tabular com status e data, consulte o [índice dos ADRs](adr/README.md).

1. [ADR-001: Arquitetura Hexagonal](adr/ADR-001-arquitetura-hexagonal.md)
2. [ADR-002: DDD, Aggregates e Eventos de Domínio](adr/ADR-002-ddd-aggregates.md)
3. [ADR-003: PostgreSQL 16, Flyway e Locking Otimista](adr/ADR-003-postgresql.md)
4. [ADR-004: Apache Kafka com SASL_SSL e SCRAM-SHA-256](adr/ADR-004-kafka.md)
5. [ADR-005: Outbox Pattern para Consistência Eventual](adr/ADR-005-outbox-pattern.md)
6. [ADR-006: OAuth2 Resource Server, JWT, Keycloak e RBAC](adr/ADR-006-oauth2-jwt-keycloak.md)
7. [ADR-007: HMAC-SHA256 para Integridade e Busca de Colunas Sensíveis](adr/ADR-007-hmac-sha256.md)
8. [ADR-008: Criptografia AES-256-GCM via JPA AttributeConverter](adr/ADR-008-criptografia-aes256gcm.md)
9. [ADR-009: UUID como Chave de Idempotência via Header HTTP](adr/ADR-009-idempotencia-uuid.md)
10. [ADR-010: Rate Limiting com Bucket4j](adr/ADR-010-rate-limiting-bucket4j.md)
11. [ADR-011: OpenTelemetry, OTLP e Jaeger](adr/ADR-011-opentelemetry-jaeger.md)
12. [ADR-012: Prometheus, Grafana e SLOs de PIX](adr/ADR-012-prometheus-grafana-slo.md)
13. [ADR-013: Strategy Pattern para Processamento de Transações](adr/ADR-013-strategy-pattern-transacao.md)
14. [ADR-014: Locking Otimista e Pessimista](adr/ADR-014-locking-otimista-pessimista.md)
15. [ADR-015: ArchUnit e Gates de Cobertura JaCoCo](adr/ADR-015-archunit-testes-arquitetura.md)
16. [ADR-016: Mascaramento de Logs com Logback e JSON em Produção](adr/ADR-016-mascaramento-logs.md)
