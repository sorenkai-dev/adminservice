# Contributing to Soren Kai Admin Service

Thank you for your interest in contributing to the Soren Kai Admin Service.

This project is guided by the principles of transparency, accountability, inclusivity, and responsible software engineering. We welcome contributions that improve the reliability, security, accessibility, and maintainability of the software.

Contributors of all backgrounds and experience levels are welcome.

## 1. Project Overview

The Soren Kai Admin Service is an open-source Kotlin/JVM microservice designed to support administrative operations within the Soren Kai technology ecosystem.

### Responsibilities

The Admin Service is responsible for:

- **Administrative role management:** Assigning, modifying, and revoking administrator and moderator roles.
- **Permission management:** Defining and enforcing administrative permissions.
- **Authorization decisions:** Determining whether an authenticated identity has permission to perform a requested administrative operation.
- **Administrative operations:** Supporting authorized actions taken by administrators and moderators.
- **Administrative auditing:** Recording administrative activity, permission changes, and other security-relevant administrative actions.
- **Accountability:** Maintaining records necessary to review administrative decisions and support appeals.

### What the Admin Service Does Not Do

**The Admin Service does not provide authentication or identity management.**

### Service Boundaries

The Admin Service is intended to operate as a private internal microservice.

Its gRPC interface is accessed through the central Ktor backend rather than being exposed directly to public clients.

The backend is responsible for coordinating requests between the user-facing applications and internal services.

The Admin Service remains responsible for enforcing its own administrative authorization policies.

A request originating from an authorized backend component does not automatically imply permission to perform every administrative operation.

### Audit Boundaries

Administrative audit records are maintained separately from the operational audit records belonging to other services and platforms.

The Admin Service must not become a centralized repository for unrelated application activity.

Contributions must preserve this separation.

## 2. Current Development Status

The project is under active development.

Implemented infrastructure includes:

- Kotlin/JVM application architecture.
- Gradle multi-module project structure.
- gRPC and Protocol Buffers.
- gRPC health checking and service reflection.
- JVM diagnostics.
- Micrometer-based gRPC observability.
- Koin dependency injection.
- Automated unit and integration tests.
- Spotless formatting and Detekt static analysis.
- Docker-based deployment infrastructure.

Planned functionality includes:

- Administrative role and permission management.
- Administrative authorization policies.
- Administrative audit records and integrity controls.
- PostgreSQL persistence and database migrations.
- Integration with the central Ktor backend.
- Administrative reporting and oversight capabilities.

Documentation and contributions should distinguish implemented behavior from planned functionality.

## 3. Project Architecture

The project uses a modular architecture:

| Module | Responsibility |
|---|---|
| `api` | Protocol Buffer definitions and generated gRPC interfaces |
| `app` | Application startup, gRPC implementation, dependency injection, diagnostics, and observability |
| `core` | Administrative domain models, business rules, and authorization logic |
| `persistence` | Database access, repositories, and persistence implementations |

Contributions should preserve clear boundaries between these modules.

### Architectural Principles

- Keep domain logic independent of transport and infrastructure frameworks.
- Use constructor injection for dependencies.
- Define application dependencies through Koin modules.
- Keep Koin-specific code out of domain business logic.
- Avoid embedding business rules in gRPC handlers.
- Preserve compatibility of published Protocol Buffer interfaces.
- Keep authentication responsibilities outside the Admin Service.
- Apply least-privilege principles to administrative authorization.
- Maintain separation between administrative audit records and unrelated platform data.
- Avoid unnecessary dependencies and framework coupling.

Major architectural changes should be discussed with maintainers before implementation.

## 4. Ways to Contribute

Contributions are welcome in areas including:

- Bug fixes.
- Administrative authorization and permission enforcement.
- Administrative role-management functionality.
- Audit integrity and accountability.
- Automated testing.
- Performance and reliability improvements.
- Security enhancements.
- Observability and diagnostics.
- Documentation.
- Developer tooling and CI/CD improvements.

