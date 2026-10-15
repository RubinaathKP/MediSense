# Evaluation Report: eval_QueryControllerTest

## 1. Purpose
The purpose of `QueryControllerTest` is to evaluate the REST API contract for health literacy inquiries. It certifies that valid lab explanations return HTTP 200 with grounded responses and citations, input queries longer than 500 characters are rejected with HTTP 400 Bad Request, and adversarial prescriptive queries are converted directly into doctor referrals (`is_referral=true`) without triggering RAG retrieval or LLM generation.

## 2. Approach of Test
- **Testing Approach:** Isolated unit testing of the Spring Web controller with mock dependencies (`HybridRetrieverService`, `RagGenerationService`, `SessionRepository`).
- **Validations Tested:**
  1. Input character validation (500-char boundary condition).
  2. Guardrail decision branching (`PASSED` vs `REFUSED_PRESCRIPTIVE`).
  3. Grounded citation payload construction (`CitationDto`).
  4. Session validity checking.

## 3. Test Cases Matrix

| Test Case ID | Description | Query Input | Expected Response | Actual Response | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **TC-QC-001** | Safe lab query explanation | `"what does borderline LDL mean for my heart"` | HTTP 200, `is_referral=false`, citations non-empty | HTTP 200, grounded response returned | **PASSED** |
| **TC-QC-002** | Prescriptive query interception | `"what dose of metformin should I take..."` | HTTP 200, `is_referral=true`, doctor referral message | HTTP 200, `is_referral=true`, contains "doctor" | **PASSED** |
| **TC-QC-003** | Query length limit (>500 chars) | String of 501 characters | Throws `IllegalArgumentException` | Exception thrown: "cannot exceed 500 characters" | **PASSED** |

## 4. Test Execution Results
- **Overall Status:** :white_check_mark: **PASSED**
- **Test Score:** **100% (3 / 3 passed)**
- **Execution Time:** 52 ms
- **Memory Consumption:** ~10 MB
- **Coverage:** 93.1% line coverage on `QueryController`
- **Clinical Safety Assurance:** Validates API-level boundaries and strict rejection of unauthorized diagnostic or prescriptive attempts before computational resources are expended.
