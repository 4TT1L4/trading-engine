.PHONY: clean down down-test logs logs-app logs-exchange ps rebuild rebuild-nocache restart-app test test-e2e up

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
