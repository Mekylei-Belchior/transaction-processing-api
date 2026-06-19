# ADR-008: Criptografia AES-256-GCM via JPA AttributeConverter

**Data:** 2026-06-17
**Status:** Aceito

## Contexto

Dados bancários sensíveis não devem ser armazenados em texto claro. Ao mesmo tempo, a aplicação precisa manipular esses valores no domínio e persisti-los com baixo acoplamento entre regras de negócio e mecanismo criptográfico.

## Decisão

Foi adotada criptografia AES-256-GCM para dados sensíveis em repouso. A implementação fica em `CriptografiaConverter`, usando `AES/GCM/NoPadding` como conversor JPA.

A chave de 256 bits é fornecida em Base64 por `app.criptografia.chave`, configurada via `APP_CRIPTOGRAFIA_CHAVE`. Cada criptografia usa IV aleatório de 12 bytes e tag de autenticação de 128 bits. O valor persistido segue o formato `Base64(IV || ciphertext)`.

O mecanismo protege campos como `agencia` e `numero_conta` na entidade de conta. Como o IV é aleatório, buscas por igualdade não usam as colunas criptografadas; para isso são usadas as colunas HMAC descritas em ADR específico.

## Consequências

### Positivas

- Dados bancários sensíveis ficam protegidos em repouso.
- AES-GCM oferece confidencialidade e autenticação do ciphertext.
- O uso de `AttributeConverter` mantém a criptografia concentrada na camada de persistência.
- O domínio não precisa conhecer detalhes do algoritmo criptográfico.

### Negativas / Trade-offs

- Perda ou rotação incorreta da chave pode impedir leitura de dados existentes.
- Não é possível buscar diretamente por colunas criptografadas com IV aleatório.
- A configuração de secrets passa a ser requisito obrigatório para execução segura.
