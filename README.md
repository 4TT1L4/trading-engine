# In-Memory Trading Engine

Spring Boot (Gradle) application implementing a simple in-memory trading
system with accounts and limit BUY orders. Designed to be safe under
concurrent requests.

## Run

The project uses Docker Compose via Make:

Start: `make up`

Rebuild: `make rebuild`

Stop: `make down`

Logs: `make logs make logs-app make logs-exchange`

Clean: `make clean`

App runs at: http://localhost:8080

## API tests

An Insomnia collection is included (`insomnia-tests.yaml`).

Import it and run requests against: http://localhost:8080
