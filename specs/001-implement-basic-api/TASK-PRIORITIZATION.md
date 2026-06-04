# RestClient Migration - Prioritized Task Breakdown

**Date**: May 12, 2026  
**Total Tasks**: 111 (42 core + 69 future)  
**Ready Now**: 42 tasks in Phase 3-5

---

## 🎯 NEXT IMMEDIATE ACTIONS (Start Here)

### TODAY - Phase 3a: Critical Integration Test Update (2-3 hours)

1. **T012**: Open `apps/service-a/src/test/java/com/agilesolutions/service_a/integration/ServiceToServiceAuthIntegrationTest.java`
   - Replace: `@MockBean private RestTemplate restTemplate;`
   - With: `@Autowired private MockRestServiceServer mockServer;` + `@Autowired RestClient restClient;`
   - Impact: Enable RestTestClient mocking
   - Time: 15 min

2. **T013**: Update imports in ServiceToServiceAuthIntegrationTest.java
   - Remove RestTemplate imports
   - Add: `import org.springframework.test.web.client.MockRestServiceServer;`
   - Add: `import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;`
   - Time: 10 min

3. **T014**: Update setUp() method
   - Initialize MockRestServiceServer
   - Configure mock expectations
   - Time: 15 min

4. **T015**: Refactor first test: `testEntityRetrievalWithValidToken()`
   - OLD: Mock RestTemplate.exchange()
   - NEW: Set up MockRestServiceServer expectation with requestTo(), header(), andRespond()
   - Reference: EntityClientTest.java line 80-102 for pattern
   - Time: 20 min

5. **T016**: Refactor remaining 12 test methods (parallel, 30 min total)
   - testEntityRetrievalWithoutToken()
   - testEntityRetrievalWithExpiredToken()
   - testEntityRetrievalWithInvalidTokenFormat()
   - testEntityRetrievalWithMalformedAuthHeader()
   - testOAuth2TokenIncludesCorrectScope()
   - testEntityClientRequestsTokenWithCorrectPrincipal()
   - testOAuth2TokenIncludedInServiceBRequest()
   - testHandle401FromServiceB()
   - testHandle403FromServiceB()
   - testOAuth2SupportServiceToServiceFlow()
   - testTokenCachingMechanism()
   - testConcurrentOAuth2Requests()
   - testAuthorizationIsStateless()

6. **T018**: Run tests locally
   ```bash
   cd apps/service-a
   ./gradlew test --tests ServiceToServiceAuthIntegrationTest -i
   ```
   - Expected: ALL PASS ✅
   - Time: 5 min

### DAY 2 - Phase 3b: Remaining Integration Tests (1.5-2 hours)

7. **T019-T025**: Update ChaosEngineeringTest.java
   - Apply same RestClient pattern as T012-T018
   - Focus on timeout/retry scenarios
   - Time: 45 min

8. **T026-T031**: Update KeycloakChaosTest.java
   - Mock Keycloak token endpoint with RestTestClient
   - Test token acquisition failures
   - Time: 30 min

9. **T032-T038**: Update ErrorHandlingTest.java
   - Update error scenario mocking
   - Verify 4xx, 5xx, network errors
   - Time: 30 min

10. **T039-T042**: Verify all integration tests pass
    ```bash
    ./gradlew integrationTest
    ```
    - Expected: 100% pass rate
    - Time: 10 min

### DAY 3 - Phase 4: Test & Validation (2-3 hours)

11. **T043-T046**: Compilation & Build Verification
    ```bash
    ./gradlew clean compileJava
    ```
    - Expected: 0 errors, 0 warnings
    - Time: 10 min

12. **T047-T052**: Test Execution & Coverage
    ```bash
    ./gradlew test
    ./gradlew jacocoTestReport
    ```
    - Expected: >95% coverage for EntityClient
    - Time: 30 min

13. **T053-T057**: Integration Test Verification
    - Full suite: `./gradlew integrationTest`
    - Time: 15 min

14. **T058-T064**: Performance Testing (optional but recommended)
    ```bash
    ./gradlew dockerBuild
    # Deploy to local k8s
    # Run load test with 100 concurrent requests
    ```
    - Time: 60 min

### DAY 4 - Phase 5: Code Review & Merge (2-3 hours)

