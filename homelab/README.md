# Reconstrucao da infraestrutura do homelab

Este diretorio contem scripts para recriar a infraestrutura do homelab ArchLab. Os arquivos `.sh` e `.ps1` fazem a mesma geracao de estrutura e configuracoes, mas para sistemas diferentes:

- `rebuild-infra.sh`: Linux/Ubuntu.
- `rebuild-infra.ps1`: Windows 11+ com PowerShell e Docker Desktop.
- `rebuild-infra-test.sh`: teste isolado do script Linux.
- `rebuild-infra-test.ps1`: teste isolado do script PowerShell.

Os scripts principais criam a pasta `infra`, os arquivos `docker-compose.yml`, arquivos `.env` e scripts auxiliares. Eles nao sobem os containers automaticamente e nao sobrescrevem arquivos que ja existem.

## Antes de comecar

Confira se voce esta no computador correto do homelab e se as configuracoes padrao fazem sentido para o seu ambiente:

- IP esperado do host: `192.168.0.102`
- Dominio local: `lab.home`
- Pasta de trabalho Linux: `/home/dell/workspace`, por exemplo, ou a pasta de trabalho que você definir
- Pasta de trabalho Windows: `$HOME\workspace`, por exemplo, ou a pasta de trabalho que você definir
- Rede Docker: `infra-net`

Se o IP do computador for diferente, ajuste a reserva de IP no roteador ou execute o script PowerShell passando outro `-HostIP`. No script Linux, o IP esta definido dentro do arquivo `rebuild-infra.sh`.

## Pre-requisitos

### Linux/Ubuntu

Instale os pacotes necessarios:

```bash
sudo apt update
sudo apt install -y curl openssl dnsutils apache2-utils
```

Instale tambem Docker CE e o plugin Docker Compose v2. Depois, confirme:

```bash
docker version
docker compose version
docker ps
```

Para gerar os certificados do Kafka, o comando `step` tambem precisa existir:

```bash
step version
```

Se `docker ps` pedir permissao, adicione seu usuario ao grupo `docker`:

```bash
sudo usermod -aG docker "$USER"
newgrp docker
```

### Windows 11+

Instale:

- Docker Desktop com suporte a Docker Compose.
- PowerShell 5.1 ou superior.
- `curl` e `openssl` disponiveis no `PATH` se voce quiser usar os scripts de verificacao.
- Smallstep CLI, com o comando `step` disponivel no `PATH`, para gerar os certificados Kafka.

Abra o PowerShell como usuario comum e confirme:

```powershell
docker version
docker compose version
```

## Opcional: rodar os testes primeiro

Os testes geram tudo em uma pasta temporaria e validam se os arquivos esperados foram criados. Eles sao a forma mais segura de conferir se os scripts estao funcionando antes de usar a infra real.

### Teste no Linux

```bash
cd /home/dell/workspace/scripts/infraestrutura
bash rebuild-infra-test.sh
```

Se quiser testar dentro de um container Ubuntu isolado:

```bash
docker run -it --rm \
  -v /home/dell/workspace/scripts:/home/dell/workspace/scripts \
  ubuntu:24.04 bash
```

Dentro do container:

```bash
apt-get update -q
apt-get install -y curl openssl dnsutils apache2-utils iproute2
bash /home/dell/workspace/scripts/infraestrutura/rebuild-infra-test.sh
```

### Teste no Windows/PowerShell

```powershell
cd $HOME\workspace\scripts\infraestrutura
Set-ExecutionPolicy -Scope Process Bypass
.\rebuild-infra-test.ps1
```

Tambem e possivel testar o script PowerShell usando Docker:

```bash
docker run --rm \
  -v /home/dell/workspace/scripts/infraestrutura:/work:ro \
  -v /tmp:/tmp \
  mcr.microsoft.com/powershell:latest \
  pwsh -NoProfile -ExecutionPolicy Bypass \
  -File /work/rebuild-infra-test.ps1 \
  -TestWorkspace /tmp/archlab-win-test
```

## Gerar a infraestrutura no Linux

Entre no diretorio dos scripts:

```bash
cd /home/dell/workspace/scripts/infraestrutura
```

De permissao de execucao e rode o script principal:

```bash
chmod +x rebuild-infra.sh
./rebuild-infra.sh
```

Ao terminar, a infra sera criada em:

```text
/home/dell/workspace/infra
```

## Gerar a infraestrutura no Windows

Abra o PowerShell, entre no diretorio dos scripts e libere a execucao apenas para esta sessao:

```powershell
cd $HOME\workspace\scripts\infraestrutura
Set-ExecutionPolicy -Scope Process Bypass
.\rebuild-infra.ps1
```

Se sua pasta de workspace for outra:

```powershell
.\rebuild-infra.ps1 -Workspace "C:\caminho\para\workspace"
```

Se o IP do host for outro:

```powershell
.\rebuild-infra.ps1 -HostIP "192.168.0.123"
```

## Ajustes obrigatorios antes de subir os containers

Depois da geracao, faca estes ajustes na pasta `infra`.

### 1. Criar a senha do step-ca

Linux:

```bash
echo 'TROQUE_POR_UMA_SENHA_FORTE' > /home/dell/workspace/infra/step-ca/data/secrets/password.txt
chmod 600 /home/dell/workspace/infra/step-ca/data/secrets/password.txt
```

Windows:

```powershell
Set-Content -Path "$HOME\workspace\infra\step-ca\data\secrets\password.txt" -Value "TROQUE_POR_UMA_SENHA_FORTE" -NoNewline
```

