-- Учебная БД для Shop Manager (PostgreSQL 16)
DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'shop') THEN
    CREATE ROLE shop LOGIN PASSWORD 'shop';
  ELSE
    ALTER ROLE shop WITH LOGIN PASSWORD 'shop';
  END IF;
END$$;

SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE datname = 'shop_manager' AND pid <> pg_backend_pid();

DROP DATABASE IF EXISTS shop_manager;
CREATE DATABASE shop_manager OWNER shop;
GRANT ALL PRIVILEGES ON DATABASE shop_manager TO shop;

\c shop_manager
GRANT ALL ON SCHEMA public TO shop;
ALTER SCHEMA public OWNER TO shop;
