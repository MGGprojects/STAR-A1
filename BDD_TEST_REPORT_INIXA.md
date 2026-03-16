# Test Execution Report: RunCucumberTests (Inixa)

**Test Runner:** `RunCucumberTests`  
**Manufacturer Under Test:** `Inixa`

## Summary
- **Total BDD Scenarios Run:** 10
- **Passed:** 3
- **Failed:** 7
- **Skipped:** 0

## Detailed Results

### Failed Scenarios
1. **`Successful protocol negotiation with version 3.0`**
   - **Status**: FAILED
   - **Reason**: `NullPointerException` while reading `CorrelationId` from response payload.

2. **`First supported version is selected when multiple are offered`**
   - **Status**: FAILED
   - **Reason**: `NullPointerException` while reading `CorrelationId` from response payload.

3. **`CorrelationId is correctly echoed in ProtocolResponse`**
   - **Status**: FAILED
   - **Reason**: `NullPointerException` while reading `CorrelationId` from response payload.

4. **`AvailableTopics is sent after version 2.0 negotiation`**
   - **Status**: FAILED
   - **Reason**: `SocketTimeoutException: Read timed out`.

5. **`AvailableTopics contains a non-empty Topics list`**
   - **Status**: FAILED
   - **Reason**: `SocketTimeoutException: Read timed out`.

6. **`Subscribe to valid topics and receive SubscribeResponse`**
   - **Status**: FAILED
   - **Reason**: `NullPointerException` while reading `CorrelationId` from response payload.

7. **`Starting new subscription replaces the previous one`**
   - **Status**: FAILED
   - **Reason**: `NullPointerException` while reading `CorrelationId` from response payload.

### Passed Scenarios
8. **`Protocol negotiation with unsupported version`** - Passed
9. **`Subscribing with invalid topics results in filtered response`** - Passed
10. **`Unsubscribing stops notifications`** - Passed

---

