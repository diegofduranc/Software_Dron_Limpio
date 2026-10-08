# AGENTS.md - Instrucciones persistentes del proyecto

## Descripción del Proyecto

Backend limpio del sistema de control y telemetría para drones (UAV) basado en
ArduPilot. Solo contiene el código que corre en la Raspberry Pi 4 o en una
máquina de desarrollo. La app móvil se desarrolla en un repositorio aparte.

## Arquitectura

```
App Móvil (externa) ←→ Backend (FastAPI :8000) ←→ Pixhawk/SITL (MAVLink)
                              ↓
                      Sensores: MTF01, YDLidar X4, RealSense
```

- **Backend:** Python 3.11+ / FastAPI / pymavlink / PostgreSQL
- **Visión:** YOLOv8n (desactivada en Pi 4 vía `DRON_VISION_ENABLED=0`)
- **Hardware:** Raspberry Pi 4 + Pixhawk

## Estructura del Proyecto

| Directorio | Contenido |
|---|---|
| `backend/` | FastAPI: API REST, WebSocket, MAVLink, visión |
| `backend/api/` | rest.py, websocket.py, auth.py, camera_stream.py |
| `backend/mavlink/` | connection.py, controller.py, commands.py, telemetry.py, rc_override.py, geo_utils.py |
| `backend/vision/` | detector.py (YOLOv8n) |
| `backend/navigation/` | avoidance.py, planner.py, controller.py |
| `backend/sensors/` | manager.py, base.py, mtf01.py, ydlidar_x4.py, depth_camera.py, obstacle_map.py |
| `backend/db/` | database.py, models.py, repository.py |
| `raspberry/` | Versión standalone para Raspberry Pi |
| `scripts/` | sim_drone.py, start_all, test_websocket.py |
| `deploy/` | dron-backend.service, deploy_raspberry.sh |

## Convenciones de Código

- Clases: `PascalCase`; funciones/variables: `snake_case`; constantes: `UPPER_SNAKE_CASE`
- Métodos privados: prefijo `_`
- Type hints obligatorios en funciones públicas
- Logging: `logging.getLogger(__name__)`

## Comandos

```bash
python -m backend.run          # Backend (siempre este comando, incluye WS ping/pong)
.\scripts\start_all.ps1        # Simulación Windows
./scripts/start_all.sh         # Simulación Linux
```

## Raspberry Pi 4

- Hostname: DRONE-2 · IP: `10.252.200.235` · Puerto: 8000
- Servicio: `dron-backend.service` (en `deploy/`)
- `ExecStart` = `python3 -m backend.run` (nunca `uvicorn backend.main:app` directo)
- `DRON_VISION_ENABLED=0` obligatorio en Pi 4
- Armado solo con GPS fix (exterior). Indoor (`satellites:0`) bloquea prearm; no es bug.

## Reglas para el Asistente

1. Leer este archivo al inicio de cada sesión
2. No agregar comentarios en código a menos que el usuario los pida
3. Usar tablas en lugar de árbol ASCII para directorios en documentos
4. Estilo formal académico para documentos técnicos
5. Preferir ediciones sobre reescrituras completas
6. No commitear a menos que el usuario lo pida explícitamente
