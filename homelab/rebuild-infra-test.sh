#!/usr/bin/env bash
# =============================================================================
# rebuild-infra-test.sh — Testa rebuild-infra.sh em container Ubuntu isolado
# =============================================================================
# USO:
#   # Subir o container:
#   docker run -it --rm \
#     -v /home/dell/workspace/scripts:/home/dell/workspace/scripts \
#     ubuntu:24.04 bash
#
#   # Dentro do container:
#   apt-get update -q && apt-get install -y curl openssl dnsutils apache2-utils iproute2
#   bash /home/dell/workspace/scripts/infraestrutura/rebuild-infra-test.sh
# =============================================================================

set -euo pipefail

readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m'

pass()    { echo -e "${GREEN}[PASS]${NC} $*"; }
fail()    { echo -e "${RED}[FAIL]${NC} $*"; FAILURES=$((FAILURES + 1)); }
info()    { echo -e "${BLUE}[INFO]${NC} $*"; }
section() { echo -e "\n${BLUE}--- $* ---${NC}"; }

FAILURES=0
TEST_WORKSPACE="/tmp/archlab-test"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ORIGINAL_SCRIPT="${SCRIPT_DIR}/rebuild-infra.sh"
PATCHED_SCRIPT="/tmp/rebuild-infra-patched.sh"
TEST_INFRA="${TEST_WORKSPACE}/infra"

# =============================================================================
# PASSO 1 — Verificar pré-requisitos do ambiente de teste
# =============================================================================
section "Verificando pré-requisitos"

for dep in curl openssl; do
    if ! command -v "${dep}" &>/dev/null; then
        echo -e "${RED}[ERRO]${NC} '${dep}' não encontrado. Execute:"
        echo "  apt-get install -y curl openssl dnsutils apache2-utils iproute2"
        exit 1
    fi
done

if [[ ! -f "${ORIGINAL_SCRIPT}" ]]; then
    echo -e "${RED}[ERRO]${NC} Script original não encontrado: ${ORIGINAL_SCRIPT}"
    exit 1
fi

info "Script original: ${ORIGINAL_SCRIPT}"

# =============================================================================
# PASSO 2 — Criar cópia patcheada com WORKSPACE redirecionado para /tmp
# =============================================================================
section "Patcheando script (WORKSPACE → ${TEST_WORKSPACE})"

rm -rf "${TEST_WORKSPACE}"
mkdir -p "${TEST_WORKSPACE}"

sed \
    -e "s|readonly WORKSPACE=.*|readonly WORKSPACE=\"${TEST_WORKSPACE}\"|" \
    "${ORIGINAL_SCRIPT}" > "${PATCHED_SCRIPT}"

chmod +x "${PATCHED_SCRIPT}"
info "Script patcheado: ${PATCHED_SCRIPT}"

# =============================================================================
# PASSO 3 — Definir mocks exportados (herdados pelo processo filho via bash)
# =============================================================================
section "Registrando mocks (docker, sudo, ip)"

# Mock: docker
# - 'docker network inspect' retorna 1 → simula rede inexistente, forçando criação
# - demais comandos retornam 0 com log
docker() {
    local cmd="${1:-}"
    local sub="${2:-}"
    case "${cmd}" in
        version)
            echo "MOCK-29.0.0"
            ;;
        compose)
            case "${sub}" in
                version) echo "MOCK-2.27.0" ;;
                *)        echo "[MOCK-DOCKER] docker compose ${*:2}" ;;
            esac
            ;;
        ps)
            echo "[MOCK-DOCKER] docker ps (sem containers reais)"
            ;;
        network)
            if [[ "${sub}" == "inspect" ]]; then
                return 1  # rede não existe → branch de criação
            fi
            echo "[MOCK-DOCKER] docker network ${*:2}"
            ;;
        *)
            echo "[MOCK-DOCKER] docker $*"
            ;;
    esac
    return 0
}
export -f docker

# Mock: sudo — executa chown sem privilégio (funciona como root no container)
sudo() {
    local subcmd="${1:-}"
    shift || true
    case "${subcmd}" in
        chown)      command chown "$@" 2>/dev/null || true ;;
        *)          echo "[MOCK-SUDO] sudo ${subcmd} $*" ;;
    esac
    return 0
}
export -f sudo

