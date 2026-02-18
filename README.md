# In-Memory Trading Engine

![CI](https://github.com/4TT1L4/trading-engine/actions/workflows/ci.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-brightgreen)
![Gradle](https://img.shields.io/badge/Gradle-8.x-blue)
![Concurrency](https://img.shields.io/badge/concurrency-safe-blueviolet)
![OpenAPI](https://img.shields.io/badge/OpenAPI-Swagger-green)
![License: Unlicense](https://img.shields.io/badge/license-Unlicense-blue)

[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=4TT1L4_trading-engine&metric=coverage)](https://sonarcloud.io/summary/new_code?id=4TT1L4_trading-engine)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=4TT1L4_trading-engine&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=4TT1L4_trading-engine)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=4TT1L4_trading-engine&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=4TT1L4_trading-engine)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=4TT1L4_trading-engine&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=4TT1L4_trading-engine)

Spring Boot (Gradle) application implementing a simple in-memory trading
system with accounts and limit BUY orders. Designed to be safe under
concurrent requests.

## Running the application

The project uses Docker Compose via Make:

Start: `make up`

Rebuild: `make rebuild`

Stop: `make down`

Logs: `make logs`

Clean: `make clean`

App runs at: http://localhost:8080

Swagger UI: http://localhost:8080/api/swagger-ui/index.html

## API tests

An Insomnia collection is included (`insomnia-tests.yaml`).

Import it and run requests against: http://localhost:8080

## Available Endpoints

### Accounts

-   `POST /api/accounts` --- create a new account with initial balances
-   `GET /api/accounts` --- list all accounts
-   `GET /api/accounts/{accountId}` --- retrieve a specific account by
    id

### Orders

-   `POST /api/orders` --- create a new limit BUY order
-   `GET /api/orders` --- list all orders
-   `GET /api/orders/{orderId}` --- retrieve an order by id
-   `GET /api/orders?status=OPEN|EXECUTING|FILLED` --- filter orders by
    status
-   `POST /api/orders/fill?price=...` --- execute eligible orders at a
    given market price

## Tests

Automated tests are executed during the Docker build.

Rebuild the image to run tests: `make rebuild`

The build fails if any test fails.

## Design Decisions

-   **In-memory storage**
    The system uses in-memory repositories to keep the focus on business
    logic and concurrency rather than persistence.

-   **Immutable domain models**
    `Account` and `Order` are implemented as immutable records to avoid
    shared mutable state and simplify thread safety.

-   **Thread-safe repositories**
    Repositories use `ConcurrentHashMap` with atomic updates
    (`computeIfPresent`) to ensure safe concurrent access.

-   **Per-account synchronization**
    A lock per account prevents double spending when multiple orders are
    executed concurrently.

-   **Order lifecycle state machine**
    Orders transition through:

        OPEN → EXECUTING → FILLED

    This ensures an order can only be executed once and prevents race
    conditions.

-   **Worker controlled via configuration**
    Background order filling can be enabled/disabled using configuration
    (`EXCHANGE_FILL_WORKER_ENABLED`), allowing deterministic test runs.

------------------------------------------------------------------------

## License

This project is released under **The Unlicense**.

    This is free and unencumbered software released into the public domain.

    Anyone is free to copy, modify, publish, use, compile, sell, or distribute this
    software, either in source code form or as a compiled binary, for any purpose,
    commercial or non-commercial, and by any means.

    In jurisdictions that recognize copyright laws, the author or authors of this
    software dedicate any and all copyright interest in the software to the public
    domain. We make this dedication for the benefit of the public at large and to
    the detriment of our heirs and successors. We intend this dedication to be an
    overt act of relinquishment in perpetuity of all present and future rights to
    this software under copyright law.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
    IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.
