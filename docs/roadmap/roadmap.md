# Roadmap

Este roadmap reflete o estado documentado e observado no projeto. Ele diferencia funcionalidades já implementadas no núcleo atual da API de evoluções planejadas para separar melhor contextos de negócio e integrações externas.

| Funcionalidade                             | Status          | Notas                                                                                                                      |
| ------------------------------------------ | --------------- | -------------------------------------------------------------------------------------------------------------------------- |
| Processamento de transações PIX, TED e TEF | ✅ Implementado | Fluxo central em `transacao`, com `ProcessaTransacaoService` e seleção por `TransacaoStrategy`.                            |
| Strategy Pattern para PIX/TED/TEF          | ✅ Implementado | Implementações `PixTransacaoStrategy`, `TedTransacaoStrategy` e `TefTransacaoStrategy`, resolvidas por `StrategyResolver`. |
| Conta                                      | ✅ Implementado | Aggregate e portas de repositório em `conta`, usado como suporte ao processamento transacional.                            |
| Saldo                                      | ✅ Implementado | Domínio, serviço de aplicação e persistência para consulta, débito e crédito de saldo.                                     |
| Limite transacional                        | ✅ Implementado | Domínio, serviço de aplicação e persistência para validação e consumo de limite por tipo de transação.                     |
| Auditoria                                  | ✅ Implementado | Registro auditável por `AuditoriaService`, domínio de auditoria e adaptadores de persistência.                             |
| Outbox Pattern                             | ✅ Implementado | Eventos de domínio são gravados em `outbox_evento` antes da publicação assíncrona.                                         |
| Kafka                                      | ✅ Implementado | Configuração, produtor, consumidores, roteamento por tipo de transação, DLQ e idempotência de consumo.                     |
| Segurança                                  | ✅ Implementado | OAuth2 Resource Server com JWT, RBAC, rate limiting, HMAC, criptografia e tratamento de erros de segurança.                |
| Observabilidade                            | ✅ Implementado | Métricas Micrometer/Prometheus, rastreamento OpenTelemetry/Jaeger, correlação e mascaramento de logs.                      |
| Documentação operacional de execução local | ✅ Implementado | Documento operacional de execução local concluído.                                                                         |
| Documentação de variáveis de ambiente      | ✅ Implementado | Documento de variáveis de ambiente concluído.                                                                              |
| Bounded context PIX dedicado               | 📋 Planejado    | Evolução para isolar regras específicas de PIX fora do core genérico de `transacao`.                                       |
| Bounded context TED dedicado               | 📋 Planejado    | Evolução para isolar regras específicas de TED fora do core genérico de `transacao`.                                       |
| Bounded context TEF dedicado               | 📋 Planejado    | Evolução para isolar regras específicas de TEF fora do core genérico de `transacao`.                                       |
| Bounded context Cliente                    | 📋 Planejado    | Evolução para modelar dados e regras de cliente de forma própria.                                                          |
| Integração BACEN                           | 📋 Planejado    | Integração externa futura; não há fluxo de negócio completo implementado para BACEN no estado atual.                       |
| Integração SPB                             | 📋 Planejado    | Integração externa futura; não há fluxo de negócio completo implementado para SPB no estado atual.                         |
| Integração STR                             | 📋 Planejado    | Integração externa futura; não há fluxo de negócio completo implementado para STR no estado atual.                         |
| Controle de conta via API                  | 📋 Planejado    | O domínio de conta existe, mas não há endpoints REST de controle de conta documentados como disponíveis.                   |
