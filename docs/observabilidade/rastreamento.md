# Rastreamento

O `transaction-processing-api` usa Micrometer Tracing com OpenTelemetry para gerar traces distribuídos e exportá-los por OTLP para o Jaeger. O objetivo é acompanhar uma requisição desde a borda HTTP até os componentes internos, incluindo processamento transacional, mensageria, persistência e publicação de eventos.

## Fluxo de rastreamento

```text
Aplicação Spring Boot
        -> Micrometer Tracing
        -> OpenTelemetry
        -> OTLP HTTP
        -> Jaeger
```

A aplicação envia spans para o endpoint OTLP:

```text
https://otlp-jaeger.lab.home/v1/traces
```

O Spring Boot auto-configura a instrumentação a partir de `spring.application.name`, `management.tracing` e `management.opentelemetry.tracing.export.otlp`. A instrumentação cobre os pontos principais do fluxo, como HTTP, Kafka e JPA, sem exigir instrumentação manual para cada operação.

## Correlação entre logs e traces

A correlação usa três identificadores complementares:

| Campo | Origem | Uso |
| --- | --- | --- |
| `idCorrelacao` | Header HTTP `X-Correlation-Id` ou UUID gerado pela aplicação | Identificador funcional da requisição. Propaga para MDC, transação, auditoria e eventos. |
| `traceId` | Micrometer Tracing/OpenTelemetry | Identificador técnico do trace distribuído no Jaeger. Agrupa todos os spans de uma execução rastreada. |
| `spanId` | Micrometer Tracing/OpenTelemetry | Identificador técnico de um span específico dentro do trace. Ajuda a localizar a operação exata. |

O filtro de contexto lê o header `X-Correlation-Id`. Quando o header está ausente ou inválido, a aplicação gera um novo UUID. O valor efetivo é colocado no MDC com a chave `idCorrelacao`, permitindo que todos os logs emitidos durante a requisição carreguem o mesmo identificador.

Em produção, os logs são emitidos em JSON estruturado com mascaramento automático de dados sensíveis. Em desenvolvimento, os logs são coloridos no console e exibem o `idCorrelacao` no padrão de log.

## Sampling por perfil

| Perfil | Probabilidade | Uso esperado |
| --- | --- | --- |
| `dev` | `1.0` | Amostra 100% das requisições para facilitar depuração local e validação de instrumentação. |
| `prod` | `0.10` | Amostra 10% das requisições para reduzir custo e volume mantendo visibilidade operacional. |

Em desenvolvimento, o endpoint OTLP padrão é:

```text
https://otlp-jaeger.lab.home/v1/traces
```

Em produção, o endpoint deve ser fornecido por `OTLP_TRACING_ENDPOINT`.

## Homelab

O Jaeger está disponível em:

```text
https://otlp-jaeger.lab.home
```

Para investigar uma requisição:

1. Acesse o Jaeger no homelab.
2. Selecione o serviço correspondente ao `spring.application.name` da API.
3. Use filtros de tempo compatíveis com o horário da ocorrência.
4. Busque pelo `traceId` quando ele estiver disponível nos logs ou navegue pelos traces recentes do serviço.
5. Abra o trace e inspecione spans de HTTP, Kafka, JPA e operações internas para identificar latência, erro ou gargalo.

## Uso do `idCorrelacao`

O `idCorrelacao` é o identificador mais útil para atravessar logs, respostas da API, registros de auditoria, transações persistidas e eventos de domínio.

Ao chamar a API, envie o header:

```bash
curl -H "X-Correlation-Id: 33333333-3333-3333-3333-333333333333" \
     -H "Authorization: Bearer <token>" \
     http://localhost:8080/actuator/health
```

Com esse valor, a investigação segue este roteiro:

1. Pesquise `idCorrelacao=33333333-3333-3333-3333-333333333333` nos logs.
2. Nos logs encontrados, identifique `traceId` e `spanId` quando disponíveis.
3. Use o `traceId` no Jaeger para abrir o trace distribuído.
4. Compare a linha do tempo dos spans com os logs que carregam o mesmo `idCorrelacao`.
5. Se necessário, consulte a transação, auditoria ou outbox usando o mesmo `idCorrelacao`.

Essa estratégia permite partir de um chamado funcional, como uma transação PIX informada pelo cliente, e chegar ao detalhe técnico do trace sem depender apenas de horário aproximado ou mensagem de erro.
