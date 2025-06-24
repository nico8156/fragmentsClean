rm -rf ./docker-postgresql-data-for-dev
docker compose -f docker-compose-postgresql-dev.yml down -v
docker compose -f docker-compose-postgresql-dev.yml up
