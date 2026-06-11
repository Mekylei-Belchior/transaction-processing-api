# Transaction Processing API

API de simulação de processamento de transações bancárias (PIX, TED e TEF) desenvolvida com Java 21 e Spring Boot 4.0.6. A aplicação segue os princípios de Arquitetura Hexagonal e Domain-Driven Design, com suporte a autenticação via OAuth2/JWT, controle de rate limiting, auditoria regulatória, validação de saldo e limites transacionais, criptografia em repouso, integridade por HMAC e mensageria com Outbox Pattern + Kafka.

Estado da documentação: junho/2026.

<img width="1916" height="1080" alt="Captura de tela de 2026-05-27 03-33-29" src="https://github.com/user-attachments/assets/bca23a44-771d-417c-84ef-6e20f1134512" />
<img width="1532" height="1076" alt="Captura de tela de 2026-05-27 03-11-49" src="https://github.com/user-attachments/assets/ea1d4f58-0c08-4d8a-b5c8-2d77c1fd8190" />
<img width="1532" height="1076" alt="Captura de tela de 2026-05-27 03-13-08" src="https://github.com/user-attachments/assets/2bdb0369-825e-40ed-9eb3-619ddea8ed6d" />

---

## Tecnologias Utilizadas

| Tecnologia                  | Versão                 | Finalidade                               |
| --------------------------- | ---------------------- | ---------------------------------------- |
| Java                        | 21                     | Linguagem principal                      |
| Spring Boot                 | 4.0.6                  | Framework de aplicação                   |
| Spring Security             | (gerenciado pelo Boot) | Autenticação e autorização               |
| Spring Data JPA / Hibernate | (gerenciado pelo Boot) | Persistência                             |
| Spring for Apache Kafka     | (gerenciado pelo Boot) | Publicação e consumo de eventos          |
| Spring Boot Actuator        | (gerenciado pelo Boot) | Health checks e informações operacionais |
| PostgreSQL                  | 42.7.11                | Banco de dados relacional                |
| Flyway                      | 12.6.2                 | Versionamento de schema                  |
| Bucket4j                    | 8.19.0                 | Rate limiting por token bucket           |
| SpringDoc OpenAPI           | 3.0.3                  | Documentação interativa (Swagger UI)     |
| Jackson 3                   | (gerenciado pelo Boot) | Serialização JSON                        |
| Logstash Logback Encoder    | 9.0                    | Logs estruturados em JSON (Logstash)     |
| Testcontainers              | (gerenciado pelo Boot) | Testes de integração com containers      |
| ArchUnit                    | 1.3.0                  | Testes de arquitetura                    |
| H2                          | (gerenciado pelo Boot) | Banco em memória para testes             |

## Módulos Existentes

| Módulo            | Responsabilidade                                                                                        |
| ----------------- | ------------------------------------------------------------------------------------------------------- |
| `transacao`       | Processamento de PIX, TED e TEF — domínio central, casos de uso, controller, DTOs, eventos e strategies |
| `conta`           | Domínio de conta, saldo e limite transacional                                                           |
| `auditoria`       | Registro de eventos de auditoria regulatória                                                            |
| `compartilhado`   | Utilitários, filtros, exceções, criptografia, HMAC, idempotência e constantes transversais              |
| `configuracao`    | Configurações Spring: segurança, Kafka, persistência, beans e filtros                                   |
| `infraestrutura`  | Adaptadores JPA, entidades e repositórios Spring Data                                                   |
| `integracao`      | Adaptadores para integrações externas (antifraude stub implementado; Bacen/STR/SPB ainda planejados)    |
| `mensageria`      | Outbox Pattern, publicação Kafka, consumo, roteamento de eventos e monitoramento de DLQ                 |
| `observabilidade` | Mascaramento de dados sensíveis nos logs e logging estruturado em JSON                                  |

## Organização do Código

- Domínio imutável: `Transacao`, `Saldo`, `LimiteTransacional` e `Conta` implementados com Builder Pattern — nenhuma mutação direta de estado.
- Value Object: `ValorMonetario` como `record` com validação inline e fábrica estática `paraReal()`.
- Strategy Pattern: `StrategyResolver` com lista injetável de `TransacaoStrategy` — extensível sem alteração do orquestrador.
- Porta de repositório: `TransacaoRepository`, `ContaRepository`, `SaldoRepository` e `LimiteRepository` como interfaces no módulo de aplicação — infraestrutura desacoplada.
- Resposta padronizada: `GlobalExceptionHandler` com `ProblemDetail` (RFC 7807).
- Correlação ponta-a-ponta: `ContextoRequisicaoFilter` com `HIGHEST_PRECEDENCE` propagando `idCorrelacao` via MDC (Mapped Diagnostic Context).
- Criptografia em repouso: `CriptografiaConverter` como `AttributeConverter` JPA usando AES-256-GCM com IV aleatório por valor — campos sensíveis das entidades armazenados criptografados no banco.
- Integridade com HMAC: `HmacService` com HMAC-SHA256 para geração de assinaturas determinísticas de dados sensíveis pesquisáveis, configurável via `HmacProperties`.
- Mascaramento de logs: subsistema `observabilidade/mascaramento` com estratégias intercambiáveis (`MascaraStrategy`) para JSON, headers, mensagens e stack traces — integrado ao Logback via `LogMascaramentoConverter` e `JsonMascaradoProvider`.
- Eventos de domínio + Outbox: eventos (`TransacaoIniciadaEvento`, `TransacaoConcluidaEvento`, `TransacaoFalhouEvento`, `TransacaoEstornadaEvento`) persistidos na tabela de outbox e publicados no Kafka por job agendado.
- Consumo resiliente: consumidores Kafka com tratamento de erro via `DefaultErrorHandler` + `DeadLetterPublishingRecoverer`, encaminhamento para `*.DLQ`, idempotência de consumo por `evento_processado` e roteamento de `transacoes.iniciadas` para processadores específicos de PIX, TED e TEF.
- Cobertura de testes: 56 arquivos de teste cobrindo domínio, services, strategies, controller, persistência, mensageria, filtros, criptografia/HMAC, arquitetura, integrações com Testcontainers e mascaramento de logs.

---

# Arquitetura Alvo

## Implementado

- Camada de controle HTTP (`TransacaoController`) separada da camada de aplicação.
- Camada de aplicação (`ProcessaTransacaoService`, `CriaTransacaoService`, `ConsultaTransacaoService`, `EstornoTransacaoService`) orquestrando casos de uso sem dependência direta de infraestrutura.
- Camada de domínio (`Transacao`, `Conta`, `Saldo`, `LimiteTransacional`, `ValorMonetario`) sem dependência de frameworks.
- Camada de infraestrutura (`TransacaoJpaAdapter`, `ContaJpaAdapter`, `SaldoJpaAdapter`, `LimiteJpaAdapter`, `AuditoriaJpaAdapter`, `OutboxEventoJpaAdapter`, `EventoProcessadoJpaAdapter`) implementando interfaces de porta.
- Validações específicas por tipo de transação via strategies: PIX simula envio ao SPI/BACEN, TED valida dia útil e horário bancário, TEF consulta antifraude stub.
- Filtros transversais (`ContextoRequisicaoFilter`, `RateLimitFilter`) na camada de entrada.
- Criptografia em repouso via `CriptografiaConverter` (AES-256-GCM, `AttributeConverter` JPA).
- Mascaramento de dados sensíveis nos logs via subsistema `observabilidade/mascaramento` integrado ao Logback com logging estruturado em JSON (`logstash-logback-encoder`).
- HMAC-SHA256 para geração de assinaturas determinísticas de dados sensíveis (`HmacService`, `HmacUtils`).
- Eventos de domínio e outbox (`DominioEventoOutboxPublicador`, `OutboxEvento`, `OutboxEventoRepository`, `OutboxEventoJpaAdapter`, `EventoOutboxPublicador`) com publicação Kafka assíncrona.
- Consumidores Kafka (`TransacaoIniciadaKafkaConsumidor`, `PixTransacaoKafkaConsumidor`, `TedTransacaoKafkaConsumidor`, `TefTransacaoKafkaConsumidor`, `DlqMonitorConsumidor`) com idempotência por `EventoProcessadoService`, roteamento por tipo de transação e publicação de eventos finais.
- Configuração de Kafka + DLQ + tópicos (`KafkaConfig`, `KafkaDlqProperties`, `OutboxProperties`, `TopicosProperties`) condicionada por `app.eventos.kafka.enabled`.

## Parcialmente Implementado

