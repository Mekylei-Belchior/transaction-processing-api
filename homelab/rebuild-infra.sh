#!/usr/bin/env bash
# =============================================================================
# rebuild-infra.sh — Reconstrução completa da infraestrutura ArchLab
# =============================================================================
# Autor    : Mekylei Belchior
# Ambiente : Dell Vostro 15 5510 | Ubuntu 24.04 LTS | Docker 29+
# Domínio  : lab.home
# Host IP  : 192.168.0.102
#
# ATENÇÃO: Este script é idempotente e seguro por padrão.
#          Ele NÃO sobrescreve arquivos existentes nem derruba containers ativos.
#          Execute com cautela em ambientes com dados já populados.
#
# USO:
#   chmod +x rebuild-infra.sh
#   ./rebuild-infra.sh
# =============================================================================

set -euo pipefail

# =============================================================================
# VARIÁVEIS CONFIGURÁVEIS
# =============================================================================
readonly HOST_IP="192.168.0.102"
readonly DOMAIN="lab.home"
readonly WORKSPACE="/home/dell/workspace"
readonly INFRA_DIR="${WORKSPACE}/infra"
readonly DOCKER_NETWORK="infra-net"
readonly DOCKER_NETWORK_SUBNET="172.20.0.0/24"
readonly INFRA_PROJECT="infra"

# Cores para output
readonly RED='\033[0;31m'
readonly YELLOW='\033[1;33m'
readonly GREEN='\033[0;32m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m' # No Color

# =============================================================================
# FUNÇÕES AUXILIARES
# =============================================================================
log()     { echo -e "${BLUE}[INFO]${NC}  $*"; }
success() { echo -e "${GREEN}[OK]${NC}    $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERRO]${NC}  $*" >&2; }
section() { echo -e "\n${BLUE}════════════════════════════════════════════${NC}"; echo -e "${BLUE}  $*${NC}"; echo -e "${BLUE}════════════════════════════════════════════${NC}"; }

# Verifica se um arquivo já existe antes de criar
safe_create_file() {
    local filepath="$1"
    local description="${2:-arquivo}"
    if [[ -f "${filepath}" ]]; then
        warn "${description} já existe — ignorando: ${filepath}"
        return 1
    fi
    return 0
}

# Verifica se um diretório já existe antes de criar
safe_create_dir() {
    local dirpath="$1"
    if [[ ! -d "${dirpath}" ]]; then
        mkdir -p "${dirpath}"
        success "Diretório criado: ${dirpath}"
    fi
}

# =============================================================================
# FUNÇÃO 1: check_dependencies
# Valida que todas as dependências estão instaladas
# =============================================================================
check_dependencies() {
    section "FASE 0 — Verificação de Dependências"

    local deps=("docker" "docker" "curl" "openssl" "dig" "htpasswd")
    local missing=0

    # Verificar Docker
    if ! command -v docker &>/dev/null; then
        error "Docker não encontrado. Instale Docker CE >= 25."
        missing=1
    else
        local docker_version
        docker_version=$(docker version --format '{{.Client.Version}}' 2>/dev/null || echo "desconhecido")
        success "Docker instalado: v${docker_version}"
    fi

    # Verificar Docker Compose (plugin v2)
    if ! docker compose version &>/dev/null; then
        error "Docker Compose plugin não encontrado. Instale docker-compose-plugin."
        missing=1
    else
        local compose_version
        compose_version=$(docker compose version --short 2>/dev/null || echo "desconhecido")
        success "Docker Compose instalado: v${compose_version}"
    fi

    # Verificar que o usuário pode usar Docker sem sudo
    if ! docker ps &>/dev/null; then
        warn "Usuário atual não está no grupo 'docker'. Alguns comandos podem exigir sudo."
        warn "Execute: sudo usermod -aG docker \$USER && newgrp docker"
    else
        success "Usuário pode executar Docker sem sudo."
    fi

    # Verificar curl
    if ! command -v curl &>/dev/null; then
        error "curl não encontrado. Instale: sudo apt install -y curl"
        missing=1
    else
        success "curl instalado."
    fi

    # Verificar openssl
    if ! command -v openssl &>/dev/null; then
        error "openssl não encontrado. Instale: sudo apt install -y openssl"
        missing=1
    else
        success "openssl instalado."
    fi

    # Verificar htpasswd (para gerar hash de senha do dashboard Traefik)
    if ! command -v htpasswd &>/dev/null; then
        warn "htpasswd não encontrado (apache2-utils). Necessário para gerar hash do Traefik dashboard."
        warn "Execute: sudo apt install -y apache2-utils"
    else
        success "htpasswd instalado."
    fi

    if [[ "${missing}" -ne 0 ]]; then
        error "Dependências ausentes. Resolva os erros acima antes de continuar."
        exit 1
    fi

    success "Todas as dependências verificadas."
}

# =============================================================================
# FUNÇÃO 2: setup_structure
# Cria a estrutura de diretórios sem sobrescrever o que já existe
# =============================================================================
setup_structure() {
    section "FASE 0 — Estrutura de Diretórios"

    log "Criando estrutura de diretórios da infra em: ${INFRA_DIR}"

    # Traefik
    safe_create_dir "${INFRA_DIR}/traefik/config/dynamic"
    safe_create_dir "${INFRA_DIR}/traefik/certs"
    safe_create_dir "${INFRA_DIR}/traefik/logs"

    # step-ca
    safe_create_dir "${INFRA_DIR}/step-ca/data/secrets"
    safe_create_dir "${INFRA_DIR}/step-ca/data/certs"
    safe_create_dir "${INFRA_DIR}/step-ca/data/config"
    safe_create_dir "${INFRA_DIR}/step-ca/data/db"
    safe_create_dir "${INFRA_DIR}/step-ca/data/templates"

    # GitLab
    safe_create_dir "${INFRA_DIR}/gitlab/config"
    safe_create_dir "${INFRA_DIR}/gitlab/data/backups"
    safe_create_dir "${INFRA_DIR}/gitlab/logs"

    # GitLab Runner
    safe_create_dir "${INFRA_DIR}/gitlab-runner/config"

    # Arcane (sem diretório de config — usa volume nomeado)
    safe_create_dir "${INFRA_DIR}/arcane"

    # Jaeger
    safe_create_dir "${INFRA_DIR}/jaeger"

    # Kafka
    safe_create_dir "${INFRA_DIR}/kafka/certs"

    # Keycloak
    safe_create_dir "${INFRA_DIR}/keycloak/keycloak/import"
    safe_create_dir "${INFRA_DIR}/keycloak/keycloak/themes"
    safe_create_dir "${INFRA_DIR}/keycloak/postgres/data"

    # Observabilidade
    safe_create_dir "${INFRA_DIR}/observabilidade/prometheus-grafana/grafana/dashboards"
    safe_create_dir "${INFRA_DIR}/observabilidade/prometheus-grafana/grafana/provisioning/dashboards"
    safe_create_dir "${INFRA_DIR}/observabilidade/prometheus-grafana/grafana/provisioning/datasources"
    safe_create_dir "${INFRA_DIR}/observabilidade/prometheus-grafana/prometheus/rules"
    safe_create_dir "${INFRA_DIR}/observabilidade/prometheus-grafana/prometheus/secrets"

    # Scripts da infra (diretório atual do script)
    safe_create_dir "${WORKSPACE}/scripts/infraestrutura"

    # Ajustar permissões
    local owner
    owner=$(stat -c '%U:%G' "${WORKSPACE}" 2>/dev/null || echo "dell:dell")
    sudo chown -R "${owner}" "${INFRA_DIR}" 2>/dev/null || true

    success "Estrutura de diretórios pronta."
}

