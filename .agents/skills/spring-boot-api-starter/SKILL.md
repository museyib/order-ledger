---
name: spring-boot-api-starter
description: Create a new Java Spring Boot REST API with a reusable Maven, PostgreSQL, Flyway, JWT security, layered architecture, validation, and API documentation setup, using domain entities defined for each project.
metadata:
  short-description: Create a configurable Spring Boot REST API
---

# Spring Boot API Starter

Create a new REST API project with the common platform features below and domain features tailored to the user's definition. Domain classes vary from project to project; use the entities the user defines. Order processing and transaction history below are illustrative workload examples, not mandatory domain features. This skill does not require any source application or sample domain.

## Define the project

Before implementation, extract or ask for information that materially affects the model or API:

- Project name, Maven group/artifact, Java package, and desired Java/Spring Boot versions if prescribed.
- Domain entities, their fields and constraints, and relationships/cardinality.
- Which resources need CRUD, pagination, filtering, sorting, ownership, soft deletion, or other business rules.
- Whether authentication is needed. Default to the JWT account feature set below unless the user asks for a different security model.

Use reasonable conventional assumptions for incidental details and record them in the README. Avoid inventing consequential business rules. Example definition:

```text
Project: order-ledger; package:llc.devlab.app
Product: name, SKU (unique), unit price, and available stock
Order: customer reference and one or more distinct product/quantity lines
Order line: product and price/name snapshot, quantity, and calculated line total
Order status: CREATED, PAID, CANCELLED, SHIPPED, COMPLETED, with immutable transition history
Features: admin-managed product catalog, atomic order placement and stock reservation, state-pattern transitions, transaction history, and rollback on failure
Authentication: JWT; product writes are admin-only and order access follows the stated ownership rules
```

## Common platform feature set

Unless the user requests a smaller or different setup, include:

- Java and Spring Boot with Maven Wrapper.
- Spring MVC REST controllers, Spring Data JPA, Bean Validation, and Lombok. Use MapStruct for entity/DTO mapping where it improves clarity.
- PostgreSQL runtime database and Flyway versioned migrations as the schema source of truth. Disable Hibernate schema creation/update; keep JPA mappings and SQL migrations aligned.
- JWT authentication: registration, login, short-lived access token, refresh token, refresh rotation/revocation, BCrypt password hashing, and protected API routes. Store token secrets in environment-based configuration, never source code.
- Centralized API error handling with a consistent error response; validation errors should identify invalid fields without leaking internals.
- Springdoc OpenAPI/Swagger UI. Permit documentation endpoints explicitly.
- Consistent pagination metadata for paginated list responses: page number, page size, total elements, total pages, and `first`/`last` flags. Enforce a maximum page size.
- JPA auditing for creation/update timestamps and, when useful, actor identity.
- H2 and Spring Boot test dependencies for automated tests; test configuration should not require a running PostgreSQL database.
- `application.yaml` with environment-variable overrides for database credentials and JWT settings, and a README with setup, configuration, routes, and domain behavior.

Select current compatible dependency versions from Spring Initializr or official dependency management when implementing. If adapting an existing project, preserve its established versions and conventions unless asked to change them. Do not copy sample app names, packages, endpoints, schema, or domain rules.

## Package layout

Use a clear layered structure under the chosen application package. Adapt names to the project, but keep responsibilities separated:

```text
<package>/
  controller/       REST endpoints
  service/          business rules and transactions
  repository/       Spring Data persistence
  mapper/           entity/DTO mapping
  model/entity/     JPA entities
  model/dto/        request and response models
  model/enums/      domain enums, when needed
  exception/        domain errors and global handler
  security/         auth controllers, DTOs, entities, services, repositories, config
```

Keep request/response DTOs separate from JPA entities. Never serialize password hashes, refresh-token internals, or persistence-only fields. Controllers should handle HTTP concerns; services should enforce business rules and ownership; repositories should encapsulate persistence queries.

## Domain implementation

For every requested entity:

