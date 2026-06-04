# Implementation Tasks: RestClient Migration + RestTestClient Testing

**Feature**: Replace RestTemplate with RestClient and implement RestTestClient  
**Branch**: `002-spring-boot-v4`  
**Created**: May 12, 2026  
**Status**: Phase 2 Complete - Ready for Phase 3+  
**Total Tasks**: 42 | **Phase 1 (Setup)**: 3 | **Phase 2 (Implementation)**: 8 | **Phase 3 (Integration Tests)**: 12 | **Phase 4 (Validation)**: 10 | **Phase 5 (Deployment)**: 9

---

## Implementation Strategy

**MVP Scope**: Phase 1-2 (Configuration + Core Service) - Already Complete ✅
- RestClientConfig created
- EntityClient refactored
- OAuth2Config updated
- 12 unit test scenarios designed
- Ready for integration test updates

**Recommended Execution Path**:
1. ✅ Phase 1 (Setup/Planning) - COMPLETE
2. ✅ Phase 2 (Implementation) - COMPLETE
3. ⏳ Phase 3 (Integration Tests) - NEXT PRIORITY
4. ⏳ Phase 4 (Validation) - Then execute
5. ⏳ Phase 5 (Deployment) - Final phase

**Parallel Opportunities**:
- Phase 3: All integration test updates [P] can run in parallel
- Phase 4: Unit tests + integration tests [P] can run in parallel
- Phase 4: Manual testing + load testing [P] can run in parallel
- Phase 5: Code review + documentation updates [P] can run in parallel

---

## Dependency Graph

```
Phase 1 (Setup/Planning) ✅ COMPLETE
    ↓
Phase 2 (Implementation) ✅ COMPLETE
    ├── RestClientConfig.java ✅
    ├── EntityClient.java ✅
    ├── OAuth2ClientConfig.java ✅
    └── EntityClientTest.java ✅
    ↓
Phase 3 (Integration Tests) ⏳ NEXT
    ├── [P] ServiceToServiceAuthIntegrationTest update
    ├── [P] ChaosEngineeringTest update
    ├── [P] KeycloakChaosTest update
    └── [P] ErrorHandlingTest update
    ↓
Phase 4 (Validation) ⏳ PENDING
    ├── [P] Unit test execution
    ├── [P] Integration test execution
    ├── [P] Code coverage analysis
    └── [P] Manual testing
    ↓
Phase 5 (Deployment) ⏳ PENDING
    ├── Code review
    ├── Merge to main
    ├── Release tagging
    ├── FluxCD deployment
    └── Production monitoring
```

---

## Phase 1: Setup & Planning ✅ COMPLETE

- [x] T001 Review feature specification and constitution requirements
- [x] T002 Create RestClient migration guide (RESTCLIENT-MIGRATION.md)
- [x] T003 Set up feature branch `002-spring-boot-v4`

---

## Phase 2: Implementation ✅ COMPLETE

- [x] T004 Create RestClientConfig.java with timeout configuration
- [x] T005 Refactor EntityClient.java to use RestClient fluent API
- [x] T006 Update OAuth2ClientConfig.java (remove RestTemplate bean)
- [x] T007 Deprecate RestTemplateConfig.java with migration notes
- [x] T008 Create EntityClientTest.java with 12 comprehensive test scenarios
- [x] T009 Generate RESTCLIENT-IMPLEMENTATION.md summary document
- [x] T010 Update copilot-instructions.md with migration references
- [x] T011 Verify Phase 2 compilation: `./gradlew compileJava`

---

## Phase 3: Integration Tests Update ⏳ READY TO START

### 3.1 ServiceToServiceAuthIntegrationTest.java Updates

- [ ] T012 [P] Open ServiceToServiceAuthIntegrationTest.java for review
- [ ] T013 [P] Replace @MockBean private RestTemplate with @Autowired RestClient
- [ ] T014 [P] Update setUp() method to configure MockRestServiceServer
- [ ] T015 [P] Refactor testEntityRetrievalWithValidToken() to use RestTestClient
- [ ] T016 [P] Update all 12 existing OAuth2 test scenarios to new framework
- [ ] T017 [P] Verify all assertions pass with RestClient mocking
- [ ] T018 [P] Run tests locally: `./gradlew test --tests ServiceToServiceAuthIntegrationTest`