# =============================================================================
# FUNÇÃO 3: setup_docker_network
# Cria a rede Docker compartilhada (infra-net)
# =============================================================================
setup_docker_network() {
    section "FASE 0 — Rede Docker"

    if docker network inspect "${DOCKER_NETWORK}" &>/dev/null; then
        local existing_subnet
        existing_subnet=$(docker network inspect "${DOCKER_NETWORK}" \
            --format '{{range .IPAM.Config}}{{.Subnet}}{{end}}' 2>/dev/null || echo "desconhecida")
        warn "Rede '${DOCKER_NETWORK}' já existe (subnet: ${existing_subnet}). Ignorando criação."
    else
        log "Criando rede Docker: ${DOCKER_NETWORK} (subnet: ${DOCKER_NETWORK_SUBNET})"
        docker network create \
            --driver bridge \
            --subnet "${DOCKER_NETWORK_SUBNET}" \
            "${DOCKER_NETWORK}"
        success "Rede '${DOCKER_NETWORK}' criada."
    fi
}

# =============================================================================
# FUNÇÃO 4: generate_configs
# Gera todos os arquivos de configuração sem sobrescrever os existentes
# =============================================================================
generate_configs() {
    section "Geração de Configurações"

    _generate_stepca_compose
    _generate_traefik_compose
    _generate_traefik_static_config
    _generate_traefik_dashboard
    _generate_traefik_middlewares
    _generate_acme_json
    _generate_gitlab_compose
    _generate_gitlab_runner_compose
    _generate_arcane_compose
    _generate_jaeger_compose
    _generate_kafka_compose
    _generate_kafka_env
    _generate_kafka_jaas
    _generate_kafka_cert_script
    _generate_keycloak_compose
    _generate_keycloak_env
    _generate_keycloak_up
    _generate_keycloak_down
    _generate_observability_compose
    _generate_observability_env
    _generate_prometheus_config
    _generate_prometheus_rules
    _generate_prometheus_keycloak_secret_placeholder
    _generate_grafana_datasource
    _generate_grafana_dashboard_provider
    _generate_grafana_dashboard_json
    _generate_infra_scripts

    success "Configurações geradas."
}

# -----------------------------------------------------------------------------
# step-ca — docker-compose.yml
# -----------------------------------------------------------------------------
_generate_stepca_compose() {
    local target="${INFRA_DIR}/step-ca/docker-compose.yml"

    if ! safe_create_file "${target}" "docker-compose.yml step-ca"; then
        return
    fi

    log "Gerando: ${target}"
    cat > "${target}" <<'EOF'
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
      - DOCKER_STEPCA_INIT_DNS_NAMES=step-ca,ca.lab.home,192.168.0.102
      - DOCKER_STEPCA_INIT_REMOTE_MANAGEMENT=true
      - DOCKER_STEPCA_INIT_ACME=true
      - DOCKER_STEPCA_INIT_PASSWORD_FILE=/home/step/secrets/password.txt
    networks:
      infra-net:
        aliases:
          - ca.lab.home

networks:
  infra-net:
    external: true
EOF

    success "step-ca docker-compose.yml gerado."
    _warn_stepca_password
}

_warn_stepca_password() {
    local password_file="${INFRA_DIR}/step-ca/data/secrets/password.txt"
    if [[ ! -f "${password_file}" ]]; then
        warn "=========================================================="
        warn "AÇÃO MANUAL NECESSÁRIA — step-ca password"
        warn "Crie o arquivo de senha ANTES de subir o step-ca:"
        warn "  echo 'SUA_SENHA_FORTE' > ${password_file}"
        warn "  chmod 600 ${password_file}"
        warn ""
        warn "Senha original do lab: ArchLabCA@2026-Homelab-Secure"
        warn "  (altere se estiver reconstruindo do zero)"
        warn "=========================================================="
    else
        success "Arquivo de senha do step-ca já existe."
    fi
}

# -----------------------------------------------------------------------------
# Traefik — docker-compose.yml
# -----------------------------------------------------------------------------
_generate_traefik_compose() {
    local target="${INFRA_DIR}/traefik/docker-compose.yml"

    if ! safe_create_file "${target}" "docker-compose.yml traefik"; then
        return
    fi

    log "Gerando: ${target}"
    cat > "${target}" <<EOF
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
      - ${INFRA_DIR}/step-ca/root_ca.crt:/etc/ssl/certs/archlab-ca.crt:ro
    environment:
      - LEGO_CA_CERTIFICATES=/etc/ssl/certs/archlab-ca.crt
    networks:
      - infra-net
    labels:
      - "traefik.enable=false"

networks:
  infra-net:
    external: true
EOF

    success "traefik docker-compose.yml gerado."
}

# -----------------------------------------------------------------------------
# Traefik — config/traefik.yml (config estática)
# -----------------------------------------------------------------------------
_generate_traefik_static_config() {
    local target="${INFRA_DIR}/traefik/config/traefik.yml"

    if ! safe_create_file "${target}" "traefik.yml (config estática)"; then
        return
    fi

    log "Gerando: ${target}"
    cat > "${target}" <<'EOF'
# Traefik v3 — Configuração estática

global:
  checkNewVersion: false
  sendAnonymousUsage: false

api:
  dashboard: true
  insecure: false   # Dashboard apenas via HTTPS com autenticação

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
      email: admin@lab.home
      storage: /certs/acme.json
      caServer: https://ca.lab.home:9000/acme/acme/directory
      tlsChallenge: {}

serversTransport:
  insecureSkipVerify: false
EOF

    success "traefik/config/traefik.yml gerado."
}

# -----------------------------------------------------------------------------
# Traefik — config/dynamic/dashboard.yml
# -----------------------------------------------------------------------------
_generate_traefik_dashboard() {
    local target="${INFRA_DIR}/traefik/config/dynamic/dashboard.yml"

    if ! safe_create_file "${target}" "dashboard.yml (Traefik dynamic)"; then
        return
    fi

    log "Gerando: ${target}"

    # Gerar hash bcrypt da senha do dashboard via htpasswd
    # Hash pré-gerado para "admin" (senha: ver archlab.md Fase 3)
    # Para regerar: htpasswd -nbB admin 'NOVA_SENHA' | sed 's/\$/\$\$/g'
    local DASHBOARD_HASH='admin:$apr1$exhDrsJp$q/MU0DZp.pbghpUwYA/6d0'

    cat > "${target}" <<EOF
http:
  routers:
    traefik-dashboard:
      rule: "Host(\`traefik.lab.home\`)"
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
          - "${DASHBOARD_HASH}"
EOF

    success "traefik/config/dynamic/dashboard.yml gerado."
    warn "Senha do dashboard Traefik: ver archlab.md (Fase 3) ou regenere com htpasswd."
}

# -----------------------------------------------------------------------------
# Traefik — config/dynamic/middlewares.yml
# -----------------------------------------------------------------------------
_generate_traefik_middlewares() {
    local target="${INFRA_DIR}/traefik/config/dynamic/middlewares.yml"

    if ! safe_create_file "${target}" "middlewares.yml (Traefik dynamic)"; then
        return
    fi

    log "Gerando: ${target}"
    cat > "${target}" <<'EOF'
http:
  middlewares:

    rate-limit:
      rateLimit:
        average: 100   # requisições por segundo (média)
        burst: 50      # rajada máxima permitida

    secure-headers:
      headers:
        # HSTS — força HTTPS por 1 ano em todos os subdomínios
        stsSeconds: 31536000
        stsIncludeSubdomains: true
        stsPreload: true
        forceSTSHeader: true

        # Impede sniffing de content-type pelo browser
        contentTypeNosniff: true

        # Proteção contra XSS em browsers legados
        browserXssFilter: true

        # Impede que a página seja embutida em iframe (clickjacking)
        frameDeny: true

        # Controle explícito de origens para iframes (reforço)
        customResponseHeaders:
          X-Frame-Options: "DENY"
          Referrer-Policy: "strict-origin-when-cross-origin"
          Permissions-Policy: "camera=(), microphone=(), geolocation=()"
          X-Content-Type-Options: "nosniff"
EOF

    success "traefik/config/dynamic/middlewares.yml gerado."
}

