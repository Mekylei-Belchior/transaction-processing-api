# ADR-003: PostgreSQL 16, Flyway e Locking Otimista

**Data:** 2026-06-17
**Status:** Aceito

## Contexto

O processamento de transações financeiras requer persistência relacional, integridade transacional, constraints, índices, suporte a JSON para auditoria/outbox e controle de concorrência. Também era necessário versionar o schema de forma reprodutível entre ambientes, sem depender de geração automática pelo Hibernate.

## Decisão

Foi escolhido PostgreSQL 16 como banco relacional do projeto. O schema é versionado por Flyway em `src/main/resources/db/migration`, com `spring.flyway.locations=classpath:db/migration`.

As migrations atuais definem:

- `V1__tabelas_iniciais.sql`: tabelas `conta`, `transacao`, `saldo` e `limite`.
- `V2__auditoria.sql`: tabela `auditoria` e regras contra update/delete.
- `V3__mensageria.sql`: tabelas `outbox_evento` e `evento_processado`.
- `V4__limite_versao.sql`: coluna `versao` em `limite`.

Em produção, `spring.jpa.hibernate.ddl-auto` fica como `none`, e o Flyway controla a evolução do schema. O PostgreSQL é acessado via driver `org.postgresql:postgresql`.

O locking otimista foi adotado com colunas `versao` mapeadas por `@Version` em entidades versionadas, especialmente `transacao`, `saldo` e `limite`. A estratégia protege atualizações concorrentes de status, saldo e consumo de limite.

## Consequências

### Positivas

- O banco oferece transações ACID, constraints, índices e tipos como `uuid`, `numeric` e `jsonb`.
- Flyway torna a evolução do schema auditável e reproduzível.
- `ddl-auto=none` evita alterações implícitas e não revisadas em produção.
- Locking otimista reduz risco de perda de atualização em cenários concorrentes.

### Negativas / Trade-offs

- Alterações de modelo exigem migrations explícitas.
- Conflitos de versão precisam ser tratados pela aplicação ou por fluxo operacional.
- O uso de recursos específicos do PostgreSQL, como `jsonb`, reduz portabilidade para outros bancos.