- Integrações externas: portas definidas (`AntiFraudeGateway`, `PixGateway`), adaptador de antifraude implementado como stub funcional com threshold configurável; integrações reais com SPI/BACEN, STR, SPB e DICT ainda sem implementação de produção.
- Auditoria: captura automática via `AuditoriaListener` (JPA lifecycle callbacks) operacional; mascaramento de dados sensíveis nos eventos de auditoria não implementado.
- Bounded contexts separados para `cliente`, `PIX`, `TED` e `TEF` já possuem estrutura inicial de pacotes, mas ainda sem implementação funcional própria; o domínio e os casos de uso permanecem concentrados em `transacao`.

## Planejado

- Consumidores Kafka por tipo estão implementados, mas ainda operam sobre o domínio central de `transacao`; a separação funcional completa por bounded context segue planejada.
- Camada de resiliência (circuit breaker, retry, bulkhead por integração).
- Domínio de `Cliente` e `ChavePix`.
- Integração real com SPI/BACEN, STR e DICT.

---

## Estrutura Atual

```
transaction-processing-api/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/mekylei/transactionprocessing/
│   │   │   ├── TransactionProcessingApiApplication.java
│   │   │   ├── auditoria/
│   │   │   │   ├── AuditoriaListener.java
│   │   │   │   ├── AuditoriaService.java
│   │   │   │   ├── DadosAuditoria.java
│   │   │   │   ├── aplicacao/
│   │   │   │   │   └── AuditoriaContextPadrao.java
│   │   │   │   ├── dominio/
│   │   │   │   │   ├── AcaoAuditoria.java
│   │   │   │   │   └── AuditoriaEvento.java
│   │   │   │   └── porta/
│   │   │   │       ├── AuditoriaContextGateway.java
│   │   │   │       ├── AuditoriaContextWriter.java
│   │   │   │       └── AuditoriaRepository.java
│   │   │   ├── cliente/                           (estrutura inicial — planejado)
│   │   │   │   ├── aplicacao/
│   │   │   │   │   └── porta/
│   │   │   │   │       └── repositorio/
│   │   │   │   └── dominio/
│   │   │   ├── compartilhado/
│   │   │   │   ├── adaptador/
│   │   │   │   │   └── Jackson3FormatMapper.java
│   │   │   │   ├── constantes/
│   │   │   │   │   ├── HeadersHttp.java
│   │   │   │   │   └── ProblemaDetailConstantes.java
│   │   │   │   ├── dominio/
│   │   │   │   │   ├── TipoTransacao.java
│   │   │   │   │   └── ValorMonetario.java
│   │   │   │   ├── evento/
│   │   │   │   │   └── EventoDominio.java
│   │   │   │   ├── exception/
│   │   │   │   │   ├── ApiException.java
│   │   │   │   │   ├── BaseException.java
│   │   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   │   ├── KafkaPublicarException.java
│   │   │   │   │   ├── RecursoNaoEncontradoException.java
│   │   │   │   │   ├── RegraNegocioException.java
│   │   │   │   │   └── SaldoInsuficienteException.java
│   │   │   │   ├── idempotencia/
│   │   │   │   │   └── IdempotenciaService.java
│   │   │   │   ├── seguranca/
│   │   │   │   │   ├── HmacService.java
│   │   │   │   │   ├── HmacUtils.java
│   │   │   │   │   └── RoleTransacao.java
│   │   │   │   └── util/
│   │   │   │       ├── CalendarioStubBacenService.java
│   │   │   │       ├── CorrelacaoUtil.java
│   │   │   │       ├── CriptografiaConverter.java
│   │   │   │       └── DateTimeUtil.java
│   │   │   ├── configuracao/
│   │   │   │   ├── docs/                          (vazio — planejado)
│   │   │   │   ├── kafka/
│   │   │   │   │   ├── KafkaConfig.java
│   │   │   │   │   ├── KafkaDlqProperties.java
│   │   │   │   │   ├── OutboxProperties.java
│   │   │   │   │   └── TopicosProperties.java
│   │   │   │   ├── persistencia/
│   │   │   │   │   └── HmacProperties.java
│   │   │   │   ├── resiliencia/                   (vazio — planejado)
│   │   │   │   ├── seguranca/
│   │   │   │   │   ├── ApiAcessoNegadoHandler.java
│   │   │   │   │   ├── ApiAutenticacaoEntryPoint.java
│   │   │   │   │   ├── JwtClaimsConverter.java
│   │   │   │   │   ├── RateLimitResposta.java
│   │   │   │   │   └── SecurityConfig.java
│   │   │   │   └── spring/
│   │   │   │       ├── bean/
│   │   │   │       │   ├── AppConfig.java
│   │   │   │       │   ├── HibernateJsonConfig.java
│   │   │   │       │   └── StrategyConfig.java
│   │   │   │       └── filter/
│   │   │   │           ├── ContextoRequisicaoFilter.java
│   │   │   │           └── RateLimitFilter.java
│   │   │   ├── conta/
│   │   │   │   ├── aplicacao/
│   │   │   │   │   ├── porta/
│   │   │   │   │   │   └── repositorio/
│   │   │   │   │   │       ├── ContaRepository.java
│   │   │   │   │   │       ├── LimiteRepository.java
│   │   │   │   │   │       └── SaldoRepository.java
│   │   │   │   │   └── servico/
│   │   │   │   │       ├── LimiteService.java
│   │   │   │   │       └── SaldoService.java
│   │   │   │   ├── controle/                         (vazio — planejado)
│   │   │   │   └── dominio/
│   │   │   │       ├── Conta.java
│   │   │   │       ├── LimiteTransacional.java
│   │   │   │       ├── Saldo.java
│   │   │   │       ├── StatusConta.java
│   │   │   │       └── TipoConta.java
│   │   │   ├── infraestrutura/
│   │   │   │   ├── entidade/
│   │   │   │   │   ├── AuditoriaEventoEntity.java
│   │   │   │   │   ├── ContaEntity.java
│   │   │   │   │   ├── EventoProcessadoEntity.java
│   │   │   │   │   ├── LimiteTransacionalEntity.java
│   │   │   │   │   ├── OutboxEventoEntity.java
│   │   │   │   │   ├── SaldoEntity.java
│   │   │   │   │   ├── StatusOutboxEvento.java
│   │   │   │   │   └── TransacaoEntity.java
│   │   │   │   ├── persistencia/
│   │   │   │   │   ├── AuditoriaJpaAdapter.java
│   │   │   │   │   ├── ContaJpaAdapter.java
│   │   │   │   │   ├── EventoProcessadoJpaAdapter.java
│   │   │   │   │   ├── LimiteJpaAdapter.java
│   │   │   │   │   ├── OutboxEventoJpaAdapter.java
│   │   │   │   │   ├── SaldoJpaAdapter.java
│   │   │   │   │   └── TransacaoJpaAdapter.java
│   │   │   │   └── repositorio/
│   │   │   │       ├── AuditoriaJpaRepository.java
│   │   │   │       ├── ContaJpaRepository.java
│   │   │   │       ├── EventoProcessadoJpaRepository.java
│   │   │   │       ├── LimiteJpaRepository.java
│   │   │   │       ├── OutboxEventoJpaRepository.java
│   │   │   │       ├── SaldoJpaRepository.java
│   │   │   │       └── TransacaoJpaRepository.java
│   │   │   ├── integracao/
│   │   │   │   ├── antifraude/
│   │   │   │   │   └── AntiFraudeStubAdapter.java
│   │   │   │   ├── bacen/                         (vazio — planejado)
│   │   │   │   ├── spb/                           (vazio — planejado)
│   │   │   │   └── str/                           (vazio — planejado)
│   │   │   ├── mensageria/
│   │   │   │   ├── consumidor/
│   │   │   │   │   ├── DlqMonitorConsumidor.java
│   │   │   │   │   ├── PixTransacaoKafkaConsumidor.java
│   │   │   │   │   ├── TedTransacaoKafkaConsumidor.java
│   │   │   │   │   ├── TefTransacaoKafkaConsumidor.java
│   │   │   │   │   └── TransacaoIniciadaKafkaConsumidor.java
│   │   │   │   ├── aplicacao/
│   │   │   │   │   └── EventoProcessadoService.java
│   │   │   │   ├── evento/
│   │   │   │   │   └── TransacaoEventoRouter.java
│   │   │   │   ├── outbox/
│   │   │   │   │   ├── DominioEventoOutboxPublicador.java
│   │   │   │   │   ├── EventoOutboxPublicador.java
│   │   │   │   │   ├── OutboxEvento.java
│   │   │   │   │   └── OutboxEventoRepository.java
│   │   │   │   └── produtor/
│   │   │   │       └── KafkaEventoProdutor.java
│   │   │   ├── observabilidade/
│   │   │   │   ├── logging/
│   │   │   │   │   ├── LogMascaramentoConverter.java
│   │   │   │   │   ├── JsonMascaradoProvider.java
│   │   │   │   │   └── TipoCampoMascarado.java
│   │   │   │   ├── mascaramento/
│   │   │   │   │   ├── DadosSensiveisMasker.java
│   │   │   │   │   ├── MascaraPadrao.java
│   │   │   │   │   └── estrategia/
│   │   │   │   │       ├── AbstractRegexMascaraStrategy.java
│   │   │   │   │       ├── HeaderMascaradoStrategy.java
│   │   │   │   │       ├── JsonMascaradoStrategy.java
│   │   │   │   │       ├── MascaraStrategy.java
│   │   │   │   │       ├── MensagemMascaradaStrategy.java
│   │   │   │   │       ├── StacktraceMascaradoStrategy.java
│   │   │   │   │       └── StrategyMascaramentoResolver.java
│   │   │   │   ├── metrica/                       (vazio — planejado)
│   │   │   │   └── rastreamento/                  (vazio — planejado)
│   │   │   ├── pix/                               (estrutura inicial — planejado)
│   │   │   │   ├── aplicacao/
│   │   │   │   │   ├── porta/
│   │   │   │   │   │   └── integracao/
│   │   │   │   │   └── servico/
│   │   │   │   ├── controle/
│   │   │   │   ├── dominio/
│   │   │   │   ├── dto/
│   │   │   │   ├── enums/
│   │   │   │   ├── mapper/
│   │   │   │   └── validador/
│   │   │   ├── ted/                               (estrutura inicial — planejado)
│   │   │   │   ├── aplicacao/
│   │   │   │   │   ├── porta/
│   │   │   │   │   │   └── integracao/
│   │   │   │   │   └── servico/
│   │   │   │   ├── controle/
│   │   │   │   └── dto/
│   │   │   ├── tef/                               (estrutura inicial — planejado)
│   │   │   │   ├── aplicacao/
│   │   │   │   │   └── servico/
│   │   │   │   └── controle/
│   │   │   └── transacao/
│   │   │       ├── aplicacao/
│   │   │       │   ├── porta/
│   │   │       │   │   ├── evento/
│   │   │       │   │   │   └── EventoPublicador.java
│   │   │       │   │   ├── integracao/
│   │   │       │   │   │   ├── AntiFraudeGateway.java
│   │   │       │   │   │   └── PixGateway.java
│   │   │       │   │   └── repositorio/
│   │   │       │   │       └── TransacaoRepository.java
│   │   │       │   └── servico/
│   │   │       │       ├── ConsultaTransacaoService.java
│   │   │       │       ├── CriaTransacaoService.java
│   │   │       │       ├── EstornoTransacaoService.java
│   │   │       │       └── ProcessaTransacaoService.java
│   │   │       ├── controle/
│   │   │       │   ├── TransacaoController.java
│   │   │       │   └── dto/
│   │   │       │       ├── EstornoRequisicao.java
│   │   │       │       ├── EstornoResposta.java
│   │   │       │       ├── TransacaoRequisicao.java
│   │   │       │       └── TransacaoResposta.java
│   │   │       ├── dominio/
│   │   │       │   ├── StatusTransacao.java
│   │   │       │   ├── TipoEventoTransacao.java
│   │   │       │   ├── Transacao.java
│   │   │       │   ├── evento/
│   │   │       │   │   ├── TransacaoConcluidaEvento.java
│   │   │       │   │   ├── TransacaoEstornadaEvento.java
│   │   │       │   │   ├── TransacaoFalhouEvento.java
│   │   │       │   │   └── TransacaoIniciadaEvento.java
│   │   │       │   └── vo/
│   │   │       └── estrategia/
│   │   │           ├── PixTransacaoStrategy.java
│   │   │           ├── StrategyResolver.java
│   │   │           ├── TedTransacaoStrategy.java
│   │   │           ├── TefTransacaoStrategy.java
│   │   │           └── TransacaoStrategy.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       ├── logback-spring.xml
│   │       └── db/migration/
│   │           ├── V1__tabelas_iniciais.sql
│   │           ├── V2__auditoria.sql
│   │           ├── V3__mensageria.sql
│   │           └── V4__limite_versao.sql
│   └── test/
│       ├── java/com/mekylei/transactionprocessing/
│       │   ├── arquitetura/
│       │   │   ├── ArquiteturaHexagonalTest.java
│       │   │   ├── CamadaSegurancaTest.java
│       │   │   └── NamingConventionTest.java
│       │   ├── compartilhado/
│       │   │   ├── dominio/
│       │   │   │   └── ValorMonetarioTest.java
│       │   │   ├── idempotencia/
│       │   │   │   └── IdempotenciaServiceTest.java
│       │   │   ├── seguranca/
│       │   │   │   ├── HmacServiceTest.java
│       │   │   │   └── HmacUtilsTest.java
│       │   │   └── util/
│       │   │       ├── CalendarioStubBacenServiceTest.java
│       │   │       ├── CorrelacaoUtilTest.java
│       │   │       ├── CriptografiaConverterIntegrationTest.java
│       │   │       └── CriptografiaConverterTest.java
│       │   ├── configuracao/spring/filter/
│       │   │   ├── ContextoRequisicaoFilterTest.java
│       │   │   └── RateLimitFilterTest.java
│       │   ├── conta/
│       │   │   ├── aplicacao/servico/
│       │   │   │   ├── LimiteServiceTest.java
│       │   │   │   ├── SaldoIntegrationTest.java
│       │   │   │   └── SaldoServiceTest.java
│       │   │   └── dominio/
│       │   │       ├── ContaTest.java
│       │   │       ├── LimiteTransacionalTest.java
│       │   │       └── SaldoTest.java
│       │   ├── infraestrutura/
│       │   │   ├── entidade/
│       │   │   │   └── ContaBancariaEntity.java
│       │   │   ├── persistencia/
│       │   │   │   ├── AuditoriaJpaAdapterTest.java
│       │   │   │   ├── ContaJpaAdapterTest.java
│       │   │   │   ├── EventoProcessadoJpaAdapterTest.java
│       │   │   │   ├── LimiteJpaAdapterTest.java
│       │   │   │   ├── SaldoJpaAdapterTest.java
│       │   │   │   └── TransacaoJpaAdapterTest.java
│       │   │   └── repositorio/
│       │   │       └── ContaBancariaTestRepository.java
│       │   ├── mensageria/
│       │   │   ├── consumidor/
│       │   │   │   ├── DlqMonitorConsumidorTest.java
│       │   │   │   ├── EventoProcessadoServiceTest.java
│       │   │   │   ├── PixTransacaoKafkaConsumidorTest.java
│       │   │   │   ├── TedTransacaoKafkaConsumidorTest.java
│       │   │   │   ├── TefTransacaoKafkaConsumidorTest.java
│       │   │   │   └── TransacaoIniciadaKafkaConsumidorTest.java
│       │   │   ├── evento/
│       │   │   │   └── TransacaoEventoRouterTest.java
│       │   │   ├── outbox/
│       │   │   │   ├── EventoOutboxPublicadorSchedulerTest.java
│       │   │   │   ├── EventoOutboxPublicadorTest.java
│       │   │   │   ├── OutboxEventoIntegrationTest.java
│       │   │   │   └── OutboxEventoJpaAdapterTest.java
│       │   │   └── produtor/
│       │   │       └── KafkaEventoProdutorTest.java
│       │   ├── observabilidade/mascaramento/
│       │   │   ├── DadosSensiveisMaskerTest.java
│       │   │   └── estrategia/
│       │   │       ├── HeaderMascaradoStrategyTest.java
│       │   │       ├── JsonMascaradoStrategyTest.java
│       │   │       ├── MensagemMascaradaStrategyTest.java
│       │   │       └── StrategyMascaramentoResolverTest.java
│       │   ├── transacao/
│       │   │   ├── aplicacao/servico/
│       │   │   │   ├── ConsultaTransacaoServiceTest.java
│       │   │   │   ├── CriaTransacaoServiceTest.java
│       │   │   │   ├── EstornoIntegrationTest.java
│       │   │   │   ├── EstornoTransacaoServiceTest.java
│       │   │   │   ├── ProcessaTransacaoIntegrationTest.java
│       │   │   │   └── ProcessaTransacaoServiceTest.java
│       │   │   ├── controle/
│       │   │   │   └── TransacaoControllerTest.java
│       │   │   ├── dominio/
│       │   │   │   ├── TransacaoTest.java
│       │   │   │   └── evento/
│       │   │   │       └── TransacaoEventosTest.java
│       │   │   └── estrategia/
│       │   │       ├── PixTransacaoStrategyTest.java
│       │   │       ├── StrategyResolverTest.java
│       │   │       ├── TedTransacaoStrategyTest.java
│       │   │       └── TefTransacaoStrategyTest.java
│       │   ├── IntegrationTestBase.java
│       │   └── TransactionProcessingApiApplicationTests.java
│       └── resources/
│           ├── application-integration-test.yml
│           └── application-test.yml
```