# Mock: ip — evita falha no validate_host (container não tem 192.168.0.102)
ip() {
    if [[ "${1:-}" == "addr" ]]; then
        printf "1: lo: <LOOPBACK>\n    inet 127.0.0.1/8 scope host lo\n"
        return 0
    fi
    command ip "$@"
}
export -f ip

info "Mocks registrados."

# =============================================================================
# PASSO 4 — Executar o script patcheado
# =============================================================================
section "Executando script"
echo ""

bash "${PATCHED_SCRIPT}" || {
    fail "Script encerrou com código de erro $?"
    exit 1
}

echo ""
info "Execução concluída. Iniciando validações..."

# =============================================================================
# PASSO 5 — Validações
# =============================================================================

check_file() {
    local f="$1" label="${2:-$1}"
    [[ -f "${f}" ]] && pass "${label}" || fail "Arquivo ausente: ${f#${TEST_WORKSPACE}/}"
}

check_absent() {
    local p="$1" label="${2:-$1}"
    [[ ! -e "${p}" ]] && pass "Ausente: ${label}" || fail "Não deveria existir: ${p#${TEST_WORKSPACE}/}"
}

check_dir() {
    local d="$1"
    [[ -d "${d}" ]] && pass "Dir: ${d#${TEST_WORKSPACE}/}" || fail "Dir ausente: ${d#${TEST_WORKSPACE}/}"
}

check_content() {
    local f="$1" pattern="$2" label="${3:-${pattern}}"
    if grep -q "${pattern}" "${f}" 2>/dev/null; then
        pass "Conteúdo: ${label}"
    else
        fail "Conteúdo ausente — '${pattern}' em ${f#${TEST_WORKSPACE}/}"
    fi
}

check_perm() {
    local f="$1" expected="$2"
    local actual
    actual=$(stat -c '%a' "${f}" 2>/dev/null || echo "???")
    [[ "${actual}" == "${expected}" ]] \
        && pass "Permissão ${expected}: ${f#${TEST_WORKSPACE}/}" \
        || fail "Permissão incorreta em ${f#${TEST_WORKSPACE}/}: esperado ${expected}, obtido ${actual}"
}

section "Diretórios"
check_dir "${TEST_INFRA}/traefik/config/dynamic"
check_dir "${TEST_INFRA}/traefik/certs"
check_dir "${TEST_INFRA}/traefik/logs"
check_dir "${TEST_INFRA}/step-ca/data/secrets"
check_dir "${TEST_INFRA}/step-ca/data/certs"
check_dir "${TEST_INFRA}/step-ca/data/config"
check_dir "${TEST_INFRA}/step-ca/data/db"
check_dir "${TEST_INFRA}/step-ca/data/templates"
check_dir "${TEST_INFRA}/gitlab/config"
check_dir "${TEST_INFRA}/gitlab/data/backups"
check_dir "${TEST_INFRA}/gitlab/logs"
check_dir "${TEST_INFRA}/gitlab-runner/config"
check_dir "${TEST_INFRA}/arcane"
check_dir "${TEST_INFRA}/jaeger"
check_dir "${TEST_INFRA}/kafka/certs"
check_dir "${TEST_INFRA}/keycloak/keycloak/import"
check_dir "${TEST_INFRA}/keycloak/keycloak/themes"
check_dir "${TEST_INFRA}/keycloak/postgres/data"
check_dir "${TEST_INFRA}/observabilidade/prometheus-grafana/grafana/dashboards"
check_dir "${TEST_INFRA}/observabilidade/prometheus-grafana/grafana/provisioning/datasources"
check_dir "${TEST_INFRA}/observabilidade/prometheus-grafana/grafana/provisioning/dashboards"
check_dir "${TEST_INFRA}/observabilidade/prometheus-grafana/prometheus/rules"
check_dir "${TEST_INFRA}/observabilidade/prometheus-grafana/prometheus/secrets"

