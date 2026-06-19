# Como contribuir

Obrigado pelo interesse em contribuir com a **transaction-processing-api**. Este projeto utiliza Java 21, Spring Boot 4.0.6 e Maven Wrapper, seguindo Arquitetura Hexagonal e Domain-Driven Design (DDD).

Os principais bounded contexts são `transacao`, `conta`, `compartilhado`, `auditoria`, `mensageria` e `observabilidade`. Preserve seus limites e responsabilidades ao propor mudanças.

## Fluxo resumido de contribuição

Novo no projeto? Siga estes passos para fazer sua primeira contribuição:

1. **Clone o repositório** e configure o ambiente local conforme [Execução Local](docs/desenvolvimento/execucao-local.md).
2. **Crie uma branch** a partir de `main`: `git checkout -b feature/minha-funcionalidade`.
3. **Implemente** sua alteração seguindo as convenções de código.
4. **Execute a verificação completa** antes de commitar:
   ```bash
   ./mvnw verify
   ```
   Todos os testes (unidade, integração, arquitetura) e gates de cobertura devem passar.
5. **Faça o commit** usando [Conventional Commits](https://www.conventionalcommits.org/):
   ```text
   feat(transacao): adiciona validação de horário para TED
   ```
6. **Abra o Pull Request** descrevendo: o problema que resolve, a mudança feita e como testar.
7. Aguarde o **code review** — veja os critérios em [Code Review](#code-review).

## Pré-requisitos

Antes de começar, tenha instalado:

- Java 21;
- Docker e Docker Compose, usados pelo Testcontainers para disponibilizar o PostgreSQL nos testes de integração;
- Git.

Use o Maven Wrapper incluído no repositório. Antes de abrir um Pull Request, execute:

```bash
./mvnw verify
```

O comando deve finalizar com sucesso.

## Padrões de branch

Crie branches com nomes curtos, descritivos e em kebab-case:

| Prefixo | Uso | Exemplo |
| --- | --- | --- |
| `feature/<descricao-curta>` | Nova funcionalidade | `feature/validacao-limite-tef` |
| `fix/<descricao-curta>` | Correção de bug | `fix/reprocessamento-outbox` |
| `chore/<descricao-curta>` | Manutenção, dependências ou refatoração sem mudança funcional | `chore/atualiza-dependencias` |
| `docs/<descricao-curta>` | Alterações somente em documentação | `docs/execucao-local` |

## Convenção de commits

Adote [Conventional Commits](https://www.conventionalcommits.org/pt-br/) no formato:

```text
<tipo>(<escopo>): <descrição>
```

Os tipos válidos são `feat`, `fix`, `docs`, `chore`, `refactor`, `test`, `style`, `perf` e `ci`.

Use como escopo o nome do bounded context ou módulo afetado, como `transacao`, `conta` ou `outbox`.

Exemplos:

```text
feat(transacao): adiciona validação de limite TEF
fix(outbox): corrige reprocessamento de eventos falhos
docs(readme): atualiza quick start para ambiente sem homelab
test(conta): adiciona teste unitário de SaldoService
```

## Abertura de issues

Antes de abrir uma issue, verifique se já existe outra que descreva o mesmo problema ou proposta.

Inclua na descrição:

- comportamento atual;
- comportamento esperado;
- passos para reproduzir.

Em relatos de bug, informe também:

- versão do Java;
- perfil utilizado;
- trecho relevante do log, removendo credenciais e outros dados sensíveis.

## Testes, arquitetura e cobertura

O projeto utiliza:

- JUnit 5 e Mockito para testes unitários;
- Testcontainers e `@SpringBootTest` para testes de integração;
- ArchUnit para validar os pacotes da Arquitetura Hexagonal, as convenções de nomenclatura e as regras da camada de segurança.

O JaCoCo aplica os seguintes gates de cobertura:

| Escopo | Linhas | Branches |
| --- | ---: | ---: |
| Global | 70% | 60% |
| Domínio | 85% | 75% |
| Serviços de aplicação | 80% | 70% |

Verifique os gates com:

```bash
./mvnw verify
```

Após a execução, o relatório detalhado estará disponível em `target/site/jacoco/index.html`.

## Checklist de Pull Request

Abra o PR somente quando todos os itens aplicáveis estiverem atendidos:

- [ ] `./mvnw verify` passa (testes + cobertura).
- [ ] Não há violações do ArchUnit (`ArquiteturaHexagonalTest`, `CamadaSegurancaTest`, `NamingConventionTest`).
- [ ] A cobertura não caiu abaixo dos gates do JaCoCo.
- [ ] A documentação foi atualizada caso o comportamento visível tenha mudado.
- [ ] Novas variáveis de ambiente foram adicionadas a `docs/operacao/variaveis-ambiente.md` e `.env.example`.
- [ ] A descrição do PR explica o contexto da mudança, o que foi feito e como testar.

Mantenha o PR focado em uma mudança coesa. Caso haja alterações independentes, prefira separá-las em PRs distintos para facilitar a revisão.

## Code Review

Durante a revisão, será verificada a conformidade com a Arquitetura Hexagonal, DDD e as convenções de nomenclatura do projeto.

PRs que removam testes ou reduzam a cobertura deverão ser corrigidos antes do merge. Toda mudança na camada de domínio deve incluir os testes unitários correspondentes.

## Onde pedir ajuda

- Consulte [`docs/INDEX.md`](docs/INDEX.md) para navegar pela documentação.
- Para decisões de arquitetura, leia os ADRs em [`docs/adr/`](docs/adr/).
- Para executar o projeto localmente, consulte [`docs/desenvolvimento/execucao-local.md`](docs/desenvolvimento/execucao-local.md).
