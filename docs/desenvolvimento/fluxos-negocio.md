# Fluxos de Negócio por Tipo de Transação

Este documento reúne as regras de negócio aplicáveis ao processamento de PIX, TED, TEF e estorno. Ele é destinado a desenvolvedores que precisam compreender o comportamento esperado das operações sem consultar o código-fonte.

## Regras Comuns a Todos os Tipos

- **Idempotência:** toda solicitação de PIX, TED ou TEF deve informar o header `X-Idempotency-Key` com um UUID v4. A chave deve ser única por cliente; ao repetir uma mesma solicitação, o cliente deve reutilizar a chave original para evitar um novo processamento.
- **Conta e saldo:** a conta de origem deve estar `ATIVA`, possuir saldo disponível suficiente e não pode ser também a conta de destino.
- **Antifraude:** as transações estão sujeitas à avaliação antifraude automática conforme as regras da modalidade. O patamar de aprovação automática é configurado por `APP_ANTIFRAUDE_LIMITE_APROVACAO_AUTOMATICA`, cujo valor padrão é `10000.00`.
- **Autenticação:** todos os endpoints de transação exigem um token de acesso válido no header `Authorization` e uma role autorizada para a operação.

## PIX

### Disponibilidade

O PIX está disponível 24 horas por dia, 7 dias por semana.

### Limites

O valor máximo de cada PIX é definido no `LimiteTransacional` da conta de origem. Valores acima do patamar configurado em `APP_ANTIFRAUDE_LIMITE_APROVACAO_AUTOMATICA` exigem aprovação antifraude automática.

### Validações

- `contaDestino` deve conter uma chave PIX válida: e-mail, CPF, telefone ou chave aleatória.
- A conta de origem deve estar ativa e possuir saldo suficiente.
- A conta de origem e a conta de destino devem ser diferentes.
- O valor não pode ultrapassar o limite por operação configurado para a conta.
- A chave de idempotência deve ser um UUID v4 e ser única por cliente.

### Erros esperados

| `codigoErro`                    | Descrição                                                            |
| ------------------------------- | -------------------------------------------------------------------- |
| `CABECALHO_AUSENTE`             | O header `X-Idempotency-Key` não foi informado.                      |
| `CONTA_INVALIDA`                | A conta de origem não existe ou não está ativa.                      |
| `SALDO_INSUFICIENTE`            | O saldo disponível não cobre o valor do PIX.                         |
| `LIMITE_NAO_CONFIGURADO`        | A conta não possui limite configurado para PIX.                      |
| `LIMITE_POR_TRANSACAO_EXCEDIDO` | O valor do PIX ultrapassa o limite por operação.                     |
| `CONFLITO_DADOS`                | A chave de idempotência já foi usada em uma solicitação conflitante. |

## TED

### Disponibilidade

A TED está disponível somente das 06h às 17h, considerando o horário de Brasília. Solicitações fora dessa janela são recusadas com HTTP `422` e `codigoErro` igual a `TED_FORA_DO_HORARIO`.

### Limites

O total de TEDs realizadas no dia não pode ultrapassar o limite diário definido no `LimiteTransacional` da conta de origem.

### Validações

- A solicitação deve ocorrer entre 06h e 17h no horário de Brasília.
- A conta de origem deve estar ativa e possuir saldo suficiente.
- A conta de origem e a conta de destino devem ser diferentes.
- A operação, somada às TEDs já realizadas no dia, deve respeitar o limite diário da conta.
- A chave de idempotência deve ser um UUID v4 e ser única por cliente.

### Erros esperados

| `codigoErro`             | Descrição                                                            |
| ------------------------ | -------------------------------------------------------------------- |
| `TED_FORA_DO_HORARIO`    | A TED foi solicitada fora da janela das 06h às 17h de Brasília.      |
| `CABECALHO_AUSENTE`      | O header `X-Idempotency-Key` não foi informado.                      |
| `CONTA_INVALIDA`         | A conta de origem não existe ou não está ativa.                      |
| `SALDO_INSUFICIENTE`     | O saldo disponível não cobre o valor da TED.                         |
| `LIMITE_NAO_CONFIGURADO` | A conta não possui limite configurado para TED.                      |
| `LIMITE_DIARIO_EXCEDIDO` | A TED faria o total movimentado no dia ultrapassar o limite diário.  |
| `CONFLITO_DADOS`         | A chave de idempotência já foi usada em uma solicitação conflitante. |

## TEF

### Disponibilidade

A TEF está disponível 24 horas por dia e é destinada exclusivamente a transferências entre contas internas do sistema.

### Limites

Os limites por operação e por dia são definidos no `LimiteTransacional` da conta de origem.

### Validações

- Tanto a conta de origem quanto a conta de destino devem pertencer ao sistema.
- A conta de origem deve estar ativa e possuir saldo suficiente.
- A conta de origem e a conta de destino devem ser diferentes.
- A operação deve respeitar os limites configurados para a conta.
- A chave de idempotência deve ser um UUID v4 e ser única por cliente.

### Erros esperados

| `codigoErro`                    | Descrição                                                                 |
| ------------------------------- | ------------------------------------------------------------------------- |
| `CABECALHO_AUSENTE`             | O header `X-Idempotency-Key` não foi informado.                           |
| `CONTA_INVALIDA`                | Uma das contas não existe no sistema ou a conta de origem não está ativa. |
| `SALDO_INSUFICIENTE`            | O saldo disponível não cobre o valor da TEF.                              |
| `LIMITE_NAO_CONFIGURADO`        | A conta não possui limite configurado para TEF.                           |
| `LIMITE_POR_TRANSACAO_EXCEDIDO` | O valor da TEF ultrapassa o limite por operação.                          |
| `LIMITE_DIARIO_EXCEDIDO`        | A TEF faria o total movimentado no dia ultrapassar o limite diário.       |
| `TEF_RECUSADO_ANTIFRAUDE`       | A TEF não foi aprovada pela avaliação antifraude.                         |
| `CONFLITO_DADOS`                | A chave de idempotência já foi usada em uma solicitação conflitante.      |

## Estorno

O estorno é solicitado por `POST /api/v1/transacoes/{id}/estorno` e segue estas regras:

- A transação deve existir e estar no status `COMPLETADA`.
- O solicitante deve possuir, no mínimo, a role `GERENTE`; `ADMIN` também pode executar a operação.
- O valor integral da transação é creditado de volta no saldo da conta de origem.
- A transação passa para o status `ESTORNADA`.
- O evento `TransacaoEstornadaEvento` é registrado na outbox para publicação.

Uma transação que não esteja `COMPLETADA` é recusada com `codigoErro` igual a `ESTORNO_INVALIDO`.

## Máquina de Estados da Transação

O ciclo completo entre `PENDENTE`, `PROCESSANDO`, `COMPLETADA`, `FALHOU` e `ESTORNADA` está documentado na [máquina de estados da transação](../arquitetura/ddd.md#máquina-de-estados-da-transação).

## Referências

- [Endpoints da API](../api/endpoints.md)
- [Domain-Driven Design](../arquitetura/ddd.md)
- [ADR-009: UUID como Chave de Idempotência](../adr/ADR-009-idempotencia-uuid.md)
- [ADR-010: Rate Limiting com Bucket4j](../adr/ADR-010-rate-limiting-bucket4j.md)