section "Arquivos gerados"
check_file "${TEST_INFRA}/step-ca/docker-compose.yml"                "step-ca/docker-compose.yml"
check_file "${TEST_INFRA}/traefik/docker-compose.yml"                "traefik/docker-compose.yml"
check_file "${TEST_INFRA}/traefik/config/traefik.yml"                "traefik/config/traefik.yml"
check_file "${TEST_INFRA}/traefik/config/dynamic/dashboard.yml"      "traefik/config/dynamic/dashboard.yml"
check_file "${TEST_INFRA}/traefik/config/dynamic/middlewares.yml"    "traefik/config/dynamic/middlewares.yml"
check_file "${TEST_INFRA}/traefik/certs/acme.json"                   "traefik/certs/acme.json"
check_file "${TEST_INFRA}/gitlab/docker-compose.yml"                 "gitlab/docker-compose.yml"
check_file "${TEST_INFRA}/gitlab-runner/docker-compose.yml"          "gitlab-runner/docker-compose.yml"
check_file "${TEST_INFRA}/arcane/docker-compose.yml"                 "arcane/docker-compose.yml"
check_file "${TEST_INFRA}/infra-up.sh"                               "infra-up.sh"
check_file "${TEST_INFRA}/infra-down.sh"                             "infra-down.sh"
check_file "${TEST_INFRA}/gitlab/backup.sh"                          "gitlab/backup.sh"
check_file "${TEST_INFRA}/check-certs.sh"                            "check-certs.sh"
check_file "${TEST_INFRA}/jaeger/docker-compose.yml"                  "jaeger/docker-compose.yml"
check_file "${TEST_INFRA}/kafka/docker-compose.yml"                   "kafka/docker-compose.yml"
check_file "${TEST_INFRA}/kafka/.env"                                 "kafka/.env"
check_file "${TEST_INFRA}/kafka/kafka_server_jaas.conf"               "kafka/kafka_server_jaas.conf"
check_file "${TEST_INFRA}/kafka/generate-kafka-certs.sh"              "kafka/generate-kafka-certs.sh"
check_file "${TEST_INFRA}/keycloak/docker-compose.yml"                "keycloak/docker-compose.yml"
check_file "${TEST_INFRA}/keycloak/.env"                              "keycloak/.env"
check_file "${TEST_INFRA}/keycloak/keycloak-up.sh"                    "keycloak/keycloak-up.sh"
check_file "${TEST_INFRA}/keycloak/keycloak-down.sh"                  "keycloak/keycloak-down.sh"
check_file "${TEST_INFRA}/observabilidade/prometheus-grafana/docker-compose.yml" "observabilidade/prometheus-grafana/docker-compose.yml"
check_file "${TEST_INFRA}/observabilidade/prometheus-grafana/.env"    "observabilidade/prometheus-grafana/.env"
check_file "${TEST_INFRA}/observabilidade/prometheus-grafana/prometheus/prometheus.yml" "prometheus/prometheus.yml"
check_file "${TEST_INFRA}/observabilidade/prometheus-grafana/prometheus/rules/transaction-processing-slo.yml" "prometheus/rules/transaction-processing-slo.yml"
check_file "${TEST_INFRA}/observabilidade/prometheus-grafana/prometheus/secrets/keycloak-client-secret" "prometheus/secrets/keycloak-client-secret"
check_file "${TEST_INFRA}/observabilidade/prometheus-grafana/grafana/provisioning/datasources/prometheus.yml" "grafana/provisioning/datasources/prometheus.yml"
check_file "${TEST_INFRA}/observabilidade/prometheus-grafana/grafana/provisioning/dashboards/transaction-processing.yml" "grafana/provisioning/dashboards/transaction-processing.yml"
check_file "${TEST_INFRA}/observabilidade/prometheus-grafana/grafana/dashboards/transacao-slo-dashboard.json" "grafana/dashboards/transacao-slo-dashboard.json"

