# Endpoints da API

Documentação dos endpoints REST expostos por `TransacaoController`, no pacote `com.mekylei.transactionprocessing.transacao.controle`.

Base path: `/api/v1/transacoes`

Autenticação: Bearer JWT via OAuth2 Resource Server.

## Visão geral

| Método | Path | Descrição | Roles |
| --- | --- | --- | --- |
| `POST` | `/api/v1/transacoes/pix` | Processa uma transação PIX. Usa `contaDestino` como chave PIX. | `CLIENTE`, `OPERADOR`, `GERENTE`, `ADMIN`, `SERVICO_INTERNO` |
| `POST` | `/api/v1/transacoes/ted` | Processa uma TED. Disponível apenas em horário bancário, das 06h às 17h BRT. | `CLIENTE`, `OPERADOR`, `GERENTE`, `ADMIN`, `SERVICO_INTERNO` |
| `POST` | `/api/v1/transacoes/tef` | Processa uma TEF entre contas do mesmo banco. Requer autorização antifraude. | `CLIENTE`, `OPERADOR`, `GERENTE`, `ADMIN`, `SERVICO_INTERNO` |
| `GET` | `/api/v1/transacoes/{id}` | Consulta o estado atual de uma transação por ID. | `CLIENTE`, `OPERADOR`, `GERENTE`, `ADMIN`, `SERVICO_INTERNO` |
| `POST` | `/api/v1/transacoes/{id}/estorno` | Estorna uma transação concluída. | `GERENTE`, `ADMIN` |

## Como obter um token de acesso

Todos os endpoints requerem um token JWT obtido via Keycloak.

Para obter um token usando o Keycloak do homelab, informe o usuário e a senha do usuário de teste:

```bash
TOKEN=$(curl -s \
  -X POST "https://keycloak.lab.home/realms/bancario/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "grant_type=password" \
  --data-urlencode "client_id=transaction-api-client" \
  --data-urlencode "username=<usuario-de-teste>" \
  --data-urlencode "password=<senha-do-usuario>" \
  | jq -r '.access_token')
```

Para obter um token usando o Keycloak local:

```bash
TOKEN=$(curl -s \
  -X POST "http://localhost:8180/realms/bancario/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "grant_type=password" \
  --data-urlencode "client_id=transaction-api-client" \
  --data-urlencode "username=<usuario-de-teste>" \
  --data-urlencode "password=<senha-do-usuario>" \
  | jq -r '.access_token')
```

Use o token salvo em `$TOKEN` no header `Authorization`:

```bash
curl -X GET "http://localhost:8080/api/v1/transacoes/11111111-1111-1111-1111-111111111111" \
  -H "Authorization: Bearer $TOKEN"
```

| Operação | Role mínima |
| --- | --- |
| Iniciar PIX/TED/TEF | `CLIENTE` ou `OPERADOR` |
| Consultar transação | `CLIENTE` (próprias) ou `OPERADOR` |
| Solicitar estorno | `GERENTE` |
| Ler métricas | role `METRICAS.LEITURA` |

