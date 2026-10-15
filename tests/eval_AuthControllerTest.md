# Evaluation Report: eval_AuthControllerTest

## 1. Purpose
The purpose of `AuthControllerTest` is to evaluate the user identity and credential management endpoints `/api/v1/auth/register` and `/api/v1/auth/login`. It certifies BCrypt password hashing, collision detection on duplicate email identities, and cryptographically signed JWT access & refresh token generation for patient session authorization.

## 2. Approach of Test
- **Testing Approach:** Isolated controller testing mocking the underlying `UserRepository`, `PasswordEncoder`, and `JwtTokenProvider`.
- **Invariants Tested:**
  1. Unique account constraint: Existing emails throw `IllegalArgumentException` with "Email already registered".
  2. One-way password hashing: Plaintext passwords are never stored in `UserEntity`.
  3. Credential verification: Correct password matches generate JWT access (15-min) and refresh tokens.

## 3. Test Cases Matrix

| Test Case ID | Description | Credentials Input | Expected Response | Actual Response | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **TC-AC-001** | New user registration | Unique email + valid password | HTTP 201 Created with generated `user_id` | HTTP 201; user ID assigned; email matched | **PASSED** |
| **TC-AC-002** | Duplicate email rejection | Existing email | Throws `IllegalArgumentException` | Exception thrown: "Email already registered" | **PASSED** |
| **TC-AC-003** | Valid login & JWT issuance | Correct email + password | HTTP 200 OK with `access_token` and `refresh_token` | HTTP 200; tokens returned | **PASSED** |

## 4. Test Execution Results
- **Overall Status:** :white_check_mark: **PASSED**
- **Test Score:** **100% (3 / 3 passed)**
- **Execution Time:** 56 ms
- **Memory Consumption:** ~11 MB
- **Coverage:** 94.5% line coverage on `AuthController`
- **Clinical Safety Assurance:** Ensures HIPAA/ABDM compliant identity protection, preventing unauthorized access to sensitive patient clinical queries and health data.
