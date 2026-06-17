# ADR-009: UUID como Chave de Idempotência via Header HTTP

**Data:** 2026-06-17
**Status:** Aceito

## Contexto
Clientes podem reenviar uma requisição por timeout, instabilidade de rede ou retry automático. Em operações financeiras, repetir o processamento da mesma solicitação pode causar débitos duplicados, consumo indevido de limite e eventos inconsistentes. Era necessário identificar solicitações repetidas antes de executar o fluxo de negócio.

## Decisão
Foi adotado UUID como chave de idempotência recebida por header HTTP. O projeto centraliza nomes de headers em `HeadersHttp` e usa `IdempotenciaService` para verificar requisições repetidas.

O valor é persistido na transação como `idIdempotencia` e propagado em eventos de domínio como `idIdempotencia`. No banco, a tabela `transacao` possui constraint `uq_transacao_id_idempotencia` e índice parcial `idx_transacao_id_idempotencia`.

No fluxo de `ProcessaTransacaoService`, a idempotência é verificada antes das validações de conta, saldo, limite e publicação de eventos. Quando já existe transação para o mesmo `idIdempotencia`, o serviço retorna a transação existente e não processa novamente a solicitação.

## Consequências
### Positivas
- Evita processamento duplicado de requisições financeiras repetidas.
- A constraint no banco protege contra concorrência e duplicidade além da lógica de aplicação.
- O `idIdempotencia` acompanha transações e eventos, facilitando rastreabilidade.
- UUID reduz risco de colisão e simplifica validação de formato.

### Negativas / Trade-offs
- Clientes precisam gerar e reenviar a mesma chave em retries da mesma operação.
- Reuso incorreto da chave pelo cliente pode retornar uma transação anterior indesejada.
- A idempotência precisa ser considerada em testes e integrações consumidoras.