**Subtasks for T016**:
- Update testEntityRetrievalWithoutToken()
- Update testEntityRetrievalWithExpiredToken()
- Update testEntityRetrievalWithInvalidTokenFormat()
- Update testEntityRetrievalWithMalformedAuthHeader()
- Update testOAuth2TokenIncludesCorrectScope()
- Update testEntityClientRequestsTokenWithCorrectPrincipal()
- Update testOAuth2TokenIncludedInServiceBRequest()
- Update testHandle401FromServiceB()
- Update testHandle403FromServiceB()
- Update testOAuth2SupportServiceToServiceFlow()
- Update testTokenCachingMechanism()
- Update testConcurrentOAuth2Requests()
- Update testAuthorizationIsStateless()

### 3.2 ChaosEngineeringTest.java Updates

- [ ] T019 [P] Open ChaosEngineeringTest.java for review
- [ ] T020 [P] Replace RestTemplate mock setup with RestClient + MockRestServiceServer
- [ ] T021 [P] Update timeout scenario tests (network delays, connection timeouts)
- [ ] T022 [P] Update service unavailability scenarios (503 responses)
- [ ] T023 [P] Update circuit breaker state transition tests
- [ ] T024 [P] Verify retry backoff timing assertions work with RestClient
- [ ] T025 [P] Run tests locally: `./gradlew test --tests ChaosEngineeringTest`

### 3.3 KeycloakChaosTest.java Updates

- [ ] T026 [P] Open KeycloakChaosTest.java for review
- [ ] T027 [P] Update mock Keycloak token endpoint configuration
- [ ] T028 [P] Update token acquisition failure scenarios
- [ ] T029 [P] Update token refresh test scenarios
- [ ] T030 [P] Verify OAuth2 retry logic works with RestClient
- [ ] T031 [P] Run tests locally: `./gradlew test --tests KeycloakChaosTest`

### 3.4 ErrorHandlingTest.java Updates

- [ ] T032 [P] Open ErrorHandlingTest.java for review
- [ ] T033 [P] Replace RestTemplate mocking with RestTestClient setup
- [ ] T034 [P] Update 4xx error scenarios (404, 400, etc.)
- [ ] T035 [P] Update 5xx error scenarios (500, 502, 503, etc.)
- [ ] T036 [P] Update network error handling (timeouts, connection refused)
- [ ] T037 [P] Update error response message validation
- [ ] T038 [P] Run tests locally: `./gradlew test --tests ErrorHandlingTest`

### 3.5 Integration Test Verification

- [ ] T039 Run full integration test suite: `./gradlew integrationTest`
- [ ] T040 Verify all integration tests pass (target: 100%)
- [ ] T041 Generate integration test report
- [ ] T042 Document any discrepancies or issues found

---

## Phase 4: Validation & Testing ⏳ PENDING

### 4.1 Compilation Verification

- [ ] T043 [P] Clean build: `./gradlew clean`
- [ ] T044 [P] Compile Java: `./gradlew compileJava`
- [ ] T045 [P] Verify zero compilation errors
- [ ] T046 [P] Verify no deprecation warnings for new code

### 4.2 Unit Test Execution & Coverage

- [ ] T047 [P] Execute all unit tests: `./gradlew test`
- [ ] T048 [P] Verify EntityClientTest scenarios all pass (12/12)
- [ ] T049 [P] Verify existing unit tests still pass
- [ ] T050 [P] Generate code coverage report: `./gradlew jacocoTestReport`
- [ ] T051 [P] Verify code coverage >90% (target: >95% for EntityClient)
- [ ] T052 [P] Document coverage metrics in VALIDATION-REPORT.md

### 4.3 Integration Test Execution

- [ ] T053 Execute integration tests: `./gradlew integrationTest`
- [ ] T054 Verify OAuth2 flow end-to-end
- [ ] T055 Verify Service A → Service B communication
- [ ] T056 Verify error handling for all scenarios
- [ ] T057 Document integration test results