15. **T076-T084**: Pull Request & Code Review
    - Create PR with RESTCLIENT-MIGRATION.md reference
    - Get code review approval
    - Time: 60-90 min

16. **T085-T101**: Merge & Deploy
    - Merge to main
    - Tag v7.1.0
    - Deploy via FluxCD
    - Monitor in production
    - Time: 60-90 min

---

## 📊 Task Grouping by Type

### Configuration Tasks (COMPLETE ✅)
- T001-T011: Already done in Phase 1-2

### Integration Test Updates (PRIORITY 1 - DO NOW)
- T012-T042: 31 tasks for updating 4 test files

### Testing & Validation (PRIORITY 2 - AFTER T012-T042)
- T043-T075: 33 tasks for execution, coverage, load testing

### Deployment (PRIORITY 3 - FINAL)
- T076-T111: 36 tasks for code review, merge, deploy, monitoring

---

## ✅ Quick Checklist for Today

- [ ] Read EntityClientTest.java (reference implementation)
- [ ] Open ServiceToServiceAuthIntegrationTest.java
- [ ] Update imports (remove RestTemplate, add RestTestClient)
- [ ] Update setUp() method (add MockRestServiceServer)
- [ ] Refactor testEntityRetrievalWithValidToken() (1 test as pilot)
- [ ] Run: `./gradlew test --tests ServiceToServiceAuthIntegrationTest`
- [ ] If passing, refactor remaining 12 tests
- [ ] Run full integration test suite

**Expected Time**: 2-3 hours for Phase 3a

---

## 📝 Task Template for Each Integration Test Update

### For Each Test File (ServiceToServiceAuthIntegrationTest, ChaosEngineeringTest, etc.):

**Step 1: Prepare (5 min)**
```
Open file in IDE
Review current RestTemplate usage
Check EntityClientTest.java for patterns
```

**Step 2: Update Imports (10 min)**
```
Remove: org.springframework.web.client.RestTemplate
Add: org.springframework.test.web.client.MockRestServiceServer
Add: org.springframework.test.web.client.match.*
```

**Step 3: Update Class Setup (15 min)**
```
@Autowired MockRestServiceServer mockServer;
@Autowired RestClient restClient;
@Autowired ObjectMapper objectMapper;

@BeforeEach void setUp() {
  mockServer = MockRestServiceServer.createServer(restClient);
}
```

**Step 4: Update Each Test Method (20 min each)**
```
OLD PATTERN:
when(restTemplate.exchange(...)).thenReturn(...)

NEW PATTERN:
mockServer.expect(once(), requestTo(...))
  .andExpect(method(GET))
  .andExpect(header(...))
  .andRespond(withStatus(...))
```

**Step 5: Verify (5 min)**
```
./gradlew test --tests [ClassName]
Expected: ALL PASS ✅
```

---

## 🎯 Success Criteria by Phase

### Phase 3 Success = All Tests Pass
```
ServiceToServiceAuthIntegrationTest: 13/13 tests pass ✅
ChaosEngineeringTest: All chaos scenarios pass ✅
KeycloakChaosTest: All auth failures handled ✅
ErrorHandlingTest: All error paths verified ✅
```

### Phase 4 Success = >90% Coverage
```
Coverage report >95% for EntityClient ✅
All unit tests pass ✅
All integration tests pass ✅
Load test latency <1s p95 ✅
```

### Phase 5 Success = Deployed & Stable
```
PR approved with code review ✅
Merged to main ✅
v7.1.0 tagged ✅
Deployed to production ✅
No errors in production logs ✅
```

---

## 💡 Tips for Success

1. **Reference Implementation**: EntityClientTest.java shows the complete RestTestClient pattern - use it as your guide

2. **Pattern Consistency**: All 4 test file updates follow same pattern - do 1st one carefully, rest become easy

3. **Incremental Validation**: After each test file, run that file's tests to catch issues early

4. **Local Testing First**: Run all tests locally before pushing (saves review cycle time)

5. **Commit Granularly**: Commit each test file update separately for cleaner git history

---

## 🚀 Ready to Start?

**Next Step**: Open EntityClientTest.java to understand RestTestClient pattern  
**Then**: Start with T012 (ServiceToServiceAuthIntegrationTest first test update)  
**Finally**: Work through T013-T018 for that file  

Good luck! The hardest part is behind you - Phase 2 is complete. Phase 3 is straightforward application of existing patterns.


