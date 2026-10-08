# Software_Dron_Limpio

Backend limpio del sistema de control de dron (DRON) para Raspberry Pi 4.
Contiene únicamente el código en producción: API FastAPI, MAVLink, sensores,
navegación autónoma y despliegue en la Pi. La app móvil se desarrolla aparte.

## Estructura

| Directorio | Contenido |
|---|---|
| `backend/` | FastAPI: API REST, WebSocket, MAVLink, visión |
| `backend/api/` | rest.py, websocket.py, auth.py, camera_stream.py |
| `backend/mavlink/` | connection.py, controller.py, commands.py, telemetry.py, rc_override.py, geo_utils.py |
| `backend/vision/` | detector.py (YOLOv8n — desactivado en Pi 4) |
| `backend/navigation/` | avoidance.py, planner.py, controller.py |
| `backend/sensors/` | manager.py, mtf01.py, ydlidar_x4.py, depth_camera.py, obstacle_map.py |
| `backend/db/` | database.py, models.py, repository.py |
| `raspberry/` | Bridge standalone para sensores en la Pi |
| `scripts/` | sim_drone.py, start_all, test_websocket.py |
| `deploy/` | dron-backend.service, deploy_raspberry.sh |

## Uso

```bash
# PostgreSQL local con Docker
docker compose up -d postgres
python -m backend.create_tables
python backend/create_user.py admin "CambiaEstaClaveSegura1!" --role admin

# Desarrollo local / simulación
.\scripts\start_all.ps1        # Windows
./scripts/start_all.sh         # Linux

# Backend directo (maquina local)
python -m backend.run

# Despliegue en Raspberry Pi 4
bash deploy/deploy_raspberry.sh
```

El backend usa PostgreSQL mediante `DB_URL`. Para desarrollo, copia
`backend/.env.example` como `backend/.env` y ajusta las credenciales si no
usas el `docker-compose.yml` incluido. El endpoint `/health` debe responder
con `{"status":"healthy","database":"connected"}` antes de conectar la
aplicación Android.

### App Android

Desde `App_Dron`, usa Android Studio para sincronizar Gradle y compilar el APK
debug. Este checkout no incluye `gradlew.bat`, por lo que la compilación por
línea de comandos requiere instalar Gradle o regenerar el wrapper:

```powershell
cd App_Dron
gradle :app:assembleDebug
```

En la aplicación configura la IP del equipo que ejecuta FastAPI y el puerto
`8000`. El backend debe estar accesible desde el dispositivo Android en la LAN.

## Raspberry Pi 4

- Hostname: DRONE-2 · IP: `10.252.200.235` · Puerto: 8000
- `DRON_VISION_ENABLED=0` es obligatorio en Pi 4 (evita SIGILL de PyTorch/YOLO)
- Iniciar siempre con `python3 -m backend.run` (configura el ping/pong WebSocket)
- Ver `deploy/dron-backend.service` y `deploy/deploy_raspberry.sh`

### Instalación en Raspberry Pi

En la Pi, clona el repositorio y ejecuta el despliegue con contraseñas reales:

```bash
cd /home/pi
git clone <URL_DEL_REPOSITORIO> Software_Dron_Limpio
cd Software_Dron_Limpio
export DB_PASSWORD='UnaClavePostgresSegura1!'
export ADMIN_PASSWORD='UnaClaveAdminSegura1!'
export MAVLINK_DEVICE=/dev/serial0
export MAVLINK_BAUD=57600
bash deploy/deploy_raspberry.sh
```

Después, habilita la UART del Pixhawk y comprueba el servicio:

```bash
sudo raspi-config
sudo systemctl status dron-backend --no-pager
curl http://127.0.0.1:8000/health
```

La aplicación Android debe apuntar a la IP de la Raspberry en la red local y
al puerto `8000`. La cámara queda desactivada en esta primera fase.
