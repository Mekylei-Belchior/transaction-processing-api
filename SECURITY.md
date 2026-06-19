# Política de Segurança

## Versões suportadas

| Versão | Suporte de segurança |
| --- | --- |
| `main` (atual) | ✅ Suportada |
| Branches antigas | ❌ Sem suporte |

## Reportando uma vulnerabilidade

**NÃO abra uma issue pública** para reportar vulnerabilidades de segurança.

Envie um relatório detalhado para [DEFINIR] incluindo:

- descrição da vulnerabilidade;
- passos para reprodução;
- impacto potencial estimado;
- versão afetada.

**Prazo de resposta:** até 72 horas para confirmação de recebimento; resolução em até 30 dias para vulnerabilidades críticas.

## Proteções implementadas

- Autenticação OAuth2/JWT via Keycloak;
- criptografia AES-256-GCM para dados sensíveis em repouso;
- HMAC-SHA256 para blind indexes;
- rate limiting por IP (Bucket4j);
- mascaramento de CPF, contas e tokens em logs;
- TLS para Kafka e Keycloak;
- locking otimista/pessimista para consistência transacional.

Para o modelo de segurança completo, consulte [Autenticação e autorização](docs/seguranca/autenticacao-autorizacao.md).
