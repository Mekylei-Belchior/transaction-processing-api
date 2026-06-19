# ADR-005: Outbox Pattern para Consistência Eventual

**Data:** 2026-06-17
**Status:** Aceito

## Contexto

O sistema precisa persistir mudanças de negócio e publicar eventos de domínio. Publicar diretamente no Kafka dentro do fluxo transacional criaria risco de inconsistência: a transação de banco poderia confirmar e a publicação falhar, ou a publicação ocorrer e o commit de negócio falhar. Era necessário preservar rastreabilidade e confiabilidade sem exigir transação distribuída entre PostgreSQL e Kafka.

## Decisão

Foi adotado o Outbox Pattern. Casos de uso publicam eventos pela porta `EventoPublicador`; a implementação `DominioEventoOutboxPublicador` grava os eventos na tabela `outbox_evento` dentro da transação de banco.

O job `EventoOutboxPublicador` busca eventos `PENDENTE` ou `FALHOU`, respeitando lote, intervalo e próxima tentativa configurados por `app.eventos.outbox`. A publicação é feita por `KafkaEventoProdutor`. Em sucesso, o evento é marcado como `PUBLICADO`; em falha, registra `ultimo_erro`, incrementa `tentativas` e agenda reprocessamento.

A tabela `outbox_evento` guarda tipo do evento, tipo do agregado, `id_agregado`, tópico, chave, payload `jsonb`, `id_correlacao`, datas, status, tentativas e erro. A busca concorrente de trabalho é protegida por locking pessimista em `OutboxEventoJpaRepository.buscarParaPublicacao`.

## Consequências

### Positivas

- Evita perda de eventos entre commit de negócio e publicação Kafka.
- Não exige transação distribuída entre banco e broker.
- Permite reprocessamento controlado de publicações com falha.
- Mantém trilha operacional por `id_correlacao`, tópico, status e tentativas.

### Negativas / Trade-offs

- A publicação deixa de ser imediata e passa a ser eventualmente consistente.
- Eventos podem ser reenviados, exigindo idempotência dos consumidores.
- A tabela de outbox precisa de monitoramento, limpeza e alertas operacionais.
