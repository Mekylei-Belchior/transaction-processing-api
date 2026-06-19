# Troubleshooting Operacional

Este documento reúne sintomas comuns em operação da `transaction-processing-api`, suas causas prováveis e verificações recomendadas. Use como checklist inicial antes de alterar configuração, infraestrutura ou dados.

## Banco de dados e migrations

| Sintoma | Causa provável | Solução/Verificação |
| --- | --- | --- |
| Falha de conexão com o banco de dados com `Connection refused`. | PostgreSQL indisponível, porta incorreta, host incorreto ou `DB_URL` apontando para um endereço que não está acessível a partir da aplicação. | Verifique `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`. Em execução com Docker Compose, confirme se o container está em execução e saudável com `docker compose ps`, especialmente o serviço `postgres-transacao`. Confira se a URL JDBC usa o host correto para o contexto: dentro do Compose, normalmente `postgres-transacao`; fora do Compose, normalmente `localhost`. |
| Falha de conexão com `FATAL: password authentication failed`. | Usuário ou senha divergente entre a aplicação e o PostgreSQL. Também pode ocorrer quando o volume do banco já foi inicializado com credenciais antigas. | Verifique `DB_USERNAME` e `DB_PASSWORD` no ambiente da aplicação e compare com as credenciais reais do banco. Em Compose, confira também `POSTGRES_USER` e `POSTGRES_PASSWORD`. Se estiver usando volume persistente local, lembre que trocar variáveis depois da primeira inicialização não recria o usuário automaticamente. |
| Migration Flyway falha ao subir a aplicação. | Migration anterior não executada, falha parcial registrada, alteração manual em migration já aplicada ou estado divergente entre schema e histórico do Flyway. | Verifique a tabela `flyway_schema_history` para identificar a última migration executada e se há registro com falha. Confirme se as migrations anteriores foram aplicadas na ordem esperada. Nunca use `flyway.clean` em produção, pois essa operação remove objetos do schema e pode causar perda de dados. |

Consulta útil para histórico do Flyway:

```sql
select installed_rank, version, description, type, script, success, installed_on
from flyway_schema_history
order by installed_rank;
```

## Segurança, JWT e Keycloak

| Sintoma | Causa provável | Solução/Verificação |
| --- | --- | --- |
| Requisições retornam `401` e os logs indicam `Could not verify token signature`. | A aplicação não conseguiu validar a assinatura do JWT com as chaves do provedor configurado, ou o token foi emitido por outro realm/issuer. | Verifique se `OAUTH2_JWKS_URI` está acessível a partir da aplicação. Confirme se `OAUTH2_ISSUER_URI` bate exatamente com o claim `iss` do token emitido. Valide também se o realm configurado é o mesmo usado para emitir o token. |
| JWKS ou issuer indisponível. | Endpoint JWKS offline, Keycloak indisponível no homelab ou falha de DNS/conectividade com `keycloak.lab.home`. | Verifique se o Keycloak está rodando no homelab. Teste conectividade a partir do mesmo ambiente da aplicação para `keycloak.lab.home` e para o endpoint configurado em `OAUTH2_JWKS_URI`. Confirme se o issuer configurado em `OAUTH2_ISSUER_URI` corresponde ao realm esperado. |
| Erro SSL/TLS com Keycloak com `PKIX path building failed`. | A JVM não confia na cadeia TLS do Keycloak porque o `root_ca.crt` da `step-ca` não foi importado no truststore da JVM. | Verifique se o `Dockerfile` importou o certificado da root CA local no `$JAVA_HOME/lib/security/cacerts`. Consulte [Certificados e Truststore](../infraestrutura/certificados-truststore.md) para o fluxo esperado. |
| Build da imagem falha com `keytool: /tmp/root_ca.crt: No such file`. | O `Dockerfile` espera copiar `certificados/root_ca.crt`, mas esse arquivo não existe no repositório ou no contexto de build. | Obtenha o `root_ca.crt` da `step-ca` do homelab e coloque no caminho esperado localmente, sem versionar o certificado se a política do projeto assim exigir. Se o ambiente não usa root CA local, remova ou comente o bloco do `Dockerfile` que copia/importa esse certificado. Consulte [Certificados e Truststore](../infraestrutura/certificados-truststore.md). |

## Kafka, Outbox e DLQ