### 4.4 Performance & Load Testing

- [ ] T058 Build Docker image: `./gradlew dockerBuild`
- [ ] T059 Deploy to local Kubernetes cluster
- [ ] T060 Run load test with 100 concurrent requests
- [ ] T061 Measure p50, p95, p99 latencies
- [ ] T062 Compare with RestTemplate baseline metrics
- [ ] T063 Verify no memory leaks under load
- [ ] T064 Document performance findings

### 4.5 Manual Testing

- [ ] T065 [P] Test successful entity retrieval via Service A
- [ ] T066 [P] Test OAuth2 token acquisition from Keycloak
- [ ] T067 [P] Test expired token refresh scenarios
- [ ] T068 [P] Test invalid token rejection
- [ ] T069 [P] Test Service B unavailability handling
- [ ] T070 [P] Test concurrent request handling
- [ ] T071 Monitor application logs for errors

### 4.6 Observability Verification

- [ ] T072 Verify structured JSON logs appear in Loki
- [ ] T073 Verify RestClient metrics exported to Prometheus
- [ ] T074 Verify OpenTelemetry traces appear in Tempo
- [ ] T075 Check Grafana dashboards for Service A metrics

---

## Phase 5: Deployment ⏳ PENDING

### 5.1 Code Review Preparation

- [ ] T076 Commit all changes to feature branch `002-spring-boot-v4`
- [ ] T077 Create pull request with detailed description:
  - Reference RESTCLIENT-MIGRATION.md
  - List all 12 files modified/created
  - Link to test results
  - Include Constitution compliance checklist
- [ ] T078 [P] Update API documentation (no API changes, but note implementation)
- [ ] T079 [P] Add release notes for v7.1.0

### 5.2 Code Review Execution

- [ ] T080 Request code review from architecture team
- [ ] T081 Verify Architecture review: RestClient config correct ✅
- [ ] T082 Verify Security review: No hardcoded credentials, Bearer tokens ✅
- [ ] T083 Verify Testing review: Coverage >90%, OAuth2 tests ✅
- [ ] T084 Verify Performance review: No regression expected ✅
- [ ] T085 Address any code review comments (if any)

### 5.3 Merge & Release

- [ ] T086 Approve pull request (all reviews complete)
- [ ] T087 Merge to main branch
- [ ] T088 Tag release: `git tag v7.1.0`
- [ ] T089 Push tags to GitHub: `git push --tags`

### 5.4 Kubernetes Deployment

- [ ] T090 Trigger FluxCD reconciliation
- [ ] T091 Monitor HelmRelease status: `kubectl get hr`
- [ ] T092 Verify pod status: `kubectl get pods`
- [ ] T093 Check service readiness: `/health/ready` endpoint
- [ ] T094 Monitor logs for startup errors

### 5.5 Post-Deployment Verification

- [ ] T095 Monitor error rates (target: 0)
- [ ] T096 Monitor latency metrics (target: <1s)
- [ ] T097 Verify OAuth2 token acquisition working
- [ ] T098 Monitor authentication failures (target: 0 for valid clients)
- [ ] T099 Check resource utilization (CPU, memory)
- [ ] T100 Review application logs for issues
- [ ] T101 Notify stakeholders of successful deployment

### 5.6 Documentation & Cleanup

- [ ] T102 Generate final deployment report
- [ ] T103 Update project documentation with RestClient approach
- [ ] T104 Archive migration guides for future reference
- [ ] T105 Schedule post-deployment review (1 week after)

---

## Phase 6: Post-Deployment (Optional)

### 6.1 Circuit Breaker Enhancement (Future)

- [ ] T106 Consider adding Resilience4j circuit breaker to EntityClient
- [ ] T107 Implement request correlation IDs for distributed tracing
- [ ] T108 Add request/response logging interceptor

### 6.2 Documentation Maintenance

- [ ] T109 Update team wiki with RestClient patterns
- [ ] T110 Document lessons learned in architecture ADR
- [ ] T111 Schedule quarterly review of HTTP client implementation