1. Define fields, Java types, nullability, lengths, uniqueness, defaults, indexes, and API exposure from the project definition.
2. Define JPA relationships and foreign keys from the stated cardinality. Make the owning side, join table, cascade behavior, and deletion semantics deliberate. Do not cascade deletion to related records by default.
3. Create input/output DTOs with Bean Validation annotations. MapStruct may generate mappings, but verify update mappings do not overwrite fields that clients cannot change (such as IDs, audit fields, owner, or soft-delete flags).
4. Implement requested CRUD and list operations. For collection endpoints, validate page and size (page must be non-negative, size positive and no larger than a documented maximum). Return a stable response envelope containing summary DTO items plus page number, page size, total elements, total pages, and `first`/`last` booleans. Map these values from Spring Data `Page<T>` metadata or calculate them accurately in a deliberate wrapper. Allow-list sort fields and directions; support multiple sort fields when requested instead of passing arbitrary client strings into persistence sorting. Return HTTP 400 with a clear message for invalid pagination, filters, or sort input.
5. Apply authenticated-user ownership filters only to user-owned data. Implement soft deletion only when requested or needed by an explicit domain rule.
6. Add domain errors for missing resources, conflicts, invalid transitions, and access violations where applicable. Ensure error status and response are consistent.
7. Write the Flyway migration to match JPA: table/column names, constraints, enum representation, foreign keys, join tables, and indexes. Include account and token tables when JWT is enabled.

### Order processing and transaction history example

Use this as an example workload only when order processing is part of the project definition. It exercises transactional writes, database constraints, state transitions, and immutable audit history. Follow the detailed integrity requirements in the Order processing workload section below.

- Model `Product`, `Order`, `OrderLine`, and append-only `OrderStatusHistory` records. Keep product catalog values separate from order-line snapshots so catalog updates cannot rewrite historical orders.
- Example API shape: admins create/update products; authenticated customers create orders with product IDs and quantities; customers or authorized staff read order details and chronological status history; authorized actors request lifecycle transitions. Never expose endpoints to edit or delete order lines or history.
- Keep one transactional service operation responsible for order creation, authoritative price loading, stock validation/decrement, initial status, line snapshots, and initial history. Any failed step must roll back all writes.
- Use a State Design Pattern to implement the allowed lifecycle, with `CREATED`, `PAID`, `CANCELLED`, `SHIPPED`, and `COMPLETED` as persisted statuses. Validate transitions before writing and append one history record atomically for each successful transition.
- Test line and order totals, inventory reduction, oversell prevention under concurrency, permitted and rejected transitions, history ordering/immutability, and rollback after failures in validation or persistence. Do not report measured performance or query counts unless actually measured.

A request DTO should contain product identifiers and quantities, but no trusted prices or totals:

```java
public record OrderLineRequest(
    @NotNull Long productId,
    @Positive int quantity
) {}

public record CreateOrderRequest(
    @NotEmpty @Valid List<OrderLineRequest> lines
) {}
```

The controller should delegate to the transactional service and return a response DTO:

```java
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
class OrderController {
    private final OrderService orderService;

    @PostMapping
    ResponseEntity<OrderDto> create(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(request));
    }

    @GetMapping("/{orderId}/history")
    List<OrderStatusHistoryDto> history(@PathVariable long orderId) {
        return orderService.getHistory(orderId);
    }
}
```

## Order processing workload

When the project definition includes product sales or order processing, treat the following as one cohesive domain workflow. Adapt names and endpoints, but preserve the integrity rules:

