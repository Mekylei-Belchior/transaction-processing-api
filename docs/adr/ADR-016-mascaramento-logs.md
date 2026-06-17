# ADR-016: Mascaramento de Logs com Logback e JSON em Produção

**Data:** 2026-06-17
**Status:** Aceito

## Contexto
Logs são essenciais para operação, auditoria técnica e investigação de incidentes, mas podem expor dados sensíveis como documentos, contas, agências, valores, tokens, senhas e payloads. Era necessário preservar utilidade operacional sem registrar segredos ou dados bancários em texto claro.

## Decisão
Foi adotado mascaramento centralizado de logs com Logback, estratégias de mascaramento e JSON estruturado em produção.

A implementação usa:

- `DadosSensiveisMasker` como componente central de mascaramento.
- `MascaraStrategy` e estratégias como `MensagemMascaradaStrategy`, `StacktraceMascaradoStrategy`, `JsonMascaradoStrategy` e `HeaderMascaradoStrategy`.
- `StrategyMascaramentoResolver` para escolher a strategy adequada.
- `LogMascaramentoConverter` para logs textuais.
- `JsonMascaradoProvider` para campos JSON no Logback.
- `TipoCampoMascarado` para categorizar `MESSAGE`, `STACKTRACE`, `JSON` e `HEADER`.

Em `logback-spring.xml`, o perfil `dev` usa console colorido com `%mascarado` e `idCorrelacao` no padrão. O perfil `prod` usa `LoggingEventCompositeJsonEncoder` da dependência `logstash-logback-encoder`, com providers para `message` e `stacktrace` mascarados e MDC incluindo `idCorrelacao`.

Os padrões cobrem documentos, contas bancárias, agência, valores monetários, Authorization Bearer, tokens, senhas e segredos em JSON.

## Consequências
### Positivas
- Reduz risco de vazamento de dados sensíveis em logs.
- Mantém logs úteis para operação, com `idCorrelacao` para rastreabilidade.
- JSON em produção facilita coleta por plataformas de observabilidade.
- Estratégias permitem evoluir regras de mascaramento por tipo de conteúdo.

### Negativas / Trade-offs
- Mascaramento por regex e estratégia pode não cobrir novos formatos sensíveis automaticamente.
- Logs mascarados podem perder detalhes úteis para depuração.
- Alterações em formato de payload ou headers exigem atualização dos testes de mascaramento.