| Sintoma | Causa provável | Solução/Verificação |
| --- | --- | --- |
| Kafka indisponível ao iniciar com `Connection to node -1 could not be established`. | Broker Kafka inacessível, `KAFKA_BOOTSTRAP_SERVERS` incorreto ou Kafka habilitado em um ambiente que não precisa dele. | Verifique `EVENTOS_KAFKA_ENABLED`. Se a execução não precisa de Kafka, defina `EVENTOS_KAFKA_ENABLED=false`. Se precisa, confirme `KAFKA_BOOTSTRAP_SERVERS`, DNS, porta e conectividade com o broker configurado. |
| Autenticação SASL/SCRAM no Kafka falha com `Authentication failed`. | Credenciais SASL inválidas ou usuário não criado no broker do homelab. | Verifique `KAFKA_USERNAME` e `KAFKA_PASSWORD`. Confirme no broker Kafka do homelab se o usuário existe e se a senha corresponde ao mecanismo `SCRAM-SHA-256` usado pela aplicação. |
| Truststore Kafka inválido com `SSL handshake failed`. | Truststore não contém a root CA correta, senha/tipo divergente ou caminho inválido. | Verifique se a truststore PKCS12 foi gerada com o `root_ca.crt` correto. Confira `KAFKA_SSL_TRUSTSTORE_LOCATION` com prefixo `file:`, por exemplo `file:/opt/secrets/kafka-client-truststore.p12`. Valide também `KAFKA_SSL_TRUSTSTORE_PASSWORD` e `KAFKA_SSL_TRUSTSTORE_TYPE`, normalmente `PKCS12`. |
| Eventos presos no Outbox com status `PENDENTE` ou `FALHOU`. | Kafka inacessível, falha de autenticação/TLS, timeout de envio ou erro transitório persistente. | Verifique se Kafka está acessível e se as variáveis Kafka estão corretas. Consulte `ultimo_erro` na tabela `outbox_evento` para identificar a causa registrada. Confira o intervalo de nova tentativa em `EVENTOS_OUTBOX_INTERVALO_REPROCESSAMENTO` e aguarde a próxima execução do job antes de concluir que o evento travou definitivamente. |
| DLQ acumulando mensagens e `DlqMonitorConsumidor` registrando alertas. | Mensagens falham repetidamente no consumo até exceder o limite de tentativas. A causa pode ser desserialização, regra de negócio, dado inconsistente ou infraestrutura. | Analise os logs do `DlqMonitorConsumidor` e correlacione com `idEvento`, chave, tópico e offset. Verifique a causa em `ultimo_erro` da outbox quando o evento veio do fluxo da aplicação. Classifique se é erro de desserialização, regra de negócio ou infraestrutura antes de reenfileirar mensagens. |

Consulta útil para eventos da outbox:

```sql
select id, topico, chave, status, tentativas, proxima_tentativa_em, ultimo_erro, criado_em, publicado_em
from outbox_evento
where status in ('PENDENTE', 'FALHOU')
order by criado_em;
```

## Observabilidade

| Sintoma | Causa provável | Solução/Verificação |
| --- | --- | --- |
| Prometheus sem métricas, endpoint `/actuator/prometheus` vazio ou retornando `403`. | Endpoint não exposto no perfil ativo, dependência/configuração de Prometheus ausente ou autorização sem a role exigida em produção. | Verifique se o perfil ativo inclui `prometheus` em `management.endpoints.web.exposure.include`. No perfil `prod`, confirme se a role `ROLE_METRICAS.LEITURA` está configurada no Keycloak e presente no token usado para acessar o endpoint. |
| Traces não aparecem no Jaeger. | Exportação OTLP só está ativa no perfil esperado, endpoint OTLP incorreto ou sampling baixo. | Verifique se o perfil ativo é `prod`, pois a exportação OTLP foi documentada para esse perfil. Confirme `OTLP_TRACING_ENDPOINT` e conectividade até o collector/Jaeger. Lembre que o sampling em `prod` é `0.10`, ou seja, apenas 10% das requisições geram trace. Gere volume suficiente de requisições antes de concluir que não há traces. |

## Checklist rápido por variável

| Área | Variáveis principais |
| --- | --- |
| Banco de dados | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `POSTGRES_DB` |
| OAuth2/JWT | `OAUTH2_JWKS_URI`, `OAUTH2_ISSUER_URI` |
| Kafka | `EVENTOS_KAFKA_ENABLED`, `KAFKA_BOOTSTRAP_SERVERS`, `KAFKA_USERNAME`, `KAFKA_PASSWORD` |
| Kafka TLS | `KAFKA_SSL_TRUSTSTORE_LOCATION`, `KAFKA_SSL_TRUSTSTORE_PASSWORD`, `KAFKA_SSL_TRUSTSTORE_TYPE` |
| Outbox | `EVENTOS_OUTBOX_INTERVALO_REPROCESSAMENTO`, `EVENTOS_OUTBOX_INTERVALO_PUBLICACAO_MS`, `EVENTOS_OUTBOX_TIMEOUT_ENVIO_MS` |
| Observabilidade | `OTLP_TRACING_ENDPOINT`, `management.endpoints.web.exposure.include` |

## Diagnóstico local sem acesso ao homelab

Quando o homelab não está disponível, use os endpoints do Spring Boot Actuator para diagnóstico básico.

| Endpoint | Descrição | Autenticação |
| --- | --- | --- |
| `GET /actuator/health` | Estado geral (DB, Kafka, Disk) | Pública (detalhes requerem role) |
| `GET /actuator/info` | Versão da build e git | Pública |
| `GET /actuator/prometheus` | Métricas no formato Prometheus | Requer `ROLE_METRICAS.LEITURA` |
| `GET /actuator/env` | Variáveis de ambiente ativas | Requer `ADMIN` |
| `GET /actuator/beans` | Beans Spring carregados | Requer `ADMIN` |

### Filtrando logs por correlação

```bash
# Executar e filtrar logs por idCorrelacao
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev 2>&1 | grep '"idCorrelacao"'

# Filtrar por traceId específico
./mvnw spring-boot:run ... 2>&1 | grep "seu-trace-id-aqui"
```

> **Nota:** Para observabilidade completa com Grafana e Jaeger, consulte [metricas.md](../observabilidade/metricas.md) e [rastreamento.md](../observabilidade/rastreamento.md).

Para configurar o ambiente sem homelab, consulte [Execução sem acesso ao homelab](../desenvolvimento/execucao-local.md#execução-sem-acesso-ao-homelab).