- **Product catalog:** Provide admin-authorized create and update operations for products, including a stable identifier, name, unit price, and current stock quantity. Validate non-negative prices and stock. Use exact decimal money types such as `BigDecimal` with an explicit database precision/scale; never use floating-point values for prices. Protect catalog writes with an explicit admin authorization rule.
- **Order creation:** Accept one or more distinct product IDs with positive quantities. Reject duplicate product lines or combine them deterministically before pricing. Load authoritative product prices from the database; never trust client-supplied prices or totals. Snapshot each product name/identifier and unit price into immutable order-line data at creation so later catalog edits cannot alter past order values. Calculate line totals and the order total server-side using decimal arithmetic and documented rounding rules.
- **Atomic inventory reservation:** Create the order, its lines, decrement all required stock, and append the initial `CREATED` history entry in a single service-layer transaction. Any missing product, insufficient stock, price/constraint failure, or history-write failure must abort the whole transaction. Use an appropriate concurrency strategy (for example, pessimistic row locks or versioned conditional updates) so concurrent orders cannot oversell; validate all requested quantities against locked/current stock before applying any decrement. Add database constraints for non-negative stock and valid positive order-line quantities as a final integrity barrier.
- **State Pattern lifecycle:** Model order lifecycle behavior with the State Design Pattern. Keep state-specific transition rules in explicit state objects (or an equivalent well-defined state strategy), rather than scattering conditionals across controllers. Persist the current status using the fixed states `CREATED`, `PAID`, `CANCELLED`, `SHIPPED`, and `COMPLETED`. Define and document the allowed graph explicitly; a typical strict flow is `CREATED -> PAID -> SHIPPED -> COMPLETED`, with cancellation allowed from `CREATED` and `PAID` only. Reject every other transition with a domain conflict response. Treat terminal `CANCELLED` and `COMPLETED` states as immutable. Require a valid transition command and perform it within a transaction. If cancellation releases reserved inventory, do so exactly once and only for permitted source states; encode that rule in the state transition/domain operation and test it.
- **Immutable status history:** Every successful transition, including initial creation into `CREATED`, must append exactly one history row in the same transaction as the current-state change. Record order reference, prior status (nullable only for initial creation), new status, transition timestamp (UTC/`Instant`), and actor or reason when available. Make history append-only in application behavior: expose no update/delete endpoint or repository operation, do not cascade order deletion into history, and keep historical rows when an order is archived/deactivated. Return history in chronological order. If a history insert fails, the status change, order creation, and inventory changes must all roll back.
- **Transaction boundaries and concurrency:** Put orchestration and transaction demarcation in service methods, not controllers. Keep product reads/locks, price snapshots, stock updates, order and line persistence, state change, and history append in the same database transaction. Avoid catching and suppressing exceptions inside that transaction; map domain exceptions at the API boundary. Ensure exceptions trigger rollback (including checked exceptions if used). Use optimistic/pessimistic locking consistently and handle stale-write or lock conflicts as a retryable/conflict response without partial effects. Do not claim atomicity across external systems; any future external side effects require an outbox or another explicit reliable-delivery design.
- **Persistence and API:** Add Flyway migrations for product, order, order-line, and order-status-history tables, with foreign keys, indexes for order/history lookups, enum constraints, money precision, and check constraints for quantities and stock. Keep JPA mappings aligned with the migration. Use request/response DTOs; never expose mutable entities or accept client-calculated price, total, status, or stock changes through order endpoints. Document catalog/admin and user order routes, allowed transitions, status-history behavior, inventory reservation/release policy, and error responses in OpenAPI and README.
- **Focused verification:** Cover successful multi-product totals and stock reduction; missing product and insufficient stock; concurrent placement against limited stock; invalid/duplicate lines; attempts to manipulate submitted prices/totals; every permitted and forbidden state transition; cancellation inventory behavior; exactly one immutable history row per transition; and rollback when inventory validation, price persistence, state update, or history persistence fails. Verify rollback by asserting no partial order, line, stock, status, or history changes.

Do not add this workload to projects that do not request orders or commerce. When requirements omit a consequential lifecycle rule (especially cancellation after payment, inventory release, refunds, or whether shipping is cancellable), make the assumption explicit in the README or ask before implementing that behavior.

## Security and configuration

When JWT is enabled, keep registration/login/refresh/logout routes and API documentation public, and require authentication on domain routes by default. Configure authorization rules explicitly. Do not assume all authenticated users can access each other's data: enforce ownership in service/repository queries when the definition says records are private. If the data is shared, do not add artificial ownership columns.

Configure issuer/token expiration/secret and database URL/user/password through environment variables. Use safe local development defaults only where appropriate; document that production secrets must be supplied externally. Add CORS rules only when a client origin is specified or required by the deployment context.

## Completion workflow

- Inspect the destination before changing it; preserve unrelated existing files and conventions.
- Create the application and tests for the requested domain, common platform features, migrations, and documentation.
- Add tests for representative service behavior, validation, authentication/authorization, and persistence/API integration. For paginated/filterable list endpoints, cover pagination boundaries, each filter independently and in combination, sorting, invalid inputs, and maximum page size. Keep tests isolated from external services using H2 or suitable mocks. For the order-processing workload example, verify that order creation and each status transition atomically append immutable history records and roll back fully when any persistence step fails.
- Run the project's relevant test/build command when permitted, fix failures caused by the changes, and report what was run. Never state that verification passed if it was not run.
- Search generated files for placeholder/sample domain names and stale project identifiers; remove them from application config, schema, README, and routes.
- Do not add domain-specific extras such as CSV export, scheduled jobs, notifications, status workflows, or per-user ownership unless the new project's definition calls for them.
