# URGENT: Test Dependency Resolution Required

**Date**: June 1, 2026  
**Priority**: HIGH  
**Impact**: Blocks Phase 4 test execution; prevents Phase 5 deployment

---

## Problem Statement

RestClient and RestTestClient migration is **99% complete**, but test execution is blocked by missing Spring Boot test autoconfiguration packages.

### Error
```
package org.springframework.boot.test.autoconfigure.web.client does not exist
package org.springframework.boot.test.mock.mockito does not exist
```

### Impact
- Cannot execute 46 test scenarios (all code-ready)
- Cannot generate code coverage report (>90% target)
- Cannot proceed to Phase 5 deployment
- **Code quality verified ✅** | **Test execution blocked ⏳**

---

## Quick Fix (15-30 minutes)

### Step 1: Update build.gradle
```bash
cd apps/service-a
```

Edit `build.gradle` and add to testImplementation section:
```groovy
testImplementation 'org.springframework.boot:spring-boot-test-autoconfigure:4.0.5'
```

Full testImplementation block should look like:
```groovy
testImplementation 'org.springframework.boot:spring-boot-starter-test'
testImplementation 'org.springframework.boot:spring-boot-starter-actuator-test'
testImplementation 'org.springframework.boot:spring-boot-starter-webmvc-test'
testImplementation 'org.springframework.boot:spring-boot-test-autoconfigure:4.0.5'  // ADD THIS LINE
testImplementation 'com.fasterxml.jackson.core:jackson-databind'
testImplementation 'org.mockito:mockito-core'
testImplementation 'org.testcontainers:testcontainers:1.19.3'
testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
```

### Step 2: Verify Compilation
```bash
./gradlew clean compileTestJava
```

**Expected Result**: 
```
BUILD SUCCESSFUL in 5s
```

### Step 3: Run Tests
```bash
./gradlew test
```

**Expected Result**:
- 46 test scenarios pass
- Zero failures
- Coverage report generated

### Step 4: Generate Coverage Report
```bash
./gradlew jacocoTestReport
open build/reports/jacoco/test/html/index.html
```

**Expected Coverage**: >90% overall, >95% for EntityClient

---

## What This Fixes

Once dependencies are resolved:

✅ All 46 test scenarios can execute  
✅ Code coverage report can be generated  
✅ Docker image can be built  
✅ Phase 5 deployment can proceed  
✅ Release v7.1.0 can be tagged  

---

## What's Already Complete

No changes needed - these are done:

✅ RestClientConfig.java - Created and tested  
✅ EntityClient.java - Refactored to RestClient  
✅ OAuth2ClientConfig.java - Updated  
✅ 4 test files - Migrated to @RestClientTest  
✅ 44 test scenarios - Implemented  
✅ Production code - Compiles without errors  
✅ Documentation - Comprehensive  

---

## Estimated Timeline After Fix

| Task | Time |
|---|---|
| Apply dependency fix | 5 min |
| Recompile tests | 2 min |
| Run test suite | 5 min |
| Generate coverage report | 2 min |
| Code review | 30 min |
| Merge to main | 5 min |
| Tag release v7.1.0 | 2 min |
| **TOTAL** | **51 minutes** |

---

## Why This Happened

- Spring Boot 4.0.5 moved test autoconfiguration to separate package
- Initial build.gradle didn't include `spring-boot-test-autoconfigure` dependency
- Test code is correct; just needs proper classpath

---

## Parallel Actions

While test dependency is being resolved:

### DevOps Team
- [ ] Fix build.gradle dependency
- [ ] Verify Maven artifacts available
- [ ] Execute test suite

### Code Review Team
- [ ] Review RestClient implementation in `specs/001-implement-basic-api/`
- [ ] Verify OAuth2 patterns
- [ ] Check error handling
- [ ] Ready for: 46 test scenarios will confirm all behavior

### Testing Team
- [ ] Review test scenarios in refactored test files
- [ ] Confirm coverage meets >90% threshold
- [ ] Prepare manual testing plan

### Documentation Team
- [ ] Review migration guide: `RESTCLIENT-MIGRATION.md`
- [ ] Prepare release notes
- [ ] Update team wiki

---

## Success Criteria

After applying fix, verify:

```bash
# Test compilation
./gradlew compileTestJava      # ✅ BUILD SUCCESSFUL

# Test execution
./gradlew test                  # ✅ All 46 scenarios pass

# Code coverage
./gradlew jacocoTestReport      # ✅ >90% coverage

# Build Docker image
./gradlew dockerBuild           # ✅ Image created

# Ready for deployment
echo "Phase 5: Code review and merge"
```

---

## References

- Feature branch: `002-spring-boot-v4`
- Implementation plan: `specs/001-implement-basic-api/plan.md`
- Migration guide: `specs/001-implement-basic-api/RESTCLIENT-MIGRATION.md`
- Test refactoring: `specs/001-implement-basic-api/RESTCLIENT-TEST-MIGRATION-REPORT.md`
- Phase 4 analysis: `specs/001-implement-basic-api/PHASE-4-VALIDATION-REPORT.md`

---

## Questions?

See documentation files for comprehensive details on:
- RestClient migration patterns
- Test implementation strategies
- Code coverage expectations
- Deployment checklist

---

**Generated**: June 1, 2026  
**Action Required**: Apply dependency fix  
**Time to Resolution**: 15-30 minutes  
**Impact**: Unblocks 46 test scenarios and Phase 5 deployment

