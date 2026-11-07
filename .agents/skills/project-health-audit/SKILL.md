---
name: project-health-audit
description: Audit a Java Spring Boot API that uses JPA/Hibernate, PostgreSQL, Flyway, Spring Security, and Springdoc OpenAPI for consistency, integrity, security, and performance. Use when asked to scan, harden, troubleshoot, review, or improve this stack; identify issues; fix safe low-risk defects directly; and suggest concrete remediations for higher-risk findings.
---

# Spring Boot Project Health Audit

Audit the repository as a Spring Boot service first, not as a generic codebase. Assume the critical health axes are application configuration, persistence correctness, migration safety, endpoint security, API contract quality, and query/runtime efficiency.

## Build Context First

- Read local instructions and project docs first: `AGENTS.md`, `README*`, `pom.xml`, `.mvn/`, CI files, `src/main/resources/application*.yml`, `src/test/resources/application*.yml`, and database migration folders.
- Inspect the Java package layout before changing code. Identify controller, service, repository, entity, DTO, mapper, exception, and security packages or their local equivalents.
- Check whether the project is already using the standard safety choices for this stack: environment-backed secrets, `ddl-auto: none`, Flyway as schema source of truth, `open-in-view: false`, DTO-based APIs, and a documented maximum page size.
- Preserve unrelated local changes. Prefer the repository's Maven wrapper, profiles, and existing test slices over ad hoc commands.

For a detailed checklist and file/command hints, read [references/checklist.md](./references/checklist.md).

## Verification Order

Use the lowest-cost commands that produce real evidence.

1. Read configuration and code structure before running anything.
2. Run targeted compilation or tests around the changed area.
3. Run broader Maven verification when affordable.
4. If dependency or vulnerability tooling is already configured in Maven or CI, use it. Do not install extra scanners by default.

Prefer commands such as:

- `./mvnw -q -DskipTests compile`
- `./mvnw test`
- `./mvnw verify`

If the environment cannot run Java or Maven, continue with static inspection and report the verification gap explicitly.

## Audit Areas

Evaluate the codebase in these areas:

- Consistency: layered boundaries, DTO/entity separation, naming, exception strategy, validation style, controller/service responsibility split, config/documentation drift, and repeated logic.
- Integrity: alignment between entity mappings and Flyway migrations, transaction boundaries, null-handling, optimistic assumptions around lazy loading, pageable/filter/sort correctness, and state invariants in service methods.
- Security: Spring Security route exposure, authentication and authorization enforcement, JWT/secret handling, password storage, overly broad `permitAll`, unsafe error detail leakage, and missing validation on request bodies and query parameters.
- Performance: N+1 queries, eager fetch misuse, collection fetch joins with pagination, missing indexes implied by repository queries, heavy startup seeders, oversized response DTOs, and chatty service/repository interactions.
- Operational quality: broken test slices, misconfigured profiles, noisy SQL logging in normal runs, weak local defaults leaking toward production, stale OpenAPI exposure rules, and incomplete README/setup instructions.

## Stack-Specific Review Rules

### Spring Boot Configuration

- Treat `application*.yml` as part of the executable design, not incidental config.
- Check that security, datasource, Flyway, pagination, and logging properties are externalized and documented.
- Flag risky defaults, especially placeholder JWT secrets, production-like credentials, and verbose SQL or bind logging enabled by default.
- Check that actuator, Swagger UI, and API docs exposure match the intended environment and security posture.

### JPA and Hibernate

- Inspect entity mappings, repository methods, and service fetch patterns together.
- Look for N+1 behavior, lazy initialization risks, unnecessary eager associations, `distinct` needs on join-based filtering, and pagination that breaks under collection fetch joins.
- Check that list endpoints use explicit sort allow-lists and bounded page sizes.
- Check that entities are not serialized directly to the API and that DTO mappings do not expose internal fields or permit unintended writes.

### PostgreSQL and Flyway

- Treat Flyway migrations as the schema source of truth. Compare entity constraints, indexes, join tables, enum representation, and nullability against migration scripts.
- Flag destructive or ambiguous migrations, baseline misuse, and schema drift hidden by local database state.
- Check whether repository query patterns imply missing PostgreSQL indexes or uniqueness constraints.

### Spring Security

- Inspect the `SecurityFilterChain`, authentication flow, token services, password handling, and any method-security annotations together.
- Look for public endpoints that should be protected, protected endpoints that are accidentally bypassed by matcher order, and authorization rules implemented only in controllers instead of service or repository boundaries where needed.
- Check logout, refresh, and token rotation/revocation paths for integrity gaps if JWT refresh tokens are used.
- Ensure error responses do not leak stack traces, token contents, usernames that aid enumeration, or internal exception detail.

### Springdoc OpenAPI

- Check that Swagger UI and API docs paths are intentionally exposed and consistent with the security configuration.
- Check that endpoint contracts, request validation, pagination parameters, and authentication requirements are reflected accurately enough in the generated docs.
- Flag stale or misleading docs when the runtime API behavior has drifted from the documented contract.

## Fix Policy

Fix issues directly when the change is local, low-risk, and clearly correct. Common direct fixes for this stack include:

- missing Bean Validation or inconsistent request validation
- unsafe sort/filter parsing
- obvious entity-to-migration drift in code or docs
- accidental OpenAPI or Swagger path exposure mismatch
- noisy or secret-bearing logging
- straightforward query/fetch-plan inefficiencies
- narrow security matcher or DTO exposure mistakes
- focused tests needed to pin the fix down

Suggest instead of editing when the change needs product or rollout decisions, such as:

- dependency upgrades across Spring Boot or Hibernate major versions
- large security model changes
- destructive migration rewrites
- caching or denormalization strategies
- broad package or architecture refactors

## Working Method

1. Read the config, migrations, and security setup before touching business code.
2. Gather evidence from code, tests, and commands. Separate confirmed defects from static-review risks.
3. Apply the smallest safe change that resolves the issue and fits existing project conventions.
4. Add or update tests when the behavior is testable.
5. Re-run the narrowest relevant Maven command after each material fix, then widen verification if justified.

## Reporting Contract

Lead with findings, ordered by severity. For each finding, provide:

- the impacted file or subsystem
- the concrete behavior or risk
- why it matters in this Spring stack
- whether you fixed it or are suggesting a remediation
- how you verified it, or what prevented verification

If no findings are discovered, say so explicitly and list residual risks or checks that could not be performed.

## Output Boundaries

- Do not install new Maven plugins or external scanners by default.
- Do not invent test results, vulnerability findings, or performance numbers.
- Do not change schema or security behavior without calling out the blast radius.
- Do not refactor unrelated code while fixing a bounded defect.