## 3. Arquitetura Alvo

```
┌─────────────────────────────────────────────────────────────────────┐
│                              API LAYER                              │
│  ┌──────────────────────────┐  ┌──────────────────────┐             │
│  │ ContextoRequisicaoFilter │  │   RateLimitFilter    │             │
│  └──────────────┬───────────┘  └──────────┬───────────┘             │
│                 │                         │                         │
│          ┌──────▼─────────────────────────▼──────┐                  │
│          │          TransacaoController          │                  │
│          │       /api/v1/transacoes/**           │                  │
│          └──────────────────────┬────────────────┘                  │
└─────────────────────────────────┼───────────────────────────────────┘
                                  │
┌─────────────────────────────────▼───────────────────────────────────┐
│                        APPLICATION LAYER                            │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │                  ProcessaTransacaoService                     │  │
│  │  1. Valida idempotência                                       │  │
│  │  2. Valida conta de origem                                    │  │
│  │  3. Valida saldo e limite                                     │  │
│  │  4. Cria transação PENDENTE e publica evento iniciado         │  │
│  │  5. Resolve a strategy e processa a transação                 │  │
│  │  6. Debita saldo e atualiza limite quando COMPLETADA          │  │
│  │  7. Persiste o estado final                                   │  │
│  │  8. Publica evento final via Outbox                           │  │
│  └───────────────────────────────────────────────────────────────┘  │
│  ┌────────────────────┐  ┌──────────────────┐  ┌────────────────┐   │
│  │ ConsultaTransacao  │  │ EstornoTransacao │  │ CriaTransacao  │   │
│  └────────────────────┘  └──────────────────┘  └────────────────┘   │
│  ┌────────────────────┐  ┌──────────────────┐  ┌────────────────┐   │
│  │ IdempotenciaService│  │   SaldoService   │  │ LimiteService  │   │
│  └────────────────────┘  └──────────────────┘  └────────────────┘   │
└─────────────────────────────────┬───────────────────────────────────┘
                                  │
┌─────────────────────────────────▼───────────────────────────────────┐
│                          DOMAIN LAYER                               │
│  ┌──────────────┐  ┌──────────┐  ┌───────────────┐                  │
│  │  Transacao   │  │  Conta   │  │ ValorMonetario│                  │
│  └──────────────┘  └──────────┘  └───────────────┘                  │
│  ┌──────────────┐  ┌────────────────────┐  ┌────────────────────┐   │
│  │    Saldo     │  │ LimiteTransacional │  │ TransacaoStrategy  │   │
│  └──────────────┘  └────────────────────┘  └────────────────────┘   │
└─────────────────────────────────┬───────────────────────────────────┘
                                  │
┌─────────────────────────────────▼───────────────────────────────────┐
│                      INFRASTRUCTURE LAYER                           │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐   │
│  │ TransacaoJpaAdap.│  │  ContaJpaAdapter │  │  SaldoJpaAdapter │   │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘   │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐   │
│  │ LimiteJpaAdapter │  │ AuditoriaJpaAdap.│  │ OutboxEventoJPA  │   │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘   │
│  ┌──────────────────┐                                               │
│  │ AntiFraudeStubAd.│                                               │
│  └──────────────────┘                                               │
└─────────────────────────────────┬───────────────────────────────────┘
                                  │
┌─────────────────────────────────▼───────────────────────────────────┐
│                     EXTERNAL INTEGRATIONS                           │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐   │
│  │    PostgreSQL    │  │ OAuth2/JWT JWKS  │  │   Kafka (SSL)    │   │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘   │
│  ┌──────────────────┐                                               │
│  │ Antifraude Stub  │                                               │
│  └──────────────────┘                                               │
└─────────────────────────────────────────────────────────────────────┘
```

