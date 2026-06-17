# ADR-007: HMAC-SHA256 para Integridade e Busca de Colunas Sensíveis

**Data:** 2026-06-17
**Status:** Aceito

## Contexto
Campos bancários como agência e número da conta precisam ficar protegidos em repouso, mas o sistema ainda precisa localizar registros por esses valores. Como a criptografia AES-GCM usa IV aleatório, o mesmo texto claro gera textos cifrados diferentes e não permite busca determinística direta.

## Decisão
Foi adotado HMAC-SHA256 como blind index para colunas sensíveis de conta. A aplicação mantém:

- `agencia`: valor criptografado.
- `agencia_hmac`: HMAC determinístico da agência.
- `numero_conta`: valor criptografado.
- `numero_conta_hmac`: HMAC determinístico do número da conta.

O cálculo é centralizado em `HmacService` e `HmacUtils`, com chave configurada por `app.hmac.chave` e variável `APP_HMAC_CHAVE`. Antes do cálculo, o valor é normalizado com `trim()` e conversão para maiúsculas.

As colunas HMAC são indexadas no PostgreSQL por migrations, como `idx_conta_numero_hmac_conta` e `idx_conta_agencia_hmac_conta`, permitindo consultas sem descriptografar os valores originais.

## Consequências
### Positivas
- Permite busca determinística por agência e número da conta sem expor texto claro.
- Complementa a criptografia AES-GCM, que permanece responsável pela confidencialidade.
- Reduz risco de vazamento direto de dados bancários no banco.
- Índices HMAC mantêm consultas eficientes.

### Negativas / Trade-offs
- HMAC não substitui criptografia; ele apenas permite comparação determinística.
- Rotação da chave HMAC exige recalcular blind indexes existentes.
- Valores de baixa cardinalidade podem exigir cuidado adicional contra análise de frequência.