section "Conteúdo dos arquivos"
check_content "${TEST_INFRA}/step-ca/docker-compose.yml"             "smallstep/step-ca"         "imagem step-ca"
check_content "${TEST_INFRA}/step-ca/docker-compose.yml"             "ca.lab.home"               "alias DNS step-ca"
check_content "${TEST_INFRA}/step-ca/docker-compose.yml"             "infra-net"                 "rede infra-net step-ca"
check_content "${TEST_INFRA}/traefik/docker-compose.yml"             "traefik:latest"            "imagem traefik"
check_content "${TEST_INFRA}/traefik/config/traefik.yml"             "ca.lab.home:9000"          "caServer ACME"
check_content "${TEST_INFRA}/traefik/config/traefik.yml"             "step-ca"                   "certResolver"
check_content "${TEST_INFRA}/traefik/config/traefik.yml"             "insecureSkipVerify: false" "TLS verification ativada"
check_content "${TEST_INFRA}/traefik/config/dynamic/dashboard.yml"   "traefik.lab.home"          "router dashboard"
check_content "${TEST_INFRA}/traefik/config/dynamic/dashboard.yml"   "basicAuth"                 "autenticação dashboard"
check_content "${TEST_INFRA}/traefik/config/dynamic/middlewares.yml" "stsSeconds: 31536000"      "HSTS 1 ano"
check_content "${TEST_INFRA}/traefik/config/dynamic/middlewares.yml" "X-Frame-Options"           "X-Frame-Options"
check_content "${TEST_INFRA}/traefik/config/dynamic/middlewares.yml" "Permissions-Policy"        "Permissions-Policy"
check_content "${TEST_INFRA}/gitlab/docker-compose.yml"              "gitlab/gitlab-ce"          "imagem GitLab"
check_content "${TEST_INFRA}/gitlab/docker-compose.yml"              "registry.lab.home"         "registry URL"
check_content "${TEST_INFRA}/gitlab/docker-compose.yml"              "2222:22"                   "porta SSH GitLab"
check_content "${TEST_INFRA}/gitlab/docker-compose.yml"              "X-Forwarded-Proto"         "header proxy GitLab"
check_content "${TEST_INFRA}/gitlab-runner/docker-compose.yml"       "gitlab/gitlab-runner"      "imagem runner"
check_content "${TEST_INFRA}/gitlab-runner/docker-compose.yml"       "SSL_CERT_FILE"             "CA cert runner"
check_content "${TEST_INFRA}/arcane/docker-compose.yml"              "ENCRYPTION_KEY"            "encryption key Arcane"
check_content "${TEST_INFRA}/arcane/docker-compose.yml"              "JWT_SECRET"                "JWT secret Arcane"
check_content "${TEST_INFRA}/arcane/docker-compose.yml"              "arcane-ws"                 "middleware WebSocket Arcane"
check_content "${TEST_INFRA}/infra-up.sh"                            "docker compose up -d"      "comando up"
check_content "${TEST_INFRA}/infra-down.sh"                          "docker compose down"       "comando down"
check_content "${TEST_INFRA}/gitlab/backup.sh"                       "gitlab-backup create"      "comando backup GitLab"
check_content "${TEST_INFRA}/gitlab/backup.sh"                       "STRATEGY=copy"             "estratégia de backup"
check_content "${TEST_INFRA}/check-certs.sh"                         "openssl s_client"          "verificação TLS"
check_content "${TEST_INFRA}/check-certs.sh"                         "gitlab.lab.home"           "domínio GitLab no check-certs"
check_content "${TEST_INFRA}/jaeger/docker-compose.yml"               "jaegertracing/all-in-one"  "imagem jaeger"
check_content "${TEST_INFRA}/jaeger/docker-compose.yml"               "COLLECTOR_OTLP_ENABLED"    "OTLP jaeger"
check_content "${TEST_INFRA}/kafka/.env"                              "SASL_SSL"                  "listener SASL_SSL kafka"
check_content "${TEST_INFRA}/kafka/docker-compose.yml"                "kafkassl"                  "entrypoint kafkassl traefik"
check_content "${TEST_INFRA}/kafka/.env"                              "KAFKA_IMAGE=apache/kafka"  "versão kafka"
check_content "${TEST_INFRA}/kafka/.env"                              "KAFKA_SASL_ENABLED"        "SASL config kafka"
check_content "${TEST_INFRA}/kafka/kafka_server_jaas.conf"            "ScramLoginModule"          "SCRAM auth kafka"
check_content "${TEST_INFRA}/kafka/generate-kafka-certs.sh"           "step certificate create"   "step CLI para kafka"
check_content "${TEST_INFRA}/keycloak/docker-compose.yml"             "keycloak:26.2"             "imagem keycloak"
check_content "${TEST_INFRA}/keycloak/docker-compose.yml"             "keycloak.lab.home"         "hostname keycloak"
check_content "${TEST_INFRA}/keycloak/docker-compose.yml"             "postgres-keycloak"         "postgres keycloak"
check_content "${TEST_INFRA}/keycloak/keycloak-up.sh"                 "docker compose up"         "comando up keycloak"
check_content "${TEST_INFRA}/keycloak/keycloak-down.sh"               "docker compose down"       "comando down keycloak"
check_content "${TEST_INFRA}/observabilidade/prometheus-grafana/docker-compose.yml" "prom/prometheus" "imagem prometheus"
check_content "${TEST_INFRA}/observabilidade/prometheus-grafana/docker-compose.yml" "grafana/grafana" "imagem grafana"
check_content "${TEST_INFRA}/observabilidade/prometheus-grafana/prometheus/prometheus.yml" "transaction-processing-api" "job prometheus"
check_content "${TEST_INFRA}/observabilidade/prometheus-grafana/prometheus/prometheus.yml" "keycloak-client-secret" "oauth2 prometheus"
check_content "${TEST_INFRA}/observabilidade/prometheus-grafana/prometheus/rules/transaction-processing-slo.yml" "PIXTaxaFalhaAlta" "alerta PIX falha"
check_content "${TEST_INFRA}/observabilidade/prometheus-grafana/grafana/provisioning/datasources/prometheus.yml" "prometheus-homelab" "datasource grafana"
check_content "${TEST_INFRA}/observabilidade/prometheus-grafana/grafana/dashboards/transacao-slo-dashboard.json" "transacao_processada_total" "métrica dashboard grafana"
check_content "${TEST_INFRA}/traefik/config/traefik.yml"              "kafkassl"                  "entrypoint kafkassl traefik"
check_content "${TEST_INFRA}/check-certs.sh"                         "keycloak.lab.home"         "domínio Keycloak no check-certs"
check_content "${TEST_INFRA}/check-certs.sh"                         "grafana.lab.home"          "domínio Grafana no check-certs"

