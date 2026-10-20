#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"

if ! pg_isready -q; then
  sudo service postgresql start || sudo pg_ctlcluster 16 main start
fi

# Ensure role/db exist without wiping data
sudo -u postgres psql -v ON_ERROR_STOP=1 <<'SQL'
DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'shop') THEN
    CREATE ROLE shop LOGIN PASSWORD 'shop';
  END IF;
END$$;
SELECT 'CREATE DATABASE shop_manager OWNER shop'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'shop_manager')\gexec
SQL

sudo -u postgres psql -v ON_ERROR_STOP=1 -d shop_manager <<'SQL'
GRANT ALL ON SCHEMA public TO shop;
ALTER SCHEMA public OWNER TO shop;
SQL

pg_isready
echo "PostgreSQL ready for shop-manager (shop@localhost/shop_manager)"
