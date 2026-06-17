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
| PostgreSQL | 42.7.11 |
| Spring Kafka | Gerenciada pelo Spring Boot 4.0.6 |
| Flyway | 12.6.2 |
| OAuth2/JWT | Gerenciada pelo Spring Boot 4.0.6 |
| JaCoCo | 0.8.15 |
| ArchUnit | 1.4.2 |

## Como executar localmente

1. Crie o arquivo `.env` a partir das variáveis esperadas pelo ambiente local.
2. Suba as dependências com `docker-compose up -d`.
3. Execute a aplicação com `./mvnw spring-boot:run -Pdev`.

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

Nenhum documento encontrado em `docs/` neste checkout.

## Como contribuir

Abra uma branch pequena e focada para cada alteração.
Antes de enviar a contribuição, execute os testes e mantenha o README enxuto.
Use a pasta `docs/` para documentação detalhada.
