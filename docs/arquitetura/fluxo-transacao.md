# Fluxo de Processamento de Transação

Este documento descreve o fluxo de processamento das transações PIX, TED e TEF, desde a requisição HTTP até a persistência dos eventos na outbox. O objetivo é explicitar a colaboração entre os componentes da arquitetura hexagonal, as variações de negócio por tipo e o caminho de estorno.

## Fluxo PIX — Caso Nominal

```mermaid
sequenceDiagram
    autonumber
    actor Cliente
    participant Controller as TransacaoController
    participant Idempotencia as IdempotenciaService
    participant Processamento as ProcessaTransacaoService
    participant ContaRepo as ContaRepository
    participant SaldoRepo as SaldoRepository
    participant LimiteRepo as LimiteRepository
    participant TransacaoRepo as TransacaoRepository
    participant Publicador as EventoPublicador
    participant OutboxPublicador as DominioEventoOutboxPublicador
    participant Resolver as StrategyResolver
    participant PixStrategy as PixTransacaoStrategy
    participant Outbox as outbox_evento

    Cliente->>Controller: POST /api/v1/transacoes/pix<br/>X-Idempotency-Key: UUID
    Controller->>Processamento: processa(valor, PIX, contaOrigem, contaDestino, chave)
    Processamento->>Idempotencia: verificar(chave)
    Idempotencia->>TransacaoRepo: buscar por chave de idempotência
    TransacaoRepo-->>Idempotencia: nenhuma transação encontrada
    Idempotencia-->>Processamento: nenhuma transação existente
    Processamento->>ContaRepo: buscar conta de origem ativa
    ContaRepo-->>Processamento: conta válida
    Processamento->>SaldoRepo: consultar saldo disponível
    SaldoRepo-->>Processamento: saldo suficiente
    Processamento->>LimiteRepo: consultar LimiteTransacional para PIX
    LimiteRepo-->>Processamento: limite disponível
    Processamento->>TransacaoRepo: salvar transação PENDENTE e chave de idempotência
    TransacaoRepo-->>Processamento: transação criada
    Processamento->>Publicador: publicar TransacaoIniciadaEvento
    Publicador->>OutboxPublicador: publica(evento)
    OutboxPublicador->>Outbox: INSERT evento PENDENTE
    Outbox-->>OutboxPublicador: evento persistido
    Processamento->>Resolver: resolve(PIX)
    Resolver-->>Processamento: PixTransacaoStrategy
    Note over Processamento,PixStrategy: fase de processamento (PROCESSANDO)
    Processamento->>PixStrategy: processa(transação)
    PixStrategy-->>Processamento: transação COMPLETADA
    Processamento->>SaldoRepo: debitar valor da conta de origem
    SaldoRepo-->>Processamento: saldo debitado
    Processamento->>LimiteRepo: registrar utilização do limite
    LimiteRepo-->>Processamento: limite atualizado
    Processamento->>TransacaoRepo: atualizar status para COMPLETADA
    TransacaoRepo-->>Processamento: transação finalizada
    Processamento->>Publicador: publicar TransacaoConcluidaEvento
    Publicador->>OutboxPublicador: publica(evento)
    OutboxPublicador->>Outbox: INSERT evento PENDENTE
    Outbox-->>OutboxPublicador: evento persistido
    Processamento-->>Controller: transação COMPLETADA
    Controller-->>Cliente: 201 Created
```

As gravações da transação e dos eventos na outbox participam da mesma fronteira transacional. A publicação no Kafka ocorre posteriormente, de forma assíncrona, a partir dos registros de `outbox_evento`.

## Variações por Tipo de Transação

| Tipo | Restrição adicional | Validação específica |
| --- | --- | --- |
| PIX | Sem restrição de horário. | Limite configurado em `LimiteTransacional`. |
| TED | Apenas em horário bancário, das 06h às 17h BRT. | Fora da janela, retorna HTTP 422 com o código `TED_FORA_DO_HORARIO`. |
| TEF | Entre contas internas, sem restrição de horário. | Valida se a transferência ocorre entre contas internas. |

## Fluxo de Estorno

```mermaid
sequenceDiagram
    autonumber
    actor Cliente
    participant Controller as TransacaoController
    participant Estorno as EstornoTransacaoService
    participant TransacaoRepo as TransacaoRepository
    participant SaldoRepo as SaldoRepository
    participant Publicador as EventoPublicador

    Cliente->>Controller: POST /api/v1/transacoes/{id}/estorno
    Controller->>Estorno: estornar(idTransacao, motivo)
    Estorno->>TransacaoRepo: buscar transação
    TransacaoRepo-->>Estorno: transação encontrada
    Estorno->>Estorno: validar status COMPLETADA
    Estorno->>TransacaoRepo: atualizar status para ESTORNADA
    TransacaoRepo-->>Estorno: transação estornada
    Estorno->>SaldoRepo: creditar valor na conta de origem
    SaldoRepo-->>Estorno: crédito reverso concluído
    Estorno->>Publicador: publicar TransacaoEstornadaEvento
    Publicador-->>Estorno: evento registrado
    Estorno-->>Controller: estorno concluído
    Controller-->>Cliente: resposta do estorno
```

O estorno somente é permitido para uma transação em estado `COMPLETADA`. A operação altera seu estado para `ESTORNADA`, recompõe o saldo da conta de origem e registra o `TransacaoEstornadaEvento` para publicação pela outbox.

## Referências

- [Domain-Driven Design](ddd.md)
- [Kafka e Outbox](../mensageria/kafka-outbox.md)
- [Autenticação e Autorização](../seguranca/autenticacao-autorizacao.md)
