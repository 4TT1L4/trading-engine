# In-Memory Trading Engine

Spring Boot (Gradle) application implementing a simple in-memory trading
system with accounts and limit BUY orders. Designed to be safe under
concurrent requests.

## Run

The project uses Docker Compose via Make:

Start: `make up`

Rebuild: `make rebuild`

Stop: `make down`

Logs: `make logs`

Clean: `make clean`

App runs at: http://localhost:8080

## API tests

An Insomnia collection is included (`insomnia-tests.yaml`).

Import it and run requests against: http://localhost:8080

## Available Endpoints

### Accounts

-   `POST /api/accounts` --- create a new account with initial balances\
-   `GET /api/accounts` --- list all accounts\
-   `GET /api/accounts/{accountId}` --- retrieve a specific account by
    id

### Orders

-   `POST /api/orders` --- create a new limit BUY order\
-   `GET /api/orders` --- list all orders\
-   `GET /api/orders/{orderId}` --- retrieve an order by id\
-   `GET /api/orders?status=OPEN|EXECUTING|FILLED` --- filter orders by
    status\
-   `POST /api/orders/fill?price=...` --- execute eligible orders at a
    given market price
