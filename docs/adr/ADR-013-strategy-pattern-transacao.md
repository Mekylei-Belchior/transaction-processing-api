# ADR-013: Strategy Pattern para Processamento de Transações

**Data:** 2026-06-17
**Status:** Aceito

## Contexto
PIX, TED e TEF compartilham parte do fluxo transacional, mas possuem validações e integrações específicas. Implementar tudo em condicionais dentro de um único serviço aumentaria complexidade, dificultaria testes e tornaria arriscado adicionar novas modalidades.

## Decisão
Foi adotado Strategy Pattern para especializar o processamento por `TipoTransacao`. O contrato comum é `TransacaoStrategy`, e as implementações atuais são:

- `PixTransacaoStrategy`
- `TedTransacaoStrategy`
- `TefTransacaoStrategy`

O `StrategyResolver` seleciona a strategy adequada para `PIX`, `TED` ou `TEF`. A configuração de beans é centralizada em `StrategyConfig`.

O fluxo principal permanece em `ProcessaTransacaoService`, que executa validações comuns de idempotência, conta, saldo e limite, cria a transação pendente, publica evento inicial e delega o comportamento específico à strategy resolvida. Após o retorno, o serviço atualiza status, efetiva saldo/limite quando aplicável e publica evento final.

ArchUnit protege a convenção por meio da regra `strategiesImplementamInterfaceCorreta`, exigindo que classes com sufixo `Strategy` no pacote `..transacao.estrategia..` implementem `TransacaoStrategy`.

## Consequências
### Positivas
- Regras específicas de PIX, TED e TEF ficam isoladas e testáveis.
- O fluxo comum permanece concentrado no serviço de aplicação.
- Novas modalidades podem ser adicionadas com nova strategy e atualização do resolver.
- ArchUnit reduz risco de strategy concreta fora do contrato esperado.

### Negativas / Trade-offs
- O `StrategyResolver` precisa ser mantido em sincronia com novos tipos.
- O excesso de lógica dentro das strategies pode criar subdomínios implícitos se não houver cuidado.
- Regras comuns e específicas precisam ter fronteira clara para evitar duplicação.
