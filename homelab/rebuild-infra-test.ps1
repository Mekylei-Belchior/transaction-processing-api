#requires -Version 5.1
<#
rebuild-infra-test.ps1 - Testa rebuild-infra.ps1 em area temporaria isolada.

O teste nao cria rede Docker, nao executa containers e nao altera configuracoes
do host. Ele valida sintaxe, arquivos gerados, conteudo essencial e
idempotencia.

Uso no Windows 11+:
  Set-ExecutionPolicy -Scope Process Bypass
  .\rebuild-infra-test.ps1

Uso no Linux com Docker:
    chmod +x rebuild-infra-test.ps1
    ./rebuild-infra-test.ps1

docker run --rm \
  -v /home/dell/workspace/scripts/infraestrutura:/work:ro \
  -v /tmp:/tmp \
  mcr.microsoft.com/powershell:latest \
  pwsh -NoProfile -ExecutionPolicy Bypass \
  -File /work/rebuild-infra-test.ps1 \
  -TestWorkspace /tmp/archlab-win-test
#>

[CmdletBinding()]
param(
    [string]$TestWorkspace = (Join-Path $env:TEMP "archlab-win-test")
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
$Failures = 0
$TempRoot = if ($env:TEMP) { $env:TEMP } else { [System.IO.Path]::GetTempPath() }

function Pass { param([string]$Message) Write-Host "[PASS] $Message" -ForegroundColor Green }
function Fail { param([string]$Message) Write-Host "[FAIL] $Message" -ForegroundColor Red; $script:Failures++ }
function Info { param([string]$Message) Write-Host "[INFO] $Message" -ForegroundColor Cyan }
function Section { param([string]$Title) Write-Host ""; Write-Host "--- $Title ---" -ForegroundColor Cyan }

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$OriginalScript = Join-Path $ScriptDir "rebuild-infra.ps1"
$TestInfra = Join-Path $TestWorkspace "infra"

function Check-File {
    param([string]$Path, [string]$Label)
    if (Test-Path -LiteralPath $Path -PathType Leaf) { Pass $Label } else { Fail "Arquivo ausente: $Path" }
}

function Check-Dir {
    param([string]$Path)
    if (Test-Path -LiteralPath $Path -PathType Container) { Pass "Dir: $Path" } else { Fail "Dir ausente: $Path" }
}

function Check-Content {
    param([string]$Path, [string]$Pattern, [string]$Label)
    if ((Test-Path -LiteralPath $Path) -and (Select-String -LiteralPath $Path -Pattern $Pattern -SimpleMatch -Quiet)) {
        Pass "Conteudo: $Label"
    } else {
        Fail "Conteudo ausente - '$Pattern' em $Path"
    }
}

Section "Verificando pre-requisitos"
if (-not (Test-Path -LiteralPath $OriginalScript -PathType Leaf)) {
    throw "Script Windows nao encontrado: $OriginalScript"
}
Info "Script Windows: $OriginalScript"

Section "Executando geracao isolada"
if (Test-Path -LiteralPath $TestWorkspace) { Remove-Item -LiteralPath $TestWorkspace -Recurse -Force }
New-Item -ItemType Directory -Path $TestWorkspace -Force | Out-Null

& $OriginalScript -Workspace $TestWorkspace -SkipDependencyCheck -SkipDockerNetwork -SkipHostValidation
if (-not $?) { Fail "Script encerrou com erro na primeira execucao" }

Section "Diretorios"
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
    "observabilidade/prometheus-grafana/grafana/provisioning/datasources",
    "observabilidade/prometheus-grafana/grafana/provisioning/dashboards",
    "observabilidade/prometheus-grafana/prometheus/rules",
    "observabilidade/prometheus-grafana/prometheus/secrets"
) | ForEach-Object { Check-Dir (Join-Path $TestInfra $_) }

