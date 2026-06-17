# Arquitetura Hexagonal

O projeto segue Arquitetura Hexagonal, também conhecida como Ports & Adapters, para manter o domínio e os casos de uso independentes de detalhes técnicos como HTTP, JPA, Kafka, segurança e observabilidade.

A regra central é: dependências apontam para dentro. A camada interna nunca importa a camada externa. O domínio não conhece aplicação, infraestrutura ou controllers; a aplicação orquestra casos de uso usando portas; a infraestrutura implementa essas portas e se conecta a frameworks e recursos externos.

## Camadas

### `dominio`

A camada `dominio` contém o modelo de negócio e suas invariantes. Ela deve ser a parte mais estável do sistema.

Pacotes reais:

- `com.mekylei.transactionprocessing.transacao.dominio`
- `com.mekylei.transactionprocessing.transacao.dominio.evento`
- `com.mekylei.transactionprocessing.conta.dominio`
- `com.mekylei.transactionprocessing.auditoria.dominio`
- `com.mekylei.transactionprocessing.compartilhado.dominio`
- `com.mekylei.transactionprocessing.compartilhado.evento`

Responsabilidades:

- Representar aggregates, value objects, enums e domain events.
- Proteger invariantes de negócio, como saldo não negativo e limites transacionais.
- Permanecer independente de Spring, JPA, Kafka, HTTP e configuração.

### `aplicacao`

A camada `aplicacao` contém os casos de uso e as portas. Ela coordena o fluxo de negócio chamando domínio e interfaces, sem depender de adaptadores concretos.

Pacotes reais:

- `com.mekylei.transactionprocessing.transacao.aplicacao.servico`
- `com.mekylei.transactionprocessing.transacao.aplicacao.porta`
- `com.mekylei.transactionprocessing.conta.aplicacao.servico`
- `com.mekylei.transactionprocessing.conta.aplicacao.porta`
- `com.mekylei.transactionprocessing.auditoria.aplicacao`
- `com.mekylei.transactionprocessing.auditoria.porta`
- `com.mekylei.transactionprocessing.mensageria.aplicacao`
- `com.mekylei.transactionprocessing.mensageria.aplicacao.porta`
- `com.mekylei.transactionprocessing.mensageria.outbox`

Responsabilidades:

- Orquestrar casos de uso como `ProcessaTransacaoService`, `CriaTransacaoService`, `ConsultaTransacaoService`, `EstornoTransacaoService`, `SaldoService` e `LimiteService`.
- Declarar portas de saída, como `TransacaoRepository`, `ContaRepository`, `SaldoRepository`, `LimiteRepository`, `EventoPublicador` e `EventoProcessadoRepository`.
- Aplicar transações e regras de fluxo sem conhecer JPA, Kafka ou implementação externa.

### `infraestrutura`

A camada `infraestrutura` contém adaptadores e detalhes técnicos. Ela depende das portas internas para implementar persistência, publicação, consumo, configuração e integração com frameworks.

Pacotes reais:

- `com.mekylei.transactionprocessing.transacao.controle`
- `com.mekylei.transactionprocessing.conta.controle`
- `com.mekylei.transactionprocessing.infraestrutura.persistencia`
- `com.mekylei.transactionprocessing.infraestrutura.repositorio`
- `com.mekylei.transactionprocessing.infraestrutura.entidade`
- `com.mekylei.transactionprocessing.mensageria.consumidor`
- `com.mekylei.transactionprocessing.mensageria.produtor`
- `com.mekylei.transactionprocessing.mensageria.evento`
- `com.mekylei.transactionprocessing.integracao.antifraude`
- `com.mekylei.transactionprocessing.configuracao`
- `com.mekylei.transactionprocessing.observabilidade`
- `com.mekylei.transactionprocessing.compartilhado.adaptador`

Responsabilidades:

- Receber requisições HTTP em controllers.
- Implementar portas usando JPA, Kafka, stubs de integração ou outros recursos externos.
- Isolar entidades JPA e repositórios Spring Data.
- Configurar segurança, Kafka, OpenAPI, filtros, métricas, logs e rastreamento.

