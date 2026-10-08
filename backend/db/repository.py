from backend.db.database import SessionLocal
from backend.db.models import Telemetry, Waypoint, Mission, Drone
import datetime
import logging

logger = logging.getLogger(__name__)

def save_telemetry(data: dict):
    """Guarda un registro de telemetría en la base de datos."""
    try:
        with SessionLocal() as db:
            # Filtrar solo campos que existen en el modelo
            allowed_fields = {'altitude', 'speed', 'pitch', 'roll', 'yaw', 
                          'battery', 'battery_remaining', 'satellites', 'hdop',
                          'mode', 'latitude', 'longitude', 'ground_speed', 'vertical_speed'}
            filtered = {k: v for k, v in data.items() if k in allowed_fields}
            t = Telemetry(**filtered)
            db.add(t)
            db.commit()
            logger.info("Telemetry saved to database")
    except Exception as e:
        logger.error(f"Error saving telemetry: {e}")

def save_waypoint(data: dict):
    """Guarda un waypoint en la base de datos."""
    try:
        with SessionLocal() as db:
            # Validar campos requeridos
            if 'name' not in data or 'latitude' not in data or 'longitude' not in data:
                logger.warning("Waypoint requerido: name, latitude, longitude")
                return False
            wp = Waypoint(
                name=data.get('name', 'unnamed'),
                latitude=data['latitude'],
                longitude=data['longitude'],
                altitude=data.get('altitude', 0.0)
            )
            db.add(wp)
            db.commit()
            logger.info(f"Waypoint '{wp.name}' saved to database")
            return True
    except Exception as e:
        logger.error(f"Error saving waypoint: {e}")
        return False

def save_mission(data: dict):
    """Guarda una misión en la base de datos."""
    try:
        with SessionLocal() as db:
            mission = Mission(
                name=data.get('name', 'unnamed'),
                description=data.get('description', ''),
                status=data.get('status', 'paused'),
                progress_percent=data.get('progress_percent', 0),
                total_distance=data.get('total_distance', 0.0),
                waypoints_count=data.get('waypoints_count', 0)
            )
            db.add(mission)
            db.commit()
            logger.info(f"Mission '{mission.name}' saved to database")
            return mission.id
    except Exception as e:
        logger.error(f"Error saving mission: {e}")
        return False

def update_mission_progress(mission_id: int, progress: int, status: str = None, total_distance: float = None):
    """Actualiza el progreso de una misión."""
    try:
        with SessionLocal() as db:
            mission = db.query(Mission).filter(Mission.id == mission_id).first()
            if mission:
                mission.progress_percent = progress
                if status:
                    mission.status = status
                if total_distance is not None:
                    mission.total_distance = total_distance
                db.commit()
                logger.info(f"Mission {mission_id} progress updated: {progress}%")
                return True
            return False
    except Exception as e:
        logger.error(f"Error updating mission progress: {e}")
        return False

def get_mission(mission_id: int):
    """Obtiene una misión por ID."""
    try:
        with SessionLocal() as db:
            return db.query(Mission).filter(Mission.id == mission_id).first()
    except Exception as e:
        logger.error(f"Error getting mission: {e}")
        return None

def save_drone(data: dict):
    """Guarda información del drone en la base de datos."""
    try:
        with SessionLocal() as db:
            drone = Drone(
                name=data.get('name', 'unknown'),
                status=data.get('status', 'idle'),
                model=data.get('model', 'unknown'),
                firmware=data.get('firmware', 'unknown')
            )
            # Usar upsert - verificar si existe o crear
            existing = db.query(Drone).filter(Drone.name == drone.name).first()
            if existing:
                existing.status = drone.status
                existing.model = drone.model
                existing.firmware = drone.firmware
                existing.last_seen = datetime.datetime.now(datetime.timezone.utc)
                db.commit()
            else:
                db.add(drone)
                db.commit()
            logger.info(f"Drone '{drone.name}' saved/updated in database")
            return True
    except Exception as e:
        logger.error(f"Error saving drone: {e}")
        return False

def get_drones():
    """Obtiene todos los drones registrados."""
    try:
        with SessionLocal() as db:
            return db.query(Drone).all()
    except Exception as e:
        logger.error(f"Error getting drones: {e}")
        return []
