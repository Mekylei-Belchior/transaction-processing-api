# ADR-012: Prometheus, Grafana e SLOs de PIX

**Data:** 2026-06-17
**Status:** Aceito

## Contexto
A operação da API precisa acompanhar saúde, volume, falhas, latência e backlog de publicação de eventos. Para o fluxo PIX, que é crítico para experiência do usuário, era necessário transformar métricas em SLOs e alertas observáveis.

## Decisão
Foi adotado Spring Boot Actuator com Micrometer e registry Prometheus. A API expõe métricas em `/actuator/prometheus`, protegido por `ROLE_METRICAS.LEITURA`.

As métricas customizadas incluem:

- `transacao.criada`
- `transacao.processada`
- `transacao.duracao`
- `outbox.pendente`
- `outbox.publicado`
- `outbox.falhou`

No Prometheus, nomes são normalizados como `transacao_criada_total`, `transacao_processada_total`, `transacao_duracao_seconds_bucket`, `outbox_pendente`, `outbox_publicado_total` e `outbox_falhou_total`.

O homelab usa Prometheus em `https://prometheus.lab.home` e Grafana em `https://grafana.lab.home`. O dashboard principal documentado é `Transacoes PIX — SLO`.

Os SLOs implementados para PIX são:

- Taxa de falha PIX menor que `2%`, com alerta `PIXTaxaFalhaAlta`.
- Latência P95 PIX menor que `8s`, com alerta `PIXLatenciaP95Alta`.

## Consequências
### Positivas
- A operação passa a ter sinais objetivos de volume, falha, latência e outbox.
- SLOs de PIX deixam claro o nível de serviço esperado.
- Prometheus e Grafana permitem dashboards e alertas usando PromQL.
- Métricas por tags de tipo e status facilitam análise por modalidade.

### Negativas / Trade-offs
- Métricas precisam ser mantidas quando novos fluxos ou status forem adicionados.
- SLOs iniciais podem precisar de ajuste após observar tráfego real.
- O endpoint de métricas exige proteção e gestão adequada de credenciais.

## Ver também
- [Dependências Externas](../infraestrutura/dependencias-externas.md)
