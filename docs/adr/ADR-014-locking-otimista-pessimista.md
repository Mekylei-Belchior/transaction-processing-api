# ADR-014: Locking Otimista e Pessimista

**Data:** 2026-06-17
**Status:** Aceito

## Contexto

Operações financeiras são sensíveis a concorrência. Duas requisições simultâneas podem tentar alterar o mesmo saldo, consumir o mesmo limite ou atualizar o mesmo status de transação. A publicação da outbox também pode ter múltiplos workers buscando eventos pendentes. Era necessário combinar controle de concorrência leve para atualizações gerais com bloqueios explícitos para trechos críticos.

## Decisão

Foram adotadas duas estratégias complementares:

- Locking otimista com `@Version` nas entidades versionadas.
- Locking pessimista com `LockModeType.PESSIMISTIC_WRITE`, equivalente a `SELECT ... FOR UPDATE`, em consultas críticas.

O locking otimista usa colunas `versao` em:

- `transacao.versao`: protege atualizações de status e alterações concorrentes da transação.
- `saldo.versao`: protege atualizações concorrentes de saldo.
- `limite.versao`: protege consumo concorrente de limite diário.

O locking pessimista é usado em:

- `SaldoJpaRepository.findByIdContaForUpdate`, para bloquear saldo durante débito, crédito ou validação crítica.
- `LimiteJpaRepository.findByIdContaAndTipoForUpdate`, para bloquear limite durante consumo.
- `OutboxEventoJpaRepository.buscarParaPublicacao`, para bloquear eventos elegíveis e evitar publicação simultânea por workers concorrentes.

## Consequências

### Positivas

- Reduz risco de perda de atualização em status, saldo e limite.
- Serializa trechos críticos de débito, crédito, consumo de limite e seleção de outbox.
- Mantém locking otimista como padrão para evitar bloqueios desnecessários.
- Permite concorrência controlada em publicação da outbox.

### Negativas / Trade-offs

- Locks pessimistas podem aumentar espera e contenção sob alta concorrência.
- Conflitos de versão precisam ser tratados de forma previsível.
- Consultas com `SELECT ... FOR UPDATE` exigem atenção a transações, índices e ordem de acesso para evitar deadlocks.