New features should align with the Admin Service's defined administrative responsibilities.

Features primarily concerned with authentication, public-facing application behavior, or unrelated platform operations belong in their respective systems.

## 5. Development Environment

### Requirements

Use the Java Development Kit version specified by the project's Gradle toolchain configuration.

The project currently uses Kotlin and Gradle with JVM-based tooling.

Use the included Gradle Wrapper rather than relying on a system-installed Gradle version.

### Repository Setup

1. Fork the repository if you do not have direct contributor access.
2. Clone your fork or the repository.
3. Install Git pre-commit hooks to automate formatting and static analysis:
   - Linux/macOS: `./gradlew installGitHooks`
   - Windows (PowerShell/CMD): `.\gradlew.bat installGitHooks`
4. Create a descriptive working branch.
5. Make your changes and add appropriate tests.
6. Run the project's quality checks.
7. Submit a pull request against the designated development branch.

Example branch names:

- `feature/admin-roles`
- `feature/permission-evaluation`
- `fix/grpc-status-handling`
- `test/audit-integrity`
- `docs/deployment-guide`

Do not commit directly to protected branches.

## 6. Testing Standards

The project follows test-driven development (TDD) wherever practical.

Contributions should include automated tests appropriate to the behavior being introduced or changed.

Tests should verify expected behavior and relevant failure conditions.

### Security-Sensitive Testing

Changes involving administrative privileges, permissions, and audit records require particular attention.

Tests should verify that:

- Unauthorized administrative operations are rejected.
- Permissions are evaluated against the correct authenticated identity and applicable administrative context.
- Administrative roles cannot be escalated without appropriate authorization.
- Administrative actions produce required audit records.
- Audit records cannot be modified or deleted through unauthorized operations.
- Service-to-service access does not bypass administrative permission checks.

These are testing requirements for the functionality as it is implemented, not claims that every capability already exists.

### Test Isolation

Tests should be deterministic and reproducible.

Avoid dependencies on:

- Production credentials.
- Live production databases.
- Publicly accessible production services.
- External infrastructure unless explicitly required by an integration test.

Prefer controlled test dependencies and isolated environments.

## 7. Code Quality

Contributions must conform to the project's formatting and static-analysis requirements.

Before submitting a pull request, run:

**Windows:**

```powershell
.\gradlew.bat clean build spotlessCheck detekt
```

**Linux/macOS:**

```bash
./gradlew clean build spotlessCheck detekt
```

To automatically apply supported formatting rules:

```bash
./gradlew spotlessApply
```

### Git Pre-Commit Hooks

To ensure local changes conform to Spotless formatting and Detekt rules before committing, install the repository's Git pre-commit hooks:

```bash
./gradlew installGitHooks
```

Once installed, the hook automatically runs `spotlessApply` and `detekt` on staged changes prior to finalizing any commit.

Pull requests should not introduce failing tests, formatting violations, or unresolved static-analysis findings.

Avoid suppressing lint rules without a documented technical justification.

## 8. Protocol Buffer Compatibility

The Admin Service uses gRPC and Protocol Buffers for internal communication.

Changes to published RPC interfaces should preserve compatibility wherever possible.

Contributors must:

- Avoid reusing previously assigned Protocol Buffer field numbers.
- Reserve removed field numbers and names where appropriate.
- Avoid unnecessary changes to existing service and message definitions.
- Document changes affecting backend consumers.
- Introduce breaking changes through an explicitly approved versioning strategy.

The absence of a public network endpoint does not eliminate the need for API compatibility.

## 9. Database and Persistence Changes

Administrative persistence must preserve consistency, accountability, and audit integrity.

Database contributions should:

- Use versioned migrations.
- Avoid destructive schema changes without an approved migration strategy.
- Preserve historical administrative records where required.
- Support appropriate transaction boundaries.
- Avoid storing unnecessary sensitive identity information.
- Maintain clear ownership of administrative data.

