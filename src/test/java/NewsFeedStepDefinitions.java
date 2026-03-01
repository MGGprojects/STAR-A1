import com.fasterxml.jackson.databind.JsonNode;
import io.cucumber.java.After;
import io.cucumber.java.en.*;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.*;

public class NewsFeedStepDefinitions {

    private NewsFeedAdapter adapter;
    private JsonNode lastProtocolResponse;
    private JsonNode lastSubscribeResponse;
    private List<JsonNode> notifyMessages = new ArrayList<>();

    @After
    public void tearDown() throws IOException {
        if (adapter != null) {
            try { adapter.sendStopSession(); } catch (Exception ignored) {}
            try { adapter.stopServer(); } catch (Exception ignored) {}
            adapter.close();
        }
    }

    @Given("the NewsFeed server is running on {string} port {int} with manufacturer {string}")
    public void connectToServer(String host, int port, String manufacturer) throws IOException {
        adapter = new NewsFeedAdapter();
        adapter.connect(host, port);
        adapter.selectManufacturer(manufacturer);
    }

    @Given("I have negotiated protocol version {string} with CorrelationId {int}")
    public void negotiateProtocol(String version, int corrId) throws IOException {
        Map<String, Object> params = new HashMap<>();
        params.put("ProtocolVersions", Collections.singletonList(version));
        params.put("CorrelationId", corrId);
        adapter.sendMessage("ProtocolRequest", params);
        adapter.receiveMessageOfType("ProtocolResponse");
        if (!version.equals("1.0")) {
            try { adapter.receiveMessageOfType("AvailableTopics"); } catch (SocketTimeoutException ignored) {}
        }
    }

    @When("I send a ProtocolRequest with versions {string} and CorrelationId {int}")
    public void sendProtocolRequest(String versionsStr, int corrId) throws IOException {
        String stripped = versionsStr.replaceAll("[\\[\\]\"]", "").trim();
        Map<String, Object> params = new HashMap<>();
        params.put("ProtocolVersions", Arrays.asList(stripped.split(",\\s*")));
        params.put("CorrelationId", corrId);
        adapter.sendMessage("ProtocolRequest", params);
    }

    @When("I receive the ProtocolResponse")
    public void receiveProtocolResponse() throws IOException {
        lastProtocolResponse = adapter.receiveMessageOfType("ProtocolResponse");
    }

    @When("I send a SubscribeRequest with topics {string} and CorrelationId {int}")
    public void sendSubscribeRequest(String topicsStr, int corrId) throws IOException {
        String stripped = topicsStr.replaceAll("[\\[\\]\"]", "").trim();
        List<String> topicList = stripped.isEmpty() ? Collections.emptyList()
                : Arrays.asList(stripped.split(",\\s*"));
        Map<String, Object> params = new HashMap<>();
        params.put("Topics", topicList);
        params.put("CorrelationId", corrId);
        adapter.sendMessage("SubscribeRequest", params);
    }

    @When("I receive the SubscribeResponse")
    public void receiveSubscribeResponse() throws IOException {
        lastSubscribeResponse = adapter.receiveMessageOfType("SubscribeResponse");
    }

    @When("I wait for at least one Notify message")
    public void waitForOneNotify() throws IOException {
        notifyMessages.add(adapter.receiveMessageOfType("Notify"));
    }

    @When("I send an Unsubscribe message")
    public void sendUnsubscribe() throws IOException {
        adapter.sendUnsubscribe();
    }

    @Then("I should receive a ProtocolResponse within 1 second")
    public void receiveProtocolResponseWithin1Second() throws IOException {
        long before = System.currentTimeMillis();
        lastProtocolResponse = adapter.receiveMessageOfType("ProtocolResponse");
        long elapsed = System.currentTimeMillis() - before;
        Assertions.assertTrue(elapsed <= 1000, "ProtocolResponse took " + elapsed + "ms");
    }

    @Then("the ProtocolResponse should have ProtocolVersion {string}")
    public void checkProtocolVersion(String expected) {
        String actual = lastProtocolResponse.get("parameters").get("ProtocolVersion").asText();
        Assertions.assertEquals(expected, actual);
    }

    @Then("the ProtocolResponse CorrelationId should be {int}")
    public void checkProtocolResponseCorrId(int expected) {
        Assertions.assertEquals(expected,
                lastProtocolResponse.get("parameters").get("CorrelationId").asInt());
    }

    @Then("I should receive an AvailableTopics message")
    public void receiveAvailableTopics() throws IOException {
        JsonNode msg = adapter.receiveMessageOfType("AvailableTopics");
        Assertions.assertEquals("AvailableTopics", msg.get("name").asText());
    }

    @Then("I should receive an AvailableTopics message with a non-empty Topics list")
    public void receiveAvailableTopicsNonEmpty() throws IOException {
        JsonNode msg = adapter.receiveMessageOfType("AvailableTopics");
        JsonNode topics = msg.get("parameters").get("Topics");
        Assertions.assertTrue(topics.isArray() && topics.size() > 0);
    }

    @Then("I should receive a SubscribeResponse within 1 second")
    public void receiveSubscribeResponseWithin1Second() throws IOException {
        long before = System.currentTimeMillis();
        lastSubscribeResponse = adapter.receiveMessageOfType("SubscribeResponse");
        long elapsed = System.currentTimeMillis() - before;
        Assertions.assertTrue(elapsed <= 1000, "SubscribeResponse took " + elapsed + "ms");
    }

    @Then("the SubscribeResponse should contain topic {string}")
    public void checkSubscribeResponseContainsTopic(String topic) {
        JsonNode topics = lastSubscribeResponse.get("parameters").get("Topics");
        boolean found = false;
        for (JsonNode t : topics) {
            if (t.asText().equals(topic)) { found = true; break; }
        }
        Assertions.assertTrue(found, "SubscribeResponse should contain topic '" + topic + "'");
    }

    @Then("the SubscribeResponse Topics list should not contain {string}")
    public void checkSubscribeResponseNotContainsTopic(String topic) {
        JsonNode topics = lastSubscribeResponse.get("parameters").get("Topics");
        for (JsonNode t : topics) {
            Assertions.assertNotEquals(topic, t.asText());
        }
    }

    @Then("the SubscribeResponse CorrelationId should be {int}")
    public void checkSubscribeResponseCorrId(int expected) {
        Assertions.assertEquals(expected,
                lastSubscribeResponse.get("parameters").get("CorrelationId").asInt());
    }

    @Then("I should not receive any new Notify messages for {int} seconds")
    public void noNotifyMessagesForSeconds(int seconds) {
        long deadline = System.currentTimeMillis() + (long) seconds * 1000;
        long graceUntil = System.currentTimeMillis() + 500;
        try {
            while (System.currentTimeMillis() < deadline) {
                JsonNode msg = adapter.receiveMessage();
                if ("Notify".equals(msg.get("name").asText())) {
                    if (System.currentTimeMillis() <= graceUntil) {
                        continue;
                    }
                    Assertions.fail("Received unexpected Notify after Unsubscribe");
                }
            }
        } catch (IOException e) {
        }
    }
}
