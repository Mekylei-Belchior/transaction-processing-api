FROM eclipse-temurin:21-jdk-alpine AS build

RUN apk add --no-cache maven

WORKDIR /app

COPY pom.xml ./

RUN mvn dependency:go-offline -B

COPY src ./src

RUN mvn package -DskipTests -B


FROM eclipse-temurin:21-jre-alpine AS runtime

RUN apk add --no-cache ca-certificates

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=build /app/target/transaction-processing-*.jar app.jar

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

RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]