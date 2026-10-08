from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from backend.config import DB_URL

# Use PostgreSQL only (configured via env var). Enable pool_pre_ping to avoid stale connections
engine = create_engine(DB_URL, pool_pre_ping=True, future=True)
# Explicit sessionmaker settings for clarity
SessionLocal = sessionmaker(bind=engine, autoflush=False, autocommit=False)


def initialize_database() -> None:
	from backend.db.models import Base

	Base.metadata.create_all(bind=engine)


def check_database() -> bool:
	from sqlalchemy import text

	with engine.connect() as connection:
		connection.execute(text("SELECT 1"))
	return True
