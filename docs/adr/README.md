# Índice de ADRs

ADR significa Architecture Decision Record. Cada ADR registra uma decisão arquitetural importante do projeto, explicando o contexto que levou à decisão, a escolha feita e as consequências positivas e negativas esperadas.

Para ler os ADRs, comece pelos registros de arquitetura base, como Arquitetura Hexagonal e DDD, e depois avance para decisões específicas de persistência, mensageria, segurança, observabilidade e testes. O status indica a situação da decisão no projeto; ADRs aceitos representam decisões vigentes.

| Número  | Título                                                    | Status | Data       | Link                                            |
| ------- | --------------------------------------------------------- | ------ | ---------- | ----------------------------------------------- |
| ADR-001 | Arquitetura Hexagonal                                     | Aceito | 2026-06-17 | [Abrir](ADR-001-arquitetura-hexagonal.md)       |
| ADR-002 | DDD, Aggregates e Eventos de Domínio                      | Aceito | 2026-06-17 | [Abrir](ADR-002-ddd-aggregates.md)              |
| ADR-003 | PostgreSQL 16, Flyway e Locking Otimista                  | Aceito | 2026-06-17 | [Abrir](ADR-003-postgresql.md)                  |
| ADR-004 | Apache Kafka com SASL_SSL e SCRAM-SHA-256                 | Aceito | 2026-06-17 | [Abrir](ADR-004-kafka.md)                       |
| ADR-005 | Outbox Pattern para Consistência Eventual                 | Aceito | 2026-06-17 | [Abrir](ADR-005-outbox-pattern.md)              |
| ADR-006 | OAuth2 Resource Server, JWT, Keycloak e RBAC              | Aceito | 2026-06-17 | [Abrir](ADR-006-oauth2-jwt-keycloak.md)         |
| ADR-007 | HMAC-SHA256 para Integridade e Busca de Colunas Sensíveis | Aceito | 2026-06-17 | [Abrir](ADR-007-hmac-sha256.md)                 |
| ADR-008 | Criptografia AES-256-GCM via JPA AttributeConverter       | Aceito | 2026-06-17 | [Abrir](ADR-008-criptografia-aes256gcm.md)      |
| ADR-009 | UUID como Chave de Idempotência via Header HTTP           | Aceito | 2026-06-17 | [Abrir](ADR-009-idempotencia-uuid.md)           |
| ADR-010 | Rate Limiting com Bucket4j                                | Aceito | 2026-06-17 | [Abrir](ADR-010-rate-limiting-bucket4j.md)      |
| ADR-011 | OpenTelemetry, OTLP e Jaeger                              | Aceito | 2026-06-17 | [Abrir](ADR-011-opentelemetry-jaeger.md)        |
| ADR-012 | Prometheus, Grafana e SLOs de PIX                         | Aceito | 2026-06-17 | [Abrir](ADR-012-prometheus-grafana-slo.md)      |
| ADR-013 | Strategy Pattern para Processamento de Transações         | Aceito | 2026-06-17 | [Abrir](ADR-013-strategy-pattern-transacao.md)  |
| ADR-014 | Locking Otimista e Pessimista                             | Aceito | 2026-06-17 | [Abrir](ADR-014-locking-otimista-pessimista.md) |
| ADR-015 | ArchUnit e Gates de Cobertura JaCoCo                      | Aceito | 2026-06-17 | [Abrir](ADR-015-archunit-testes-arquitetura.md) |
| ADR-016 | Mascaramento de Logs com Logback e JSON em Produção       | Aceito | 2026-06-17 | [Abrir](ADR-016-mascaramento-logs.md)           |