section "Permissões"
check_perm "${TEST_INFRA}/traefik/certs/acme.json" "600"
check_perm "${TEST_INFRA}/kafka/generate-kafka-certs.sh" "755"
check_perm "${TEST_INFRA}/keycloak/keycloak-up.sh"       "755"
check_perm "${TEST_INFRA}/keycloak/keycloak-down.sh"     "755"

section "Idempotência (2ª execução não deve sobrescrever)"
echo "# MARCADOR-TESTE-IDEMPOTENCIA" >> "${TEST_INFRA}/step-ca/docker-compose.yml"
bash "${PATCHED_SCRIPT}" > /dev/null 2>&1 || true

if grep -q "MARCADOR-TESTE-IDEMPOTENCIA" "${TEST_INFRA}/step-ca/docker-compose.yml"; then
    pass "Arquivo existente preservado na 2ª execução"
else
    fail "Arquivo existente foi sobrescrito na 2ª execução!"
fi

section "Chaves Arcane são únicas por execução"
# Rodar em área temporária limpa para comparar chaves de duas execuções
TEMP_A="/tmp/archlab-run-a"
TEMP_B="/tmp/archlab-run-b"

for dir in "${TEMP_A}" "${TEMP_B}"; do
    PATCHED_TMP="/tmp/rebuild-infra-$(basename ${dir}).sh"
    sed -e "s|readonly WORKSPACE=.*|readonly WORKSPACE=\"${dir}\"|" "${ORIGINAL_SCRIPT}" > "${PATCHED_TMP}"
    chmod +x "${PATCHED_TMP}"
    bash "${PATCHED_TMP}" > /dev/null 2>&1 || true
done

KEY_A=$(grep "ENCRYPTION_KEY=" "${TEMP_A}/infra/arcane/docker-compose.yml" | head -1)
KEY_B=$(grep "ENCRYPTION_KEY=" "${TEMP_B}/infra/arcane/docker-compose.yml" | head -1)

if [[ "${KEY_A}" != "${KEY_B}" ]]; then
    pass "Chaves Arcane são únicas entre execuções (openssl rand)"
else
    fail "Chaves Arcane idênticas entre execuções diferentes!"
fi

rm -rf "${TEMP_A}" "${TEMP_B}" /tmp/rebuild-infra-archlab-run-*.sh 2>/dev/null || true

# =============================================================================
# PASSO 6 — Resultado final
# =============================================================================
echo ""
echo "════════════════════════════════════════════"
if [[ "${FAILURES}" -eq 0 ]]; then
    echo -e "${GREEN}  TODOS OS TESTES PASSARAM${NC}"
    echo "  Arquivos gerados inspecionáveis em: ${TEST_WORKSPACE}"
    EXIT_CODE=0
else
    echo -e "${RED}  ${FAILURES} TESTE(S) FALHARAM${NC}"
    EXIT_CODE=1
fi
echo "════════════════════════════════════════════"
echo ""

rm -f "${PATCHED_SCRIPT}"
exit "${EXIT_CODE}"
