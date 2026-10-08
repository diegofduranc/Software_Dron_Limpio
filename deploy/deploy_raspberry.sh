#!/usr/bin/env bash
# Despliegue del backend DRON en Raspberry Pi 4.
# Uso (ejecutar DENTRO de la Pi, en una copia de este repo):
#   bash deploy/deploy_raspberry.sh
set -euo pipefail

REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SERVICE_NAME="dron-backend.service"
MAVLINK_DEVICE="${MAVLINK_DEVICE:-/dev/serial0}"
MAVLINK_BAUD="${MAVLINK_BAUD:-57600}"
DB_PASSWORD="${DB_PASSWORD:-dronix_password}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-CAMBIA_ESTO}"

echo "==> 1/5  Dependencias del sistema"
sudo apt update
sudo apt install -y python3-pip python3-venv postgresql postgresql-contrib

echo "==> Configurando PostgreSQL"
sudo -u postgres psql -c "DO \$\$ BEGIN IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'dronix_user') THEN CREATE ROLE dronix_user LOGIN PASSWORD '${DB_PASSWORD}'; ELSE ALTER ROLE dronix_user WITH PASSWORD '${DB_PASSWORD}'; END IF; END \$\$;"
sudo -u postgres psql -tc "SELECT 1 FROM pg_database WHERE datname = 'drones'" | grep -q 1 || sudo -u postgres createdb -O dronix_user drones

echo "==> 2/5  Entorno virtual y dependencias Python"
python3 -m venv "$REPO_DIR/.venv"
"$REPO_DIR/.venv/bin/pip" install --upgrade pip
"$REPO_DIR/.venv/bin/pip" install -r "$REPO_DIR/requirements.txt"

echo "==> 3/5  Servicio systemd"
sudo cp "$REPO_DIR/deploy/$SERVICE_NAME" "/etc/systemd/system/$SERVICE_NAME"
# Ajustar rutas si el repo no está en /home/pi/Software_Dron_Limpio
sudo sed -i "s|/home/pi/Software_Dron_Limpio|$REPO_DIR|g" "/etc/systemd/system/$SERVICE_NAME"
sudo sed -i "s|CAMBIA_DB_PASSWORD|$DB_PASSWORD|g; s|CAMBIA_ADMIN_PASSWORD|$ADMIN_PASSWORD|g; s|/dev/serial0|$MAVLINK_DEVICE|g; s|57600|$MAVLINK_BAUD|g" "/etc/systemd/system/$SERVICE_NAME"
DB_URL="postgresql+psycopg2://dronix_user:${DB_PASSWORD}@127.0.0.1:5432/drones" \
	"$REPO_DIR/.venv/bin/python" -m backend.create_tables
sudo systemctl daemon-reload
sudo systemctl enable --now "$SERVICE_NAME"

echo "==> 4/5  Estado del servicio"
sleep 2
systemctl status "$SERVICE_NAME" --no-pager || true

echo "==> 5/5  Verificación"
echo "  - Login:    curl -X POST http://localhost:8000/api/auth/login -H 'Content-Type: application/json' -d '{\"username\":\"admin\",\"password\":\"CAMBIA_ESTO\"}'"
echo "  - Health:   curl http://localhost:8000/health"
echo "  - WS:       python3 scripts/test_websocket.py"
echo
echo "Recuerda:"
echo "  * UART habilitado (raspi-config -> Interface Options -> Serial: NO login, SI hardware)"
echo "  * MAVLINK_DEVICE=$MAVLINK_DEVICE (editar en /etc/systemd/system/$SERVICE_NAME si usas /dev/ttyACM0)"
echo "  * DRON_VISION_ENABLED=0 ya viene en el servicio (requerido en Pi 4)"
