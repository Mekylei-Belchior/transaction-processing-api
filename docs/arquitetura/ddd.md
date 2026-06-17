# Domain-Driven Design

O `transaction-processing-api` organiza o domínio em bounded contexts para separar responsabilidades de negócio e manter a evolução dos fluxos transacionais sob controle. O core domain atual é o processamento de transações; conta, auditoria e componentes compartilhados dão suporte às invariantes e à rastreabilidade.

## Bounded Contexts

| Bounded Context | Pacote | Status | Responsabilidade |
| --- | --- | --- | --- |
| Transação | `com.mekylei.transactionprocessing.transacao` | Implementado | Core domain. Processa PIX, TED e TEF, controla ciclo de vida da transação e publica eventos. |
| Conta | `com.mekylei.transactionprocessing.conta` | Implementado | Supporting domain. Mantém conta, saldo e limite transacional usados no processamento. |
| Auditoria | `com.mekylei.transactionprocessing.auditoria` | Implementado | Supporting domain. Registra dados e eventos auditáveis. |
| Compartilhado | `com.mekylei.transactionprocessing.compartilhado` | Implementado | Shared Kernel. Contém value objects, eventos-base, exceções, segurança e utilitários comuns. |
| Mensageria | `com.mekylei.transactionprocessing.mensageria` | Implementado | Suporte técnico ao domínio para outbox, roteamento, produção e consumo de eventos. |
| Observabilidade | `com.mekylei.transactionprocessing.observabilidade` | Implementado | Métricas, mascaramento de dados sensíveis, logs e rastreamento. |
| Configuração | `com.mekylei.transactionprocessing.configuracao` | Implementado | Configuração de Spring, Kafka, segurança, persistência e filtros. |
| Infraestrutura | `com.mekylei.transactionprocessing.infraestrutura` | Implementado | Adaptadores JPA, entidades e repositórios Spring Data. |
| PIX | `com.mekylei.transactionprocessing.pix` | Planejado | Evolução do domínio específico de pagamentos PIX. |
| TED | `com.mekylei.transactionprocessing.ted` | Planejado | Evolução do domínio específico de transferências TED. |
| TEF | `com.mekylei.transactionprocessing.tef` | Planejado | Evolução do domínio específico de transferências TEF. |
| Cliente | `com.mekylei.transactionprocessing.cliente` | Planejado | Evolução de dados e regras relacionadas a clientes. |
| Integração BACEN | `com.mekylei.transactionprocessing.integracao.bacen` | Planejado | Integrações futuras com serviços do BACEN. |
| Integração SPB | `com.mekylei.transactionprocessing.integracao.spb` | Planejado | Integrações futuras com o Sistema de Pagamentos Brasileiro. |
| Integração STR | `com.mekylei.transactionprocessing.integracao.str` | Planejado | Integrações futuras com STR. |

## Aggregates

### `Transacao`

Aggregate root em `com.mekylei.transactionprocessing.transacao.dominio.Transacao`.

Responsabilidades:

- Representar uma operação financeira processada pelo sistema.
- Manter identidade (`id`), correlação (`idCorrelacao`) e idempotência (`idIdempotencia`).
- Carregar valor, tipo da transação, conta de origem, conta de destino e status.
- Permitir transição de status por meio de `comStatus(StatusTransacao)`.

Eventos de domínio associados:

- `TransacaoIniciadaEvento`
- `TransacaoConcluidaEvento`
- `TransacaoFalhouEvento`
- `TransacaoEstornadaEvento`

Esses eventos implementam `EventoDominio`, usam `tipoAgregado()` igual a `Transacao` e são publicados pela porta `EventoPublicador`.

### `Conta`

Aggregate root em `com.mekylei.transactionprocessing.conta.dominio.Conta`.

Responsabilidades:

- Representar a conta bancária e seu estado operacional.
- Validar se a conta está ativa por meio de `estaAtiva()`.
- Relacionar número da conta, agência, cliente, tipo e status.

Objetos do agregado:

- `Saldo`: representa valores disponíveis e bloqueados da conta. Protege a invariante de saldo não negativo ao debitar e permite crédito para fluxos como estorno.
- `LimiteTransacional`: representa limites por tipo de transação. Valida limite por operação e limite diário, além de atualizar o valor utilizado no dia.

## Value Objects

### `ValorMonetario`

Value object em `com.mekylei.transactionprocessing.compartilhado.dominio.ValorMonetario`.

Responsabilidades:

- Representar valor e moeda.
- Rejeitar valor nulo, valor menor ou igual a zero e moeda nula.
- Normalizar escala monetária para duas casas decimais.
- Oferecer `paraReal(BigDecimal)` para criar valores em BRL.

