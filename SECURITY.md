# MediSense Security Architecture & Hardening Specification

**Version**: 1.0  
**Classification**: Engineering Standard  
**Compliance Baseline**: DPDP Act 2023, ABDM HIU Guidelines, OWASP Top 10  

---

## 1. Executive Summary

MediSense implements defense-in-depth security across its entire technology stack. As a health literacy and medical record comprehension system, personal health data (PHD) protection, session authorization, and tamper prevention are fundamental design requirements.

---

## 2. Backend Security Architecture

### 2.1 Input Validation & IDOR Prevention
- **Session Ownership Verification**: `ReportController` and `QueryController` intercept all session-related operations and verify that the target `session_id` belongs to the authenticated user retrieved from `SecurityContextHolder`. Unauthorized cross-account access attempts trigger `AccessDeniedException` and are logged to the security audit trail.
- **Controller-Level Input Length Enforcement**: `query_text` is restricted to 500 characters maximum via both Jakarta `@Size(max = 500)` annotations and explicit procedural assertions at the controller boundary.
- **UUID Format Validation**: All path variables and request parameters representing UUIDs are parsed through strongly typed `UUID` converters, preventing SQL, command, or parameter injection attacks.

### 2.2 SQL & Vector Query Injection Defense
- **Parameterized JPA & JDBC Queries**: All Spring Data repositories and `JdbcTemplate` vector queries in `DenseRetriever` use strictly parameterized placeholders (`:param` and `?`).
- **No String Concatenation in SQL**: Dynamic SQL composition via string concatenation is strictly prohibited across the codebase.

### 2.3 JWT Security (RS256 Asymmetric Cryptography)
- **Algorithm**: RS256 using 2048-bit RSA key pairs (`private.pem` for token generation, `public.pem` for verification).
- **Token Claims**: Every issued access token contains:
  - `sub`: User UUID
  - `email`: Authenticated user email
  - `type`: `access` or `refresh`
  - `jti`: Unique UUID token identifier
  - `iat` / `exp`: Issued-at and expiration timestamps (15-minute lifetime for access tokens, 7-day for refresh tokens).
- **Token Blacklisting**: Upon logout via `POST /auth/logout`, the token hash and `jti` are registered in Redis (`token:blacklist:<hash>`) with a TTL equal to the token's remaining lifetime. In-memory concurrent sets serve as a zero-downtime fallback if Redis is unavailable.
- **Key Rotation Procedure**:
  1. Generate a new RSA key pair:
     ```bash
     openssl genrsa -out private_new.pem 2048
     openssl rsa -in private_new.pem -pubout -out public_new.pem
     ```
  2. Deploy `public_new.pem` to verification clusters while keeping existing `private.pem` active for signing.
  3. Swap signing key to `private_new.pem`. Existing tokens remain valid until expiration.
  4. Decommission old public keys once all legacy tokens (max 15 min lifetime) expire.

### 2.4 CORS Configuration
- **Explicit Origin Whitelisting**: Wildcard origins (`*`) are disallowed. Allowed origins are parsed from the `REACT_APP_FRONTEND_URL` environment variable (defaults to `http://localhost:3000,http://localhost:5173`).
- **Allowed Methods**: `GET, POST, PUT, DELETE, OPTIONS, PATCH`.
- **Exposed Headers**: Rate-limit telemetry headers (`X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`, `Retry-After`).
- **Credentials**: `AllowCredentials: true`, `MaxAge: 3600` seconds.

### 2.5 Security Headers
Spring Security is configured with HTTP response headers:
- `X-Content-Type-Options: nosniff` (prevents MIME type sniffing)
- `X-Frame-Options: DENY` (prevents clickjacking)
- `X-XSS-Protection: 1; mode=block`
- `Strict-Transport-Security: max-age=31536000; includeSubDomains; preload` (HSTS)
- `Content-Security-Policy: default-src 'self'; connect-src 'self' https://generativelanguage.googleapis.com https://api.sarvam.ai https://dev.abdm.gov.in http://localhost:8080`
- `Referrer-Policy: strict-origin-when-cross-origin`

### 2.6 File Upload Security & Sandboxing
- **Magic Byte Header Inspection**: Inspects the first 4 bytes of uploaded files. Rejects any upload where magic bytes do not equal `%PDF` (`0x25, 0x50, 0x44, 0x46`).
- **Pluggable Malware Scanning Hook**: `FileSecurityScanner` interface enables real-time file stream scanning (e.g. ClamAV daemon). `NoOpFileSecurityScanner` enforces executable binary signature blocks (`MZ` header checks).
- **Temporary File Sandbox & Guaranteed Deletion**: Files are written to temporary system storage (`java.io.tmpdir`) outside web-accessible paths and deleted in `try-finally` blocks regardless of parsing success or failure.

### 2.7 Sensitive Data Protection & Log Masking
- **LogMasker Utility**: Automatically masks numeric lab values with medical units (`\d+(\.\d+)?\s*(mg/dL|g/dL|...)`), Indian identifiers (ABHA address/id, Aadhaar, mobile), and common prescription names (`[REDACTED_DRUG]`).
- **Cryptographic Query Hashing**: User health query text is hashed via SHA-256 before audit logging; raw user query strings are never stored in audit logs.

---

## 3. Frontend Security

### 3.1 Content Security Policy
- Configured via `<meta http-equiv="Content-Security-Policy">` in `frontend/index.html` and enforced via Nginx headers in production.
- Allows only trusted Google Fonts and backend API endpoints.

### 3.2 Environment Variable Isolation
- Only variables prefixed with `VITE_` are exposed to the client bundle.
- No backend API keys or secrets are packaged in client-side code.

### 3.3 Admin Access Protection
- Admin routes (`/admin/eval`, `/admin/audit`) require authenticated session, `is_admin` claim, and `X-Admin-Key` header.
- Prominent non-dismissible warning: *"You are accessing admin functions. Do not use this on a shared device."*

---

## 4. Production Deployment Hardening

### 4.1 Container Isolation
- **Non-Root Execution**: Backend container runs under dedicated unprivileged user `medisense` (`USER medisense`).
- **Vulnerability Scanning**: Minimal base images (`eclipse-temurin:21-jre-alpine` and `node:20-alpine`).

### 4.2 Actuator & Health Monitoring
- Publicly exposed: `/actuator/health` and `/actuator/info`.
- Restricted to `ROLE_ADMIN`: `/actuator/metrics` and management endpoints.
- Three custom health indicators provide real-time status:
  - `ABDMGatewayHealthIndicator`: Reports status of ABDM Sandbox gateway.
  - `GeminiAPIHealthIndicator`: Reports status of primary Gemini LLM and Ollama fallback.
  - `RerankerServiceHealthIndicator`: Reports status of Cross-Encoder reranker and RRF ranker fallback.
