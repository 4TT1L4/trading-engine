.PHONY: clean down down-test logs logs-app logs-exchange ps rebuild rebuild-nocache restart-app test test-e2e up fmt test build

up:
	docker compose up

down:
	docker compose down --remove-orphans

rebuild:
	docker compose down
	docker compose up --build

rebuild-nocache:
	docker compose down --volumes --remove-orphans
	docker compose build --no-cache
	docker compose up

logs:
	docker compose logs -f

logs-app:
	docker compose logs -f app

logs-exchange:
	docker compose logs -f exchange

restart-app:
	docker compose restart app

ps:
	docker compose ps

clean:
	docker compose down --volumes --remove-orphans
	docker system prune -f

# MSYS_NO_PATHCONV=1 stops Git Bash from converting paths (e.g. /app -> C:/Program Files/Git/app)
# See https://andydote.co.uk/2018/06/18/git-bash-docker-volume-paths/
fmt:
	MSYS_NO_PATHCONV=1 docker run --rm -v "$(CURDIR)/app:/app" -w /app gradle:8.7-jdk21 gradle spotlessApply --no-daemon

test:
	MSYS_NO_PATHCONV=1 docker run --rm -v "$(CURDIR)/app:/app" -w /app gradle:8.7-jdk21 gradle test --no-daemon

build:
	MSYS_NO_PATHCONV=1 docker run --rm -v "$(CURDIR)/app:/app" -w /app gradle:8.7-jdk21 gradle clean test bootJar --no-daemon

