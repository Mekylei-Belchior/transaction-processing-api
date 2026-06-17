# Certificados e Truststore

O homelab usa uma root CA local, emitida pelo `step-ca`, para assinar certificados TLS de serviços internos como Keycloak, Kafka e outros endpoints. Para que a aplicação confie nesses certificados, a JVM e o cliente Kafka precisam receber material de confiança apropriado.

## JVM e Keycloak HTTPS

Quando o Keycloak usa HTTPS com certificado emitido pela root CA local, a JVM precisa confiar nessa autoridade certificadora. Para isso, copie a root CA local para:

```text
certificados/root_ca.crt
```

O `Dockerfile` importa esse certificado no truststore padrão da JVM durante o build da imagem:

```dockerfile
# Copia o certificado da unidade certificadora para o container
COPY certificados/root_ca.crt /tmp/root_ca.crt

# Importa no truststore da JVM
RUN keytool -importcert \
    -noprompt \
    -trustcacerts \
    -alias root-ca \
    -file /tmp/root_ca.crt \
    -keystore $JAVA_HOME/lib/security/cacerts \
    -storepass changeit
```

Esse bloco copia o certificado público da root CA para dentro da imagem e usa `keytool` para adicioná-lo ao arquivo `cacerts` da JVM. Assim, conexões HTTPS da aplicação para endpoints internos, como `https://keycloak.lab.home`, podem validar a cadeia TLS.

Se o ambiente não usa root CA local, comente ou remova esse bloco do `Dockerfile`.

## Kafka com SASL_SSL

Para Kafka, a configuração `SASL_SSL` exige uma truststore do cliente contendo a root CA que assinou o certificado do broker. Gere localmente uma truststore PKCS12 com o `root_ca.crt`.

Comando registrado em `certificados/criar-truststore-kafka-cliente.txt`:

```bash
keytool -importcert \
  -alias archlab-root \
  -file root_ca.crt \
  -keystore /home/mekylei-belchior/workspace/transaction-processing-api/certificados/kafka-client-truststore.p12 \
  -storetype PKCS12 \
  -storepass changeit \
  -noprompt
```

O arquivo `.p12` gerado é material local de ambiente e não deve ser versionado.

## Variáveis Kafka

Configure a localização da truststore com o prefixo `file:` apontando para o arquivo `.p12` local:

```env
KAFKA_SSL_TRUSTSTORE_LOCATION=file:/home/mekylei-belchior/workspace/transaction-processing-api/certificados/kafka-client-truststore.p12
KAFKA_SSL_TRUSTSTORE_PASSWORD=<senha-do-truststore>
KAFKA_SSL_TRUSTSTORE_TYPE=PKCS12
```

Em ambientes diferentes, ajuste o caminho para o local real do arquivo.

## Regras de versionamento

Nunca versione:

- `root_ca.crt`
- `*.p12`
- `*.jks`
- arquivos de chave privada
- certificados ou bundles que contenham chave privada

Use placeholders para documentar valores esperados:

```env
KAFKA_SSL_TRUSTSTORE_PASSWORD=<senha-do-truststore>
KAFKA_SSL_TRUSTSTORE_TYPE=PKCS12
```