> Para configurar o Keycloak local, consulte [Execução sem acesso ao homelab](../desenvolvimento/execucao-local.md#execução-sem-acesso-ao-homelab).

## Headers

| Header | Tipo | Obrigatoriedade | Observação |
| --- | --- | --- | --- |
| `Authorization` | `Bearer <token JWT>` | Obrigatório em todos os endpoints | O token deve conter uma role autorizada para a operação. |
| `X-Idempotency-Key` | `UUID` | Obrigatório em `POST /pix`, `POST /ted` e `POST /tef` | Usado para evitar processamento duplicado da mesma requisição de transação. |
| `X-Correlation-Id` | `UUID` | Recomendado em todos os endpoints | Usado para rastreamento. Quando ausente, a aplicação gera um UUID internamente; quando inválido, o valor é ignorado. |

## Modelo de erro

As respostas de erro seguem `ProblemDetail`. Dependendo da origem do erro, o `Content-Type` pode ser `application/problem+json` ou `application/json`.

Exemplo:

```json
{
  "type": "about:blank",
  "title": "Violação de Regra de Negócio",
  "status": 422,
  "detail": "TED fora do horário permitido",
  "codigoErro": "TED_FORA_DO_HORARIO",
  "horario": "17/06/2026 14:30:00"
}
```

## `POST /api/v1/transacoes/pix`

> Para as regras de negócio detalhadas, consulte [Fluxos de Negócio](../desenvolvimento/fluxos-negocio.md#pix).

Processa uma transação PIX. O tipo da transação é definido pelo endpoint como `PIX`; não há campo `tipo` no body.

### Headers obrigatórios

| Header | Valor |
| --- | --- |
| `Authorization` | `Bearer <token JWT>` |
| `X-Idempotency-Key` | UUID, por exemplo `44444444-4444-4444-4444-444444444444` |

Header de rastreamento recomendado:

| Header | Valor |
| --- | --- |
| `X-Correlation-Id` | UUID, por exemplo `33333333-3333-3333-3333-333333333333` |

### Body da requisição

DTO: `TransacaoRequisicao`

| Campo | Tipo | Validações | Descrição |
| --- | --- | --- | --- |
| `valor` | decimal | Obrigatório; mínimo `0.01` | Valor da transação em BRL. |
| `idContaOrigem` | UUID | Obrigatório | UUID da conta de origem cadastrada no sistema. |
| `contaDestino` | string | Obrigatório; não pode ser vazio | Conta ou chave de destino. Para PIX, representa a chave PIX. |

Exemplo:

```json
{
  "valor": 150.00,
  "idContaOrigem": "22222222-2222-2222-2222-222222222222",
  "contaDestino": "cliente@email.com"
}
```

### Respostas

#### `201 Created`

DTO: `TransacaoResposta`

```json
{
  "id": "11111111-1111-1111-1111-111111111111",
  "idContaOrigem": "22222222-2222-2222-2222-222222222222",
  "valor": 150.00,
  "tipo": "PIX",
  "status": "COMPLETADA",
  "idCorrelacao": "33333333-3333-3333-3333-333333333333",
  "idIdempotencia": "44444444-4444-4444-4444-444444444444",
  "criadoEm": "2026-06-03T12:00:00Z"
}
```

#### Erros

Possíveis status: `400`, `401`, `403`, `409`, `422`, `429` e `500`.

Exemplo de body inválido:

```json
{
  "type": "about:blank",
  "title": "Dados Inválidos",
  "status": 400,
  "Campos": [
    "valor: Valor deve ser maior que zero",
    "idContaOrigem: Conta de origem é obrigatória",
    "contaDestino: Conta de destino é obrigatória"
  ],
  "horario": "17/06/2026 14:30:00"
}
```

Exemplo de saldo insuficiente:

```json
{
  "type": "about:blank",
  "title": "Saldo insuficiente",
  "status": 422,
  "detail": "Saldo insuficiente para a operação.",
  "codigoErro": "SALDO_INSUFICIENTE",
  "horario": "17/06/2026 14:30:00"
}
```

## `POST /api/v1/transacoes/ted`

> Para as regras de negócio detalhadas, consulte [Fluxos de Negócio](../desenvolvimento/fluxos-negocio.md#ted).

Processa uma TED. O tipo da transação é definido pelo endpoint como `TED`; não há campo `tipo` no body. A regra de negócio informa disponibilidade apenas em horário bancário, das 06h às 17h BRT.

### Headers obrigatórios

| Header | Valor |
| --- | --- |
| `Authorization` | `Bearer <token JWT>` |
| `X-Idempotency-Key` | UUID, por exemplo `44444444-4444-4444-4444-444444444444` |

Header de rastreamento recomendado:

| Header | Valor |
| --- | --- |
| `X-Correlation-Id` | UUID, por exemplo `33333333-3333-3333-3333-333333333333` |

### Body da requisição

DTO: `TransacaoRequisicao`

| Campo | Tipo | Validações | Descrição |
| --- | --- | --- | --- |
| `valor` | decimal | Obrigatório; mínimo `0.01` | Valor da transação em BRL. |
| `idContaOrigem` | UUID | Obrigatório | UUID da conta de origem cadastrada no sistema. |
| `contaDestino` | string | Obrigatório; não pode ser vazio | Conta de destino da TED. |

Exemplo:

```json
{
  "valor": 150.00,
  "idContaOrigem": "22222222-2222-2222-2222-222222222222",
  "contaDestino": "12345-6"
}
```

### Respostas

#### `201 Created`

DTO: `TransacaoResposta`

```json
{
  "id": "11111111-1111-1111-1111-111111111111",
  "idContaOrigem": "22222222-2222-2222-2222-222222222222",
  "valor": 150.00,
  "tipo": "TED",
  "status": "COMPLETADA",
  "idCorrelacao": "33333333-3333-3333-3333-333333333333",
  "idIdempotencia": "44444444-4444-4444-4444-444444444444",
  "criadoEm": "2026-06-03T12:00:00Z"
}
```

#### Erros

Possíveis status: `400`, `401`, `403`, `409`, `422`, `429` e `500`.

Exemplo de TED fora do horário:

```json
{
  "type": "about:blank",
  "title": "Violação de Regra de Negócio",
  "status": 422,
  "detail": "TED fora do horário permitido",
  "codigoErro": "TED_FORA_DO_HORARIO",
  "horario": "17/06/2026 14:30:00"
}
```

## `POST /api/v1/transacoes/tef`

> Para as regras de negócio detalhadas, consulte [Fluxos de Negócio](../desenvolvimento/fluxos-negocio.md#tef).

Processa uma TEF entre contas do mesmo banco. O tipo da transação é definido pelo endpoint como `TEF`; não há campo `tipo` no body.

### Headers obrigatórios

| Header | Valor |
| --- | --- |
| `Authorization` | `Bearer <token JWT>` |
| `X-Idempotency-Key` | UUID, por exemplo `44444444-4444-4444-4444-444444444444` |

Header de rastreamento recomendado:

| Header | Valor |
| --- | --- |
| `X-Correlation-Id` | UUID, por exemplo `33333333-3333-3333-3333-333333333333` |

### Body da requisição

DTO: `TransacaoRequisicao`

| Campo | Tipo | Validações | Descrição |
| --- | --- | --- | --- |
| `valor` | decimal | Obrigatório; mínimo `0.01` | Valor da transação em BRL. |
| `idContaOrigem` | UUID | Obrigatório | UUID da conta de origem cadastrada no sistema. |
| `contaDestino` | string | Obrigatório; não pode ser vazio | Conta de destino da TEF. |

Exemplo:

```json
{
  "valor": 150.00,
  "idContaOrigem": "22222222-2222-2222-2222-222222222222",
  "contaDestino": "98765-4"
}
```

### Respostas

#### `201 Created`

DTO: `TransacaoResposta`

```json
{
  "id": "11111111-1111-1111-1111-111111111111",
  "idContaOrigem": "22222222-2222-2222-2222-222222222222",
  "valor": 150.00,
  "tipo": "TEF",
  "status": "COMPLETADA",
  "idCorrelacao": "33333333-3333-3333-3333-333333333333",
  "idIdempotencia": "44444444-4444-4444-4444-444444444444",
  "criadoEm": "2026-06-03T12:00:00Z"
}
```

#### Erros

Possíveis status: `400`, `401`, `403`, `409`, `422`, `429` e `500`.

Exemplo de regra de negócio:

```json
{
  "type": "about:blank",
  "title": "Violação de Regra de Negócio",
  "status": 422,
  "detail": "Regra violada",
  "codigoErro": "REGRA_NEGOCIO",
  "horario": "17/06/2026 14:30:00"
}
```

## `GET /api/v1/transacoes/{id}`

Consulta o estado atual de uma transação pelo identificador.

### Path parameters

| Parâmetro | Tipo | Validações | Descrição |
| --- | --- | --- | --- |
| `id` | UUID | Obrigatório | Identificador da transação. |

### Headers obrigatórios

| Header | Valor |
| --- | --- |
| `Authorization` | `Bearer <token JWT>` |

Header de rastreamento recomendado:

| Header | Valor |
| --- | --- |
| `X-Correlation-Id` | UUID, por exemplo `33333333-3333-3333-3333-333333333333` |

### Body da requisição

Não há body.

### Respostas

#### `200 OK`

DTO: `TransacaoResposta`

```json
{
  "id": "11111111-1111-1111-1111-111111111111",
  "idContaOrigem": "22222222-2222-2222-2222-222222222222",
  "valor": 150.00,
  "tipo": "PIX",
  "status": "COMPLETADA",
  "idCorrelacao": "33333333-3333-3333-3333-333333333333",
  "idIdempotencia": "44444444-4444-4444-4444-444444444444",
  "criadoEm": "2026-06-03T12:00:00Z"
}
```

#### Erros

Possíveis status: `401`, `403`, `404`, `429` e `500`.

Exemplo de transação não encontrada:

```json
{
  "type": "about:blank",
  "title": "Recurso não encontrado",
  "status": 404,
  "detail": "Transação não encontrada para o id: 11111111-1111-1111-1111-111111111111",
  "codigoErro": "TRANSACAO_NAO_ENCONTRADA",
  "horario": "17/06/2026 14:30:00"
}
```

## `POST /api/v1/transacoes/{id}/estorno`

> Para as regras de negócio completas do estorno, consulte [Fluxos de Negócio](../desenvolvimento/fluxos-negocio.md#estorno).

Estorna uma transação concluída. O estorno só é aceito para transações com status `COMPLETADA`; a transação passa para `ESTORNADA`, o valor é creditado de volta na conta de origem e um evento de estorno é publicado.

### Pré-condições

- A transação deve existir.
- A transação deve estar no status `COMPLETADA`.
- O solicitante deve estar autenticado com, no mínimo, a role `GERENTE`.

### Path parameters

| Parâmetro | Tipo | Validações | Descrição |
| --- | --- | --- | --- |
| `id` | UUID | Obrigatório | UUID da transação a estornar. |

### Headers obrigatórios

| Header | Valor |
| --- | --- |
| `Authorization` | `Bearer <token JWT>` com, no mínimo, a role `GERENTE` |
| `X-Correlation-Id` | UUID, por exemplo `33333333-3333-3333-3333-333333333333` |

### Body da requisição

Não há body.

### Respostas

#### `200 OK`

DTO: `TransacaoResposta`

```json
{
  "id": "11111111-1111-1111-1111-111111111111",
  "idContaOrigem": "22222222-2222-2222-2222-222222222222",
  "valor": 150.00,
  "tipo": "PIX",
  "status": "ESTORNADA",
  "idCorrelacao": "33333333-3333-3333-3333-333333333333",
  "idIdempotencia": "44444444-4444-4444-4444-444444444444",
  "criadoEm": "2026-06-03T12:00:00Z"
}
```

#### Erros

Possíveis status: `401`, `403`, `404`, `409`, `422`, `429` e `500`.

##### `404 Not Found` — `TRANSACAO_NAO_ENCONTRADA`

```json
{
  "type": "about:blank",
  "title": "Recurso não encontrado",
  "status": 404,
  "detail": "Transação não encontrada para o id: 11111111-1111-1111-1111-111111111111",
  "codigoErro": "TRANSACAO_NAO_ENCONTRADA",
  "horario": "17/06/2026 14:30:00"
}
```

##### `409 Conflict` — `TRANSACAO_JA_ESTORNADA`

```json
{
  "type": "about:blank",
  "title": "Conflito de Dados",
  "status": 409,
  "detail": "A transação 11111111-1111-1111-1111-111111111111 já foi estornada",
  "codigoErro": "TRANSACAO_JA_ESTORNADA",
  "horario": "17/06/2026 14:30:00"
}
```

##### `422 Unprocessable Entity` — `TRANSACAO_NAO_ESTORNAVEL`

```json
{
  "type": "about:blank",
  "title": "Violação de Regra de Negócio",
  "status": 422,
  "detail": "Apenas transações com status COMPLETADA podem ser estornadas. Status atual: PENDENTE",
  "codigoErro": "TRANSACAO_NAO_ESTORNAVEL",
  "horario": "17/06/2026 14:30:00"
}
```

### Pós-condições

- O valor da transação é creditado no saldo da conta de origem.
- O evento `TransacaoEstornadaEvento` é publicado na outbox.

## Códigos de erro padrão

| HTTP | Título | `codigoErro` | Quando ocorre |
| --- | --- | --- | --- |
| `400` | `Dados Inválidos` | Não se aplica | Validações de DTO falham, por exemplo `valor` nulo, `valor` menor que `0.01`, `idContaOrigem` nulo ou `contaDestino` vazio. |
| `400` | `Cabeçalho obrigatório ausente` | `CABECALHO_AUSENTE` | Header obrigatório ausente, como `X-Idempotency-Key` nos endpoints de processamento. |
| `401` | `Não autenticado` | Não se aplica | Token ausente, inválido ou expirado. |
| `403` | `Acesso negado` | `ACESSO_NEGADO` | Token autenticado, mas sem role suficiente para a operação. |
| `404` | `Recurso não encontrado` | Código definido pela exceção, por exemplo `TRANSACAO_NAO_ENCONTRADA` | Transação não encontrada pelo ID informado. |
| `409` | `Conflito de Dados` | `CONFLITO_DADOS` | Conflito de integridade de dados, incluindo chave de idempotência já registrada em cenário conflitante. |
| `409` | `Conflito de Concorrência` | `CONFLITO_CONCORRENCIA` | Atualização concorrente detectada por locking otimista. |
| `422` | `Violação de Regra de Negócio` | Código definido pela exceção, por exemplo `TED_FORA_DO_HORARIO`, `TRANSACAO_NAO_ESTORNAVEL`, `CONTA_INVALIDA` | Regra de negócio violada. |
| `422` | `Saldo insuficiente` | `SALDO_INSUFICIENTE` | Saldo disponível menor que o valor solicitado. |
| `429` | `Limite excedido` | `LIMITE_EXCEDIDO` | Limite de requisições excedido. A resposta inclui `Retry-After: 60`. |
| `500` | `Erro Interno` | `ERRO_INTERNO_SERVIDOR` | Erro inesperado não tratado por handlers específicos. |

Exemplos adicionais:

```json
{
  "type": "about:blank",
  "title": "Não autenticado",
  "status": 401,
  "detail": "Token ausente, inválido ou expirado.",
  "horario": "17/06/2026 14:30:00"
}
```

```json
{
  "type": "about:blank",
  "title": "Acesso negado",
  "status": 403,
  "detail": "Sem permissão para executar esta operação.",
  "codigoErro": "ACESSO_NEGADO",
  "horario": "17/06/2026 14:30:00"
}
```

```json
{
  "type": "about:blank",
  "title": "Limite excedido",
  "status": 429,
  "detail": "Limite de requisições excedido. Tente novamente em instantes.",
  "codigoErro": "LIMITE_EXCEDIDO",
  "horario": "17/06/2026 14:30:00"
}
```

## Swagger UI

No perfil `dev`, a Swagger UI fica disponível em `/swagger-ui.html`.
