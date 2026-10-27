# Security Policy

## Overview

The Soren Kai Admin Service is an open-source administrative microservice designed to manage authorization, administrative roles, and audit capabilities.

The project is developed with security, transparency, accountability, and responsible engineering as core principles.

Open-source availability does not imply that deployed instances are intended to be publicly accessible.

## Deployment Security Model

The Admin Service is designed to operate as a private internal service, accessed through authorized backend components rather than directly by public clients.

Implementations should enforce the following principles:

- **Authenticated service communication:** Only explicitly authorized service identities may invoke internal RPCs.
- **Least privilege:** Access to the service does not automatically grant permission to perform every administrative operation.
- **Independent authorization:** Administrative permissions must be evaluated by the Admin Service, rather than relying solely on upstream authorization decisions.
- **Protected audit records:** Audit events must be protected against unauthorized modification and deletion.
- **Restricted network access:** Production deployments should limit ingress to authorized infrastructure and services.
- **Secure configuration:** Secrets and environment-specific credentials must not be embedded in source code.
- **Minimal information disclosure:** Diagnostic endpoints, metrics, and logs must not expose sensitive information.

These are architectural security objectives. Deployment operators remain responsible for configuring and validating the security controls appropriate to their environments.

## Supported Versions

Security support applies to the latest maintained release unless otherwise specified in a published security advisory.

Pre-release and development builds are not guaranteed to receive backported security fixes.

## Reporting a Vulnerability

Please report suspected vulnerabilities privately using GitHub's **Report a vulnerability** feature under the repository's Security tab.

Do not disclose exploitable vulnerabilities through public issues, pull requests, or discussions before a coordinated disclosure process has occurred.

A useful report should include:

- A description of the vulnerability and its potential impact.
- The affected version, commit, or component.
- Steps to reproduce the issue.
- A proof of concept, where appropriate.
- Relevant logs or error messages, with credentials and personal information removed.
- Suggested mitigation measures, if known.

Do not attempt to exploit production systems, access data belonging to others, or perform disruptive testing without explicit authorization.

## Vulnerability Handling

Maintainers will evaluate reported vulnerabilities based on their reproducibility, exploitability, and potential impact.

Confirmed vulnerabilities will be prioritized according to severity.

Where appropriate, remediation may include patches, configuration guidance, dependency updates, and published security advisories.

Responsible disclosure will be coordinated with reporters whenever practical.

No fixed response or resolution deadline is guaranteed.

## Scope

Security reports may concern:

- Authentication and authorization weaknesses.
- Privilege escalation.
- Service-to-service trust boundaries.
- gRPC request validation and access controls.
- Administrative role and permission management.
- Audit-log integrity.
- Sensitive information exposure.
- Dependency vulnerabilities.
- Unsafe default configurations.

Vulnerabilities in externally operated infrastructure or deployments outside the project's control should be reported to their respective operators.

## Security and Transparency

Public availability of source code is intended to support review, accountability, and improvement.

It is not a guarantee of security.

The project aims to apply established security practices, maintain appropriate automated testing, and address identified vulnerabilities without relying on source-code secrecy as a primary security control.