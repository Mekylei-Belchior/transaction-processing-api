# transaction-processing-api

[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![JaCoCo](https://img.shields.io/badge/coverage-JaCoCo-orange.svg)](https://www.jacoco.org/jacoco/)

API para processamento de transações financeiras com suporte a PIX, TED e TEF.
O projeto centraliza fluxos de autorização, liquidação e auditoria com consistência transacional.
Foi criado para apoiar operações financeiras com respostas previsíveis e rastreáveis.

## Objetivo de negócio

Processar transações PIX, TED e TEF com consistência transacional, garantindo integridade de saldos, rastreabilidade operacional e resposta confiável para clientes e sistemas integrados.

## Tecnologias principais

| Tecnologia | Versão |
| --- | --- |
| Java | 21 |
| Spring Boot | 4.0.6 |
| PostgreSQL | 16 |
| Kafka | Gerenciada pelo Spring Boot 4.0.6 |
| Flyway | 12.6.2 |
| OAuth2/JWT | Gerenciada pelo Spring Boot 4.0.6 |
| JaCoCo | 0.8.15 |
| ArchUnit | 1.4.2 |

## Como executar localmente

1. Crie o arquivo `.env` com as variáveis do ambiente local.
2. Suba as dependências com `docker-compose up -d`.
3. Execute a aplicação com `./mvnw spring-boot:run -Pdev`.

**Importante:** Para maiores detalhes de como executar localmente, consulte a documentação `Execução local`.

## Estrutura simplificada

| Pacote | Descrição |
| --- | --- |
| `auditoria` | Registro e consulta de eventos auditáveis. |
| `cliente` | Dados e operações relacionadas a clientes. |
| `conta` | Regras e casos de uso de contas. |
| `transacao` | Fluxo principal de processamento transacional. |
| `pix` | Contexto de pagamentos PIX. |
| `ted` | Contexto de transferências TED. |
| `tef` | Contexto de transferências TEF. |
| `compartilhado` | Tipos, exceções e utilitários comuns. |

## Documentação

- [Índice geral](docs/INDEX.md)
- [ADRs](docs/adr/README.md)
- [Endpoints](docs/api/endpoints.md)
- [Domain-Driven Design](docs/arquitetura/ddd.md)
- [Arquitetura Hexagonal](docs/arquitetura/hexagonal.md)
- [Visão geral](docs/arquitetura/visao-geral.md)
- [Modelo de dados](docs/banco-de-dados/modelo-dados.md)
- [Execução local](docs/desenvolvimento/execucao-local.md)
- [Kafka e Outbox](docs/mensageria/kafka-outbox.md)
- [Métricas](docs/observabilidade/metricas.md)
- [Rastreamento](docs/observabilidade/rastreamento.md)
- [Variáveis de ambiente](docs/operacao/variaveis-ambiente.md)
- [Roadmap](docs/roadmap/roadmap.md)
- [Autenticação e autorização](docs/seguranca/autenticacao-autorizacao.md)

## Como contribuir

Abra uma branch pequena e focada para cada alteração.
Antes de enviar a contribuição, execute os testes e mantenha o README enxuto.
Use a pasta `docs/` para documentação detalhada.
