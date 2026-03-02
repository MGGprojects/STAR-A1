import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.*;

public class NewsFeedAutomatedTests {

    private NewsFeedAdapter adapter;

    @BeforeEach
    public void setup() throws IOException {
        adapter = new NewsFeedAdapter();
        adapter.connect("localhost", 2222);
        adapter.selectManufacturer("Inixa");
    }

    @AfterEach
    public void teardown() {
        if (adapter != null) {
            try {
                adapter.sendStopSession();
            } catch (Exception ignored) {
            }
            try {
                adapter.close();
            } catch (Exception ignored) {
            }
        }
    }

    private void negotiateProtocol(String version, int corrId) throws IOException {
        Map<String, Object> params = new HashMap<>();
        params.put("ProtocolVersions", Collections.singletonList(version));
        params.put("CorrelationId", corrId);
        adapter.sendMessage("ProtocolRequest", params);
        adapter.receiveMessageOfType("ProtocolResponse");
        if (!version.equals("1.0")) {
            try {
                adapter.receiveMessageOfType("AvailableTopics");
            } catch (SocketTimeoutException ignored) {
            }
        }
    }

    private void subscribeTo(List<String> topics, int corrId) throws IOException {
        Map<String, Object> params = new HashMap<>();
        params.put("Topics", topics);
        params.put("CorrelationId", corrId);
        adapter.sendMessage("SubscribeRequest", params);
        adapter.receiveMessageOfType("SubscribeResponse");
    }

    @Test
    public void testCaseSensitivity() throws IOException {
        // CASE-01
        Map<String, Object> params = new HashMap<>();
        params.put("ProtocolVersions", Collections.singletonList("3.0"));
        params.put("CorrelationId", 1);
        adapter.sendMessage("PROTOCOLREQUEST", params);

        JsonNode fault = adapter.receiveMessageOfType("Fault");
        Assertions.assertEquals("Fault", fault.get("name").asText());
    }

    @Test
    public void testUnexpectedMessageReturnsFault() throws IOException {
        // FAULT-01
        Map<String, Object> params = new HashMap<>();
        adapter.sendMessage("RandomMessageXYZ", params);

        JsonNode fault = adapter.receiveMessageOfType("Fault");
        Assertions.assertEquals("Fault", fault.get("name").asText());
    }

    @Test
    public void testEmptyTopicSubscriptionSendsHeartbeats() throws IOException {
        // SUBSCR-04
        negotiateProtocol("3.0", 1);
        subscribeTo(Collections.emptyList(), 2);
        
        // Wait for snapshot or heartbeat
        JsonNode notify = adapter.receiveMessageOfType("Notify");
        Assertions.assertEquals("Notify", notify.get("name").asText());
        
        // Heartbeats should have empty Topic, empty Content, IsSnapshot false (HEARTBEAT-02)
        boolean isSnapshot = notify.get("parameters").get("IsSnapshot").asBoolean();
        if (!isSnapshot) {
            Assertions.assertEquals("", notify.get("parameters").get("Topic").asText());
            Assertions.assertEquals("", notify.get("parameters").get("Content").asText());
        }
    }

    @Test
    public void testFirstNotifyHasSequenceNumberZero() throws IOException {
        // NOTIFY-02
        negotiateProtocol("3.0", 1);
        subscribeTo(Collections.singletonList("sport"), 2);

        JsonNode notify = adapter.receiveMessageOfType("Notify");
        Assertions.assertEquals(0, notify.get("parameters").get("SequenceNumber").asInt());
    }

    @Test
    public void testSequenceNumberIncrements() throws IOException {
        // NOTIFY-02
        negotiateProtocol("3.0", 1);
        subscribeTo(Collections.singletonList("sport"), 2);

        JsonNode notify1 = adapter.receiveMessageOfType("Notify");
        int seq1 = notify1.get("parameters").get("SequenceNumber").asInt();
        
        JsonNode notify2 = adapter.receiveMessageOfType("Notify");
        int seq2 = notify2.get("parameters").get("SequenceNumber").asInt();

        Assertions.assertTrue(seq2 > seq1, "SequenceNumber should increment");
    }

