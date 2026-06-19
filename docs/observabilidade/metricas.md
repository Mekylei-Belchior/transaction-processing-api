# Métricas

O `transaction-processing-api` expõe métricas pelo Spring Boot Actuator em `/actuator/prometheus`, usando Micrometer com registry Prometheus. As métricas customizadas acompanham o ciclo de vida das transações, a latência de processamento e o estado da outbox.

## Métricas customizadas

| Nome                   | Tipo            | Tags                                              | Descrição                                                                                          |
| ---------------------- | --------------- | ------------------------------------------------- | -------------------------------------------------------------------------------------------------- |
| `transacao.criada`     | Counter         | `tipo=PIX\|TED\|TEF`                              | Conta transações criadas pela API, separadas por modalidade.                                       |
| `transacao.processada` | Counter         | `tipo=PIX\|TED\|TEF`, `status=COMPLETADA\|FALHOU` | Conta transações processadas, separando sucesso e falha por modalidade.                            |
| `transacao.duracao`    | Histogram/Timer | `tipo=PIX\|TED\|TEF`                              | Mede a duração do processamento da transação. Publica histograma e percentis `p50`, `p95` e `p99`. |
| `outbox.pendente`      | Gauge           | Nenhuma                                           | Informa a quantidade atual de eventos pendentes na outbox.                                         |
| `outbox.publicado`     | Counter         | Nenhuma                                           | Conta eventos publicados com sucesso a partir da outbox.                                           |
| `outbox.falhou`        | Counter         | Nenhuma                                           | Conta falhas ao publicar eventos a partir da outbox.                                               |

No formato Prometheus, o Micrometer normaliza nomes com ponto para underscore. Counters recebem o sufixo `_total`, e timers/histogramas publicam séries auxiliares como buckets, contagem e soma. Exemplos:

```text
transacao_criada_total{tipo="PIX"}
transacao_processada_total{tipo="PIX",status="COMPLETADA"}
transacao_duracao_seconds_bucket{tipo="PIX",le="8.0"}
outbox_pendente
outbox_publicado_total
outbox_falhou_total
```

## Consulta local

Com a aplicação rodando localmente, consulte o endpoint Prometheus:

```bash
curl -H "Authorization: Bearer <token>" http://localhost:8080/actuator/prometheus
```

Para filtrar apenas métricas transacionais:

```bash
curl -H "Authorization: Bearer <token>" http://localhost:8080/actuator/prometheus | grep '^transacao_'
```

Para filtrar métricas da outbox:

```bash
curl -H "Authorization: Bearer <token>" http://localhost:8080/actuator/prometheus | grep '^outbox_'
```

O endpoint `/actuator/prometheus` requer permissão `ROLE_METRICAS.LEITURA`. Em ambientes protegidos por OAuth2, use um token válido para o client ou usuário autorizado a ler métricas.

## Homelab

### Prometheus

O Prometheus v3.12.0 está disponível em:

```text
https://prometheus.lab.home
```

Ele coleta as métricas da API pelo job `transaction-processing-api`, em `/actuator/prometheus`, com intervalo de scrape de `15s`. O scrape é autenticado via OAuth2 com:

| Campo             | Valor                        |
| ----------------- | ---------------------------- |
| Client            | `transaction-api-prometheus` |
| Realm             | `bancario`                   |
| Endpoint coletado | `/actuator/prometheus`       |
| Target            | `192.168.0.105:8080`         |

No Prometheus, use a tela de targets para confirmar se o alvo da API está `UP` e a tela de consulta para executar PromQL sobre as séries `transacao_*` e `outbox_*`.

### Grafana

O Grafana v13.0.2 está disponível em:

```text
https://grafana.lab.home
```

O dashboard principal para a API é:

```text
Transacoes PIX — SLO
```

Esse dashboard acompanha taxa de falha, latência P95, volume de transações PIX e sinais da outbox.

## SLOs implementados

| SLO               | Indicador                                                                                      | Limiar | Alerta               | Severidade |
| ----------------- | ---------------------------------------------------------------------------------------------- | ------ | -------------------- | ---------- |
| Taxa de falha PIX | Percentual de transações PIX com `status="FALHOU"` sobre o total de transações PIX processadas | `< 2%` | `PIXTaxaFalhaAlta`   | `warning`  |
| Latência P95 PIX  | Percentil 95 da duração de processamento de transações PIX                                     | `< 8s` | `PIXLatenciaP95Alta` | `critical` |

PromQL de referência para taxa de falha PIX:

```promql
sum(rate(transacao_processada_total{tipo="PIX",status="FALHOU"}[5m]))
/
sum(rate(transacao_processada_total{tipo="PIX"}[5m]))
```

O alerta `PIXTaxaFalhaAlta` dispara quando o resultado fica acima de `0.02`.

PromQL de referência para latência P95 PIX:

```promql
histogram_quantile(
  0.95,
  sum by (le) (rate(transacao_duracao_seconds_bucket{tipo="PIX"}[5m]))
)
```

O alerta `PIXLatenciaP95Alta` dispara quando o resultado fica acima de `8`.