## Diagrama de Relacionamento de Agregados

```
Conta
  │
  ├── Saldo
  └── LimiteTransacional

Transacao
  │
  ├── idContaOrigem ───────► Conta
  └── valor ───────────────► ValorMonetario

AuditoriaEvento
  └── referencia recurso e idRecurso auditado
```

### RBAC (Controle de Acesso Baseado em Papéis)

| Role              | Permissões                                                        |
| ----------------- | ----------------------------------------------------------------- |
| `CLIENTE`         | Processar PIX, TED e TEF, e consultar transações                  |
| `OPERADOR`        | Processar PIX, TED e TEF, e consultar transações                  |
| `GERENTE`         | Processar PIX, TED e TEF, consultar transações e realizar estorno |
| `ADMIN`           | Processar PIX, TED e TEF, consultar transações e realizar estorno |
| `SERVICO_INTERNO` | Processar PIX, TED e TEF, e consultar transações                  |

---

# Segurança

## Implementado

- **OAuth2 Resource Server**: configurado em `SecurityConfig.java` com validação de JWT via JWKS endpoint. Suporta Keycloak (dev) e provedores externos (prod) via variáveis de ambiente `OAUTH2_JWKS_URI` e `OAUTH2_ISSUER_URI`.
- **JWT Claims Converter**: `JwtClaimsConverter` extrai a claim `roles` do token e mapeia para `GrantedAuthority` no formato `ROLE_<NOME>`.
- **RBAC (Role-Based Access Control)**: cinco papéis definidos em `RoleTransacao` — `CLIENTE`, `OPERADOR`, `GERENTE`, `ADMIN`, `SERVICO_INTERNO`. Os endpoints de PIX, TED, TEF e consulta exigem uma dessas roles; estorno é restrito a `GERENTE` e `ADMIN`.
- **Sessão stateless**: `SessionCreationPolicy.STATELESS` configurado.
- **Rate limiting por IP**: `RateLimitFilter` via Bucket4j com capacidade configurável (`app.rate-limit.requests-per-minute`). Respeita cabeçalho `X-Forwarded-For`.
- **Auditoria regulatória**: `AuditoriaListener` captura eventos JPA lifecycle (`@PostPersist`, `@PostUpdate`, `@PostLoad`) e registra em tabela append-only com regras PostgreSQL impedindo UPDATE e DELETE.
- **Tratamento de erros de autenticação/autorização**: `ApiAutenticacaoEntryPoint` e `ApiAcessoNegadoHandler` retornam respostas padronizadas.
- **Endpoints públicos**: `/actuator/health`, `/actuator/info`, `/v3/api-docs/**`, `/swagger-ui/**` e `/swagger-ui.html` acessíveis sem autenticação.
- **Criptografia em repouso**: `CriptografiaConverter` implementado como `AttributeConverter` JPA com AES-256-GCM e IV aleatório gerado por `SecureRandom` a cada operação de escrita — campos sensíveis das entidades armazenados criptografados no banco de dados.
- **Integridade com HMAC**: `HmacService` com HMAC-SHA256 para geração de assinaturas determinísticas, com normalização de valores (`trim` + `toUpperCase`) antes do cálculo. Usado para manter campos criptografados pesquisáveis por HMAC, como agência e número de conta. Configurado via `HmacProperties`.
- **Mascaramento de dados sensíveis nos logs**: subsistema `observabilidade/mascaramento` com interface `MascaraStrategy` e implementações intercambiáveis para JSON fields, headers HTTP, mensagens de log e stack traces. Integrado ao Logback via `LogMascaramentoConverter` e ao encoder JSON via `JsonMascaradoProvider` (logstash-logback-encoder).

---

# Instalação e Execução

## Pré-requisitos

- Java 21 (JDK)
- Maven 3.9+ (opcional, caso não utilize o wrapper `./mvnw`)
- PostgreSQL 16+
- Kafka 3.9+ (necessário quando `EVENTOS_KAFKA_ENABLED=true`)
- Authorization Server compatível com OAuth2/OIDC (ex: Keycloak) com JWKS endpoint exposto

