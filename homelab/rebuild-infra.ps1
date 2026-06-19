#requires -Version 5.1
<#
rebuild-infra.ps1 - Reconstrucao da infraestrutura ArchLab no Windows 11+

Este script gera a estrutura e os arquivos de configuracao da infra para uso com
Docker Desktop no Windows. Ele e idempotente: arquivos existentes nao sao
sobrescritos.

Uso:
  Set-ExecutionPolicy -Scope Process Bypass
  .\rebuild-infra.ps1

Teste isolado:
  .\rebuild-infra.ps1 -Workspace $env:TEMP\archlab-test -SkipDependencyCheck -SkipDockerNetwork -SkipHostValidation
#>

[CmdletBinding()]
param(
    [string]$HostIP = "192.168.0.102",
    [string]$Domain = "lab.home",
    [string]$Workspace = (Join-Path $HOME "workspace"),
    [string]$DockerNetwork = "infra-net",
    [string]$DockerNetworkSubnet = "172.20.0.0/24",
    [switch]$SkipDependencyCheck,
    [switch]$SkipDockerNetwork,
    [switch]$SkipHostValidation
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$InfraDir = Join-Path $Workspace "infra"

function Write-Info { param([string]$Message) Write-Host "[INFO]  $Message" -ForegroundColor Cyan }
function Write-Ok { param([string]$Message) Write-Host "[OK]    $Message" -ForegroundColor Green }
function Write-Warn { param([string]$Message) Write-Host "[WARN]  $Message" -ForegroundColor Yellow }
function Write-Err { param([string]$Message) Write-Host "[ERRO]  $Message" -ForegroundColor Red }
function Write-Section {
    param([string]$Title)
    Write-Host ""
    Write-Host "============================================" -ForegroundColor Cyan
    Write-Host "  $Title" -ForegroundColor Cyan
    Write-Host "============================================" -ForegroundColor Cyan
}

function Test-CommandExists {
    param([string]$Name)
    return [bool](Get-Command $Name -ErrorAction SilentlyContinue)
}

function New-SafeDirectory {
    param([string]$Path)
    if (-not (Test-Path -LiteralPath $Path -PathType Container)) {
        New-Item -ItemType Directory -Path $Path -Force | Out-Null
        Write-Ok "Diretorio criado: $Path"
    }
}

function New-SafeFile {
    param(
        [string]$Path,
        [string]$Description,
        [string]$Content,
        [switch]$NoNewline
    )
    if (Test-Path -LiteralPath $Path -PathType Leaf) {
        Write-Warn "$Description ja existe - ignorando: $Path"
        return
    }
    $parent = Split-Path -Parent $Path
    if ($parent) { New-SafeDirectory $parent }
    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    if ($NoNewline) {
        [System.IO.File]::WriteAllText($Path, $Content, $utf8NoBom)
    } else {
        [System.IO.File]::WriteAllText($Path, ($Content.TrimStart([char[]]"`r`n") + "`n"), $utf8NoBom)
    }
    Write-Ok "$Description gerado."
}

function Convert-ToComposePath {
    param([string]$Path)
    return ($Path -replace "\\", "/")
}

function New-RandomHex {
    param([int]$Bytes = 32)
    $buffer = New-Object byte[] $Bytes
    [System.Security.Cryptography.RandomNumberGenerator]::Fill($buffer)
    return (($buffer | ForEach-Object { $_.ToString("x2") }) -join "")
}

function Check-Dependencies {
    Write-Section "FASE 0 - Verificacao de Dependencias"
    if ($SkipDependencyCheck) {
        Write-Warn "Verificacao de dependencias ignorada por parametro."
        return
    }

    $missing = $false
    if (-not (Test-CommandExists docker)) {
        Write-Err "Docker nao encontrado. Instale Docker Desktop para Windows."
        $missing = $true
    } else {
        $dockerVersion = (& docker version --format '{{.Client.Version}}' 2>$null)
        if (-not $dockerVersion) { $dockerVersion = "desconhecido" }
        Write-Ok "Docker instalado: v$dockerVersion"

        $composeVersion = (& docker compose version --short 2>$null)
        if (-not $composeVersion) {
            Write-Err "Docker Compose plugin nao encontrado no Docker Desktop."
            $missing = $true
        } else {
            Write-Ok "Docker Compose instalado: v$composeVersion"
        }
    }

    foreach ($cmd in @("curl", "openssl")) {
        if (Test-CommandExists $cmd) {
            Write-Ok "$cmd instalado."
        } else {
            Write-Warn "$cmd nao encontrado no PATH. Instale via winget/choco ou use Git for Windows/OpenSSL."
        }
    }

    if ($missing) { throw "Dependencias obrigatorias ausentes." }
}

function Setup-Structure {
    Write-Section "FASE 0 - Estrutura de Diretorios"
    Write-Info "Criando estrutura de diretorios da infra em: $InfraDir"

    @(
        "traefik/config/dynamic",
        "traefik/certs",
        "traefik/logs",
        "step-ca/data/secrets",
        "step-ca/data/certs",
        "step-ca/data/config",
        "step-ca/data/db",
        "step-ca/data/templates",
        "gitlab/config",
        "gitlab/data/backups",
        "gitlab/logs",
        "gitlab-runner/config",
        "arcane",
        "jaeger",
        "kafka/certs",
        "keycloak/keycloak/import",
        "keycloak/keycloak/themes",
        "keycloak/postgres/data",
        "observabilidade/prometheus-grafana/grafana/dashboards",
        "observabilidade/prometheus-grafana/grafana/provisioning/dashboards",
        "observabilidade/prometheus-grafana/grafana/provisioning/datasources",
        "observabilidade/prometheus-grafana/prometheus/rules",
        "observabilidade/prometheus-grafana/prometheus/secrets"
    ) | ForEach-Object { New-SafeDirectory (Join-Path $InfraDir $_) }

    New-SafeDirectory (Join-Path $Workspace "scripts/infraestrutura")

    Write-Ok "Estrutura de diretorios pronta."
}

function Setup-DockerNetwork {
    Write-Section "FASE 0 - Rede Docker"
    if ($SkipDockerNetwork) {
        Write-Warn "Criacao/inspecao da rede Docker ignorada por parametro."
        return
    }

    & docker network inspect $DockerNetwork *> $null
    if ($LASTEXITCODE -eq 0) {
        $subnet = (& docker network inspect $DockerNetwork --format '{{range .IPAM.Config}}{{.Subnet}}{{end}}' 2>$null)
        Write-Warn "Rede '$DockerNetwork' ja existe (subnet: $subnet). Ignorando criacao."
    } else {
        Write-Info "Criando rede Docker: $DockerNetwork (subnet: $DockerNetworkSubnet)"
        & docker network create --driver bridge --subnet $DockerNetworkSubnet $DockerNetwork | Out-Null
        if ($LASTEXITCODE -ne 0) { throw "Falha ao criar rede Docker $DockerNetwork." }
        Write-Ok "Rede '$DockerNetwork' criada."
    }
}

function Generate-Configs {
    Write-Section "Geracao de Configuracoes"

    $infraMount = Convert-ToComposePath $InfraDir
    $workspaceMount = Convert-ToComposePath $Workspace
    $dashboardHash = 'admin:$apr1$exhDrsJp$q/MU0DZp.pbghpUwYA/6d0'
    $arcaneEncryptionKey = New-RandomHex
    $arcaneJwtSecret = New-RandomHex

    New-SafeFile (Join-Path $InfraDir "step-ca/docker-compose.yml") "step-ca docker-compose.yml" @"
services:
  step-ca:
    image: smallstep/step-ca:latest
    container_name: step-ca
    restart: unless-stopped
    ports:
      - "9000:9000"
    volumes:
      - ./data:/home/step
    environment:
      - DOCKER_STEPCA_INIT_NAME=ArchLab CA
      - DOCKER_STEPCA_INIT_DNS_NAMES=step-ca,ca.$Domain,$HostIP
      - DOCKER_STEPCA_INIT_REMOTE_MANAGEMENT=true
      - DOCKER_STEPCA_INIT_ACME=true
      - DOCKER_STEPCA_INIT_PASSWORD_FILE=/home/step/secrets/password.txt
    networks:
      infra-net:
        aliases:
          - ca.$Domain

networks:
  infra-net:
    external: true
"@
    $passwordFile = Join-Path $InfraDir "step-ca/data/secrets/password.txt"
    if (-not (Test-Path -LiteralPath $passwordFile)) {
        Write-Warn "ACAO MANUAL - crie $passwordFile antes de subir o step-ca."
    }

    New-SafeFile (Join-Path $InfraDir "traefik/docker-compose.yml") "traefik docker-compose.yml" @"
services:
  traefik:
    image: traefik:latest
    container_name: traefik
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock:ro
      - ./config/traefik.yml:/traefik.yml:ro
      - ./config/dynamic:/config/dynamic:ro
      - ./certs:/certs
      - ./logs:/logs
      - $infraMount/step-ca/root_ca.crt:/etc/ssl/certs/archlab-ca.crt:ro
    environment:
      - LEGO_CA_CERTIFICATES=/etc/ssl/certs/archlab-ca.crt
    networks:
      - infra-net
    labels:
      - "traefik.enable=false"

networks:
  infra-net:
    external: true
"@

    New-SafeFile (Join-Path $InfraDir "traefik/config/traefik.yml") "traefik.yml" @"
global:
  checkNewVersion: false
  sendAnonymousUsage: false

api:
  dashboard: true
  insecure: false

log:
  level: INFO
  filePath: /logs/traefik.log

accessLog:
  filePath: /logs/access.log
  bufferingSize: 100

entryPoints:
  web:
    address: ":80"
    http:
      redirections:
        entryPoint:
          to: websecure
          scheme: https
          permanent: true
  websecure:
    address: ":443"
    http:
      tls:
        certResolver: step-ca
  kafkassl:
    address: ":9094"

providers:
  docker:
    endpoint: "unix:///var/run/docker.sock"
    exposedByDefault: false
    network: infra-net
  file:
    directory: /config/dynamic
    watch: true

certificatesResolvers:
  step-ca:
    acme:
      email: admin@$Domain
      storage: /certs/acme.json
      caServer: https://ca.${Domain}:9000/acme/acme/directory
      tlsChallenge: {}

serversTransport:
  insecureSkipVerify: false
"@

    New-SafeFile (Join-Path $InfraDir "traefik/config/dynamic/dashboard.yml") "dashboard.yml" @"
http:
  routers:
    traefik-dashboard:
      rule: "Host(``traefik.$Domain``)"
      service: api@internal
      entryPoints:
        - websecure
      tls:
        certResolver: step-ca
      middlewares:
        - auth-dashboard

  middlewares:
    auth-dashboard:
      basicAuth:
        users:
          - "$dashboardHash"
"@

    New-SafeFile (Join-Path $InfraDir "traefik/config/dynamic/middlewares.yml") "middlewares.yml" @"
http:
  middlewares:
    rate-limit:
      rateLimit:
        average: 100
        burst: 50
    secure-headers:
      headers:
        stsSeconds: 31536000
        stsIncludeSubdomains: true
        stsPreload: true
        forceSTSHeader: true
        contentTypeNosniff: true
        browserXssFilter: true
        frameDeny: true
        customResponseHeaders:
          X-Frame-Options: "DENY"
          Referrer-Policy: "strict-origin-when-cross-origin"
          Permissions-Policy: "camera=(), microphone=(), geolocation=()"
          X-Content-Type-Options: "nosniff"
"@

    $acmePath = Join-Path $InfraDir "traefik/certs/acme.json"
    if (Test-Path -LiteralPath $acmePath) {
        $size = (Get-Item -LiteralPath $acmePath).Length
        if ($size -gt 10) {
            Write-Warn "acme.json ja existe e contem dados ($size bytes). Preservando certificados existentes."
        }
    } else {
        New-SafeFile $acmePath "acme.json" "" -NoNewline | Out-Null
    }

    New-SafeFile (Join-Path $InfraDir "gitlab/docker-compose.yml") "gitlab docker-compose.yml" @"
services:
  gitlab:
    image: gitlab/gitlab-ce:latest
    container_name: gitlab
    restart: unless-stopped
    environment:
      GITLAB_OMNIBUS_CONFIG: |
        external_url 'https://gitlab.$Domain'
        nginx['listen_port'] = 80
        nginx['listen_https'] = false
        nginx['proxy_set_headers'] = {
          "X-Forwarded-Proto" => "https",
          "X-Forwarded-Ssl" => "on"
        }
        registry_external_url 'https://registry.$Domain'
        registry_nginx['listen_port'] = 5050
        registry_nginx['listen_https'] = false
        registry_nginx['proxy_set_headers'] = {
          "X-Forwarded-Proto" => "https",
          "X-Forwarded-Ssl" => "on"
        }
        gitlab_rails['time_zone'] = 'America/Sao_Paulo'
        unicorn['worker_processes'] = 2
        sidekiq['concurrency'] = 5
        postgresql['shared_buffers'] = "256MB"
        prometheus_monitoring['enable'] = false
        gitlab_rails['gitlab_shell_ssh_port'] = 2222
    ports:
      - "2222:22"
    volumes:
      - ./config:/etc/gitlab
      - ./logs:/var/log/gitlab
      - ./data:/var/opt/gitlab
    shm_size: '256m'
    networks:
      - infra-net
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.gitlab.rule=Host(``gitlab.$Domain``)"
      - "traefik.http.routers.gitlab.entrypoints=websecure"
      - "traefik.http.routers.gitlab.tls=true"
      - "traefik.http.routers.gitlab.tls.certresolver=step-ca"
      - "traefik.http.routers.gitlab.service=gitlab-svc"
      - "traefik.http.routers.gitlab.middlewares=secure-headers@file,rate-limit@file"
      - "traefik.http.services.gitlab-svc.loadbalancer.server.port=80"
      - "traefik.http.routers.registry.rule=Host(``registry.$Domain``)"
      - "traefik.http.routers.registry.entrypoints=websecure"
      - "traefik.http.routers.registry.tls=true"
      - "traefik.http.routers.registry.tls.certresolver=step-ca"
      - "traefik.http.routers.registry.service=registry-svc"
      - "traefik.http.routers.registry.middlewares=secure-headers@file,rate-limit@file"
      - "traefik.http.services.registry-svc.loadbalancer.server.port=5050"

networks:
  infra-net:
    external: true
"@

    New-SafeFile (Join-Path $InfraDir "gitlab-runner/docker-compose.yml") "gitlab-runner docker-compose.yml" @"
services:
  gitlab-runner:
    image: gitlab/gitlab-runner:latest
    container_name: gitlab-runner
    restart: unless-stopped
    volumes:
      - ./config:/etc/gitlab-runner
      - /var/run/docker.sock:/var/run/docker.sock
      - $infraMount/step-ca/root_ca.crt:/etc/ssl/certs/archlab-ca.crt:ro
    environment:
      - SSL_CERT_FILE=/etc/ssl/certs/archlab-ca.crt
    networks:
      - infra-net
    extra_hosts:
      - "gitlab.$Domain`:$HostIP"
      - "registry.$Domain`:$HostIP"

networks:
  infra-net:
    external: true
"@

    New-SafeFile (Join-Path $InfraDir "arcane/docker-compose.yml") "arcane docker-compose.yml" @"
services:
  arcane:
    image: ghcr.io/getarcaneapp/arcane:latest
    container_name: arcane
    restart: unless-stopped
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
      - arcane-data:/app/data
      - $workspaceMount/projects:$workspaceMount/projects
    environment:
      - APP_URL=https://arcane.$Domain
      - PUID=1000
      - PGID=1000
      - ENCRYPTION_KEY=$arcaneEncryptionKey
      - JWT_SECRET=$arcaneJwtSecret
      - PROJECTS_DIRECTORY=$workspaceMount/projects
    networks:
      - infra-net
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.arcane.rule=Host(``arcane.$Domain``)"
      - "traefik.http.routers.arcane.entrypoints=websecure"
      - "traefik.http.routers.arcane.tls=true"
      - "traefik.http.routers.arcane.tls.certresolver=step-ca"
      - "traefik.http.routers.arcane.service=arcane-svc"
      - "traefik.http.routers.arcane.middlewares=arcane-ws,secure-headers@file,rate-limit@file"
      - "traefik.http.services.arcane-svc.loadbalancer.server.port=3552"
      - "traefik.http.middlewares.arcane-ws.headers.customrequestheaders.Upgrade=websocket"
      - "traefik.http.middlewares.arcane-ws.headers.customrequestheaders.Connection=Upgrade"

volumes:
  arcane-data:

networks:
  infra-net:
    external: true
"@

    New-SafeFile (Join-Path $InfraDir "jaeger/docker-compose.yml") "jaeger docker-compose.yml" @"
services:
  jaeger-tracing:
    image: jaegertracing/all-in-one:1.76.0
    container_name: jaeger
    restart: unless-stopped
    networks:
      - infra-net
    environment:
      COLLECTOR_OTLP_ENABLED: "true"
    labels:
      - "traefik.enable=true"
      - "traefik.docker.network=infra-net"
      - "traefik.http.routers.jaeger-ui.rule=Host(``jaeger.$Domain``)"
      - "traefik.http.routers.jaeger-ui.entrypoints=websecure"
      - "traefik.http.routers.jaeger-ui.tls=true"
      - "traefik.http.routers.jaeger-ui.tls.certresolver=step-ca"
      - "traefik.http.routers.jaeger-ui.middlewares=rate-limit@file"
      - "traefik.http.routers.jaeger-ui.service=jaeger-ui-svc"
      - "traefik.http.services.jaeger-ui-svc.loadbalancer.server.port=16686"
      - "traefik.http.routers.jaeger-otlp.rule=Host(``otlp-jaeger.$Domain``)"
      - "traefik.http.routers.jaeger-otlp.entrypoints=websecure"
      - "traefik.http.routers.jaeger-otlp.tls=true"
      - "traefik.http.routers.jaeger-otlp.tls.certresolver=step-ca"
      - "traefik.http.routers.jaeger-otlp.service=jaeger-otlp-svc"
      - "traefik.http.services.jaeger-otlp-svc.loadbalancer.server.port=4318"

networks:
  infra-net:
    external: true
"@

    New-SafeFile (Join-Path $InfraDir "kafka/docker-compose.yml") "kafka docker-compose.yml" @'
services:
  kafka:
    image: ${KAFKA_IMAGE}
    container_name: kafka
    restart: unless-stopped
    networks:
      - infra-net
    environment:
      KAFKA_NODE_ID: ${KAFKA_NODE_ID}
      KAFKA_PROCESS_ROLES: ${KAFKA_PROCESS_ROLES}
      KAFKA_LISTENERS: ${KAFKA_LISTENERS}
      KAFKA_ADVERTISED_LISTENERS: ${KAFKA_ADVERTISED_LISTENERS}
      KAFKA_INTER_BROKER_LISTENER_NAME: ${KAFKA_INTER_BROKER_LISTENER_NAME}
      KAFKA_CONTROLLER_LISTENER_NAMES: ${KAFKA_CONTROLLER_LISTENER_NAMES}
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: ${KAFKA_LISTENER_SECURITY_PROTOCOL_MAP}
      KAFKA_CONTROLLER_QUORUM_VOTERS: ${KAFKA_CONTROLLER_QUORUM_VOTERS}
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: ${KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR}
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: ${KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR}
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: ${KAFKA_TRANSACTION_STATE_LOG_MIN_ISR}
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: ${KAFKA_AUTO_CREATE_TOPICS_ENABLE}
      KAFKA_LOG_DIRS: ${KAFKA_LOG_DIRS}
      KAFKA_SSL_KEYSTORE_TYPE: ${KAFKA_SSL_KEYSTORE_TYPE}
      KAFKA_SSL_KEYSTORE_FILENAME: ${KAFKA_SSL_KEYSTORE_FILENAME}
      KAFKA_SSL_KEYSTORE_CREDENTIALS: ${KAFKA_SSL_KEYSTORE_CREDENTIALS}
      KAFKA_SSL_KEY_CREDENTIALS: ${KAFKA_SSL_KEY_CREDENTIALS}
      KAFKA_SSL_TRUSTSTORE_TYPE: ${KAFKA_SSL_TRUSTSTORE_TYPE}
      KAFKA_SSL_TRUSTSTORE_FILENAME: ${KAFKA_SSL_TRUSTSTORE_FILENAME}
      KAFKA_SSL_TRUSTSTORE_CREDENTIALS: ${KAFKA_SSL_TRUSTSTORE_CREDENTIALS}
      KAFKA_SSL_CLIENT_AUTH: ${KAFKA_SSL_CLIENT_AUTH}
      KAFKA_SASL_ENABLED_MECHANISMS: ${KAFKA_SASL_ENABLED_MECHANISMS}
      KAFKA_OPTS: ${KAFKA_OPTS}
    volumes:
      - ${KAFKA_DATA_VOLUME}:/var/lib/kafka/data
      - ./certs:/etc/kafka/secrets:ro
      - ./kafka_server_jaas.conf:/etc/kafka/kafka_server_jaas.conf:ro
    healthcheck:
      test: ["CMD-SHELL", "bash -ec 'echo > /dev/tcp/127.0.0.1/${KAFKA_HEALTHCHECK_PORT}'"]
      interval: ${KAFKA_HEALTHCHECK_INTERVAL}
      timeout: ${KAFKA_HEALTHCHECK_TIMEOUT}
      retries: ${KAFKA_HEALTHCHECK_RETRIES}
      start_period: ${KAFKA_HEALTHCHECK_START_PERIOD}
    labels:
      - "traefik.enable=true"
      - "traefik.tcp.routers.kafka.rule=HostSNI(`kafka.lab.home`)"
      - "traefik.tcp.routers.kafka.entrypoints=kafkassl"
      - "traefik.tcp.routers.kafka.tls=true"
      - "traefik.tcp.routers.kafka.tls.passthrough=true"
      - "traefik.tcp.services.kafka.loadbalancer.server.port=9094"

volumes:
  kafka-data:

networks:
  infra-net:
    external: true
'@

    New-SafeFile (Join-Path $InfraDir "kafka/.env") "kafka .env" @"
KAFKA_IMAGE=apache/kafka:3.9.0
KAFKA_NODE_ID=1
KAFKA_PROCESS_ROLES=broker,controller
KAFKA_LISTENERS=INTERNAL://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093,SASL_SSL://0.0.0.0:9094
KAFKA_ADVERTISED_LISTENERS=INTERNAL://kafka:9092,SASL_SSL://kafka.$Domain`:9094,CONTROLLER://kafka:9093
KAFKA_INTER_BROKER_LISTENER_NAME=INTERNAL
KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER
KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,INTERNAL:PLAINTEXT,SASL_SSL:SASL_SSL
KAFKA_CONTROLLER_QUORUM_VOTERS=1@kafka:9093
KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1
KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=1
KAFKA_TRANSACTION_STATE_LOG_MIN_ISR=1
KAFKA_AUTO_CREATE_TOPICS_ENABLE=true
KAFKA_LOG_DIRS=/var/lib/kafka/data
KAFKA_DATA_VOLUME=kafka-data
KAFKA_SSL_KEYSTORE_TYPE=PKCS12
KAFKA_SSL_KEYSTORE_FILENAME=kafka.keystore.p12
KAFKA_SSL_KEYSTORE_CREDENTIALS=kafka_ssl_keystore_creds
KAFKA_SSL_KEY_CREDENTIALS=kafka_ssl_key_creds
KAFKA_SSL_TRUSTSTORE_TYPE=PKCS12
KAFKA_SSL_TRUSTSTORE_FILENAME=kafka.truststore.p12
KAFKA_SSL_TRUSTSTORE_CREDENTIALS=kafka_ssl_truststore_creds
KAFKA_SSL_CLIENT_AUTH=none
KAFKA_SASL_ENABLED_MECHANISMS=SCRAM-SHA-256
KAFKA_OPTS=-Djava.security.auth.login.config=/etc/kafka/kafka_server_jaas.conf
KAFKA_HEALTHCHECK_PORT=9092
KAFKA_HEALTHCHECK_INTERVAL=15s
KAFKA_HEALTHCHECK_TIMEOUT=10s
KAFKA_HEALTHCHECK_RETRIES=5
KAFKA_HEALTHCHECK_START_PERIOD=30s
KAFKA_CERT_CN=kafka.$Domain
KAFKA_CERT_SAN_DOCKER=kafka
KAFKA_CERT_TTL=720h
STEP_CA_ROOT_CERT=../step-ca/root_ca.crt
STEP_CA_INTERMEDIATE_CERT=../step-ca/data/certs/intermediate_ca.crt
STEP_CA_INTERMEDIATE_KEY=../step-ca/data/secrets/intermediate_ca_key
STEP_CA_PASSWORD_FILE=../step-ca/data/secrets/password.txt
KAFKA_SSL_SECRET=changeit
KAFKA_SASL_USERNAME=app-prod
KAFKA_SASL_PASSWORD=trocar-esta-senha
"@

    New-SafeFile (Join-Path $InfraDir "kafka/kafka_server_jaas.conf") "kafka_server_jaas.conf" @"
KafkaServer {
  org.apache.kafka.common.security.scram.ScramLoginModule required;
};
"@

    New-SafeFile (Join-Path $InfraDir "kafka/generate-kafka-certs.ps1") "generate-kafka-certs.ps1" @'
#requires -Version 5.1
$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$EnvFile = Join-Path $ScriptDir ".env"
$CertDir = Join-Path $ScriptDir "certs"

if (-not (Test-Path $EnvFile)) { throw "Arquivo .env nao encontrado em $ScriptDir" }
if (-not (Get-Command step -ErrorAction SilentlyContinue)) { throw "Comando 'step' nao encontrado. Instale a step CLI." }
if (-not (Get-Command openssl -ErrorAction SilentlyContinue)) { throw "Comando 'openssl' nao encontrado." }

Get-Content $EnvFile | Where-Object { $_ -match "^[A-Za-z_][A-Za-z0-9_]*=" } | ForEach-Object {
    $parts = $_ -split "=", 2
    Set-Item -Path "Env:$($parts[0])" -Value $parts[1]
}

New-Item -ItemType Directory -Path $CertDir -Force | Out-Null
$RootCert = Join-Path $ScriptDir $env:STEP_CA_ROOT_CERT
$IntermediateCert = Join-Path $ScriptDir $env:STEP_CA_INTERMEDIATE_CERT
$IntermediateKey = Join-Path $ScriptDir $env:STEP_CA_INTERMEDIATE_KEY
$PasswordFile = Join-Path $ScriptDir $env:STEP_CA_PASSWORD_FILE

& step certificate create $env:KAFKA_CERT_CN "$CertDir/kafka.crt" "$CertDir/kafka.key" --profile leaf --ca $IntermediateCert --ca-key $IntermediateKey --ca-password-file $PasswordFile --san $env:KAFKA_CERT_CN --san $env:KAFKA_CERT_SAN_DOCKER --not-after $env:KAFKA_CERT_TTL --force --no-password --insecure
& openssl pkcs12 -export -name kafka -in "$CertDir/kafka.crt" -inkey "$CertDir/kafka.key" -certfile $IntermediateCert -out "$CertDir/kafka.keystore.p12" -passout "pass:$env:KAFKA_SSL_SECRET"
& openssl pkcs12 -export -nokeys -in $RootCert -certfile $IntermediateCert -out "$CertDir/kafka.truststore.p12" -name step-ca -passout "pass:$env:KAFKA_SSL_SECRET"

Set-Content -Path (Join-Path $CertDir $env:KAFKA_SSL_KEY_CREDENTIALS) -Value $env:KAFKA_SSL_SECRET -NoNewline
Set-Content -Path (Join-Path $CertDir $env:KAFKA_SSL_KEYSTORE_CREDENTIALS) -Value $env:KAFKA_SSL_SECRET -NoNewline
Set-Content -Path (Join-Path $CertDir $env:KAFKA_SSL_TRUSTSTORE_CREDENTIALS) -Value $env:KAFKA_SSL_SECRET -NoNewline
Write-Host "Certificados Kafka gerados em: $CertDir"
'@

    New-SafeFile (Join-Path $InfraDir "keycloak/docker-compose.yml") "keycloak docker-compose.yml" @'
services:
  postgres-keycloak:
    image: postgres:17
    container_name: postgres-keycloak
    restart: unless-stopped
    networks:
      - infra-net
    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    volumes:
      - postgres-keycloak-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U $$POSTGRES_USER -d $$POSTGRES_DB"]
      interval: 10s
      timeout: 5s
      retries: 10

  keycloak:
    image: quay.io/keycloak/keycloak:26.2
    container_name: keycloak
    restart: unless-stopped
    depends_on:
      postgres-keycloak:
        condition: service_healthy
    networks:
      - infra-net
    environment:
      KC_BOOTSTRAP_ADMIN_USERNAME: ${KEYCLOAK_ADMIN}
      KC_BOOTSTRAP_ADMIN_PASSWORD: ${KEYCLOAK_ADMIN_PASSWORD}
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://postgres-keycloak:5432/${POSTGRES_DB}
      KC_DB_USERNAME: ${POSTGRES_USER}
      KC_DB_PASSWORD: ${POSTGRES_PASSWORD}
      KC_HOSTNAME: https://keycloak.lab.home
      KC_HOSTNAME_STRICT: "false"
      KC_HTTP_ENABLED: "true"
      KC_HTTP_PORT: "8080"
      KC_PROXY_HEADERS: xforwarded
      KC_HEALTH_ENABLED: "true"
      KC_METRICS_ENABLED: "true"
    command:
      - start
    volumes:
      - ./keycloak/import:/opt/keycloak/data/import
      - ./keycloak/themes:/opt/keycloak/themes
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.keycloak.rule=Host(`keycloak.lab.home`)"
      - "traefik.http.routers.keycloak.entrypoints=websecure"
      - "traefik.http.routers.keycloak.tls=true"
      - "traefik.http.routers.keycloak.tls.certresolver=step-ca"
      - "traefik.http.routers.keycloak.service=keycloak-svc"
      - "traefik.http.routers.keycloak.middlewares=rate-limit@file"
      - "traefik.http.services.keycloak-svc.loadbalancer.server.port=8080"

volumes:
  postgres-keycloak-data:

networks:
  infra-net:
    external: true
'@

    New-SafeFile (Join-Path $InfraDir "keycloak/.env") "keycloak .env" @"
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=TROCAR_SENHA_FORTE

POSTGRES_DB=keycloak
POSTGRES_USER=keycloak
POSTGRES_PASSWORD=TROCAR_SENHA_FORTE
"@

    New-SafeFile (Join-Path $InfraDir "keycloak/keycloak-up.ps1") "keycloak-up.ps1" @"
`$ErrorActionPreference = "Stop"
`$InfraDir = "$InfraDir"
Push-Location "`$InfraDir/step-ca"; docker compose up -d; Pop-Location
Push-Location "`$InfraDir/traefik"; docker compose up -d; Pop-Location
Push-Location "`$InfraDir/keycloak"; docker compose up -d; Pop-Location
docker ps --format "table {{.Names}}`t{{.Status}}" | Select-String "step-ca|traefik|keycloak"
"@

    New-SafeFile (Join-Path $InfraDir "keycloak/keycloak-down.ps1") "keycloak-down.ps1" @"
`$ErrorActionPreference = "Stop"
`$InfraDir = "$InfraDir"
Push-Location "`$InfraDir/keycloak"; docker compose down; Pop-Location
Push-Location "`$InfraDir/traefik"; docker compose down; Pop-Location
Push-Location "`$InfraDir/step-ca"; docker compose down; Pop-Location
"@

    New-SafeFile (Join-Path $InfraDir "observabilidade/prometheus-grafana/docker-compose.yml") "prometheus-grafana docker-compose.yml" @"
services:
  prometheus:
    image: prom/prometheus:v3.12.0
    container_name: prometheus
    restart: unless-stopped
    command:
      - "--config.file=/etc/prometheus/prometheus.yml"
      - "--storage.tsdb.path=/prometheus"
      - "--web.enable-lifecycle"
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro
      - ./prometheus/rules:/etc/prometheus/rules:ro
      - ./prometheus/secrets:/etc/prometheus/secrets:ro
      - ../../step-ca/root_ca.crt:/etc/prometheus/certs/root_ca.crt:ro
      - prometheus_data:/prometheus
    networks:
      - infra-net
    extra_hosts:
      - "host.docker.internal:host-gateway"
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.prometheus.rule=Host(``prometheus.$Domain``)"
      - "traefik.http.routers.prometheus.entrypoints=websecure"
      - "traefik.http.routers.prometheus.tls=true"
      - "traefik.http.routers.prometheus.tls.certresolver=step-ca"
      - "traefik.http.routers.prometheus.middlewares=rate-limit@file"
      - "traefik.http.services.prometheus.loadbalancer.server.port=9090"

  grafana:
    image: grafana/grafana:13.0.2
    container_name: grafana
    restart: unless-stopped
    environment:
      GF_SECURITY_ADMIN_USER: `${GRAFANA_ADMIN_USER}
      GF_SECURITY_ADMIN_PASSWORD: `${GRAFANA_ADMIN_PASSWORD}
      GF_SERVER_ROOT_URL: https://grafana.$Domain
      GF_USERS_ALLOW_SIGN_UP: "false"
    volumes:
      - grafana_data:/var/lib/grafana
      - ./grafana/provisioning:/etc/grafana/provisioning:ro
      - ./grafana/dashboards:/var/lib/grafana/dashboards:ro
    networks:
      - infra-net
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.grafana.rule=Host(``grafana.$Domain``)"
      - "traefik.http.routers.grafana.entrypoints=websecure"
      - "traefik.http.routers.grafana.tls=true"
      - "traefik.http.routers.grafana.tls.certresolver=step-ca"
      - "traefik.http.routers.grafana.middlewares=rate-limit@file"
      - "traefik.http.services.grafana.loadbalancer.server.port=3000"

volumes:
  prometheus_data:
  grafana_data:

networks:
  infra-net:
    external: true
"@

    New-SafeFile (Join-Path $InfraDir "observabilidade/prometheus-grafana/.env") "prometheus-grafana .env" @"
GRAFANA_ADMIN_USER=mekylei
GRAFANA_ADMIN_PASSWORD=TROCAR_SENHA_FORTE
"@

    New-SafeFile (Join-Path $InfraDir "observabilidade/prometheus-grafana/prometheus/prometheus.yml") "prometheus.yml" @"
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - /etc/prometheus/rules/*.yml

scrape_configs:
  - job_name: transaction-processing-api
    metrics_path: /actuator/prometheus
    scheme: http
    oauth2:
      client_id: transaction-api-prometheus
      client_secret_file: /etc/prometheus/secrets/keycloak-client-secret
      token_url: https://keycloak.$Domain/realms/bancario/protocol/openid-connect/token
      tls_config:
        ca_file: /etc/prometheus/certs/root_ca.crt
    static_configs:
      - targets:
          - 192.168.0.105:8080
        labels:
          application: transaction-processing-api
          environment: homelab
"@

    New-SafeFile (Join-Path $InfraDir "observabilidade/prometheus-grafana/prometheus/rules/transaction-processing-slo.yml") "transaction-processing-slo.yml" @'
groups:
  - name: transaction-processing-pix-slo
    rules:
      - alert: PIXTaxaFalhaAlta
        expr: |
          100 * sum(increase(transacao_processada_total{tipo="PIX",status="FALHOU"}[5m]))
          /
          clamp_min(sum(increase(transacao_processada_total{tipo="PIX",status=~"COMPLETADA|FALHOU"}[5m])), 1) > 2
        for: 5m
        labels:
          severity: warning
          service: transaction-processing-api
        annotations:
          summary: "Taxa de falha PIX acima de 2%"
          description: "Verificar regras de negocio, saldo/limite, integracoes externas e logs correlacionados pelo idCorrelacao."
      - alert: PIXLatenciaP95Alta
        expr: |
          histogram_quantile(0.95, sum(rate(transacao_duracao_seconds_bucket{tipo="PIX"}[5m])) by (le)) > 8
        for: 5m
        labels:
          severity: critical
          service: transaction-processing-api
'@

    New-SafeFile (Join-Path $InfraDir "observabilidade/prometheus-grafana/prometheus/secrets/keycloak-client-secret") "keycloak-client-secret" "" -NoNewline | Out-Null
    New-SafeFile (Join-Path $InfraDir "observabilidade/prometheus-grafana/grafana/provisioning/datasources/prometheus.yml") "grafana datasource" @'
apiVersion: 1

datasources:
  - name: Prometheus Homelab
    uid: prometheus-homelab
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: true
'@
    New-SafeFile (Join-Path $InfraDir "observabilidade/prometheus-grafana/grafana/provisioning/dashboards/transaction-processing.yml") "grafana dashboard provider" @'
apiVersion: 1

providers:
  - name: transaction-processing-api
    orgId: 1
    folder: Transaction Processing API
    type: file
    disableDeletion: false
    editable: true
    options:
      path: /var/lib/grafana/dashboards
'@
    New-SafeFile (Join-Path $InfraDir "observabilidade/prometheus-grafana/grafana/dashboards/transacao-slo-dashboard.json") "grafana dashboard json" @'
{
  "title": "Transaction Processing API - SLO PIX",
  "uid": "transaction-processing-pix-slo",
  "schemaVersion": 41,
  "version": 1,
  "refresh": "30s",
  "panels": [
    {
      "id": 1,
      "title": "Transacoes PIX Iniciadas Por Minuto",
      "type": "timeseries",
      "targets": [
        {
          "expr": "sum(rate(transacao_criada_total{tipo=\"PIX\"}[$__rate_interval])) * 60",
          "legendFormat": "PIX iniciadas/min"
        }
      ]
    },
    {
      "id": 2,
      "title": "Taxa de Sucesso e Falha PIX",
      "type": "timeseries",
      "targets": [
        {
          "expr": "100 * sum(rate(transacao_processada_total{tipo=\"PIX\",status=\"COMPLETADA\"}[$__rate_interval])) / clamp_min(sum(rate(transacao_processada_total{tipo=\"PIX\",status=~\"COMPLETADA|FALHOU\"}[$__rate_interval])), 0.000001)"
        }
      ]
    }
  ]
}
'@

    New-SafeFile (Join-Path $InfraDir "infra-up.ps1") "infra-up.ps1" @"
`$ErrorActionPreference = "Stop"
`$InfraDir = Split-Path -Parent `$MyInvocation.MyCommand.Path
Write-Host "AVISO: Kafka requer certificados gerados previamente: `$InfraDir/kafka/generate-kafka-certs.ps1"
foreach (`$dir in @("step-ca","traefik","jaeger","kafka","keycloak","gitlab","gitlab-runner","observabilidade/prometheus-grafana","arcane")) {
    Write-Host "==> `$dir"
    Push-Location (Join-Path `$InfraDir `$dir)
    docker compose up -d
    Pop-Location
}
docker ps --format "table {{.Names}}`t{{.Status}}"
"@

    New-SafeFile (Join-Path $InfraDir "infra-down.ps1") "infra-down.ps1" @"
`$ErrorActionPreference = "Stop"
`$InfraDir = Split-Path -Parent `$MyInvocation.MyCommand.Path
foreach (`$dir in @("arcane","observabilidade/prometheus-grafana","gitlab-runner","gitlab","keycloak","kafka","jaeger","traefik","step-ca")) {
    Write-Host "==> `$dir"
    Push-Location (Join-Path `$InfraDir `$dir)
    docker compose down
    Pop-Location
}
"@

    New-SafeFile (Join-Path $InfraDir "gitlab/backup.ps1") "gitlab backup.ps1" @"
`$ErrorActionPreference = "Stop"
`$BackupDir = Join-Path "$InfraDir" "gitlab/data/backups"
`$running = docker inspect -f "{{.State.Running}}" gitlab 2>`$null
if (`$running -ne "true") { throw "Container 'gitlab' nao esta rodando." }
docker exec gitlab gitlab-backup create STRATEGY=copy BACKUP=scheduled
`$backups = Get-ChildItem `$BackupDir -Filter "*_gitlab_backup.tar" -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending
`$backups | Select-Object -Skip 7 | Remove-Item -Force
`$backups | Format-Table Name,Length,LastWriteTime
"@

    New-SafeFile (Join-Path $InfraDir "check-certs.ps1") "check-certs.ps1" @"
param([int]`$WarnDays = 14)
`$Domains = @("gitlab.$Domain","registry.$Domain","traefik.$Domain","arcane.$Domain","keycloak.$Domain","prometheus.$Domain","grafana.$Domain","jaeger.$Domain","otlp-jaeger.$Domain")
`$exitCode = 0
foreach (`$domain in `$Domains) {
    `$raw = "" | openssl s_client -connect "`$domain`:443" -servername `$domain 2>`$null | openssl x509 -noout -enddate 2>`$null
    if (-not `$raw) { Write-Host "[ERRO]  `$domain - nao foi possivel obter o certificado"; `$exitCode = 2; continue }
    `$expiry = [DateTime]::Parse((`$raw -replace "^notAfter=", ""))
    `$days = [int](`$expiry - (Get-Date)).TotalDays
    if (`$days -lt 0) { Write-Host "[EXPIRADO] `$domain - expirou em `$expiry"; `$exitCode = 2 }
    elseif (`$days -lt `$WarnDays) { Write-Host "[AVISO]  `$domain - expira em `$days dias"; if (`$exitCode -lt 1) { `$exitCode = 1 } }
    else { Write-Host "[OK]     `$domain - expira em `$days dias" }
}
exit `$exitCode
"@

    Write-Ok "Configuracoes geradas."
}

function Validate-Host {
    Write-Section "Validacao do Host"
    if ($SkipHostValidation) {
        Write-Warn "Validacao do host ignorada por parametro."
        return
    }

    $ipFound = Get-NetIPAddress -ErrorAction SilentlyContinue | Where-Object { $_.IPAddress -eq $HostIP }
    if ($ipFound) {
        Write-Ok "IP do host confirmado: $HostIP"
    } else {
        Write-Warn "IP $HostIP nao encontrado nas interfaces de rede."
    }

    try {
        $dns = Resolve-DnsName "gitlab.$Domain" -ErrorAction Stop | Where-Object { $_.IPAddress } | Select-Object -First 1 -ExpandProperty IPAddress
        if ($dns -eq $HostIP) { Write-Ok "DNS wildcard *.$Domain -> $HostIP funcionando." }
        else { Write-Warn "DNS gitlab.$Domain resolveu '$dns', esperado '$HostIP'." }
    } catch {
        Write-Warn "DNS gitlab.$Domain nao resolveu localmente."
    }
}

function Show-StartInstructions {
    Write-Section "Instrucoes para Subir a Infraestrutura"
    Write-Host "A infraestrutura foi configurada. Para subir os servicos no Windows:"
    Write-Host ""
    Write-Host "  PowerShell -ExecutionPolicy Bypass -File `"$InfraDir\infra-up.ps1`""
    Write-Host ""
    Write-Host "Acoes manuais principais:"
    Write-Host "  1. Crie a senha do step-ca em: $InfraDir\step-ca\data\secrets\password.txt"
    Write-Host "  2. Exporte root_ca.crt do step-ca e instale em Certificados > Autoridades Raiz Confiaveis."
    Write-Host "  3. Ajuste senhas em kafka\.env, keycloak\.env e observabilidade\prometheus-grafana\.env."
    Write-Host "  4. Gere certificados Kafka com: PowerShell -File `"$InfraDir\kafka\generate-kafka-certs.ps1`""
    Write-Host "  5. Preencha o secret do Prometheus em: $InfraDir\observabilidade\prometheus-grafana\prometheus\secrets\keycloak-client-secret"
}

function Main {
    Write-Host ""
    Write-Host "rebuild-infra.ps1 - ArchLab Homelab Windows 11+"
    Write-Host ""
    Write-Host "  Host IP  : $HostIP"
    Write-Host "  Dominio  : $Domain"
    Write-Host "  Infra dir: $InfraDir"
    Write-Host "  Rede     : $DockerNetwork ($DockerNetworkSubnet)"
    Write-Host ""
    Write-Warn "Este script gera arquivos de configuracao e nao sobe containers automaticamente."
    Write-Warn "Arquivos existentes nao serao sobrescritos."

    Check-Dependencies
    Setup-Structure
    Setup-DockerNetwork
    Generate-Configs
    Validate-Host
    Show-StartInstructions

    Write-Host ""
    Write-Ok "Script concluido. Siga as instrucoes acima para subir a infra."
}

Main