### 2. Alterar senhas dos arquivos `.env`

Edite os arquivos abaixo e troque valores como `TROCAR_SENHA_FORTE`, `changeit` e `trocar-esta-senha`:

- `infra/kafka/.env`
- `infra/keycloak/.env`
- `infra/observabilidade/prometheus-grafana/.env`

### 3. Preparar o secret do Prometheus

Preencha o arquivo:

```text
infra/observabilidade/prometheus-grafana/prometheus/secrets/keycloak-client-secret
```

Esse valor deve ser o client secret do realm `bancario` no Keycloak.

## Primeira subida da infraestrutura

Na primeira vez, use uma sequencia um pouco mais cuidadosa para inicializar a CA interna e gerar os certificados do Kafka.

### Linux

Suba primeiro o `step-ca`:

```bash
cd /home/dell/workspace/infra/step-ca
docker compose up -d
```

Se o container inicializar a CA e parar, suba novamente:

```bash
docker compose up -d
```

Exporte e instale o certificado raiz:

```bash
docker exec step-ca cat /home/step/certs/root_ca.crt > /home/dell/workspace/infra/step-ca/root_ca.crt
sudo cp /home/dell/workspace/infra/step-ca/root_ca.crt /usr/local/share/ca-certificates/archlab-ca.crt
sudo update-ca-certificates
sudo systemctl restart docker
```

Gere os certificados Kafka:

```bash
cd /home/dell/workspace/infra/kafka
./generate-kafka-certs.sh
```

Depois suba todos os servicos:

```bash
/home/dell/workspace/infra/infra-up.sh
```

Se preferir subir manualmente, use esta ordem:

```bash
cd /home/dell/workspace/infra/step-ca && docker compose up -d
cd /home/dell/workspace/infra/traefik && docker compose up -d
cd /home/dell/workspace/infra/jaeger && docker compose up -d
cd /home/dell/workspace/infra/kafka && ./generate-kafka-certs.sh && docker compose up -d
cd /home/dell/workspace/infra/keycloak && docker compose up -d
cd /home/dell/workspace/infra/gitlab && docker compose up -d
cd /home/dell/workspace/infra/gitlab-runner && docker compose up -d
cd /home/dell/workspace/infra/observabilidade/prometheus-grafana && docker compose up -d
cd /home/dell/workspace/infra/arcane && docker compose up -d
```

### Windows

Suba primeiro o `step-ca`:

```powershell
cd $HOME\workspace\infra\step-ca
docker compose up -d
```

Exporte o certificado raiz:

```powershell
docker exec step-ca cat /home/step/certs/root_ca.crt > "$HOME\workspace\infra\step-ca\root_ca.crt"
```

Depois importe `root_ca.crt` em `Certificados > Autoridades de Certificacao Raiz Confiaveis`.

Gere os certificados Kafka:

```powershell
PowerShell -ExecutionPolicy Bypass -File "$HOME\workspace\infra\kafka\generate-kafka-certs.ps1"
```

Depois suba todos os servicos:

```powershell
PowerShell -ExecutionPolicy Bypass -File "$HOME\workspace\infra\infra-up.ps1"
```

## Verificar se esta funcionando

Linux:

```bash
docker ps --format 'table {{.Names}}\t{{.Status}}'
curl -sk https://ca.lab.home:9000/health
curl -sIL http://traefik.lab.home
curl -sk https://gitlab.lab.home/-/health
curl -sk https://keycloak.lab.home/health/ready
curl -sk https://prometheus.lab.home/-/healthy
curl -sk https://grafana.lab.home/api/health
```

Windows:

```powershell
docker ps --format "table {{.Names}}`t{{.Status}}"
```

Depois acesse pelo navegador:

- `https://traefik.lab.home`
- `https://gitlab.lab.home`
- `https://registry.lab.home`
- `https://keycloak.lab.home`
- `https://jaeger.lab.home`
- `https://prometheus.lab.home`
- `https://grafana.lab.home`
- `https://arcane.lab.home`

## Acessos iniciais e pos-configuracao

- GitLab: apos subir, obtenha a senha inicial com `docker exec gitlab grep 'Password:' /etc/gitlab/initial_root_password`.
- GitLab Runner: registre depois que o GitLab estiver saudavel com `docker exec -it gitlab-runner gitlab-runner register`.
- Arcane: primeiro login `arcane / arcane-admin`; altere imediatamente.
- Grafana: usuario e senha ficam em `infra/observabilidade/prometheus-grafana/.env`.
- Keycloak: usuario e senha ficam em `infra/keycloak/.env`.

## Parar a infraestrutura

Linux:

```bash
/home/dell/workspace/infra/infra-down.sh
```

Windows:

```powershell
PowerShell -ExecutionPolicy Bypass -File "$HOME\workspace\infra\infra-down.ps1"
```

## Observacoes importantes

- Os scripts sao idempotentes: se um arquivo ja existir, ele sera preservado.
- Se quiser recriar um arquivo especifico, renomeie ou remova apenas esse arquivo antes de executar o script novamente.
- A resolucao de `*.lab.home` precisa apontar para `192.168.0.102`.
- O GitLab pode levar alguns minutos para ficar pronto na primeira execucao.
- O Kafka depende do `step-ca` ja inicializado e dos certificados gerados.
- Em ambiente real, troque todas as senhas padrao antes de expor os servicos na rede.