## Variáveis de Ambiente

| Variável                                      | Descrição                                      | Exemplo                                                               |
| --------------------------------------------- | ---------------------------------------------- | --------------------------------------------------------------------- |
| `DB_URL`                                      | URL JDBC do banco                              | `jdbc:postgresql://localhost:5432/transacaodb`                        |
| `DB_USERNAME`                                 | Usuário do banco                               | `ozzy`                                                                |
| `DB_PASSWORD`                                 | Senha do banco                                 | `ozzy`                                                                |
| `OAUTH2_JWKS_URI`                             | Endpoint JWKS do Authorization Server          | `https://keycloak.host/realms/bancario/protocol/openid-connect/certs` |
| `OAUTH2_ISSUER_URI`                           | Issuer URI do Authorization Server             | `https://keycloak.host/realms/bancario`                               |
| `APP_CRIPTOGRAFIA_CHAVE`                      | Chave de criptografia Base64 (AES-256)         | `MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=`                        |
| `APP_HMAC_CHAVE`                              | Chave usada no HMAC-SHA256                     | `xK9mP2vL8nQ5rT1wY4zB7cF3hJ6kU8aR0dG2hN5pS7vL9m`                      |
| `RATE_LIMIT_REQUISICOES_POR_MINUTO`           | Limite de requisições por IP por minuto        | `60`                                                                  |
| `ANTIFRAUDE_LIMITE_APROVACAO_AUTOMATICA`      | Valor máximo para aprovação automática no stub | `10000.00`                                                            |
| `EVENTOS_KAFKA_ENABLED`                       | Habilita publicação/consumo de eventos Kafka   | `true`                                                                |
| `EVENTOS_KAFKA_DLQ_MAX_TENTATIVAS`            | Número máximo de tentativas antes da DLQ       | `3`                                                                   |
| `EVENTOS_KAFKA_DLQ_INTERVALO_REPROCESSAMENTO` | Intervalo de reprocessamento da DLQ            | `10s`                                                                 |
| `EVENTOS_OUTBOX_LOTE_PUBLICACAO`              | Tamanho do lote de publicação do outbox        | `50`                                                                  |
| `EVENTOS_OUTBOX_INTERVALO_PUBLICACAO_MS`      | Intervalo do job de publicação do outbox       | `5000`                                                                |
| `EVENTOS_OUTBOX_INTERVALO_REPROCESSAMENTO`    | Intervalo de reprocessamento do outbox         | `30s`                                                                 |
| `EVENTOS_OUTBOX_TIMEOUT_ENVIO_MS`             | Timeout de envio de eventos                    | `5000`                                                                |
| `KAFKA_BOOTSTRAP_SERVERS`                     | Endereço do broker Kafka                       | `kafka.lab.home:9094`                                                 |
| `KAFKA_USERNAME`                              | Usuário SASL/SCRAM do Kafka                    | `app-prod`                                                            |
| `KAFKA_PASSWORD`                              | Senha SASL/SCRAM do Kafka                      | `trocar-esta-senha`                                                   |
| `KAFKA_SSL_TRUSTSTORE_LOCATION`               | Local do truststore cliente Kafka              | `file:/caminho/kafka-client-truststore.p12`                           |
| `KAFKA_SSL_TRUSTSTORE_PASSWORD`               | Senha do truststore cliente Kafka              | `changeit`                                                            |
| `KAFKA_SSL_TRUSTSTORE_TYPE`                   | Tipo do truststore                             | `PKCS12`                                                              |

## Execução Local

### IMPORTANTE

Com o perfil `dev`, as variáveis de banco, criptografia, HMAC e OAuth2 possuem valores padrão, mas são normalmente sobrescritas via variáveis de ambiente na IDE, por exemplo, ou utilizando um arquivo `.env` para build com `docker compose`.

Rodo o Keycloak em um servidor homelab e utilizo um `.env` para definir os dados de acesso ao banco dele. Este servidor possui uma unidade certificadora que, junto com o gateway (Traefik), possibilita a utilização de HTTPS entre os serviços na rede local. Dessa forma, a chave de criptografia e as URIs OAuth2 apontam para o recurso do homelab (`keycloak.lab.home`). Mais abaixo disponibilizo o script para criar os containers do Keycloak e o seu banco de dados. O broker do Kafka também roda na mesma infra.

É preciso configurar as `ROLES` [CLIENTE, OPERADOR, GERENTE, ADMIN, SERVICO_INTERNO] no provedor de autenticação e expô-las na claim `roles` do token.

### .env para variáveis de ambiente de exemplo

**Importante:** salvar o arquivo `.env` na raiz do projeto para que o `docker compose` consiga lê-lo automaticamente ou definir as variáveis na sua IDE. O `docker-compose.yml` atual sobe a aplicação e o PostgreSQL, mas repassa ao container apenas banco, OAuth2, criptografia, rate limit e antifraude; para executar via compose com Kafka habilitado, inclua as variáveis Kafka no bloco `environment` da app. Se não for usar Kafka no compose, inclua `EVENTOS_KAFKA_ENABLED=false` no `environment` da app, pois o perfil `dev` habilita Kafka por padrão. É fundamental adicionar o arquivo `.env` no `.gitignore` para não versioná-lo.

```env
POSTGRES_DB=transacaodb
DB_USERNAME=ozzy
DB_PASSWORD=ozzy

OAUTH2_JWKS_URI=https://keycloak.lab.home/realms/bancario/protocol/openid-connect/certs
OAUTH2_ISSUER_URI=https://keycloak.lab.home/realms/bancario

APP_CRIPTOGRAFIA_CHAVE=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=

RATE_LIMIT_REQUISICOES_POR_MINUTO=60

ANTIFRAUDE_LIMITE_APROVACAO_AUTOMATICA=10000.00

APP_HMAC_CHAVE=xK9mP2vL8nQ5rT1wY4zB7cF3hJ6kU8aR0dG2hN5pS7vL9m

EVENTOS_KAFKA_ENABLED=true
EVENTOS_KAFKA_DLQ_MAX_TENTATIVAS=3
EVENTOS_KAFKA_DLQ_INTERVALO_REPROCESSAMENTO=10s
EVENTOS_OUTBOX_LOTE_PUBLICACAO=50
EVENTOS_OUTBOX_INTERVALO_PUBLICACAO_MS=5000
EVENTOS_OUTBOX_INTERVALO_REPROCESSAMENTO=30s
EVENTOS_OUTBOX_TIMEOUT_ENVIO_MS=5000
KAFKA_BOOTSTRAP_SERVERS=kafka.lab.home:9094
KAFKA_USERNAME=app-prod
KAFKA_PASSWORD=trocar-esta-senha
KAFKA_SSL_TRUSTSTORE_LOCATION=file:/<CAMINHO_DO_CERTIFICADO>/kafka-client-truststore.p12
KAFKA_SSL_TRUSTSTORE_PASSWORD=changeit
KAFKA_SSL_TRUSTSTORE_TYPE=PKCS12
```

Para execução local apontando para o broker do homelab, as variáveis mínimas complementares são:

```env
EVENTOS_KAFKA_ENABLED=true
KAFKA_BOOTSTRAP_SERVERS=kafka.lab.home:9094
KAFKA_PASSWORD=trocar-esta-senha
KAFKA_SSL_TRUSTSTORE_LOCATION=file:/<CAMINHO_DO_CERTIFICADO>/kafka-client-truststore.p12
KAFKA_SSL_TRUSTSTORE_PASSWORD=changeit
KAFKA_SSL_TRUSTSTORE_TYPE=PKCS12
KAFKA_USERNAME=app-prod
```

### Rodar de forma direta

```bash
# Clonar o repositório
git clone <url-do-repositorio>
cd transaction-processing-api

# Executar com perfil de desenvolvimento
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

#### Build

```bash
# Build sem execução de testes
./mvnw clean package -DskipTests

# Build com execução de testes
./mvnw clean package
```

O artefato gerado fica no formato `target/transaction-processing-<versao>.jar` e, atualmente, é `target/transaction-processing-0.0.1-SNAPSHOT.jar`.

#### Testes

```bash
# Executar todos os testes
./mvnw test

# Executar teste específico
./mvnw test -Dtest=TransactionProcessingApiApplicationTests
```

### Rodar usando o Dockerfile e docker-compose.yml

**Importante:** Se não for utilizar uma unidade certificadora local para HTTPS, comentar ou remover as linhas abaixo do Dockerfile.

```bash
# === COMENTAR OU REMOVER ===
# USADO PARA IMPORTAR O CERTIFICADO PARA A JVM E EVITAR QUE ERRO DE CONFIABILIDADE NAS REQUISIÇÕES PARA O KEYCLOAK

