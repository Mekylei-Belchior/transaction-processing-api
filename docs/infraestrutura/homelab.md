# Homelab

O homelab é uma infraestrutura local gerenciada separadamente do repositório da `transaction-processing-api`. Ele concentra serviços de borda, identidade, mensageria, observabilidade, CI e certificados usados pela aplicação em cenários locais próximos de produção.

Os arquivos `docker-compose.yml` do homelab não fazem parte deste repositório.

## Serviços

| Serviço | Função no homelab | Relação com a aplicação |
| --- | --- | --- |
| `arcane` | Serviço operacional do homelab para gestão e apoio da infraestrutura local. | Não é dependência direta da API. |
| `database` | Bancos e serviços de dados compartilhados do homelab. | Pode apoiar serviços externos, mas o PostgreSQL da app é provisionado pelo Compose deste repositório. |
| `gitlab` | Git remoto e interface de gestão de repositórios. | Apoia o ciclo de desenvolvimento e entrega. |
| `gitlab-runner` | Executor de pipelines do GitLab CI. | Executa jobs de build, testes e entrega quando configurado. |
| `jaeger` | Backend de rastreamento distribuído. | Recebe traces da API via OTLP HTTP. |
| `kafka` | Broker de eventos. | Recebe e entrega eventos transacionais quando `EVENTOS_KAFKA_ENABLED=true`. |
| `keycloak` | Authorization Server OAuth2/OIDC. | Emite JWTs e expõe issuer/JWKS para validação pela API. |
| `observabilidade/prometheus-grafana` | Stack de métricas e dashboards. | Prometheus coleta `/actuator/prometheus`; Grafana exibe dashboards e SLOs. |
| `step-ca` | Unidade certificadora local. | Emite a `root_ca.crt` usada pela JVM e pelos certificados do Kafka. |
| `traefik` | Reverse proxy do homelab. | Faz terminação TLS e roteia requisições para Keycloak, API e demais serviços. |

## Certificados locais

O `step-ca` é a autoridade certificadora local do homelab. Ele emite a root CA usada para confiar nos certificados TLS dos serviços internos, como Keycloak, Kafka e outros endpoints expostos via HTTPS.

A `root_ca.crt` gerada pelo homelab pode ser usada pela JVM da aplicação e pela truststore do cliente Kafka. Esses arquivos são artefatos locais de ambiente e não devem ser tratados como código-fonte.

## Roteamento HTTPS

O Traefik atua como reverse proxy com terminação TLS. Ele publica hosts internos como `keycloak.lab.home` e pode rotear requisições HTTPS para a API e demais serviços do homelab.

A responsabilidade de criar routers, services, middlewares, certificados e secrets do Traefik é da infraestrutura do homelab, não deste repositório.

## Segredos

Os segredos de configuração do homelab, como passwords, client secrets, senhas de truststore, credenciais SASL/SCRAM e chaves privadas, não devem ser versionados neste repositório.

Use variáveis de ambiente, secret manager, arquivos locais ignorados pelo Git ou mecanismo equivalente para fornecer esses valores em execução.
