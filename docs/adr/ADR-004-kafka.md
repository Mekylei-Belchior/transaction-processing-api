# ADR-004: Apache Kafka com SASL_SSL e SCRAM-SHA-256

**Data:** 2026-06-17
**Status:** Aceito

## Contexto
O sistema precisa publicar e consumir eventos de transação de forma assíncrona, desacoplando o processamento interno de consumidores externos e fluxos posteriores. Como os eventos carregam dados operacionais sensíveis e são trafegados fora do processo da API, era necessário definir transporte seguro, autenticação no broker e semântica resiliente de entrega.

## Decisão
Foi adotado Apache Kafka para mensageria de eventos transacionais. A aplicação usa `spring-boot-starter-kafka`, `KafkaConfig`, `KafkaEventoProdutor` e consumidores em `com.mekylei.transactionprocessing.mensageria.consumidor`.

A conexão com Kafka usa:

- `spring.kafka.security.protocol=SASL_SSL`
- `spring.kafka.properties.sasl.mechanism=SCRAM-SHA-256`
- truststore configurada por `KAFKA_SSL_TRUSTSTORE_LOCATION`, `KAFKA_SSL_TRUSTSTORE_PASSWORD` e `KAFKA_SSL_TRUSTSTORE_TYPE`
- credenciais configuradas por `KAFKA_USERNAME` e `KAFKA_PASSWORD`

Os tópicos de domínio são configurados em `app.eventos.topicos`:

- `transacoes.iniciadas`
- `transacoes.concluidas`
- `transacoes.falhas`
- `transacoes.estornadas`

O produtor usa `acks=all`, `enable.idempotence=true` e chave Kafka baseada no `idAgregado` da transação. Consumidores usam `enable-auto-commit=false` e `isolation.level=read_committed`. A DLQ é configurada com sufixo `.DLQ` por `KafkaConfig`, `DefaultErrorHandler` e `DeadLetterPublishingRecoverer`.

## Consequências
### Positivas
- Eventos transacionais podem ser consumidos de forma assíncrona e desacoplada.
- SASL_SSL protege transporte e autenticação com o broker.
- Chave por agregado favorece ordenação por transação dentro da mesma partição.
- DLQ e retries controlados melhoram a operação de falhas de consumo.

### Negativas / Trade-offs
- A operação passa a depender de broker Kafka, certificados, truststore e secrets.
- A semântica prática é at-least-once, então consumidores precisam ser idempotentes.
- Diagnóstico de falhas exige correlacionar API, outbox, Kafka, DLQ e consumidores.

## Ver também
- [Certificados e Truststore](../infraestrutura/certificados-truststore.md)
- [Dependências Externas](../infraestrutura/dependencias-externas.md)
