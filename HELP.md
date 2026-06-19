# FAQ — Dúvidas Frequentes de Desenvolvimento

> Para a documentação completa, consulte o [índice de documentação](docs/INDEX.md).

## Onde começo?

Siga o [guia de execução local](docs/desenvolvimento/execucao-local.md) para subir a aplicação em menos de 30 minutos.

## Como obtenho um token para testar a API?

Consulte [Como obter um token de acesso](docs/api/endpoints.md#como-obter-um-token-de-acesso).

## Quais variáveis de ambiente preciso configurar?

Copie `.env.example` para `.env` e preencha os valores necessários. Consulte os detalhes em [Variáveis de ambiente](docs/operacao/variaveis-ambiente.md).

## Como executo os testes?

```bash
./mvnw test    # testes unitários
./mvnw verify  # testes + integração + arquitetura + cobertura
```

## Por que a aplicação não inicia?

Consulte o [guia de troubleshooting](docs/operacao/troubleshooting.md) para fazer o diagnóstico por categoria.

## O que é o Outbox Pattern usado aqui?

Consulte [Kafka e Outbox](docs/mensageria/kafka-outbox.md).

## Como contribuo?

Consulte o [guia de contribuição](CONTRIBUTING.md).
