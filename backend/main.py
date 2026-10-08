# backend/main.py
import os
import logging
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi import Header
from typing import Optional
import logging

from backend.api import auth, rest, websocket, camera_stream
from backend.config import API_HOST, API_PORT, CAMERA_ENABLED, LOG_LEVEL, MAVLINK_BAUD, detect_mavlink_device
from backend.db.database import check_database, initialize_database
from backend.db.repository import get_drones, get_mission
try:
    from sqlalchemy import text
except ImportError:
    text = lambda x: x
logger = logging.getLogger(__name__)


def _seed_default_admin() -> None:
    """Crea el admin inicial si no existe ningún usuario (primer arranque)."""
    try:
        if auth.store.list_users():
            return
        username = os.getenv('ADMIN_USERNAME')
        password = os.getenv('ADMIN_PASSWORD')
        
        if not username or not password:
            logger.warning(
                "ADMIN_USERNAME y ADMIN_PASSWORD no configurados. "
                "Usando valores por defecto (cambiar en producción)."
            )
            username = 'admin'
            password = 'CAMBIAR_ESTA_CONTRASENA'
        
        auth.ensure_admin_user(username, password)
        if password == 'CAMBIAR_ESTA_CONTRASENA':
            logger.warning(
                "🔑 Usuario admin creado con contraseña por defecto. "
                "Cámbiala con: python backend/create_user.py %s <nueva_password> --role admin",
                username,
            )
        else:
            logger.info("✅ Usuario admin '%s' creado", username)
    except Exception as e:
        logger.error("Error creando admin inicial: %s", e)

logging.basicConfig(
    level=LOG_LEVEL,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)

logger = logging.getLogger(__name__)

app = FastAPI(
    title="Drone Control API",
    description="API REST para control de dron vía MAVLink",
    version="1.0.0"
)

# CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Incluir rutas
app.include_router(auth.router)  # /api/auth/* (prefix propio)
app.include_router(rest.router, prefix="/api", tags=["drone"])
app.include_router(websocket.router)  # WebSocket no lleva prefix
app.include_router(camera_stream.router)  # Ya tiene su propio prefix

# Inicialización en eventos de ciclo de vida
@app.on_event("startup")
def startup_event():
    import threading
    import asyncio as _asyncio

    try:
        initialize_database()
        logger.info("PostgreSQL disponible y tablas verificadas")
    except Exception as e:
        logger.error("No se pudo inicializar PostgreSQL: %s", e)

    _seed_default_admin()

    # Capturar el event loop del main thread para usarlo desde background threads
    try:
        _main_loop = _asyncio.get_event_loop()
    except RuntimeError:
        _main_loop = None

    def _init_mavlink_bg():
        try:
            device = detect_mavlink_device()
            logger.info(f"Selected MAVLink device: {device}")
            rest.init_mav(device, MAVLINK_BAUD)
            rest.start_monitoring(MAVLINK_BAUD, interval=5)
            if CAMERA_ENABLED:
                try:
                    camera_stream.camera.start(width=640, height=480, fps=30)
                    logger.info("✅ Cámara RealSense iniciada")
                except Exception as e:
                    logger.warning(f"⚠️ Error iniciando cámara (continuando sin ella): {e}")
            if getattr(rest, 'mav', None):
                def _setup_params_bg():
                    try:
                        rest.mav.setup_params()
                        logger.info("✅ Parámetros críticos configurados")
                    except Exception as e:
                        logger.warning("⚠️ Error configurando parámetros: %s", e)
                threading.Thread(target=_setup_params_bg, daemon=True).start()
                websocket.start_telemetry_broadcast(rest.mav, loop=_main_loop)
                logger.info("✅ Telemetry WebSocket iniciado")
                def vision_emergency():
                    try:
                        rest.mav.set_mode("BRAKE")
                        logger.warning("🛑 Vision auto-avoid: BRAKE mode set")
                    except Exception as e:
                        logger.error(f"Vision emergency failed: {e}")
                camera_stream.set_emergency_callback(vision_emergency)
                logger.info("✅ Vision → MAVLink emergency callback registrado")
                rest.init_sensors(rest.mav)
                rest.init_navigation(rest.mav)
                logger.info("✅ Sensores y navegación autónoma iniciados")
            else:
                logger.warning("⚠️ MAV controller no disponible, WebSocket no iniciado")
        except Exception as e:
            logger.error(f"Failed to initialize MAVLink: {e}")

    threading.Thread(target=_init_mavlink_bg, daemon=True).start()
    logger.info("✅ API lista — inicializando MAVLink en background...")