Section "Arquivos gerados"
@(
    "step-ca/docker-compose.yml",
    "traefik/docker-compose.yml",
    "traefik/config/traefik.yml",
    "traefik/config/dynamic/dashboard.yml",
    "traefik/config/dynamic/middlewares.yml",
    "traefik/certs/acme.json",
    "gitlab/docker-compose.yml",
    "gitlab-runner/docker-compose.yml",
    "arcane/docker-compose.yml",
    "jaeger/docker-compose.yml",
    "kafka/docker-compose.yml",
    "kafka/.env",
    "kafka/kafka_server_jaas.conf",
    "kafka/generate-kafka-certs.ps1",
    "keycloak/docker-compose.yml",
    "keycloak/.env",
    "keycloak/keycloak-up.ps1",
    "keycloak/keycloak-down.ps1",
    "observabilidade/prometheus-grafana/docker-compose.yml",
    "observabilidade/prometheus-grafana/.env",
    "observabilidade/prometheus-grafana/prometheus/prometheus.yml",
    "observabilidade/prometheus-grafana/prometheus/rules/transaction-processing-slo.yml",
    "observabilidade/prometheus-grafana/prometheus/secrets/keycloak-client-secret",
    "observabilidade/prometheus-grafana/grafana/provisioning/datasources/prometheus.yml",
    "observabilidade/prometheus-grafana/grafana/provisioning/dashboards/transaction-processing.yml",
    "observabilidade/prometheus-grafana/grafana/dashboards/transacao-slo-dashboard.json",
    "infra-up.ps1",
    "infra-down.ps1",
    "gitlab/backup.ps1",
    "check-certs.ps1"
) | ForEach-Object { Check-File (Join-Path $TestInfra $_) $_ }

Section "Conteudo dos arquivos"
Check-Content (Join-Path $TestInfra "step-ca/docker-compose.yml") "smallstep/step-ca" "imagem step-ca"
Check-Content (Join-Path $TestInfra "step-ca/docker-compose.yml") "ca.lab.home" "alias DNS step-ca"
Check-Content (Join-Path $TestInfra "traefik/docker-compose.yml") "traefik:latest" "imagem traefik"
Check-Content (Join-Path $TestInfra "traefik/config/traefik.yml") "ca.lab.home:9000" "caServer ACME"
Check-Content (Join-Path $TestInfra "traefik/config/traefik.yml") "kafkassl" "entrypoint kafkassl"
Check-Content (Join-Path $TestInfra "traefik/config/dynamic/dashboard.yml") "basicAuth" "autenticacao dashboard"
Check-Content (Join-Path $TestInfra "traefik/config/dynamic/middlewares.yml") "Permissions-Policy" "Permissions-Policy"
Check-Content (Join-Path $TestInfra "gitlab/docker-compose.yml") "gitlab/gitlab-ce" "imagem GitLab"
Check-Content (Join-Path $TestInfra "gitlab/docker-compose.yml") "2222:22" "porta SSH GitLab"
Check-Content (Join-Path $TestInfra "gitlab-runner/docker-compose.yml") "SSL_CERT_FILE" "CA cert runner"
Check-Content (Join-Path $TestInfra "arcane/docker-compose.yml") "ENCRYPTION_KEY" "encryption key Arcane"
Check-Content (Join-Path $TestInfra "arcane/docker-compose.yml") "JWT_SECRET" "JWT secret Arcane"
Check-Content (Join-Path $TestInfra "jaeger/docker-compose.yml") "jaegertracing/all-in-one" "imagem Jaeger"
Check-Content (Join-Path $TestInfra "kafka/.env") "SASL_SSL" "listener SASL_SSL Kafka"
Check-Content (Join-Path $TestInfra "kafka/docker-compose.yml") "kafkassl" "entrypoint Kafka"
Check-Content (Join-Path $TestInfra "kafka/kafka_server_jaas.conf") "ScramLoginModule" "SCRAM auth Kafka"
Check-Content (Join-Path $TestInfra "kafka/generate-kafka-certs.ps1") "step certificate create" "step CLI Kafka"
Check-Content (Join-Path $TestInfra "keycloak/docker-compose.yml") "keycloak:26.2" "imagem Keycloak"
Check-Content (Join-Path $TestInfra "observabilidade/prometheus-grafana/docker-compose.yml") "prom/prometheus" "imagem Prometheus"
Check-Content (Join-Path $TestInfra "observabilidade/prometheus-grafana/docker-compose.yml") "grafana/grafana" "imagem Grafana"
Check-Content (Join-Path $TestInfra "observabilidade/prometheus-grafana/prometheus/prometheus.yml") "transaction-processing-api" "job Prometheus"
Check-Content (Join-Path $TestInfra "observabilidade/prometheus-grafana/prometheus/rules/transaction-processing-slo.yml") "PIXTaxaFalhaAlta" "alerta PIX"
Check-Content (Join-Path $TestInfra "observabilidade/prometheus-grafana/grafana/provisioning/datasources/prometheus.yml") "prometheus-homelab" "datasource Grafana"
Check-Content (Join-Path $TestInfra "observabilidade/prometheus-grafana/grafana/dashboards/transacao-slo-dashboard.json") "transacao_processada_total" "metrica dashboard"
Check-Content (Join-Path $TestInfra "infra-up.ps1") "docker compose up -d" "script up"
Check-Content (Join-Path $TestInfra "infra-down.ps1") "docker compose down" "script down"
Check-Content (Join-Path $TestInfra "gitlab/backup.ps1") "gitlab-backup create" "backup GitLab"
Check-Content (Join-Path $TestInfra "check-certs.ps1") "openssl s_client" "check TLS"

