from sqlalchemy import Column, Integer, Float, Boolean, TIMESTAMP, String
from sqlalchemy.ext.declarative import declarative_base
import datetime

Base = declarative_base()

class Telemetry(Base):
    __tablename__ = "telemetry"

    id = Column(Integer, primary_key=True)
    altitude = Column(Float)
    speed = Column(Float)
    pitch = Column(Float)
    roll = Column(Float)
    yaw = Column(Float)
    battery = Column(Float)
    timestamp = Column(TIMESTAMP, default=datetime.datetime.now(datetime.timezone.utc))
    battery_remaining = Column(Float)
    satellites = Column(Integer)
    hdop = Column(Float)
    mode = Column(String(50))
    latitude = Column(Float)
    longitude = Column(Float)
    ground_speed = Column(Float)
    vertical_speed = Column(Float)

class Waypoint(Base):
    __tablename__ = "waypoints"

    id = Column(Integer, primary_key=True)
    name = Column(String(100))
    latitude = Column(Float)
    longitude = Column(Float)
    altitude = Column(Float)
    created_at = Column(TIMESTAMP, default=datetime.datetime.now(datetime.timezone.utc))

class Mission(Base):
    __tablename__ = "missions"

    id = Column(Integer, primary_key=True)
    name = Column(String(100))
    description = Column(String(255))
    status = Column(String(50))  # paused, running, completed, failed
    progress_percent = Column(Integer, default=0)
    total_distance = Column(Float)
    created_at = Column(TIMESTAMP, default=datetime.datetime.now(datetime.timezone.utc))
    waypoints_count = Column(Integer, default=0)

class Drone(Base):
    __tablename__ = "drones"

    id = Column(Integer, primary_key=True)
    name = Column(String(100))
    status = Column(String(50))  # idle, flying, offline
    model = Column(String(50))   # PX4, Pixhawk, etc.
    last_seen = Column(TIMESTAMP, default=datetime.datetime.now(datetime.timezone.utc))
    firmware = Column(String(50))