### `TipoTransacao`

Value object conceitual implementado como enum em `com.mekylei.transactionprocessing.compartilhado.dominio.TipoTransacao`.

Valores suportados:

- `PIX`
- `TED`
- `TEF`

Esse tipo é compartilhado pelos contexts `transacao` e `conta`, sendo usado para escolher a strategy de processamento e validar limites transacionais.

## Domain Events

| Evento | Pacote | Quando é usado |
| --- | --- | --- |
| `TransacaoIniciadaEvento` | `com.mekylei.transactionprocessing.transacao.dominio.evento` | Criado em `CriaTransacaoService` após persistir a transação pendente. |
| `TransacaoConcluidaEvento` | `com.mekylei.transactionprocessing.transacao.dominio.evento` | Publicado por `ProcessaTransacaoService` quando a transação termina com `StatusTransacao.COMPLETADA`. |
| `TransacaoFalhouEvento` | `com.mekylei.transactionprocessing.transacao.dominio.evento` | Publicado por `ProcessaTransacaoService` quando a transação termina com `StatusTransacao.FALHOU`. |
| `TransacaoEstornadaEvento` | `com.mekylei.transactionprocessing.transacao.dominio.evento` | Publicado no fluxo de estorno para registrar a reversão da transação. |

Todos os eventos implementam `com.mekylei.transactionprocessing.compartilhado.evento.EventoDominio`, carregam `idEvento`, `idAgregado`, `idCorrelacao`, `idIdempotencia`, dados da conta, tipo, valor, moeda e data de ocorrência. Eventos de falha e estorno também carregam o motivo.

## Shared Kernel

O Shared Kernel fica em `com.mekylei.transactionprocessing.compartilhado`.

Conteúdos principais:

- `compartilhado.dominio`: `ValorMonetario` e `TipoTransacao`.
- `compartilhado.evento`: contrato `EventoDominio`.
- `compartilhado.exception`: exceções e tratamento global de erro.
- `compartilhado.idempotencia`: serviço de idempotência.
- `compartilhado.seguranca`: utilitários HMAC e roles.
- `compartilhado.util`: correlação, datas, criptografia e calendário stub.
- `compartilhado.constantes`: constantes HTTP e de problema.
- `compartilhado.adaptador`: adaptadores técnicos compartilhados.

O Shared Kernel deve continuar pequeno e estável, contendo apenas conceitos realmente compartilhados entre contexts.

## Fluxo principal de processamento

Os endpoints reais do controller usam o prefixo `/api/v1/transacoes` e especializam o tipo no caminho:

- `POST /api/v1/transacoes/pix`
- `POST /api/v1/transacoes/ted`
- `POST /api/v1/transacoes/tef`

Fluxo:

```text
POST /api/v1/transacoes/{pix|ted|tef}
        -> TransacaoController
        -> ProcessaTransacaoService
        -> IdempotenciaService
        -> ContaRepository
        -> SaldoService
        -> LimiteService
        -> CriaTransacaoService
        -> EventoPublicador
        -> DominioEventoOutboxPublicador
        -> StrategyResolver
        -> TransacaoStrategy
        -> TransacaoRepository
        -> EventoPublicador
        -> DominioEventoOutboxPublicador
```

Descrição do fluxo:

1. `TransacaoController` recebe a requisição HTTP e define o `TipoTransacao` conforme o endpoint chamado.
2. `ProcessaTransacaoService` verifica idempotência por `IdempotenciaService`.
3. O serviço valida se a conta existe e está ativa por `ContaRepository`.
4. `SaldoService` valida saldo disponível.
5. `LimiteService` valida o limite transacional.
6. `CriaTransacaoService` cria a `Transacao` pendente, persiste pela porta `TransacaoRepository` e publica `TransacaoIniciadaEvento`.
7. `DominioEventoOutboxPublicador` recebe o evento pela porta `EventoPublicador` e grava o evento na outbox.
8. `StrategyResolver` escolhe a `TransacaoStrategy` adequada para `PIX`, `TED` ou `TEF`.
9. A strategy processa a transação e retorna o estado final.
10. Quando a transação é concluída, `SaldoService` debita o saldo e `LimiteService` decrementa o limite utilizado.
11. `TransacaoRepository` atualiza o status final.
12. `ProcessaTransacaoService` publica `TransacaoConcluidaEvento` ou `TransacaoFalhouEvento`.
13. `DominioEventoOutboxPublicador` persiste o evento final na outbox para publicação assíncrona.
