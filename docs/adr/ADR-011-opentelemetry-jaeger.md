# ADR-011: OpenTelemetry, OTLP e Jaeger

**Data:** 2026-06-17
**Status:** Aceito

## Contexto
O processamento de uma transação atravessa HTTP, serviços de aplicação, persistência, outbox, Kafka e consumidores. Logs isolados não são suficientes para investigar latência, erros e gargalos em fluxos distribuídos. Era necessário rastreamento ponta a ponta com correlação entre logs e traces.

## Decisão
Foi adotado Micrometer Tracing com OpenTelemetry e exportação OTLP para Jaeger. A aplicação usa `spring-boot-starter-opentelemetry`, `micrometer-tracing-bridge-otel` e `opentelemetry-exporter-otlp`.

O endpoint OTLP é configurado em `management.opentelemetry.tracing.export.otlp.endpoint`, com `OTLP_TRACING_ENDPOINT` em produção. No homelab, o endpoint documentado é `https://otlp-jaeger.lab.home/v1/traces`.

A correlação usa:

- `idCorrelacao`: identificador funcional recebido por `X-Correlation-Id` ou gerado pela aplicação.
- `traceId`: identificador técnico do trace distribuído.
- `spanId`: identificador técnico do span.

O `ContextoRequisicaoFilter` propaga `idCorrelacao` para o MDC, e os logs incluem esse valor. O sampling é `1.0` em desenvolvimento e `0.10` em produção.

## Consequências
### Positivas
- Permite investigar uma transação ponta a ponta no Jaeger.
- Facilita correlação entre logs, traces, auditoria, transações e eventos.
- A instrumentação cobre pontos comuns de HTTP, Kafka e JPA com baixa necessidade de código manual.
- Sampling em produção reduz volume e custo operacional.

### Negativas / Trade-offs
- Traces dependem da disponibilidade e configuração correta do endpoint OTLP.
- Sampling de 10% em produção pode não capturar todas as ocorrências.
- Dados em spans e logs precisam continuar respeitando políticas de mascaramento.

## Ver também
- [Dependências Externas](../infraestrutura/dependencias-externas.md)
