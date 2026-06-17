# ADR-001: Arquitetura Hexagonal

**Data:** 2026-06-17
**Status:** Aceito

## Contexto
O `transaction-processing-api` processa transações financeiras e precisa manter regras de negócio independentes de detalhes técnicos como HTTP, Spring, JPA, Kafka, segurança e observabilidade. Sem uma fronteira arquitetural explícita, o domínio tenderia a depender de frameworks e adaptadores concretos, dificultando evolução, testes e troca de tecnologias.

Também era necessário criar uma forma objetiva de impedir regressões arquiteturais durante a evolução do projeto.

## Decisão
Foi adotada Arquitetura Hexagonal, também conhecida como Ports & Adapters. As dependências devem apontar para dentro:

- `dominio`: contém aggregates, value objects, enums e domain events, sem dependência de Spring, JPA, Kafka, HTTP ou configuração.
- `aplicacao`: contém casos de uso e portas, como `ProcessaTransacaoService`, `CriaTransacaoService`, `TransacaoRepository`, `ContaRepository`, `SaldoRepository`, `LimiteRepository` e `EventoPublicador`.
- `infraestrutura`: contém adaptadores e detalhes técnicos, como controllers, entidades JPA, repositories Spring Data, consumidores e produtores Kafka, configuração, segurança, observabilidade e integrações externas.

A implementação usa portas em pacotes como `com.mekylei.transactionprocessing.transacao.aplicacao.porta` e adaptadores em pacotes como `com.mekylei.transactionprocessing.infraestrutura.persistencia`. O domínio não conhece entidades como `TransacaoEntity`, repositories como `TransacaoJpaRepository` nem detalhes de transporte.

As regras são protegidas por ArchUnit em `ArquiteturaHexagonalTest`, `CamadaSegurancaTest` e `NamingConventionTest`, incluindo verificação de domínio puro, aplicação sem dependência de infraestrutura, entidades JPA isoladas, ausência de ciclos entre pacotes principais e convenções de nomenclatura.

## Consequências
### Positivas
- O domínio permanece testável e independente de frameworks.
- Casos de uso dependem de contratos, não de implementações concretas.
- Adaptadores técnicos podem evoluir sem contaminar regras de negócio.
- ArchUnit transforma decisões arquiteturais em verificações automatizadas.

### Negativas / Trade-offs
- A separação aumenta a quantidade de interfaces, adaptadores e conversões.
- Mudanças simples podem exigir alteração em mais de uma camada.
- O time precisa manter disciplina para não contornar portas em nome de velocidade.
