# Test Execution Report: NewsFeedAutomatedTests

**Date:** March 2, 2026  
**Test Class:** `NewsFeedAutomatedTests`

## Summary
- **Total Tests Run:** 10
- **Passed:** 6
- **Failures:** 2
- **Errors:** 2
- **Skipped:** 0
- **Total Time Elapsed:** ~48 seconds

## Detailed Results

### Failed Tests
1. **`testSequenceNumberIncrements`**
   - **Status**: FAILURE
   - **Reason**: `SequenceNumber should increment ==> expected: <true> but was: <false>`
   - **Details**: The sequence number for the `Notify` message did not increment as expected, remaining the same across notifications.

2. **`testSnapshotMessageBooleanIsSnapshotTrue`**
   - **Status**: FAILURE
   - **Reason**: `expected: <true> but was: <false>`
   - **Details**: The `IsSnapshot` parameter in the `Notify` message was expected to be `true` for a snapshot message but evaluated to `false`.

### Tests with Errors
3. **`testUnexpectedMessageReturnsFault`**
   - **Status**: ERROR
   - **Reason**: `java.net.SocketTimeoutException: Read timed out`
   - **Details**: The NewsFeed application failed to send a `Fault` message when provided with an unexpected message format (`RandomMessageXYZ`), causing the socket to time out.

4. **`testCaseSensitivity`**
   - **Status**: ERROR
   - **Reason**: `java.net.SocketTimeoutException: Read timed out`
   - **Details**: A case-sensitive mismatch (`PROTOCOLREQUEST` instead of `ProtocolRequest`) did not elicit the expected `Fault` response, causing a socket timeout.

### Passed Tests
5. **`testEmptyTopicSubscriptionSendsHeartbeats`** - Passed
6. **`testFirstNotifyHasSequenceNumberZero`** - Passed
7. **`testSequenceNumberResetsOnNewSubscription`** - Passed
8. **`testProtocolRequestTerminatesActiveSubscription`** - Passed
9. **`testHeartbeatFormat`** - Passed
10. **`testUpdateMessageHasIsSnapshotFalse`** - Passed

---

*Note: These tests are executed against the "Inixa" manufacturer implementation of the NewsFeed server, which is known to contain bugs according to the documentation.*