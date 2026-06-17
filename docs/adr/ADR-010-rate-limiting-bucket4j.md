# ADR-010: Rate Limiting com Bucket4j

**Data:** 2026-06-17
**Status:** Aceito

## Contexto
A API precisa reduzir abuso, rajadas acidentais e pressão excessiva sobre recursos de processamento transacional. Como as operações envolvem banco, validações, segurança e mensageria, era necessário aplicar controle antes que a requisição chegasse aos casos de uso.

## Decisão
Foi adotado rate limiting com Bucket4j, implementado por `RateLimitFilter`. O filtro usa buckets por IP do cliente. Quando há proxy ou load balancer, o filtro considera o primeiro IP do header `X-Forwarded-For`.

O limite é configurado por `app.rate-limit.requests-per-minute`, com variável `RATE_LIMIT_REQUISICOES_POR_MINUTO`. O padrão geral é `60` requisições por minuto, com ajuste em `application-dev.yml` para facilitar desenvolvimento local.

Quando o limite é excedido, a requisição não segue para o endpoint. `RateLimitResposta` retorna `429 Too Many Requests`, header `Retry-After: 60`, título `Limite excedido`, detalhe padronizado e `codigoErro=LIMITE_EXCEDIDO`.

## Consequências
### Positivas
- Protege a API contra rajadas simples antes de acionar lógica de negócio.
- O comportamento é centralizado em um filtro HTTP.
- A configuração por ambiente permite limites diferentes para desenvolvimento e produção.
- A resposta `429` comunica claramente a necessidade de retry posterior.

### Negativas / Trade-offs
- Bucket por IP pode punir usuários legítimos atrás do mesmo NAT ou proxy.
- Em execução com múltiplas instâncias, buckets em memória não compartilham estado.
- Limites precisam ser calibrados conforme tráfego real e topologia de rede.