# -----------------------------------------------------------------------------
# Traefik — certs/acme.json
# -----------------------------------------------------------------------------
_generate_acme_json() {
    local target="${INFRA_DIR}/traefik/certs/acme.json"

    if [[ -f "${target}" ]]; then
        local size
        size=$(stat -c '%s' "${target}" 2>/dev/null || echo "0")
        if [[ "${size}" -gt 10 ]]; then
            warn "acme.json já existe e contém dados (${size} bytes). Preservando certificados existentes."
            return
        fi
    fi

    log "Criando acme.json vazio com permissão 600."
    touch "${target}"
    chmod 600 "${target}"
    success "traefik/certs/acme.json criado (chmod 600)."
}

# -----------------------------------------------------------------------------
# GitLab — docker-compose.yml
# -----------------------------------------------------------------------------
_generate_gitlab_compose() {
    local target="${INFRA_DIR}/gitlab/docker-compose.yml"

    if ! safe_create_file "${target}" "docker-compose.yml gitlab"; then
        return
    fi

    log "Gerando: ${target}"
    cat > "${target}" <<'EOF'
services:
  gitlab:
    image: gitlab/gitlab-ce:latest
    container_name: gitlab
    restart: unless-stopped
    environment:
      GITLAB_OMNIBUS_CONFIG: |
        external_url 'https://gitlab.lab.home'
        nginx['listen_port'] = 80
        nginx['listen_https'] = false
        nginx['proxy_set_headers'] = {
          "X-Forwarded-Proto" => "https",
          "X-Forwarded-Ssl" => "on"
        }

        registry_external_url 'https://registry.lab.home'
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
      # GitLab web
      - "traefik.enable=true"
      - "traefik.http.routers.gitlab.rule=Host(`gitlab.lab.home`)"
      - "traefik.http.routers.gitlab.entrypoints=websecure"
      - "traefik.http.routers.gitlab.tls=true"
      - "traefik.http.routers.gitlab.tls.certresolver=step-ca"
      - "traefik.http.routers.gitlab.service=gitlab-svc"
      - "traefik.http.routers.gitlab.middlewares=secure-headers@file,rate-limit@file"
      - "traefik.http.services.gitlab-svc.loadbalancer.server.port=80"
      # Registry
      - "traefik.http.routers.registry.rule=Host(`registry.lab.home`)"
      - "traefik.http.routers.registry.entrypoints=websecure"
      - "traefik.http.routers.registry.tls=true"
      - "traefik.http.routers.registry.tls.certresolver=step-ca"
      - "traefik.http.routers.registry.service=registry-svc"
      - "traefik.http.routers.registry.middlewares=secure-headers@file,rate-limit@file"
      - "traefik.http.services.registry-svc.loadbalancer.server.port=5050"

networks:
  infra-net:
    external: true
EOF

    success "gitlab docker-compose.yml gerado."

    warn "=========================================================="
    warn "AÇÃO MANUAL NECESSÁRIA — GitLab (primeira execução)"
    warn "Após subir o container, obtenha a senha root com:"
    warn "  docker exec gitlab grep 'Password:' /etc/gitlab/initial_root_password"
    warn ""
    warn "GitLab demora 3-5 minutos para inicializar completamente."
    warn "Porta SSH: ${HOST_IP}:2222"
    warn "=========================================================="
}

# -----------------------------------------------------------------------------
# GitLab Runner — docker-compose.yml
# -----------------------------------------------------------------------------
_generate_gitlab_runner_compose() {
    local target="${INFRA_DIR}/gitlab-runner/docker-compose.yml"

    if ! safe_create_file "${target}" "docker-compose.yml gitlab-runner"; then
        return
    fi

    log "Gerando: ${target}"
    cat > "${target}" <<EOF
services:
  gitlab-runner:
    image: gitlab/gitlab-runner:latest
    container_name: gitlab-runner
    restart: unless-stopped
    volumes:
      - ./config:/etc/gitlab-runner
      - /var/run/docker.sock:/var/run/docker.sock
      - ${INFRA_DIR}/step-ca/root_ca.crt:/etc/ssl/certs/archlab-ca.crt:ro
    environment:
      - SSL_CERT_FILE=/etc/ssl/certs/archlab-ca.crt
    networks:
      - infra-net
    extra_hosts:
      - "gitlab.lab.home:${HOST_IP}"
      - "registry.lab.home:${HOST_IP}"

networks:
  infra-net:
    external: true
EOF

    success "gitlab-runner docker-compose.yml gerado."

    warn "=========================================================="
    warn "AÇÃO MANUAL NECESSÁRIA — GitLab Runner"
    warn "Após subir o container, registre o runner no GitLab:"
    warn "  docker exec -it gitlab-runner gitlab-runner register"
    warn ""
    warn "URL:      https://gitlab.lab.home"
    warn "Token:    Obtenha em Admin Area → CI/CD → Runners"
    warn "Executor: docker"
    warn "Imagem:   docker:24"
    warn ""
    warn "Runner registrado anteriormente:"
    warn "  Name:  homelab-runner"
    warn "  Token: glrt-doqgrKEM6uEHrhs0zgBzjG86MQp0OjEKdToxCw.01.1209ura6z"
    warn "  (token pode estar expirado — crie um novo no GitLab UI)"
    warn "=========================================================="
}