# Copia o certificado da unidade certificadora para o container
COPY certificados/root_ca.crt /tmp/root_ca.crt

# Importa no truststore da JVM
RUN keytool -importcert \
    -noprompt \
    -trustcacerts \
    -alias root-ca \
    -file /tmp/root_ca.crt \
    -keystore $JAVA_HOME/lib/security/cacerts \
    -storepass changeit

```

```bash
# docker compose v2

docker compose build --no-cache
docker compose up -d

```

### Executar script para popular a base com dados para teste

**Importante:** O SQL abaixo é disponibilizado para quem precisar popular a base com dados iniciais. Mantenha um arquivo local `seed.sql` com esse conteúdo para uso manual quando necessário. O diretório `/seed-db` está no `.gitignore`, portanto esse arquivo não deve ser versionado.

```bash
# abra o terminal no diretório do arquivo seed.sql

# envia o arquivo para a pasta /tmp/ do container
docker cp seed.sql transacao_postgres:/tmp/seed.sql


-- ============================================================
-- Executar o script considerando usuário e banco do .env
-- ============================================================

# entrar no container e executar o script
docker exec -it transacao_postgres /bin/sh
psql -U ozzy -d transacaodb -f /tmp/seed.sql

# ou em um só comando
docker exec transacao_postgres psql -U ozzy -d transacaodb -f /tmp/seed.sql
```

```sql
-- ============================================================
-- SEED — Transaction Processing API — Fase 1
-- Executar após as migrations do Flyway terem criado as tabelas
-- ============================================================

-- IDs fixos para uso direto nos testes via curl
-- Alice  → ATIVA,    saldo R$ 10.000  (fluxos normais + limite)
-- Bob    → ATIVA,    saldo R$    100  (cenário saldo insuficiente)
-- Carol  → BLOQUEADA, saldo R$  2.000 (cenário conta inativa)

-- -------------------------------------------------------
-- CONTAS
-- -------------------------------------------------------
-- CONTAS
-- agencia: 0001
-- 1 - número conta: 00001-9
-- 2 - número conta: 00002-7
-- 3 - número conta: 00003-5
-- -------------------------------------------------------
INSERT INTO public.conta (id, agencia, agencia_hmac, criado_em, id_cliente, numero_conta, numero_conta_hmac, status, tipo)
VALUES
    ('aaaaaaaa-0000-0000-0000-000000000001', 'ndmQxuQoMUN8023lOFhONmW+cTAwTG/ob04lM9i8WQk=', '4f3baf8df9c85e5a9d56f3df7f6c4a9efdb8f3c13f3d0b4c4cbfdcf05a4a9c0d',  NOW(), '11111111-0000-0000-0000-000000000001', 'Gp9UEf+hVdymVh+gxKDVI6v9dD2n6XfOPY+duIq5rTCt2Xc=', '74e38e5e3a9e1f61d36f22e6b59c1f7b33a2db18f4d0a4d876c0a1d8a2d3f2e9',  'ATIVA',      'CORRENTE'),
    ('bbbbbbbb-0000-0000-0000-000000000002', 'ndmQxuQoMUN8023lOFhONmW+cTAwTG/ob04lM9i8WQk=', '4f3baf8df9c85e5a9d56f3df7f6c4a9efdb8f3c13f3d0b4c4cbfdcf05a4a9c0d', NOW(), '22222222-0000-0000-0000-000000000002', 'cawdBLoH00mTeqODJhn9Dh3iAZfbUcbeP4VdkA7pAwATP2U=', '2dcf0d48f7a2a4bb3f59cb34d1e08c89f0cf57c65bc46a52f9b2f67a15a42a87',  'ATIVA',      'CORRENTE'),
    ('cccccccc-0000-0000-0000-000000000003', 'ndmQxuQoMUN8023lOFhONmW+cTAwTG/ob04lM9i8WQk=', '4f3baf8df9c85e5a9d56f3df7f6c4a9efdb8f3c13f3d0b4c4cbfdcf05a4a9c0d', NOW(), '33333333-0000-0000-0000-000000000003', 'GY+ApqCbh1PBcfZ8OGr+x7x33X6OREvAYV+Njd2d+PuHJ3E=', '6f81db6c2ecfd0f3d0a7a3cb8f0dfd8cb4eec0b02a9e67b09cf4f4aef16cb8a3',  'BLOQUEADA',  'CORRENTE');


-- -------------------------------------------------------
-- SALDOS
-- -------------------------------------------------------
INSERT INTO public.saldo (id, atualizado_em, bloqueado, disponivel, id_conta, versao)
VALUES
    ('a1a1a1a1-0000-0000-0000-000000000001', NOW(), 0.00, 10000.00, 'aaaaaaaa-0000-0000-0000-000000000001', 0),
    ('b2b2b2b2-0000-0000-0000-000000000002', NOW(), 0.00,   100.00, 'bbbbbbbb-0000-0000-0000-000000000002', 0),
    ('c3c3c3c3-0000-0000-0000-000000000003', NOW(), 0.00,  2000.00, 'cccccccc-0000-0000-0000-000000000003', 0);

-- -------------------------------------------------------
-- LIMITES TRANSACIONAIS
--   limite_utilizado = limite por transação (campo mal nomeado na DDL)
--   limite_diario    = teto acumulado no dia
--   utilizado_hoje   = quanto já foi consumido hoje
--
-- Alice — limites normais, zerado hoje
-- Bob   — limites normais, zerado hoje (saldo é o gargalo)
-- Carol — bloqueada, limites irrelevantes mas necessários para consistência
-- -------------------------------------------------------

-- Alice — PIX: até R$ 2.000 por transação, R$ 5.000/dia
INSERT INTO public.limite (id, data_referencia, id_conta, limite_diario, limite_utilizado, tipo, utilizado_hoje)
VALUES ('a1000001-0000-0000-0000-000000000001', CURRENT_DATE, 'aaaaaaaa-0000-0000-0000-000000000001', 5000.00,  2000.00, 'PIX', 0.00);

-- Alice — TED: até R$ 5.000 por transação, R$ 10.000/dia
INSERT INTO public.limite (id, data_referencia, id_conta, limite_diario, limite_utilizado, tipo, utilizado_hoje)
VALUES ('a1000002-0000-0000-0000-000000000001', CURRENT_DATE, 'aaaaaaaa-0000-0000-0000-000000000001', 10000.00, 5000.00, 'TED', 0.00);

-- Alice — TEF: até R$ 500 por transação, R$ 1.500/dia
INSERT INTO public.limite (id, data_referencia, id_conta, limite_diario, limite_utilizado, tipo, utilizado_hoje)
VALUES ('a1000003-0000-0000-0000-000000000001', CURRENT_DATE, 'aaaaaaaa-0000-0000-0000-000000000001', 1500.00,   500.00, 'TEF', 0.00);

-- Bob — PIX
INSERT INTO public.limite (id, data_referencia, id_conta, limite_diario, limite_utilizado, tipo, utilizado_hoje)
VALUES ('b2000001-0000-0000-0000-000000000002', CURRENT_DATE, 'bbbbbbbb-0000-0000-0000-000000000002', 5000.00,  2000.00, 'PIX', 0.00);

-- Bob — TED
INSERT INTO public.limite (id, data_referencia, id_conta, limite_diario, limite_utilizado, tipo, utilizado_hoje)
VALUES ('b2000002-0000-0000-0000-000000000002', CURRENT_DATE, 'bbbbbbbb-0000-0000-0000-000000000002', 10000.00, 5000.00, 'TED', 0.00);

-- Bob — TEF
INSERT INTO public.limite (id, data_referencia, id_conta, limite_diario, limite_utilizado, tipo, utilizado_hoje)
VALUES ('b2000003-0000-0000-0000-000000000002', CURRENT_DATE, 'bbbbbbbb-0000-0000-0000-000000000002', 1500.00,   500.00, 'TEF', 0.00);

-- Carol — PIX (bloqueada, mas inserção mantém consistência)
INSERT INTO public.limite (id, data_referencia, id_conta, limite_diario, limite_utilizado, tipo, utilizado_hoje)
VALUES ('c3000001-0000-0000-0000-000000000003', CURRENT_DATE, 'cccccccc-0000-0000-0000-000000000003', 5000.00,  2000.00, 'PIX', 0.00);