Section "Idempotencia"
$markerFile = Join-Path $TestInfra "step-ca/docker-compose.yml"
Add-Content -LiteralPath $markerFile -Value "# MARCADOR-TESTE-IDEMPOTENCIA"
& $OriginalScript -Workspace $TestWorkspace -SkipDependencyCheck -SkipDockerNetwork -SkipHostValidation *> $null
if (Select-String -LiteralPath $markerFile -Pattern "MARCADOR-TESTE-IDEMPOTENCIA" -SimpleMatch -Quiet) {
    Pass "Arquivo existente preservado na segunda execucao"
} else {
    Fail "Arquivo existente foi sobrescrito na segunda execucao"
}

Section "Chaves Arcane unicas"
$TempA = Join-Path $TempRoot "archlab-win-a"
$TempB = Join-Path $TempRoot "archlab-win-b"
foreach ($dir in @($TempA, $TempB)) {
    if (Test-Path -LiteralPath $dir) { Remove-Item -LiteralPath $dir -Recurse -Force }
    & $OriginalScript -Workspace $dir -SkipDependencyCheck -SkipDockerNetwork -SkipHostValidation *> $null
}
$keyA = Select-String -LiteralPath (Join-Path $TempA "infra/arcane/docker-compose.yml") -Pattern "ENCRYPTION_KEY=" | Select-Object -First 1 -ExpandProperty Line
$keyB = Select-String -LiteralPath (Join-Path $TempB "infra/arcane/docker-compose.yml") -Pattern "ENCRYPTION_KEY=" | Select-Object -First 1 -ExpandProperty Line
if ($keyA -and $keyB -and $keyA -ne $keyB) { Pass "Chaves Arcane sao unicas entre execucoes" } else { Fail "Chaves Arcane identicas entre execucoes" }
Remove-Item -LiteralPath $TempA, $TempB -Recurse -Force -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "============================================"
if ($Failures -eq 0) {
    Write-Host "  TODOS OS TESTES PASSARAM" -ForegroundColor Green
    Write-Host "  Arquivos gerados inspecionaveis em: $TestWorkspace"
    $exitCode = 0
} else {
    Write-Host "  $Failures TESTE(S) FALHARAM" -ForegroundColor Red
    $exitCode = 1
}
Write-Host "============================================"
exit $exitCode