Changes affecting audit records should receive additional review.

## 10. Security Contributions

Security is a fundamental requirement of administrative infrastructure.

Particular attention should be given to:

- Administrative privilege escalation.
- Authorization bypasses.
- Incorrect permission evaluation.
- Service-to-service trust boundaries.
- Audit-record tampering.
- Exposure of sensitive information through diagnostics or logging.
- Unsafe configuration defaults.
- Supply-chain and dependency vulnerabilities.

### Reporting Vulnerabilities

Do not disclose an exploitable security vulnerability through a public issue or pull request before coordinating disclosure.

Follow the private reporting procedure described in [SECURITY.md](SECURITY.md).

Do not test against production deployments without explicit authorization.

## 11. Dependency Management

Dependencies are managed through the Gradle version catalog (`gradle/libs.versions.toml`).

Automated dependency maintenance is configured via Renovate targeting the `dev` branch:
- Non-breaking updates (`minor`, `patch`, digest updates) are grouped and automatically merged once CI verification checks pass.
- Major and potentially breaking updates require manual maintainer review and approval.

Contributors should:

- Prefer existing dependencies when appropriate.
- Explain the need for newly introduced libraries.
- Evaluate maintenance status and security implications.
- Confirm license compatibility with Apache License 2.0.
- Avoid introducing redundant libraries or frameworks.
- Use dependency BOMs or other appropriate version-alignment mechanisms where beneficial.

Changes to core infrastructure dependencies should include relevant compatibility testing.

## 12. Pull Request Requirements

All contributions must be submitted through pull requests.

The `master` branch is intended to remain protected.

Pull requests should include:

- A clear description of the problem.
- An explanation of the proposed solution.
- References to related issues where applicable.
- Relevant automated tests.
- Documentation updates where necessary.
- Compatibility and migration considerations.
- Security implications for sensitive changes.

### Review and Approval

Changes targeting `master` require review under the repository's configured branch-protection rules.

Designated code owners are Soren Kai and Barret Vogtman, represented by their authorized GitHub accounts.

The applicable GitHub ruleset and `CODEOWNERS` configuration determine which approvals are required.

Contributors must not bypass required reviews or CI checks.

Approval does not guarantee immediate merging. Maintainers may request additional changes or reject proposals that do not align with the project's architecture, security requirements, or scope.

## 13. Licensing and Contributions

The Soren Kai Admin Service is licensed under the **Apache License, Version 2.0**.

By intentionally submitting a contribution for inclusion in this project, you agree that your contribution is submitted under the terms of Apache License 2.0, unless explicitly stated otherwise and agreed to by the maintainers.

Contributors retain applicable copyright ownership in their original contributions.

Copyright assignment to the project maintainer is not required.

Contributors must have the legal right to submit the code or other materials they contribute.

Do not include third-party code, proprietary materials, or assets without verifying that their licenses permit the proposed use.

## 14. Community Standards

The Soren Kai Admin Service welcomes contributors regardless of background, identity, or experience.

Participation must remain respectful, inclusive, and constructive.

Harassment, discrimination, intimidation, threats, and deliberately exclusionary conduct are not acceptable.

Technical disagreements are expected and welcome. Reviews should focus on evidence, engineering tradeoffs, correctness, maintainability, and security.

Maintainers reserve the right to restrict participation when necessary to protect contributors or the integrity of the project.

A separate `CODE_OF_CONDUCT.md` will establish detailed community standards and reporting procedures.

## 15. Maintainer Responsibility

Maintainers are responsible for preserving the project's technical direction, security boundaries, and long-term maintainability.

Contributions may be declined for reasons including:

- Architectural incompatibility.
- Security concerns.
- Insufficient testing.
- Unnecessary complexity.
- Excessive maintenance costs.
- Misalignment with the project's administrative purpose.

Review feedback should be constructive and provide actionable technical reasoning wherever possible.

The objective is to maintain an administrative service that is secure, understandable, reliable, and open to meaningful community contribution.