-- VERIFICAÇÃO

select c.id as idconta,
       c.numero_conta,
	     c.status as status_conta,
	     s.disponivel,
	     s.bloqueado,
	     l.tipo as limite_tipo,
	     l.limite_utilizado as limite_por_transacao,
	     l.limite_diario,
	     l.utilizado_hoje
  from conta c
       join saldo s on s.id_conta = c.id
       join limite l on l.id_conta = c.id
 order by c.numero_conta, l.tipo;

```

---

### Infraestrutura do homelab

```
.homelab
├── arcane
│   └── docker-compose.yml
├── check-certs.sh
├── database
│   └── docker-compose.yml
├── docs
│   └── gitlab-access-context.md
├── gitlab
│   ├── backup.sh
│   ├── config
│   ├── data
│   ├── docker-compose.yml
│   └── logs
├── gitlab-runner
│   ├── config
│   └── docker-compose.yml
├── infra-down.sh
├── infra-up.sh
├── k8s-states
│   ├── myblog-dev.txt
│   └── myblog.txt
├── kafka
│   ├── certs
│   ├── docker-compose.yml
│   ├── generate-kafka-certs.sh
│   └── kafka_server_jaas.conf
├── keycloak
│   ├── backups
│   ├── docker-compose.yml
│   ├── keycloak
│   ├── keycloak-down.sh
│   ├── keycloak-up.sh
│   └── postgres
├── README.md
├── step-ca
│   ├── data
│   ├── docker-compose.yml
│   └── root_ca.crt
└── traefik
    ├── certs
    ├── config
    ├── docker-compose.yml
    └── logs

```

### docker-compose.yml (postgre e keycloak)

A variável `KC_HOSTNAME: https://keycloak.lab.home` define o host do keycloak.
Pode ser diferente dependendo de como o serviço será provido. O mesmo vale para as `labels` do `traefik`.
É claro que pode ser utilizado um provedor externo para autenticação via OAuth2/JWT.

```yml
services:
  postgres-keycloak:
    image: postgres:17
    container_name: postgres-keycloak
    restart: unless-stopped

    networks:
      - infra-net

    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}

    volumes:
      - postgres-keycloak-data:/var/lib/postgresql/data

    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U $$POSTGRES_USER -d $$POSTGRES_DB"]
      interval: 10s
      timeout: 5s
      retries: 10

  keycloak:
    image: quay.io/keycloak/keycloak:26.2
    container_name: keycloak
    restart: unless-stopped

    depends_on:
      postgres-keycloak:
        condition: service_healthy

    networks:
      - infra-net

    healthcheck:
      test:
        [
          "CMD-SHELL",
          "exec 3<>/dev/tcp/localhost/9000 && echo -e 'GET /health/ready HTTP/1.1\\r\\nHost: localhost\\r\\nConnection: close\\r\\n\\r\\n' >&3 && cat <&3 | grep -q '200 OK'",
        ]
      interval: 15s
      timeout: 5s
      retries: 20
      start_period: 90s

    environment:
      KC_BOOTSTRAP_ADMIN_USERNAME: ${KEYCLOAK_ADMIN}
      KC_BOOTSTRAP_ADMIN_PASSWORD: ${KEYCLOAK_ADMIN_PASSWORD}

      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://postgres-keycloak:5432/${POSTGRES_DB}
      KC_DB_USERNAME: ${POSTGRES_USER}
      KC_DB_PASSWORD: ${POSTGRES_PASSWORD}

      KC_HOSTNAME: https://keycloak.lab.home
      KC_HOSTNAME_STRICT: "false"
      KC_HTTP_ENABLED: "true"
      KC_HTTP_PORT: "8080"
      KC_PROXY_HEADERS: xforwarded

      KC_HEALTH_ENABLED: "true"
      KC_METRICS_ENABLED: "true"

    command:
      - start

    volumes:
      - ./keycloak/import:/opt/keycloak/data/import
      - ./keycloak/themes:/opt/keycloak/themes

    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.keycloak.rule=Host(`keycloak.lab.home`)"
      - "traefik.http.routers.keycloak.entrypoints=websecure"
      - "traefik.http.routers.keycloak.tls=true"
      - "traefik.http.routers.keycloak.tls.certresolver=step-ca"
      - "traefik.http.routers.keycloak.service=keycloak-svc"
      - "traefik.http.routers.keycloak.middlewares=rate-limit@file"
      - "traefik.http.services.keycloak-svc.loadbalancer.server.port=8080"

volumes:
  postgres-keycloak-data:

networks:
  infra-net:
    external: true
```

### docker-compose.yml (kafka no homelab)

```yml
services:
  kafka:
    image: ${KAFKA_IMAGE}
    container_name: kafka
    restart: unless-stopped

    networks:
      - infra-net

    environment:
      KAFKA_NODE_ID: ${KAFKA_NODE_ID}
      KAFKA_PROCESS_ROLES: ${KAFKA_PROCESS_ROLES}
      KAFKA_LISTENERS: ${KAFKA_LISTENERS}
      KAFKA_ADVERTISED_LISTENERS: ${KAFKA_ADVERTISED_LISTENERS}
      KAFKA_INTER_BROKER_LISTENER_NAME: ${KAFKA_INTER_BROKER_LISTENER_NAME}
      KAFKA_CONTROLLER_LISTENER_NAMES: ${KAFKA_CONTROLLER_LISTENER_NAMES}
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: ${KAFKA_LISTENER_SECURITY_PROTOCOL_MAP}
      KAFKA_CONTROLLER_QUORUM_VOTERS: ${KAFKA_CONTROLLER_QUORUM_VOTERS}
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: ${KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR}
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: ${KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR}
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: ${KAFKA_TRANSACTION_STATE_LOG_MIN_ISR}
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: ${KAFKA_AUTO_CREATE_TOPICS_ENABLE}
      KAFKA_LOG_DIRS: ${KAFKA_LOG_DIRS}
      KAFKA_SSL_KEYSTORE_TYPE: ${KAFKA_SSL_KEYSTORE_TYPE}
      KAFKA_SSL_KEYSTORE_FILENAME: ${KAFKA_SSL_KEYSTORE_FILENAME}
      KAFKA_SSL_KEYSTORE_CREDENTIALS: ${KAFKA_SSL_KEYSTORE_CREDENTIALS}
      KAFKA_SSL_KEY_CREDENTIALS: ${KAFKA_SSL_KEY_CREDENTIALS}
      KAFKA_SSL_TRUSTSTORE_TYPE: ${KAFKA_SSL_TRUSTSTORE_TYPE}
      KAFKA_SSL_TRUSTSTORE_FILENAME: ${KAFKA_SSL_TRUSTSTORE_FILENAME}
      KAFKA_SSL_TRUSTSTORE_CREDENTIALS: ${KAFKA_SSL_TRUSTSTORE_CREDENTIALS}
      KAFKA_SSL_CLIENT_AUTH: ${KAFKA_SSL_CLIENT_AUTH}
      KAFKA_SASL_ENABLED_MECHANISMS: ${KAFKA_SASL_ENABLED_MECHANISMS}
      KAFKA_OPTS: ${KAFKA_OPTS}

    volumes:
      - ${KAFKA_DATA_VOLUME}:/var/lib/kafka/data
      - ./certs:/etc/kafka/secrets:ro
      - ./kafka_server_jaas.conf:/etc/kafka/kafka_server_jaas.conf:ro

    healthcheck:
      test:
        [
          "CMD-SHELL",
          "bash -ec 'echo > /dev/tcp/127.0.0.1/${KAFKA_HEALTHCHECK_PORT}'",
        ]
      interval: ${KAFKA_HEALTHCHECK_INTERVAL}
      timeout: ${KAFKA_HEALTHCHECK_TIMEOUT}
      retries: ${KAFKA_HEALTHCHECK_RETRIES}
      start_period: ${KAFKA_HEALTHCHECK_START_PERIOD}

    labels:
      - "traefik.enable=true"
      - "traefik.tcp.routers.kafka.rule=HostSNI(`kafka.lab.home`)"
      - "traefik.tcp.routers.kafka.entrypoints=kafkassl"
      - "traefik.tcp.routers.kafka.tls=true"
      - "traefik.tcp.routers.kafka.tls.passthrough=true"
      - "traefik.tcp.services.kafka.loadbalancer.server.port=9094"

volumes:
  kafka-data:

networks:
  infra-net:
    external: true
```

### Exemplo do .env (kafka)