@app.on_event("shutdown")
def shutdown_event():
    try:
        rest.shutdown_sensors_and_nav()
        if CAMERA_ENABLED:
            try:
                camera_stream.camera.stop()
                logger.info("✅ Cámara detenida")
            except Exception as e:
                logger.error(f"Error deteniendo cámara: {e}")
        if getattr(rest, 'mav', None):
            try:
                conn = getattr(rest.mav, 'conn', None)
                if conn is not None:
                    conn.disconnect()
                    logger.info("✅ MAVLink desconectado")
            except Exception as e:
                logger.error(f"Error disconnecting MAV: {e}")
    except Exception as e:
        logger.error(f"Error during shutdown cleanup: {e}")

@app.get("/")
def root():
    return {
        "message": "Drone Control API",
        "version": "1.0.0",
        "status": "running"
    }

@app.get("/health")
def health():
    try:
        check_database()
        return {"status": "healthy", "database": "connected"}
    except Exception:
        return {"status": "degraded", "database": "unavailable"}

# Rutas adicionales para el frontend (protegidas opcionalmente - si hay token, se verifica; si no, datos de demo)
# Rutas de lista - protección opcional: si viene token Bearer, se verifica;
# sino, retorna datos de demo para compatibilidad frontend durante transición.
def _get_current_user_token(authorization: Optional[str] = Header(None)):
    """Extrae y valida token opcional de auth."""
    from backend.api.auth import _extract_bearer, get_user_from_token
    token = _extract_bearer(authorization)
    if token:
        return get_user_from_token(token)
    return None

@app.get("/drones")
def list_drones(authorization: Optional[str] = Header(None)):
    user = _get_current_user_token(authorization)
    if user:
        drones = get_drones()
        if drones:
            return {"message": f"Usuario {user['username']} autenticado", "drones": [
                {"id": d.id, "name": d.name, "status": d.status, "model": d.model} for d in drones
            ]}
    # Fallback a datos de demo si no hay base de datos configurada
    return [
        {"id": 1, "name": "Test Drone", "status": "idle", "model": "PX4"},
        {"id": 2, "name": "Drone Secundario", "status": "offline", "model": "Pixhawk"}
    ]

@app.get("/missions")
def list_missions(authorization: Optional[str] = Header(None)):
    user = _get_current_user_token(authorization)
    if user:
        mission = get_mission(1)  # Get first mission or None
        if mission:
            return {"message": f"Usuario {user['username']} autenticado", "missions": [{
                "id": mission.id,
                "name": mission.name,
                "status": mission.status,
                "progress_percent": mission.progress_percent,
                "total_distance": mission.total_distance or 0
            }]}
    # Fallback a datos de demo
    return [
        {"id": 1, "name": "Mission Alpha", "status": "paused", "progress_percent": 0}
    ]

@app.get("/users")
def list_users(authorization: Optional[str] = Header(None)):
    user = _get_current_user_token(authorization)
    if user:
        # Try to get users from auth store
        all_users = auth.store.list_users()
        if all_users:
            return {"message": f"Usuario {user['username']} autenticado", "users": [
                {"id": i+1, "username": uname, "role": "operator"} 
                for i, uname in enumerate(all_users[:10])  # Limit to 10
            ]}
    # Fallback a datos de demo
    return [
        {"id": 1, "username": "operator", "role": "pilot"},
        {"id": 2, "username": "observer", "role": "observer"}
    ]

@app.get("/flight-routes")
def list_routes(authorization: Optional[str] = Header(None)):
    user = _get_current_user_token(authorization)
    if user:
        # Try to get routes from database (using missions data as fallback)
        mission = get_mission(1)
        if mission:
            return {"message": f"Usuario {user['username']} autenticado", "routes": [{
                "id": mission.id,
                "name": mission.name,
                "total_distance": mission.total_distance or 1.2
            }]}
    # Fallback a datos de demo
    return [
        {"id": 1, "name": "Route 1", "total_distance": 1.2},
        {"id": 2, "name": "Route 2", "total_distance": 2.5}
    ]

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        app,
        host=API_HOST,
        port=API_PORT,
        ws_ping_interval=20,   # ping cada 20s para detectar half-open
        ws_ping_timeout=10,    # timeout 10s para considerar muerta la conexión
    )