    @Test
    public void testSequenceNumberResetsOnNewSubscription() throws IOException {
        // NOTIFY-02
        negotiateProtocol("3.0", 1);
        subscribeTo(Collections.singletonList("sport"), 2);
        adapter.receiveMessageOfType("Notify"); // consume first notify

        subscribeTo(Collections.singletonList("weather"), 3);
        JsonNode notifyNewSub = adapter.receiveMessageOfType("Notify");
        Assertions.assertEquals(0, notifyNewSub.get("parameters").get("SequenceNumber").asInt());
    }

    @Test
    public void testSnapshotMessageBooleanIsSnapshotTrue() throws IOException {
        // SNAPSHOT-01
        negotiateProtocol("3.0", 1);
        subscribeTo(Collections.singletonList("sport"), 2);

        JsonNode notify = adapter.receiveMessageOfType("Notify");
        Assertions.assertTrue(notify.get("parameters").get("IsSnapshot").asBoolean());
    }

    @Test
    public void testProtocolRequestTerminatesActiveSubscription() throws IOException {
        // SESSION-05
        negotiateProtocol("3.0", 1);
        subscribeTo(Collections.singletonList("sport"), 2);
        adapter.receiveMessageOfType("Notify"); // verify active

        // send new ProtocolRequest
        Map<String, Object> params = new HashMap<>();
        params.put("ProtocolVersions", Collections.singletonList("3.0"));
        params.put("CorrelationId", 3);
        adapter.sendMessage("ProtocolRequest", params);
        adapter.receiveMessageOfType("ProtocolResponse");
        try {
            adapter.receiveMessageOfType("AvailableTopics");
        } catch (SocketTimeoutException ignored) {
        }
        
        // Ensure no extra notifies from previous sub arrive, should just wait or at most get fresh snapshot if subscribed again
        // We will wait and expect a heartbeat or no notify
        // Actually since session restarted, no subscription is active, so we should NOT receive Notify anymore.
        long deadline = System.currentTimeMillis() + 4000;
        try {
            while (System.currentTimeMillis() < deadline) {
                JsonNode msg = adapter.receiveMessage();
                if ("Notify".equals(msg.get("name").asText())) {
                    Assertions.fail("Received Notify after new ProtocolRequest terminated subscription");
                }
            }
        } catch (IOException e) {
            // timeout expected
        }
    }

    @Test
    public void testHeartbeatFormat() throws IOException {
        // HEARTBEAT-02
        negotiateProtocol("3.0", 1);
        subscribeTo(Collections.emptyList(), 2);
        
        // Wait long enough to get past snapshot (if any) and get heartbeat
        JsonNode heartbeat = null;
        long deadline = System.currentTimeMillis() + 10000;
        while (System.currentTimeMillis() < deadline) {
            try {
                JsonNode notify = adapter.receiveMessage();
                if ("Notify".equals(notify.get("name").asText()) && !notify.get("parameters").has("IsSnapshot") == false && !notify.get("parameters").get("IsSnapshot").asBoolean()) {
                    heartbeat = notify;
                    break;
                }
            } catch (SocketTimeoutException e) {
                // ignore and continue
            }
        }
        
        Assertions.assertNotNull(heartbeat, "Should have received a heartbeat");
        Assertions.assertEquals("", heartbeat.get("parameters").get("Topic").asText());
        Assertions.assertEquals("", heartbeat.get("parameters").get("Content").asText());
        Assertions.assertFalse(heartbeat.get("parameters").get("IsSnapshot").asBoolean());
    }

    @Test
    public void testUpdateMessageHasIsSnapshotFalse() throws IOException {
        // UPDATES-01
        negotiateProtocol("3.0", 1);
        subscribeTo(Collections.singletonList("sport"), 2);
        
        boolean foundUpdate = false;
        long deadline = System.currentTimeMillis() + 10000;
        while (System.currentTimeMillis() < deadline) {
             try {
                 JsonNode notify = adapter.receiveMessage();
                 if ("Notify".equals(notify.get("name").asText())) {
                     if (!notify.get("parameters").get("IsSnapshot").asBoolean()) {
                         foundUpdate = true;
                         break;
                     }
                 }
             } catch (SocketTimeoutException e) {
                 // ignore and continue
             }
        }
        Assertions.assertTrue(foundUpdate, "Should have received an update message with IsSnapshot=false");
    }
}