```env
KAFKA_IMAGE=apache/kafka:3.9.0
KAFKA_NODE_ID=1
KAFKA_PROCESS_ROLES=broker,controller
KAFKA_LISTENERS=INTERNAL://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093,SASL_SSL://0.0.0.0:9094
KAFKA_ADVERTISED_LISTENERS=INTERNAL://kafka:9092,SASL_SSL://kafka.lab.home:9094,CONTROLLER://kafka:9093
KAFKA_INTER_BROKER_LISTENER_NAME=INTERNAL
KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER
KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,INTERNAL:PLAINTEXT,SASL_SSL:SASL_SSL
KAFKA_CONTROLLER_QUORUM_VOTERS=1@kafka:9093
KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1
KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=1
KAFKA_TRANSACTION_STATE_LOG_MIN_ISR=1
KAFKA_AUTO_CREATE_TOPICS_ENABLE=true
KAFKA_LOG_DIRS=/var/lib/kafka/data
KAFKA_DATA_VOLUME=kafka-data
KAFKA_SSL_KEYSTORE_TYPE=PKCS12
KAFKA_SSL_KEYSTORE_FILENAME=kafka.keystore.p12
KAFKA_SSL_KEYSTORE_CREDENTIALS=kafka_ssl_keystore_creds
KAFKA_SSL_KEY_CREDENTIALS=kafka_ssl_key_creds
KAFKA_SSL_TRUSTSTORE_TYPE=PKCS12
KAFKA_SSL_TRUSTSTORE_FILENAME=kafka.truststore.p12
KAFKA_SSL_TRUSTSTORE_CREDENTIALS=kafka_ssl_truststore_creds
KAFKA_SSL_CLIENT_AUTH=none
KAFKA_SASL_ENABLED_MECHANISMS=SCRAM-SHA-256
KAFKA_OPTS=-Djava.security.auth.login.config=/etc/kafka/kafka_server_jaas.conf
KAFKA_HEALTHCHECK_PORT=9092
KAFKA_HEALTHCHECK_INTERVAL=15s
KAFKA_HEALTHCHECK_TIMEOUT=10s
KAFKA_HEALTHCHECK_RETRIES=5
KAFKA_HEALTHCHECK_START_PERIOD=30s
KAFKA_CERT_CN=kafka.lab.home
KAFKA_CERT_SAN_DOCKER=kafka
KAFKA_CERT_TTL=720h
STEP_CA_ROOT_CERT=../step-ca/root_ca.crt
STEP_CA_INTERMEDIATE_CERT=../step-ca/data/certs/intermediate_ca.crt
STEP_CA_INTERMEDIATE_KEY=../step-ca/data/secrets/intermediate_ca_key
STEP_CA_PASSWORD_FILE=../step-ca/data/secrets/password.txt
KAFKA_SSL_SECRET=changeit
KAFKA_SASL_USERNAME=app-prod
KAFKA_SASL_PASSWORD=trocar-esta-senha
```

### Exemplo do .env

```
KEYCLOAK_ADMIN=administrador
KEYCLOAK_ADMIN_PASSWORD=Adm123*

POSTGRES_DB=keycloak
POSTGRES_USER=keycloak
POSTGRES_PASSWORD=keycloak
```

---

# Dependências Principais

| Tecnologia                                   | Finalidade                              |
| -------------------------------------------- | --------------------------------------- |
| `spring-boot-starter-web`                    | API REST com Spring MVC                 |
| `spring-boot-starter-data-jpa`               | Persistência via Hibernate/JPA          |
| `spring-boot-starter-security`               | Framework de segurança                  |
| `spring-boot-starter-oauth2-resource-server` | Validação de JWT OAuth2                 |
| `spring-boot-starter-validation`             | Bean Validation (Jakarta Validation)    |
| `spring-boot-starter-actuator`               | Health checks e endpoints operacionais  |
| `spring-boot-starter-json`                   | Serialização Jackson 3                  |
| `spring-boot-starter-kafka`                  | Publicação e consumo de eventos Kafka   |
| `springdoc-openapi-starter-webmvc-ui`        | Documentação OpenAPI / Swagger UI       |
| `postgresql`                                 | Driver JDBC PostgreSQL                  |
| `flyway-core` + `flyway-database-postgresql` | Versionamento e migração de schema      |
| `bucket4j_jdk17-core`                        | Rate limiting via token bucket          |
| `logstash-logback-encoder`                   | Logs estruturados em JSON (Logstash)    |
| `spring-boot-testcontainers`                 | Integração Spring Boot + Testcontainers |
| `testcontainers-postgresql`                  | PostgreSQL em testes de integração      |
| `testcontainers-kafka`                       | Kafka em testes de integração           |
| `archunit-junit5`                            | Testes de regras arquiteturais          |
| `h2`                                         | Banco em memória para testes            |

---

## ToDo

### 1. Event-Driven Architecture

- [x] Criar tabela `outbox_evento` no banco de dados
- [x] Implementar `OutboxEventoEntity` e adaptador JPA
- [x] Implementar publicador agendado (Outbox Publisher)
- [x] Adicionar dependência Spring Kafka ao `pom.xml`
- [x] Implementar produtores Kafka para eventos de domínio (`transacoes.iniciadas`, `transacoes.concluidas`, `transacoes.falhas`, `transacoes.estornadas`)
- [x] Implementar lógica de negócio nos consumidores Kafka para PIX, TED e TEF (`PixTransacaoKafkaConsumidor`, `TedTransacaoKafkaConsumidor`, `TefTransacaoKafkaConsumidor`)
- [x] Implementar Dead Letter Queue com reprocessamento configurável
- [x] Implementar Domain Events: `TransacaoIniciadaEvento`, `TransacaoConcluidaEvento`, `TransacaoFalhouEvento`, `TransacaoEstornadaEvento`
- [x] Implementar idempotência de consumidor via tabela `evento_processado`

### 2. Segurança

- [x] Implementar `AttributeConverter` JPA para criptografia em repouso dos campos sensíveis (`CriptografiaConverter` — AES-256-GCM)
- [x] Implementar filtro de mascaramento de dados sensíveis nos logs (`observabilidade/mascaramento` + `LogMascaramentoConverter`)
- [ ] Configurar integração com HashiCorp Vault ou AWS Secrets Manager para gestão de segredos

### 3. Observabilidade

- [ ] Instrumentar serviços com métricas customizadas Micrometer (contadores, timers, gauges)
- [ ] Adicionar dependência `micrometer-tracing-bridge-otel`
- [ ] Configurar exportação de traces para Jaeger (dev) ou Grafana Tempo (prod)
- [x] Configurar logs estruturados em JSON (`logstash-logback-encoder` + `JsonMascaradoProvider` integrado ao Logback)
- [ ] Configurar dashboards Grafana com alertas de SLO

### 4. Resiliência

- [ ] Adicionar dependência Resilience4j ao `pom.xml`
- [ ] Implementar Circuit Breaker para SPI, STR, DICT e antifraude
- [ ] Implementar Retry com backoff exponencial e jitter por integração
- [ ] Implementar Bulkhead para isolamento de recursos por tipo de transação
- [ ] Implementar Timeout configurável por integração

### 5. Testes

- [x] Implementar testes unitários de segurança (`HmacServiceTest`, `HmacUtilsTest`, `CriptografiaConverterTest`)
- [x] Implementar testes de integração de criptografia (`CriptografiaConverterIntegrationTest`)
- [x] Implementar testes unitários de domínio (`TransacaoTest`, `TransacaoEventosTest`, `ContaTest`, `SaldoTest`, `LimiteTransacionalTest`, `ValorMonetarioTest`)
- [x] Implementar testes unitários de serviços de aplicação (`ProcessaTransacaoServiceTest`, `CriaTransacaoServiceTest`, `ConsultaTransacaoServiceTest`, `EstornoTransacaoServiceTest`, `LimiteServiceTest`, `SaldoServiceTest`)
- [x] Implementar testes unitários de estratégias (`PixTransacaoStrategyTest`, `TedTransacaoStrategyTest`, `TefTransacaoStrategyTest`, `StrategyResolverTest`)
- [x] Implementar testes de integração com Testcontainers (`IntegrationTestBase`, `ProcessaTransacaoIntegrationTest`, `EstornoIntegrationTest`, `SaldoIntegrationTest`, `OutboxEventoIntegrationTest`)
- [x] Implementar testes arquiteturais com ArchUnit (`ArquiteturaHexagonalTest`, `CamadaSegurancaTest`, `NamingConventionTest`)
- [ ] Configurar relatório de cobertura de código (JaCoCo)
