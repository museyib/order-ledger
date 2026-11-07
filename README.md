# Order Ledger API

Spring Boot REST API for an admin-managed product catalog and JWT-secured order lifecycle tracking.

## Assumptions

- The repo name `order-ledger` was treated as the domain definition, so the API implements a product catalog plus order placement and immutable status history.
- Authentication uses JWT access tokens plus rotating refresh tokens.
- Product writes are admin-only.
- Order reads are owner-scoped for regular users; admins can read every order.
- Order transitions assume:
  - `CREATED -> PAID -> SHIPPED -> COMPLETED`
  - `CREATED -> CANCELLED`
  - `PAID -> CANCELLED`
  - `CANCELLED` and `COMPLETED` are terminal
- Cancellation releases reserved inventory exactly once.
- Non-admin users may cancel their own orders, but only admins may move orders into `PAID`, `SHIPPED`, or `COMPLETED`.

## Stack

- Java 17
- Spring Boot
- Spring MVC, Spring Security, Spring Data JPA
- PostgreSQL + Flyway
- JJWT for access tokens
- H2 for tests
- Springdoc OpenAPI
- MapStruct + Lombok

## Configuration

Environment-driven settings are defined in [src/main/resources/application.yaml](/home/museyib/IdeaProjects/order-ledger/src/main/resources/application.yaml).

Required production variables:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `APP_JWT_SECRET`

Optional variables:

- `APP_JWT_ISSUER`
- `APP_JWT_ACCESS_TTL_MINUTES`
- `APP_JWT_REFRESH_TTL_DAYS`
- `APP_MAX_PAGE_SIZE`
- `APP_BOOTSTRAP_ADMIN_EMAIL`
- `APP_BOOTSTRAP_ADMIN_PASSWORD`
- `APP_BOOTSTRAP_ADMIN_DISPLAY_NAME`

## Run

```bash
./mvnw spring-boot:run
```

Swagger UI is exposed at `/swagger-ui.html`.

## Auth Routes

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`

Registration creates `USER` accounts. To create an initial admin account, set `APP_BOOTSTRAP_ADMIN_EMAIL` and `APP_BOOTSTRAP_ADMIN_PASSWORD`.

## Product Routes

- `POST /api/products` admin only
- `PUT /api/products/{productId}` admin only
- `GET /api/products/{productId}`
- `GET /api/products`

Pagination response fields:

- `items`
- `pageNumber`
- `pageSize`
- `totalElements`
- `totalPages`
- `first`
- `last`

Allowed product sort fields:

- `sku`
- `name`
- `unitPrice`
- `availableStock`
- `createdAt`

## Order Routes

- `POST /api/orders`
- `GET /api/orders`
- `GET /api/orders/{orderId}`
- `GET /api/orders/{orderId}/history`
- `POST /api/orders/{orderId}/transitions`

Order creation rules:

- Clients submit only product IDs and quantities.
- Duplicate product lines are rejected.
- Prices, totals, and stock validation are server-side only.
- Product name, SKU, and unit price are snapshotted into order lines.
- Stock reservation, order persistence, and initial history append happen in one transaction.

Transition request example:

```json
{
  "targetStatus": "CANCELLED",
  "reason": "Customer requested cancellation"
}
```

## Persistence

- Flyway schema is in [src/main/resources/db/migration/V1__initial_schema.sql](/home/museyib/IdeaProjects/order-ledger/src/main/resources/db/migration/V1__initial_schema.sql).
- Hibernate DDL generation is disabled; entity mappings are validated against Flyway.
- History rows are append-only and returned in chronological order.

## Tests

The test suite covers:

- registration, refresh, authentication, and authorization boundaries
- paginated product listing validation
- atomic order creation and stock decrement
- duplicate line rejection
- cancellation restock behavior
- concurrent ordering against limited stock
- rollback when history persistence fails
