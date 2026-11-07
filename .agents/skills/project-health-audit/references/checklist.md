# Spring Boot Audit Checklist

Use this checklist only for Java services built with Spring Boot, JPA/Hibernate, PostgreSQL, Flyway, Spring Security, and Springdoc OpenAPI.

## Severity Rubric

- Critical: auth bypass, secret exposure, destructive migration risk, broken token revocation, data corruption, or behavior likely to break production correctness.
- High: exploitable security weakness, schema/mapping drift that can fail at runtime, missing authorization on important paths, or performance behavior that scales badly on common queries.
- Medium: incomplete validation, documentation/config drift, avoidable inefficiency, flaky verification, or error handling that obscures root cause.
- Low: local inconsistency, minor logging/config waste, narrow edge-case bug, or documentation mismatch with low blast radius.

## Read These Files First

- `pom.xml`
- `.mvn/` and `mvnw`
- `src/main/resources/application*.yml`
- `src/test/resources/application*.yml`
- `src/main/resources/db/migration/**`
- controller, service, repository, entity, DTO, mapper, exception, and security packages

Useful `rg` patterns:

- `rg -n "SecurityFilterChain|@EnableMethodSecurity|@PreAuthorize|@Secured" src/main/java`
- `rg -n "@Entity|@Table|@ManyToOne|@OneToMany|@ManyToMany|@EntityGraph" src/main/java`
- `rg -n "@RestController|@ControllerAdvice|@ExceptionHandler" src/main/java`
- `rg -n "Page<|Pageable|Sort|Specification<|JpaSpecificationExecutor" src/main/java`
- `rg -n "springdoc|swagger|api-docs" src/main/resources src/main/java`

## Command Ladder

Prefer the repository wrapper and existing profiles.

1. `./mvnw -q -DskipTests compile`
2. `./mvnw test`
3. `./mvnw verify`

If test classes are clearly scoped and the build supports it, use narrow commands first. If Java or Maven is unavailable, state that the audit is partly static.

## Spring Boot Configuration Checks

- Check that secrets and credentials come from environment variables or external config, not fixed source values intended for production.
- Check that `spring.jpa.hibernate.ddl-auto` is compatible with Flyway ownership of the schema, typically `none` or equivalent.
- Check that `spring.jpa.open-in-view` is intentionally disabled unless there is a justified reason.
- Check that SQL and bind logging are environment-controlled and not too verbose for normal runs.
- Check that pagination, issuer, token expiration, and similar operational limits are configurable and documented.
- Check that test config does not silently diverge from production behavior in ways that mask mapping, migration, or security problems.

## JPA and Hibernate Checks

- Compare entity annotations against migration scripts for table names, lengths, nullability, uniqueness, foreign keys, and indexes.
- Check that relationship ownership and cascade rules are deliberate, especially around shared entities and many-to-many join tables.
- Check list and detail code paths for N+1 risks, lazy initialization failures, and over-fetching.
- Check pageable repository flows for collection fetch joins that break or distort pagination.
- Check `Specification` or dynamic query code for missing `distinct`, unsafe property-path sorting, or unbounded filters.
- Check that request DTOs, update DTOs, and entity mappers do not allow client control over IDs, audit fields, ownership fields, or security-sensitive state.

## PostgreSQL and Flyway Checks

- Check each Flyway migration for deterministic ordering, safe DDL, and compatibility with existing data.
- Check that join tables, indexes, uniqueness constraints, and foreign keys support the query patterns used by repositories.
- Check for PostgreSQL-specific assumptions that tests on H2 may not catch, such as case sensitivity, JSON behavior, enum storage, or SQL dialect differences.
- Check baseline usage carefully; it can hide missing early migrations or drift if used casually.

## Spring Security Checks

- Check matcher order in `SecurityFilterChain`; broad permit rules earlier in the chain can neutralize later restrictions.
- Check which endpoints are intentionally public: auth entrypoints, Swagger/OpenAPI endpoints, and any health or static routes.
- Check whether authorization is enforced only at HTTP routing level or also in service/repository logic where ownership rules require it.
- Check password hashing, token signing, refresh rotation, logout invalidation, and secret length/format assumptions.
- Check authentication and authorization failure handling for information leaks and inconsistent status codes.
- Check tests for positive and negative access cases, not only happy-path authentication.

## Springdoc OpenAPI Checks

- Check that Swagger UI and API docs paths are explicitly allowed or blocked in line with the intended deployment model.
- Check that controller validation, request bodies, query parameters, and auth expectations are not misleading in the generated docs.
- Check for stale examples or endpoint descriptions that conflict with current behavior.

## Performance Checks

- Check pageable endpoints for bounded page size and an explicit sort allow-list.
- Check Hibernate access patterns on list endpoints that render nested collections or related names.
- Check repository methods and specifications for queries that likely need indexes on foreign keys, unique columns, or frequent filter columns.
- Check startup seeders and bootstrap routines for expensive full-table scans or non-idempotent behavior.
- Check payload DTOs for fields that force unnecessary joins or oversized JSON responses.

## Test Expectations

- Add or update tests that reproduce the bug or enforce the contract you changed.
- Prefer integration tests for persistence, migrations, security flows, and pagination/filter/sort behavior.
- Prefer focused unit tests for parsing, validation, token utilities, and pure business rules.
- Report exactly which Maven command was run and whether the environment blocked broader verification.

## Fix-vs-Suggest Heuristic

Apply the fix now when:

- the defect is well-bounded
- the intended behavior is clear from code, tests, or docs
- the blast radius is small
- you can verify the result locally with reasonable confidence

Suggest instead when:

- the change affects external consumers or production rollout order
- the migration is destructive or hard to reverse
- the security model itself is underdefined
- the environment prevents meaningful verification

## Final Check

- Re-run the narrowest relevant Maven command after each material fix.
- Summarize what was confirmed by execution, what was found by static review only, and what could not be verified.
- Keep findings ordered by severity and include concrete file references in the final report.
