# ADR-006: OAuth2 Resource Server, JWT, Keycloak e RBAC

**Data:** 2026-06-17
**Status:** Aceito

## Contexto

A API expõe operações financeiras que exigem autenticação forte, autorização por perfil e integração com um provedor de identidade. O domínio não deve conhecer detalhes de segurança, mas endpoints HTTP precisam bloquear acessos indevidos e produzir respostas padronizadas para falhas de autenticação e autorização.

## Decisão

A aplicação atua como OAuth2 Resource Server com JWT emitido pelo Keycloak no realm `bancario`. A validação usa:

- `spring.security.oauth2.resourceserver.jwt.jwks-uri`, configurado por `OAUTH2_JWKS_URI`
- `spring.security.oauth2.resourceserver.jwt.issuer-uri`, configurado por `OAUTH2_ISSUER_URI`

O `JwtClaimsConverter` lê o claim `roles` do token e converte valores para authorities Spring Security com prefixo `ROLE_`. A autorização RBAC usa roles como `CLIENTE`, `OPERADOR`, `GERENTE`, `ADMIN`, `SERVICO_INTERNO` e `METRICAS.LEITURA`.

As regras HTTP e de segurança ficam em `SecurityConfig`, enquanto as permissões de endpoints transacionais são aplicadas com `@PreAuthorize` em `TransacaoController`. Falhas são tratadas por `ApiAutenticacaoEntryPoint` para `401 Unauthorized` e `ApiAcessoNegadoHandler` para `403 Forbidden`.

Endpoints públicos incluem `/actuator/health`, `/actuator/info`, `/v3/api-docs/**`, `/swagger-ui/**` e `/swagger-ui.html`. O endpoint `/actuator/prometheus` exige `ROLE_METRICAS.LEITURA`.

## Consequências

### Positivas

- A API delega autenticação ao Keycloak e valida JWT de forma padronizada.
- RBAC deixa permissões explícitas por operação.
- O domínio permanece livre de anotações e dependências de segurança.
- Erros de autenticação e autorização têm respostas consistentes.

### Negativas / Trade-offs

- A disponibilidade da validação JWT depende de configuração correta de issuer, JWKS e confiança TLS.
- Mudanças em claims ou roles no Keycloak exigem alinhamento com `JwtClaimsConverter` e `@PreAuthorize`.
- Testes de controller precisam simular authorities corretamente.

## Ver também

- [Homelab](../infraestrutura/homelab.md)