## Diagrama de camadas

```text
┌──────────────────────────────────────────────────────────────────────────────┐
│ Infraestrutura / Adaptadores                                                 │
│                                                                              │
│ transacao.controle              conta.controle                               │
│ infraestrutura.persistencia     infraestrutura.repositorio                   │
│ infraestrutura.entidade         mensageria.consumidor/produtor/evento        │
│ integracao.antifraude           configuracao                                 │
│ observabilidade                 compartilhado.adaptador                      │
└───────────────────────────────┬──────────────────────────────────────────────┘
                                │ depende de portas e casos de uso
                                ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│ Aplicação / Portas                                                           │
│                                                                              │
│ transacao.aplicacao.servico     transacao.aplicacao.porta                    │
│ conta.aplicacao.servico         conta.aplicacao.porta                        │
│ auditoria.aplicacao             auditoria.porta                              │
│ mensageria.aplicacao            mensageria.aplicacao.porta                   │
│ mensageria.outbox                                                            │
└───────────────────────────────┬──────────────────────────────────────────────┘
                                │ depende de domínio e portas internas
                                ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│ Domínio                                                                      │
│                                                                              │
│ transacao.dominio             transacao.dominio.evento                       │
│ conta.dominio                 auditoria.dominio                              │
│ compartilhado.dominio         compartilhado.evento                           │
└──────────────────────────────────────────────────────────────────────────────┘
```

## Regras de dependência

As regras arquiteturais são protegidas por testes com ArchUnit em `ArquiteturaHexagonalTest`.

- Classes em `..dominio..` não devem depender de `..aplicacao..`, `..infraestrutura..` ou `..controle..`.
- Classes em `..transacao.dominio..`, `..conta.dominio..` e `..compartilhado.dominio..` não devem depender de `..infraestrutura..`, `..configuracao..` ou `..controle..`.
- Serviços em `..transacao.aplicacao.servico..` e `..conta.aplicacao.servico..` não devem depender de `..infraestrutura.persistencia..` ou `..infraestrutura.repositorio..`.
- Controllers devem atuar como borda HTTP fina e chamar serviços de aplicação.
- Entidades anotadas com `@Entity` devem ficar em `..infraestrutura.entidade..`.
- Implementações de portas com sufixo `Repository` ou `Gateway` devem ficar em `..infraestrutura..` ou `..integracao..`.
- Pacotes principais não devem formar ciclos de dependência.

## Exemplo concreto: porta e adaptador JPA

A porta de persistência da transação fica na aplicação:

```java
package com.mekylei.transactionprocessing.transacao.aplicacao.porta.repositorio;

public interface TransacaoRepository {
    Optional<Transacao> findById(UUID id);
    Optional<Transacao> findByIdCorrelacao(UUID idCorrelacao);
    Optional<Transacao> findByIdIdempotencia(UUID idIdempotencia);
    Transacao save(Transacao transacao);
    Transacao update(Transacao transacao);
}
```

O caso de uso `ProcessaTransacaoService` depende dessa porta, não de JPA:

```text
ProcessaTransacaoService -> TransacaoRepository
```

O adaptador concreto fica na infraestrutura e implementa a porta:

```text
TransacaoJpaAdapter -> implements TransacaoRepository
TransacaoJpaAdapter -> TransacaoJpaRepository
TransacaoJpaAdapter -> TransacaoEntity
```

Assim, o fluxo de dependência permanece correto:

```text
transacao.aplicacao.servico.ProcessaTransacaoService
        -> transacao.aplicacao.porta.repositorio.TransacaoRepository
        <- infraestrutura.persistencia.TransacaoJpaAdapter
        -> infraestrutura.repositorio.TransacaoJpaRepository
        -> infraestrutura.entidade.TransacaoEntity
```

O domínio `Transacao` não conhece `TransacaoEntity`, `TransacaoJpaRepository` ou Spring Data JPA. A conversão entre modelo de domínio e entidade de persistência acontece no adaptador `TransacaoJpaAdapter`.
