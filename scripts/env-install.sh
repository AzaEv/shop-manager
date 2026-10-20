#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

sudo apt-get update -y
sudo DEBIAN_FRONTEND=noninteractive apt-get install -y \
  openjdk-17-jdk \
  postgresql \
  postgresql-contrib \
  curl \
  unzip \
  ca-certificates

sudo update-alternatives --set java /usr/lib/jvm/java-17-openjdk-amd64/bin/java || true
sudo update-alternatives --set javac /usr/lib/jvm/java-17-openjdk-amd64/bin/javac || true

export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"

chmod +x gradlew
./gradlew dependencies --no-daemon

sudo service postgresql start || sudo pg_ctlcluster 16 main start || true
sudo -u postgres psql -v ON_ERROR_STOP=1 -f scripts/setup-db.sql
