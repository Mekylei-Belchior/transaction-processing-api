# ADR-015: ArchUnit e Gates de Cobertura JaCoCo

**Data:** 2026-06-17
**Status:** Aceito

## Contexto

As decisões arquiteturais do projeto precisam sobreviver à evolução do código. Revisões manuais nem sempre detectam dependências indevidas entre domínio, aplicação e infraestrutura, vazamento de segurança para o domínio, ciclos entre pacotes ou perda de convenções. Além disso, era necessário definir um nível mínimo de cobertura para preservar confiança nos fluxos centrais.

## Decisão

Foram adotados testes de arquitetura com ArchUnit e gates de cobertura com JaCoCo.

As três classes principais de teste arquitetural são:

- `ArquiteturaHexagonalTest`: protege dependências da arquitetura hexagonal, entidades JPA isoladas, strategies, ausência de ciclos e value objects imutáveis.
- `CamadaSegurancaTest`: isola configuração de segurança e impede anotações de segurança no domínio.
- `NamingConventionTest`: protege sufixos como `JpaAdapter`, `JpaRepository`, `Entity`, `Service`, `Controller` e `Filter`.

O JaCoCo é configurado no `pom.xml` com fases `prepare-agent`, `report` e `check`. Os gates atuais são:

- Cobertura global mínima: `70%` de linhas e `60%` de branches.
- Pacotes de domínio de transação e conta: `85%` de linhas e `75%` de branches.
- Serviços de aplicação de transação e conta: `80%` de linhas e `70%` de branches.

Arquivos de configuração, entidades JPA, repositories Spring Data, DTOs, logging técnico, tracing e componentes condicionais de Kafka são excluídos dos gates por terem baixo valor de cobertura unitária ou forte acoplamento a framework.

## Consequências

### Positivas

- Decisões arquiteturais são verificadas automaticamente no build.
- Regressões estruturais aparecem como falhas de teste.
- Gates de cobertura priorizam domínio e aplicação, onde há maior valor de negócio.
- Convenções de nomenclatura tornam responsabilidades mais fáceis de localizar.

### Negativas / Trade-offs

- Regras ArchUnit precisam ser ajustadas quando a arquitetura evolui de forma legítima.
- Gates de cobertura podem exigir refatorações ou testes adicionais antes de mudanças serem aceitas.
- Exclusões do JaCoCo precisam ser revisadas para não esconder código relevante.