---

## Task Priority & Effort Matrix

### High Priority, Low Effort (Quick Wins)
- T012-T038: Integration test updates (already designed, straightforward)
- T043-T046: Compilation verification (automated)
- T047-T052: Test execution & coverage (automated)

### High Priority, Medium Effort (Core Work)
- T053-T064: Performance & load testing (requires setup, execution)
- T076-T084: Code review process (requires coordination)
- T090-T105: Deployment (requires Kubernetes access)

### Medium Priority, Low Effort (Administrative)
- T077-T079: Documentation (using existing templates)
- T102-T111: Post-deployment (cleanup & planning)

---

## Estimated Timeline

### Phase 3: Integration Tests (3-4 hours)
- ServiceToServiceAuthIntegrationTest: 45 min
- ChaosEngineeringTest: 45 min
- KeycloakChaosTest: 30 min
- ErrorHandlingTest: 30 min
- Full integration test run: 30 min

### Phase 4: Validation (2-3 hours)
- Compilation verification: 30 min
- Unit test execution: 15 min
- Integration test execution: 30 min
- Coverage analysis: 15 min
- Load testing: 45 min

### Phase 5: Deployment (2-3 hours)
- Code review preparation: 30 min
- Code review execution: 45 min
- Merge & release: 15 min
- Kubernetes deployment: 30 min
- Post-deployment verification: 30 min

**Total Remaining Effort**: 7-10 hours

---

## Success Criteria

### Phase 3 Completion
- [x] All 4 integration test files updated
- [x] All test scenarios run successfully
- [x] No test failures
- [x] OAuth2 flow verified end-to-end

### Phase 4 Completion
- [x] Code compiles with zero errors
- [x] All unit tests pass (100%)
- [x] All integration tests pass (100%)
- [x] Code coverage >90% (target: >95%)
- [x] Performance metrics acceptable
- [x] No memory leaks detected

### Phase 5 Completion
- [x] Pull request created and reviewed
- [x] All code review feedback addressed
- [x] Merged to main branch
- [x] Release tagged v7.1.0
- [x] Successfully deployed to Kubernetes
- [x] No errors in production logs
- [x] Metrics normal in production

---

## Risk Mitigation

### Risk: Integration tests fail after RestClient migration
**Mitigation**: Phase 3 tasks specifically designed to verify each test scenario. Run locally before committing.

### Risk: Performance regression
**Mitigation**: Phase 4 includes load testing against baseline. RestClient uses same HTTP transport, no regression expected.

### Risk: OAuth2 flow breaks during migration
**Mitigation**: OAuth2AuthorizedClientManager unchanged. Verified in EntityClient.java.

### Risk: Deployment fails
**Mitigation**: Rollback plan documented in RESTCLIENT-MIGRATION.md. Simple `git revert` and push.

---

## Rollback Plan

If issues arise at any phase:

1. **Phase 3 Issues**: Revert changes to test files, restart Phase 3
2. **Phase 4 Issues**: Fix identified bugs, re-run tests
3. **Phase 5 Issues**: 
   ```bash
   git revert <commit-hash>
   git push
   # FluxCD automatically rolls back to previous version
   ```

---

## Key Stakeholders & Approval

- **Implementing Agent**: Execute tasks T012-T105 as assigned
- **Code Review**: Architecture team verifies Constitution compliance
- **QA**: Verifies test scenarios comprehensive
- **DevOps**: Manages Kubernetes deployment
- **Product**: Approves release notes and messaging

---

## Next Steps

1. **Immediately**: Start Phase 3 task T012 (ServiceToServiceAuthIntegrationTest updates)
2. **After Phase 3**: Run full test suite (Phase 4)
3. **After Phase 4**: Code review and merge (Phase 5)
4. **After Phase 5**: Production monitoring and post-deployment review

**Current Status**: ✅ Phase 1-2 Complete → ⏳ Ready to start Phase 3

---

**Generated**: May 12, 2026  
**Branch**: `002-spring-boot-v4`  
**Status**: Tasks ready for execution  
**Format**: Speckit task checklist (T001-T111)