# -----------------------------------------------------------------------------
# Arcane — docker-compose.yml
# -----------------------------------------------------------------------------
_generate_arcane_compose() {
    local target="${INFRA_DIR}/arcane/docker-compose.yml"

    if ! safe_create_file "${target}" "docker-compose.yml arcane"; then
        return
    fi

    log "Gerando: ${target}"

    # Gerar chaves criptográficas novas (não reutilizar as do ambiente original por segurança)
    local ENCRYPTION_KEY
    local JWT_SECRET
    ENCRYPTION_KEY=$(openssl rand -hex 32)
    JWT_SECRET=$(openssl rand -hex 32)

    cat > "${target}" <<EOF
services:
  arcane:
    image: ghcr.io/getarcaneapp/arcane:latest
    container_name: arcane
    restart: unless-stopped
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
      - arcane-data:/app/data
      # O path dentro e fora do container deve ser idêntico (requisito do Arcane)
      - ${WORKSPACE}/projects:${WORKSPACE}/projects
    environment:
      - APP_URL=https://arcane.lab.home
      - PUID=1000
      - PGID=1000
      - ENCRYPTION_KEY=${ENCRYPTION_KEY}
      - JWT_SECRET=${JWT_SECRET}
      - PROJECTS_DIRECTORY=${WORKSPACE}/projects
    networks:
      - infra-net
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.arcane.rule=Host(\`arcane.lab.home\`)"
      - "traefik.http.routers.arcane.entrypoints=websecure"
      - "traefik.http.routers.arcane.tls=true"
      - "traefik.http.routers.arcane.tls.certresolver=step-ca"
      - "traefik.http.routers.arcane.service=arcane-svc"
      - "traefik.http.routers.arcane.middlewares=arcane-ws,secure-headers@file,rate-limit@file"
      - "traefik.http.services.arcane-svc.loadbalancer.server.port=3552"
      # WebSocket support required by Arcane
      - "traefik.http.middlewares.arcane-ws.headers.customrequestheaders.Upgrade=websocket"
      - "traefik.http.middlewares.arcane-ws.headers.customrequestheaders.Connection=Upgrade"

volumes:
  arcane-data:

networks:
  infra-net:
    external: true
EOF

    success "arcane docker-compose.yml gerado."

    warn "=========================================================="
    warn "AÇÃO MANUAL NECESSÁRIA — Arcane"
    warn "Chaves criptográficas NOVAS foram geradas para segurança."
    warn "Se estiver restaurando um ambiente existente com dados do"
    warn "volume 'arcane-data', os dados estarão inacessíveis com"
    warn "chaves diferentes. Nesse caso, edite o compose com as"
    warn "chaves originais do ambiente."
    warn ""
    warn "Credenciais padrão (primeiro acesso): arcane / arcane-admin"
    warn "Altere a senha imediatamente após o primeiro login."
    warn ""
    warn "Token de deploy Arcane (CI/CD):"
    warn "  Gere um novo em: https://arcane.lab.home → Settings → API Tokens"
    warn "=========================================================="
}

# -----------------------------------------------------------------------------
# Jaeger — docker-compose.yml
# -----------------------------------------------------------------------------
_generate_jaeger_compose() {
    local target="${INFRA_DIR}/jaeger/docker-compose.yml"

    if ! safe_create_file "${target}" "docker-compose.yml jaeger"; then
        return
    fi

    log "Gerando: ${target}"
    cat > "${target}" <<'EOF'
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
      - "traefik.http.routers.jaeger-ui.rule=Host(`jaeger.lab.home`)"
      - "traefik.http.routers.jaeger-ui.entrypoints=websecure"
      - "traefik.http.routers.jaeger-ui.tls=true"
      - "traefik.http.routers.jaeger-ui.tls.certresolver=step-ca"
      - "traefik.http.routers.jaeger-ui.middlewares=rate-limit@file"
      - "traefik.http.routers.jaeger-ui.service=jaeger-ui-svc"
      - "traefik.http.services.jaeger-ui-svc.loadbalancer.server.port=16686"
      - "traefik.http.routers.jaeger-otlp.rule=Host(`otlp-jaeger.lab.home`)"
      - "traefik.http.routers.jaeger-otlp.entrypoints=websecure"
      - "traefik.http.routers.jaeger-otlp.tls=true"
      - "traefik.http.routers.jaeger-otlp.tls.certresolver=step-ca"
      - "traefik.http.routers.jaeger-otlp.service=jaeger-otlp-svc"
      - "traefik.http.services.jaeger-otlp-svc.loadbalancer.server.port=4318"

networks:
  infra-net:
    external: true
EOF

    success "jaeger docker-compose.yml gerado."
}

# -----------------------------------------------------------------------------
# Kafka — docker-compose.yml, .env, JAAS e certificados
# -----------------------------------------------------------------------------
_generate_kafka_compose() {
    local target="${INFRA_DIR}/kafka/docker-compose.yml"

    if ! safe_create_file "${target}" "docker-compose.yml kafka"; then
        return
    fi

    log "Gerando: ${target}"
    cat > "${target}" <<'EOF'
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
EOF

    success "kafka docker-compose.yml gerado."
}

_generate_kafka_env() {
    local target="${INFRA_DIR}/kafka/.env"

    if ! safe_create_file "${target}" ".env kafka"; then
        return
    fi

    log "Gerando: ${target}"
    cat > "${target}" <<'EOF'
KAFKA_IMAGE=apache/kafka:3.9.0
KAFKA_NODE_ID=1
KAFKA_PROCESS_ROLES=broker,controller
KAFKA_LISTENERS=INTERNAL://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093,SASL_SSL://0.0.0.0:9094
KAFKA_ADVERTISED_LISTENERS=INTERNAL://kafka:9092,SASL_SSL://kafka.lab.home:9094,CONTROLLER://kafka:9093
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
KAFKA_CERT_CN=kafka.lab.home
KAFKA_CERT_SAN_DOCKER=kafka
KAFKA_CERT_TTL=720h
STEP_CA_ROOT_CERT=../step-ca/root_ca.crt
STEP_CA_INTERMEDIATE_CERT=../step-ca/data/certs/intermediate_ca.crt
STEP_CA_INTERMEDIATE_KEY=../step-ca/data/secrets/intermediate_ca_key
STEP_CA_PASSWORD_FILE=../step-ca/data/secrets/password.txt
KAFKA_SSL_SECRET=changeit
KAFKA_SASL_USERNAME=app-prod
KAFKA_SASL_PASSWORD=trocar-esta-senha
EOF

    warn "Kafka: altere KAFKA_SSL_SECRET, KAFKA_SASL_USERNAME e KAFKA_SASL_PASSWORD antes de usar em produção."
    success "kafka/.env gerado."
}

_generate_kafka_jaas() {
    local target="${INFRA_DIR}/kafka/kafka_server_jaas.conf"

    if ! safe_create_file "${target}" "kafka_server_jaas.conf"; then
        return
    fi

    log "Gerando: ${target}"
    cat > "${target}" <<'EOF'
KafkaServer {
  org.apache.kafka.common.security.scram.ScramLoginModule required;
};
EOF

    success "kafka_server_jaas.conf gerado."
}

_generate_kafka_cert_script() {
    local target="${INFRA_DIR}/kafka/generate-kafka-certs.sh"

    if ! safe_create_file "${target}" "generate-kafka-certs.sh"; then
        return
    fi

    log "Gerando: ${target}"
    cat > "${target}" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"
CERT_DIR="$SCRIPT_DIR/certs"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Arquivo .env nao encontrado em $SCRIPT_DIR"
  exit 1
fi

if ! command -v step >/dev/null 2>&1; then
  echo "Comando 'step' nao encontrado. Instale a step CLI para emitir certificados."
  exit 1
fi

if ! command -v openssl >/dev/null 2>&1; then
  echo "Comando 'openssl' nao encontrado."
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

ROOT_CERT_PATH="$SCRIPT_DIR/$STEP_CA_ROOT_CERT"
INTERMEDIATE_CERT_PATH="$SCRIPT_DIR/$STEP_CA_INTERMEDIATE_CERT"
INTERMEDIATE_KEY_PATH="$SCRIPT_DIR/$STEP_CA_INTERMEDIATE_KEY"
PASSWORD_FILE_PATH="$SCRIPT_DIR/$STEP_CA_PASSWORD_FILE"

mkdir -p "$CERT_DIR"

step certificate create \
  "$KAFKA_CERT_CN" \
  "$CERT_DIR/kafka.crt" \
  "$CERT_DIR/kafka.key" \
  --profile leaf \
  --ca "$INTERMEDIATE_CERT_PATH" \
  --ca-key "$INTERMEDIATE_KEY_PATH" \
  --ca-password-file "$PASSWORD_FILE_PATH" \
  --san "$KAFKA_CERT_CN" \
  --san "$KAFKA_CERT_SAN_DOCKER" \
  --not-after "$KAFKA_CERT_TTL" \
  --force \
  --no-password \
  --insecure

openssl pkcs12 -export \
  -name kafka \
  -in "$CERT_DIR/kafka.crt" \
  -inkey "$CERT_DIR/kafka.key" \
  -certfile "$INTERMEDIATE_CERT_PATH" \
  -out "$CERT_DIR/kafka.keystore.p12" \
  -passout pass:"$KAFKA_SSL_SECRET"

openssl pkcs12 -export \
  -nokeys \
  -in "$ROOT_CERT_PATH" \
  -certfile "$INTERMEDIATE_CERT_PATH" \
  -out "$CERT_DIR/kafka.truststore.p12" \
  -name step-ca \
  -passout pass:"$KAFKA_SSL_SECRET"

printf "%s" "$KAFKA_SSL_SECRET" > "$CERT_DIR/$KAFKA_SSL_KEY_CREDENTIALS"
printf "%s" "$KAFKA_SSL_SECRET" > "$CERT_DIR/$KAFKA_SSL_KEYSTORE_CREDENTIALS"
printf "%s" "$KAFKA_SSL_SECRET" > "$CERT_DIR/$KAFKA_SSL_TRUSTSTORE_CREDENTIALS"

chmod 600 \
  "$CERT_DIR/kafka.key" \
  "$CERT_DIR/kafka.keystore.p12" \
  "$CERT_DIR/kafka.truststore.p12" \
  "$CERT_DIR/$KAFKA_SSL_KEY_CREDENTIALS" \
  "$CERT_DIR/$KAFKA_SSL_KEYSTORE_CREDENTIALS" \
  "$CERT_DIR/$KAFKA_SSL_TRUSTSTORE_CREDENTIALS"

cat <<MSG
Certificados Kafka gerados em:
  - $CERT_DIR/kafka.crt
  - $CERT_DIR/kafka.key
  - $CERT_DIR/kafka.keystore.p12
  - $CERT_DIR/kafka.truststore.p12
MSG
EOF
    chmod 755 "${target}"
    warn "Kafka: gere certificados antes do primeiro up com ${target}."
    success "generate-kafka-certs.sh gerado."
}

# -----------------------------------------------------------------------------
# Keycloak — docker-compose.yml, .env e scripts
# -----------------------------------------------------------------------------
_generate_keycloak_compose() {
    local target="${INFRA_DIR}/keycloak/docker-compose.yml"

    if ! safe_create_file "${target}" "docker-compose.yml keycloak"; then
        return
    fi

    log "Gerando: ${target}"
    cat > "${target}" <<'EOF'
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
    healthcheck:
      test: ["CMD-SHELL", "exec 3<>/dev/tcp/localhost/9000 && echo -e 'GET /health/ready HTTP/1.1\\r\\nHost: localhost\\r\\nConnection: close\\r\\n\\r\\n' >&3 && cat <&3 | grep -q '200 OK'"]
      interval: 15s
      timeout: 5s
      retries: 20
      start_period: 90s
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
EOF

    success "keycloak docker-compose.yml gerado."
}

_generate_keycloak_env() {
    local target="${INFRA_DIR}/keycloak/.env"

    if ! safe_create_file "${target}" ".env keycloak"; then
        return
    fi

    log "Gerando: ${target}"
    cat > "${target}" <<'EOF'
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=TROCAR_SENHA_FORTE

POSTGRES_DB=keycloak
POSTGRES_USER=keycloak
POSTGRES_PASSWORD=TROCAR_SENHA_FORTE
EOF

    warn "Keycloak: defina senhas fortes em ${target} antes de subir."
    success "keycloak/.env gerado."
}

_generate_keycloak_up() {
    local target="${INFRA_DIR}/keycloak/keycloak-up.sh"

    if ! safe_create_file "${target}" "keycloak-up.sh"; then
        return
    fi

    log "Gerando: ${target}"
    cat > "${target}" <<'EOF'
#!/bin/bash
set -e

INFRA_DIR="/home/dell/workspace/infra/"

# cores
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

START_TIME=$(date +%s)

log() {
  echo -e "${GREEN}==>${NC} $1"
}

warn() {
  echo -e "${YELLOW}⚠ $1${NC}"
}

error() {
  echo -e "${RED}✖ $1${NC}"
}

# spinner
spinner() {
  local pid=$1
  local spin='|/-\'
  local i=0

  while kill -0 "$pid" 2>/dev/null; do
    i=$(( (i+1) %4 ))
    printf "\r[%c] " "${spin:$i:1}"
    sleep 0.1
  done

  printf "\r"
}

# espera URL responder
wait_for_url() {
  local url=$1

  echo -ne "   Aguardando $url ... "

  (
    until curl -sk "$url" >/dev/null 2>&1; do
      sleep 1
    done
  ) &

  spinner $!
  wait $! 2>/dev/null

  printf "\r\033[K🔎 Aguardando %s ... %b✔ OK%b\n" "$url" "$GREEN" "$NC"
  echo ""
}

log "[1/3] step-ca"
(cd "$INFRA_DIR/step-ca" && docker compose up -d)

log "[2/3] traefik"
(cd "$INFRA_DIR/traefik" && docker compose up -d)

log "[3/3] keycloak"
(docker compose up -d)

echo ""

# health checks reais
sleep 1
wait_for_url https://traefik.lab.home

# status final
echo ""
log "Status Docker:"
docker ps --format "table {{.Names}}\t{{.Status}}" | grep -E "step-ca|traefik|keycloak" || true

END_TIME=$(date +%s)
ELAPSED=$((END_TIME - START_TIME))

echo ""
echo -e "${GREEN} Keycloak UP em ${ELAPSED}s${NC}"
EOF
    chmod 755 "${target}"
    success "keycloak-up.sh gerado."
}

_generate_keycloak_down() {
    local target="${INFRA_DIR}/keycloak/keycloak-down.sh"

    if ! safe_create_file "${target}" "keycloak-down.sh"; then
        return
    fi

    log "Gerando: ${target}"
    cat > "${target}" <<'EOF'
#!/bin/bash
set -e

INFRA_DIR="/home/dell/workspace/infra/"

# cores
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

START_TIME=$(date +%s)

log() {
  echo -e "${GREEN}==>${NC} $1"
}

warn() {
  echo -e "${YELLOW}⚠ $1${NC}"
}


# docker down

log "[1/3] keycloak"
(docker compose down)

log "[2/3] traefik"
(cd "$INFRA_DIR/traefik" && docker compose down)

log "[3/3] step-ca"
(cd "$INFRA_DIR/step-ca" && docker compose down)

END_TIME=$(date +%s)
ELAPSED=$((END_TIME - START_TIME))

echo ""
echo -e "${GREEN}🛑 keycloak DOWN em ${ELAPSED}s${NC}"
EOF
    chmod 755 "${target}"
    success "keycloak-down.sh gerado."
}

# -----------------------------------------------------------------------------
# Observabilidade — Prometheus e Grafana
# -----------------------------------------------------------------------------
_generate_observability_compose() {
    local target="${INFRA_DIR}/observabilidade/prometheus-grafana/docker-compose.yml"

    if ! safe_create_file "${target}" "docker-compose.yml prometheus-grafana"; then
        return
    fi

    log "Gerando: ${target}"
    cat > "${target}" <<'EOF'
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
      - "traefik.http.routers.prometheus.rule=Host(`prometheus.lab.home`)"
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
      GF_SECURITY_ADMIN_USER: ${GRAFANA_ADMIN_USER}
      GF_SECURITY_ADMIN_PASSWORD: ${GRAFANA_ADMIN_PASSWORD}
      GF_SERVER_ROOT_URL: https://grafana.lab.home
      GF_USERS_ALLOW_SIGN_UP: "false"
    volumes:
      - grafana_data:/var/lib/grafana
      - ./grafana/provisioning:/etc/grafana/provisioning:ro
      - ./grafana/dashboards:/var/lib/grafana/dashboards:ro
    networks:
      - infra-net
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.grafana.rule=Host(`grafana.lab.home`)"
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
EOF

    success "observabilidade docker-compose.yml gerado."
}

_generate_observability_env() {
    local target="${INFRA_DIR}/observabilidade/prometheus-grafana/.env"

    if ! safe_create_file "${target}" ".env prometheus-grafana"; then
        return
    fi

    log "Gerando: ${target}"
    cat > "${target}" <<'EOF'
GRAFANA_ADMIN_USER=mekylei
GRAFANA_ADMIN_PASSWORD=TROCAR_SENHA_FORTE
EOF

    warn "Grafana: altere GRAFANA_ADMIN_PASSWORD em ${target} antes de subir."
    success "observabilidade/.env gerado."
}

_generate_prometheus_config() {
    local target="${INFRA_DIR}/observabilidade/prometheus-grafana/prometheus/prometheus.yml"

    if ! safe_create_file "${target}" "prometheus.yml"; then
        return
    fi

    log "Gerando: ${target}"
    cat > "${target}" <<'EOF'
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
      token_url: https://keycloak.lab.home/realms/bancario/protocol/openid-connect/token
      tls_config:
        ca_file: /etc/prometheus/certs/root_ca.crt
    static_configs:
      - targets:
          - 192.168.0.105:8080
        labels:
          application: transaction-processing-api
          environment: homelab
EOF

    success "prometheus.yml gerado."
}

_generate_prometheus_rules() {
    local target="${INFRA_DIR}/observabilidade/prometheus-grafana/prometheus/rules/transaction-processing-slo.yml"

    if ! safe_create_file "${target}" "transaction-processing-slo.yml"; then
        return
    fi

    log "Gerando: ${target}"
    cat > "${target}" <<'EOF'
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
          histogram_quantile(
            0.95,
            sum(rate(transacao_duracao_seconds_bucket{tipo="PIX"}[5m])) by (le)
          ) > 8
        for: 5m
        labels:
          severity: critical
          service: transaction-processing-api
        annotations:
          summary: "p95 de latencia PIX acima de 8s"
          description: "Verificar banco, Kafka/outbox, integracoes externas, saturacao da aplicacao e traces no Jaeger."
EOF

    success "transaction-processing-slo.yml gerado."
}

_generate_prometheus_keycloak_secret_placeholder() {
    local target="${INFRA_DIR}/observabilidade/prometheus-grafana/prometheus/secrets/keycloak-client-secret"

    if ! safe_create_file "${target}" "keycloak-client-secret"; then
        return
    fi

    log "Criando placeholder vazio: ${target}"
    touch "${target}"
    warn "Prometheus: preencha ${target} com o client secret do realm bancario no Keycloak."
    success "keycloak-client-secret placeholder criado."
}

_generate_grafana_datasource() {
    local target="${INFRA_DIR}/observabilidade/prometheus-grafana/grafana/provisioning/datasources/prometheus.yml"

    if ! safe_create_file "${target}" "grafana datasource prometheus.yml"; then
        return
    fi

    log "Gerando: ${target}"
    cat > "${target}" <<'EOF'
apiVersion: 1

datasources:
  - name: Prometheus Homelab
    uid: prometheus-homelab
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: true
EOF

    success "grafana datasource prometheus.yml gerado."
}

_generate_grafana_dashboard_provider() {
    local target="${INFRA_DIR}/observabilidade/prometheus-grafana/grafana/provisioning/dashboards/transaction-processing.yml"

    if ! safe_create_file "${target}" "grafana dashboard provider"; then
        return
    fi

    log "Gerando: ${target}"
    cat > "${target}" <<'EOF'
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
EOF

    success "grafana dashboard provider gerado."
}

_generate_grafana_dashboard_json() {
    local target="${INFRA_DIR}/observabilidade/prometheus-grafana/grafana/dashboards/transacao-slo-dashboard.json"

    if ! safe_create_file "${target}" "transacao-slo-dashboard.json"; then
        return
    fi

    log "Gerando: ${target}"
    cat > "${target}" <<'EOF'
{
  "__requires": [
    {
      "type": "grafana",
      "id": "grafana",
      "name": "Grafana",
      "version": "13.0.2"
    },
    {
      "type": "datasource",
      "id": "prometheus",
      "name": "Prometheus",
      "version": "3.12.0"
    }
  ],
  "annotations": {
    "list": []
  },
  "editable": true,
  "fiscalYearStartMonth": 0,
  "graphTooltip": 1,
  "id": null,
  "links": [],
  "panels": [
    {
      "datasource": {
        "type": "prometheus",
        "uid": "prometheus-homelab"
      },
      "gridPos": {
        "h": 8,
        "w": 24,
        "x": 0,
        "y": 0
      },
      "id": 1,
      "targets": [
        {
          "datasource": {
            "type": "prometheus",
            "uid": "prometheus-homelab"
          },
          "expr": "sum(rate(transacao_criada_total{tipo=\"PIX\"}[$__rate_interval])) * 60",
          "legendFormat": "PIX iniciadas/min",
          "refId": "A"
        }
      ],
      "title": "Transacoes PIX Iniciadas Por Minuto",
      "type": "timeseries"
    },
    {
      "datasource": {
        "type": "prometheus",
        "uid": "prometheus-homelab"
      },
      "gridPos": {
        "h": 8,
        "w": 24,
        "x": 0,
        "y": 8
      },
      "id": 2,
      "targets": [
        {
          "datasource": {
            "type": "prometheus",
            "uid": "prometheus-homelab"
          },
          "expr": "100 * sum(rate(transacao_processada_total{tipo=\"PIX\",status=\"COMPLETADA\"}[$__rate_interval])) / clamp_min(sum(rate(transacao_processada_total{tipo=\"PIX\",status=~\"COMPLETADA|FALHOU\"}[$__rate_interval])), 0.000001)",
          "legendFormat": "sucesso",
          "refId": "A"
        },
        {
          "datasource": {
            "type": "prometheus",
            "uid": "prometheus-homelab"
          },
          "expr": "100 * sum(rate(transacao_processada_total{tipo=\"PIX\",status=\"FALHOU\"}[$__rate_interval])) / clamp_min(sum(rate(transacao_processada_total{tipo=\"PIX\",status=~\"COMPLETADA|FALHOU\"}[$__rate_interval])), 0.000001)",
          "legendFormat": "falha",
          "refId": "B"
        }
      ],
      "title": "Taxa de Sucesso e Falha PIX",
      "type": "timeseries"
    },
    {
      "datasource": {
        "type": "prometheus",
        "uid": "prometheus-homelab"
      },
      "gridPos": {
        "h": 8,
        "w": 24,
        "x": 0,
        "y": 16
      },
      "id": 3,
      "targets": [
        {
          "datasource": {
            "type": "prometheus",
            "uid": "prometheus-homelab"
          },
          "expr": "histogram_quantile(0.95, sum(rate(transacao_duracao_seconds_bucket{tipo=\"PIX\"}[$__rate_interval])) by (le))",
          "legendFormat": "p95",
          "refId": "A"
        },
        {
          "datasource": {
            "type": "prometheus",
            "uid": "prometheus-homelab"
          },
          "expr": "histogram_quantile(0.99, sum(rate(transacao_duracao_seconds_bucket{tipo=\"PIX\"}[$__rate_interval])) by (le))",
          "legendFormat": "p99",
          "refId": "B"
        }
      ],
      "title": "Latencia PIX p95 e p99",
      "type": "timeseries"
    }
  ],
  "refresh": "30s",
  "schemaVersion": 41,
  "tags": [
    "transaction-processing",
    "pix",
    "slo"
  ],
  "templating": {
    "list": []
  },
  "time": {
    "from": "now-6h",
    "to": "now"
  },
  "timezone": "browser",
  "title": "Transaction Processing API - SLO PIX",
  "uid": "transaction-processing-pix-slo",
  "version": 1
}
EOF

    success "transacao-slo-dashboard.json gerado."
}

# -----------------------------------------------------------------------------
# Scripts da infra (infra-up.sh, infra-down.sh, backup.sh, check-certs.sh)
# -----------------------------------------------------------------------------
_generate_infra_scripts() {
    _generate_infra_up
    _generate_infra_down
    _generate_backup_script
    _generate_check_certs
}

_generate_infra_up() {
    local target="${INFRA_DIR}/infra-up.sh"

    if ! safe_create_file "${target}" "infra-up.sh"; then
        return
    fi

    log "Gerando: ${target}"
    cat > "${target}" <<'EOF'
#!/bin/bash
# Sobe toda a infra do ArchLab na ordem correta
set -e

INFRA_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "AVISO: Kafka requer certificados gerados previamente:"
echo "  $INFRA_DIR/kafka/generate-kafka-certs.sh"
echo ""

echo "==> [1/9] step-ca"
cd "$INFRA_DIR/step-ca" && docker compose up -d

echo "==> [2/9] traefik"
cd "$INFRA_DIR/traefik" && docker compose up -d

echo "==> [3/9] jaeger"
cd "$INFRA_DIR/jaeger" && docker compose up -d

echo "==> [4/9] kafka"
cd "$INFRA_DIR/kafka" && docker compose up -d

echo "==> [5/9] keycloak"
cd "$INFRA_DIR/keycloak" && docker compose up -d

echo "==> [6/9] gitlab"
cd "$INFRA_DIR/gitlab" && docker compose up -d

echo "==> [7/9] gitlab-runner"
cd "$INFRA_DIR/gitlab-runner" && docker compose up -d

echo "==> [8/9] prometheus-grafana"
cd "$INFRA_DIR/observabilidade/prometheus-grafana" && docker compose up -d

echo "==> [9/9] arcane"
cd "$INFRA_DIR/arcane" && docker compose up -d

echo ""
echo "Infra UP. Status:"
docker ps --format "table {{.Names}}\t{{.Status}}" | grep -E "step-ca|traefik|jaeger|kafka|keycloak|gitlab|prometheus|grafana|arcane"
EOF
    chmod +x "${target}"
    success "infra-up.sh gerado."
}

_generate_infra_down() {
    local target="${INFRA_DIR}/infra-down.sh"

    if ! safe_create_file "${target}" "infra-down.sh"; then
        return
    fi

    log "Gerando: ${target}"
    cat > "${target}" <<'EOF'
#!/bin/bash
# Para toda a infra do ArchLab na ordem inversa
set -e

INFRA_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "==> [1/9] arcane"
cd "$INFRA_DIR/arcane" && docker compose down

echo "==> [2/9] prometheus-grafana"
cd "$INFRA_DIR/observabilidade/prometheus-grafana" && docker compose down

echo "==> [3/9] gitlab-runner"
cd "$INFRA_DIR/gitlab-runner" && docker compose down

echo "==> [4/9] gitlab"
cd "$INFRA_DIR/gitlab" && docker compose down

echo "==> [5/9] keycloak"
cd "$INFRA_DIR/keycloak" && docker compose down

echo "==> [6/9] kafka"
cd "$INFRA_DIR/kafka" && docker compose down

echo "==> [7/9] jaeger"
cd "$INFRA_DIR/jaeger" && docker compose down

echo "==> [8/9] traefik"
cd "$INFRA_DIR/traefik" && docker compose down

echo "==> [9/9] step-ca"
cd "$INFRA_DIR/step-ca" && docker compose down

echo ""
echo "Infra DOWN."
EOF
    chmod +x "${target}"
    success "infra-down.sh gerado."
}

_generate_backup_script() {
    local target="${INFRA_DIR}/gitlab/backup.sh"

    if ! safe_create_file "${target}" "backup.sh (gitlab)"; then
        return
    fi

    log "Gerando: ${target}"
    cat > "${target}" <<'EOF'
#!/bin/bash
# backup.sh — Backup automático do GitLab
# Executa backup completo e mantém os 7 mais recentes

set -e

BACKUP_DIR="/home/dell/workspace/infra/gitlab/data/backups"
LOG_PREFIX="[gitlab-backup]"
TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')

echo "${LOG_PREFIX} ${TIMESTAMP} — Iniciando backup..."

# Garantir que o container GitLab está rodando
if ! docker inspect -f '{{.State.Running}}' gitlab 2>/dev/null | grep -q "true"; then
  echo "${LOG_PREFIX} ERRO: container 'gitlab' não está rodando. Abortando."
  exit 1
fi

# Executar backup dentro do container (STRATEGY=copy evita lock de arquivos)
docker exec gitlab gitlab-backup create STRATEGY=copy BACKUP=scheduled

echo "${LOG_PREFIX} Backup criado com sucesso."

# Remover backups antigos — manter apenas os 7 mais recentes
BACKUP_COUNT=$(ls "${BACKUP_DIR}"/*_gitlab_backup.tar 2>/dev/null | wc -l)
if [ "${BACKUP_COUNT}" -gt 7 ]; then
  echo "${LOG_PREFIX} Removendo backups antigos (mantendo 7)..."
  ls -t "${BACKUP_DIR}"/*_gitlab_backup.tar | tail -n +8 | xargs rm -f
  echo "${LOG_PREFIX} Limpeza concluída."
fi

# Listar backups disponíveis
echo "${LOG_PREFIX} Backups disponíveis:"
ls -lh "${BACKUP_DIR}"/*_gitlab_backup.tar 2>/dev/null || echo "${LOG_PREFIX} Nenhum backup encontrado."

echo "${LOG_PREFIX} $(date '+%Y-%m-%d %H:%M:%S') — Backup finalizado com sucesso."
EOF
    chmod +x "${target}"
    success "gitlab/backup.sh gerado."
}

_generate_check_certs() {
    local target="${INFRA_DIR}/check-certs.sh"

    if ! safe_create_file "${target}" "check-certs.sh"; then
        return
    fi

    log "Gerando: ${target}"
    cat > "${target}" <<'EOF'
#!/bin/bash
# check-certs.sh — Monitoramento de expiração de certificados TLS
# Uso: ./check-certs.sh [--warn-days N]
# Alerta quando um certificado expira em menos de N dias (padrão: 14)

set -euo pipefail

WARN_DAYS="${1:-14}"
WARN_SECONDS=$((WARN_DAYS * 86400))
TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')
EXIT_CODE=0

DOMAINS=(
  "gitlab.lab.home"
  "registry.lab.home"
  "traefik.lab.home"
  "arcane.lab.home"
  "keycloak.lab.home"
  "prometheus.lab.home"
  "grafana.lab.home"
  "jaeger.lab.home"
  "otlp-jaeger.lab.home"
)

echo "======================================================"
echo " Verificação de Certificados TLS — ${TIMESTAMP}"
echo " Alertar se expira em menos de ${WARN_DAYS} dias"
echo "======================================================"

for domain in "${DOMAINS[@]}"; do
  EXPIRY_RAW=$(echo | openssl s_client -connect "${domain}:443" \
    -servername "${domain}" 2>/dev/null \
    | openssl x509 -noout -enddate 2>/dev/null | cut -d= -f2)

  if [ -z "${EXPIRY_RAW}" ]; then
    echo "[ERRO]  ${domain} — não foi possível obter o certificado"
    EXIT_CODE=2
    continue
  fi

  EXPIRY_EPOCH=$(date -d "${EXPIRY_RAW}" +%s 2>/dev/null || date -j -f "%b %d %T %Y %Z" "${EXPIRY_RAW}" +%s 2>/dev/null)
  NOW_EPOCH=$(date +%s)
  DIFF_SECONDS=$(( EXPIRY_EPOCH - NOW_EPOCH ))
  DIFF_DAYS=$(( DIFF_SECONDS / 86400 ))

  if [ "${DIFF_SECONDS}" -lt 0 ]; then
    echo "[EXPIRADO] ${domain} — expirou em: ${EXPIRY_RAW}"
    EXIT_CODE=2
  elif [ "${DIFF_SECONDS}" -lt "${WARN_SECONDS}" ]; then
    echo "[AVISO]  ${domain} — expira em ${DIFF_DAYS} dias (${EXPIRY_RAW})"
    EXIT_CODE=1
  else
    echo "[OK]     ${domain} — expira em ${DIFF_DAYS} dias (${EXPIRY_RAW})"
  fi
done

echo "======================================================"
echo " Finalizado. Código de saída: ${EXIT_CODE}"
echo " 0=OK | 1=AVISO | 2=ERRO"
echo "======================================================"

exit "${EXIT_CODE}"
EOF
    chmod +x "${target}"
    success "check-certs.sh gerado."
}

# =============================================================================
# FUNÇÃO 5: validate_host
# Valida configurações do host (DNS, IP, Docker daemon)
# =============================================================================
validate_host() {
    section "Validação do Host"

    # Verificar IP do host
    if ip addr show | grep -q "${HOST_IP}"; then
        success "IP do host confirmado: ${HOST_IP}"
    else
        warn "IP ${HOST_IP} não encontrado nas interfaces de rede."
        warn "Certifique-se de que o roteador reservou ${HOST_IP} para este host."
        local current_ip
        current_ip=$(ip addr show | grep "inet " | grep -v "127.0.0.1\|172\." | awk '{print $2}' | cut -d/ -f1 | head -1 || echo "desconhecido")
        warn "IP atual detectado: ${current_ip}"
    fi

    # Verificar DNS wildcard
    if command -v dig &>/dev/null; then
        local dns_result
        dns_result=$(dig "gitlab.${DOMAIN}" @127.0.0.1 +short 2>/dev/null || echo "")
        if [[ "${dns_result}" == "${HOST_IP}" ]]; then
            success "DNS wildcard *.${DOMAIN} → ${HOST_IP} funcionando."
        else
            warn "DNS wildcard não resolveu corretamente."
            warn "Resultado de 'dig gitlab.${DOMAIN} @127.0.0.1': '${dns_result}'"
            warn "Verifique se dnsmasq está rodando: sudo systemctl status dnsmasq"
            warn "Verifique /etc/dnsmasq.d/lab.home.conf"
        fi
    fi

    # Verificar daemon.json
    if [[ -f /etc/docker/daemon.json ]]; then
        success "Docker daemon.json presente."
    else
        warn "Docker daemon.json não encontrado."
        warn "Recomendado configurar em /etc/docker/daemon.json (log rotation + address pool)."
    fi

    # Verificar ufw
    if command -v ufw &>/dev/null; then
        local ufw_status
        ufw_status=$(sudo ufw status 2>/dev/null | head -1 || echo "desconhecido")
        log "UFW status: ${ufw_status}"
    fi
}

# =============================================================================
# FUNÇÃO 6: start_infra
# Exibe os comandos para subir a infra e instruções pós-geração
# NÃO EXECUTA os containers — apenas informa o operador
# =============================================================================
start_infra() {
    section "Instruções para Subir a Infraestrutura"

    echo ""
    log "A infraestrutura foi configurada. Para subir os serviços, execute:"
    echo ""
    echo "  # Opção 1 — Script de conveniência:"
    echo "  ${INFRA_DIR}/infra-up.sh"
    echo ""
    echo "  # Opção 2 — Manual (ordem correta de dependências):"
    echo ""
    echo "  # 1. step-ca (CA interna — deve ser a primeira)"
    echo "  cd ${INFRA_DIR}/step-ca && docker compose up -d"
    echo ""
    echo "  # 2. Traefik (edge proxy — depende de step-ca estar acessível)"
    echo "  cd ${INFRA_DIR}/traefik && docker compose up -d"
    echo ""
    echo "  # 3. Jaeger"
    echo "  cd ${INFRA_DIR}/jaeger && docker compose up -d"
    echo ""
    echo "  # 4. Kafka (requer certificados gerados previamente)"
    echo "  cd ${INFRA_DIR}/kafka && ./generate-kafka-certs.sh && docker compose up -d"
    echo ""
    echo "  # 5. Keycloak"
    echo "  cd ${INFRA_DIR}/keycloak && docker compose up -d"
    echo ""
    echo "  # 6. GitLab (demora 3-5 min para inicializar)"
    echo "  cd ${INFRA_DIR}/gitlab && docker compose up -d"
    echo ""
    echo "  # 7. GitLab Runner"
    echo "  cd ${INFRA_DIR}/gitlab-runner && docker compose up -d"
    echo ""
    echo "  # 8. Prometheus + Grafana"
    echo "  cd ${INFRA_DIR}/observabilidade/prometheus-grafana && docker compose up -d"
    echo ""
    echo "  # 9. Arcane"
    echo "  cd ${INFRA_DIR}/arcane && docker compose up -d"
    echo ""

    section "Verificação de Status (após subir)"
    echo ""
    echo "  docker ps --format 'table {{.Names}}\t{{.Status}}'"
    echo "  curl -sk https://ca.lab.home:9000/health"
    echo "  curl -sIL http://traefik.lab.home | grep -i 'location\|HTTP/'"
    echo "  curl -sk https://gitlab.lab.home/-/health"
    echo "  curl -sk https://keycloak.lab.home/health/ready"
    echo "  curl -sk https://jaeger.lab.home | head -5"
    echo "  curl -sk https://prometheus.lab.home/-/healthy"
    echo "  curl -sk https://grafana.lab.home/api/health"
    echo "  curl -sk https://arcane.lab.home | head -5"
    echo ""

    section "Ações Manuais Obrigatórias (pós-subida)"
    echo ""
    echo "  1. step-ca:"
    echo "     - Crie o arquivo de senha ANTES de subir:"
    echo "       echo 'SUA_SENHA' > ${INFRA_DIR}/step-ca/data/secrets/password.txt"
    echo "       chmod 600 ${INFRA_DIR}/step-ca/data/secrets/password.txt"
    echo "     - Na primeira execução, o container inicializa a CA e para."
    echo "       Suba novamente: cd ${INFRA_DIR}/step-ca && docker compose up -d"
    echo "     - Exporte o root_ca.crt:"
    echo "       docker exec step-ca cat /home/step/certs/root_ca.crt > ${INFRA_DIR}/step-ca/root_ca.crt"
    echo "     - Instale no sistema:"
    echo "       sudo cp ${INFRA_DIR}/step-ca/root_ca.crt /usr/local/share/ca-certificates/archlab-ca.crt"
    echo "       sudo update-ca-certificates"
    echo "     - Reinicie o Docker para recarregar os CAs:"
    echo "       sudo systemctl restart docker"
    echo ""
    echo "  2. Kafka:"
    echo "     - Altere KAFKA_SSL_SECRET, KAFKA_SASL_USERNAME e KAFKA_SASL_PASSWORD em:"
    echo "       ${INFRA_DIR}/kafka/.env"
    echo "     - Antes do primeiro up, gere os certificados:"
    echo "       cd ${INFRA_DIR}/kafka && ./generate-kafka-certs.sh"
    echo "     - Requer step CLI instalada e step-ca inicializado."
    echo ""
    echo "  3. Keycloak:"
    echo "     - Defina KEYCLOAK_ADMIN_PASSWORD e POSTGRES_PASSWORD em:"
    echo "       ${INFRA_DIR}/keycloak/.env"
    echo ""
    echo "  4. Prometheus:"
    echo "     - Preencha o client secret do realm bancario em:"
    echo "       ${INFRA_DIR}/observabilidade/prometheus-grafana/prometheus/secrets/keycloak-client-secret"
    echo ""
    echo "  5. Grafana:"
    echo "     - Altere GRAFANA_ADMIN_PASSWORD em:"
    echo "       ${INFRA_DIR}/observabilidade/prometheus-grafana/.env"
    echo "     - Acesse https://grafana.lab.home com as credenciais do .env."
    echo ""
    echo "  6. GitLab Runner:"
    echo "     - Registre o runner após o GitLab estar healthy:"
    echo "       docker exec -it gitlab-runner gitlab-runner register"
    echo "     - Executor: docker | Imagem: docker:24"
    echo ""
    echo "  7. Arcane:"
    echo "     - Acesse https://arcane.lab.home"
    echo "     - Login: arcane / arcane-admin (altere imediatamente)"
    echo "     - Configure os projetos em Settings → Projects"
    echo ""
    echo "  8. GitLab CI/CD:"
    echo "     - Crie variáveis de instância em Admin Area → CI/CD → Variables:"
    echo "       REGISTRY_HOST  = registry.lab.home"
    echo "       REGISTRY_USER  = root (ou usuário dedicado)"
    echo "       REGISTRY_PASS  = Personal Access Token (read/write_registry)"
    echo "       ARCANE_URL     = http://arcane:3552"
    echo "       ARCANE_TOKEN   = (token gerado no Arcane UI)"
    echo ""
    echo "  9. Backup GitLab (cron diário às 02:00):"
    echo "     - Adicione ao crontab do usuário dell:"
    echo "       0 2 * * * /home/dell/workspace/infra/gitlab/backup.sh >> /var/log/gitlab-backup.log 2>&1"
    echo ""
    echo "  10. Monitor de certificados:"
    echo "     ${INFRA_DIR}/check-certs.sh"
    echo ""
}

# =============================================================================
# FUNÇÃO MAIN
# Orquestra a execução de todas as funções na ordem correta
# =============================================================================
main() {
    echo ""
    echo "╔══════════════════════════════════════════════════════════╗"
    echo "║         rebuild-infra.sh — ArchLab Homelab               ║"
    echo "║  Reconstituição da Infraestrutura (Geração de Configs)   ║"
    echo "╚══════════════════════════════════════════════════════════╝"
    echo ""
    echo "  Host IP  : ${HOST_IP}"
    echo "  Domínio  : ${DOMAIN}"
    echo "  Infra dir: ${INFRA_DIR}"
    echo "  Rede     : ${DOCKER_NETWORK} (${DOCKER_NETWORK_SUBNET})"
    echo ""

    warn "ATENÇÃO: Este script gera arquivos de configuração."
    warn "NÃO sobe containers automaticamente."
    warn "Arquivos existentes NÃO serão sobrescritos."
    echo ""

    check_dependencies
    setup_structure
    setup_docker_network
    generate_configs
    validate_host
    start_infra

    echo ""
    echo "╔══════════════════════════════════════════════════════════╗"
    echo "║                    GERAÇÃO CONCLUÍDA                     ║"
    echo "╚══════════════════════════════════════════════════════════╝"
    echo ""
    success "Script concluído. Siga as instruções acima para subir a infra."
    echo ""
}

# Executar
main "$@"
