# ADR-002: DDD, Aggregates e Eventos de Domínio

**Data:** 2026-06-17
**Status:** Aceito

## Contexto

O sistema possui regras de negócio relevantes para processamento transacional, saldo, limite, auditoria, rastreabilidade e idempotência. Era necessário organizar essas regras em uma linguagem explícita, reduzir acoplamento entre áreas do domínio e evitar que regras financeiras ficassem espalhadas por controllers, entidades JPA ou consumidores Kafka.

## Decisão

Foi adotado Domain-Driven Design como abordagem de modelagem. O projeto organiza responsabilidades em bounded contexts:

- `transacao`: core domain de processamento de PIX, TED e TEF.
- `conta`: supporting domain para conta, saldo e limite transacional.
- `auditoria`: supporting domain para eventos auditáveis.
- `compartilhado`: shared kernel com conceitos comuns, como `ValorMonetario`, `TipoTransacao`, exceções, segurança e eventos-base.
- `mensageria`, `observabilidade`, `configuracao` e `infraestrutura`: contextos de suporte técnico.

Os aggregates principais são `Transacao` e `Conta`. O aggregate `Transacao` controla identidade, correlação, idempotência, valor, tipo, conta de origem, conta de destino e status. O aggregate `Conta` representa a conta bancária e se relaciona conceitualmente com `Saldo` e `LimiteTransacional`.

O value object `ValorMonetario` centraliza validação de valor positivo, moeda e escala monetária. O enum `TipoTransacao` define as modalidades `PIX`, `TED` e `TEF`.

Eventos de domínio implementam `EventoDominio`, incluindo `TransacaoIniciadaEvento`, `TransacaoConcluidaEvento`, `TransacaoFalhouEvento` e `TransacaoEstornadaEvento`. Esses eventos carregam dados como `idEvento`, `idAgregado`, `idCorrelacao`, `idIdempotencia`, tipo, valor, moeda e ocorrência, sendo publicados por meio da porta `EventoPublicador`.

## Consequências

### Positivas

- A linguagem de negócio fica explícita no código e na documentação.
- Invariantes como saldo não negativo, limite transacional e transições de status ficam concentradas em objetos de domínio e serviços de aplicação.
- Eventos de domínio permitem rastreabilidade e integração assíncrona sem acoplar o domínio ao Kafka.
- O shared kernel evita duplicação de conceitos comuns.

### Negativas / Trade-offs

- A modelagem exige manutenção contínua do glossário e dos limites dos contexts.
- O shared kernel pode crescer indevidamente se conceitos específicos forem promovidos cedo demais.
- A separação entre modelo de domínio e modelo de persistência exige mapeamento adicional nos adaptadores.
