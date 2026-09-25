#!/bin/sh
set -eu

cd "$(dirname "$0")"
mkdir -p backups

docker compose -f docker-compose.prod.yaml exec -T db pg_dump -U naji NajiDB | gzip > "backups/naji-$(date +%F).sql.gz"
find backups -name 'naji-*.sql.gz' -mtime +7 -delete
