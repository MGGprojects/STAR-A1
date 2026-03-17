package com.axini.adapter.newsfeed;

import com.axini.adapter.generic.AxiniProtobuf;
import com.axini.adapter.generic.Handler;

import PluginAdapter.Api.ConfigurationOuterClass.Configuration;
import PluginAdapter.Api.LabelOuterClass.Label;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class NewsFeedHandler extends Handler {

    private static final Logger logger =
            LoggerFactory.getLogger(com.axini.adapter.newsfeed.NewsFeedHandler.class);

    private static final String CHANNEL = "newsfeed";
    private static final String HOST = "localhost";
    private static final int PORT = 2222;
    private static final String IMPLEMENTATION = "Axini";

    private static final Set<String> STIMULI = Set.of(
            "ProtocolRequest",
            "SubscribeRequest",
            "Unsubscribe",
            "StopSession"
    );

    private static final Set<String> RESPONSES = Set.of(
            "ProtocolResponse",
            "AvailableTopics",
            "SubscribeResponse",
            "Notify",
            "NewTopicAvailable",
            "Fault"
    );

    private com.axini.adapter.newsfeed.NewsFeedConnection connection;
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Configuration defaultConfiguration() {
        Configuration.Item host =
                AxiniProtobuf.createItem("host",
                        "Host of NewsFeed SUT", HOST);

        Configuration.Item port =
                AxiniProtobuf.createItem("port",
                        "Port of NewsFeed SUT", PORT);

        Configuration.Item implementation =
                AxiniProtobuf.createItem("implementation",
                        "Which NewsFeed implementation will be used", IMPLEMENTATION);

        List<Configuration.Item> items = new ArrayList<>();
        items.add(host);
        items.add(port);
        items.add(implementation);

        return AxiniProtobuf.createConfiguration(items);
    }

    @Override
    public void start() {
        logger.info("Starting NewsFeed adapter");

        // Get config values
        Configuration config = getConfiguration();
        String host = AxiniProtobuf.getStringFromConfig(config, "host");
        int port = (int) AxiniProtobuf.getIntegerFromConfig(config, "port");
        String implementation = AxiniProtobuf.getStringFromConfig(config, "implementation");

        try {
            // Establish connection to NewsFeed
            connection = new com.axini.adapter.newsfeed.NewsFeedConnection();
            connection.registerHandler(this);
            connection.connect(host, port);

            // Select manufacturer
            connection.selectManufacturer(implementation);

            // Signal readiness to AMP
            adapterCore.sendReady();
        } catch (IOException e) {
            logger.error("Failed to start NewsFeed connection", e);
            connection = null;
            sendErrorToAmp("Failed to start NewsFeed connection: " + e.getMessage());
        }
    }

    @Override
    public void stop() {
        logger.info("Stopping NewsFeed adapter");

        if (connection == null) {
            return;
        }

        if (connection.isConnected()) {
            try {
                connection.sendStopServer();
            } catch (IOException e) {
                logger.warn("Failed to send STOP to NewsFeed server: {}", e.getMessage());
            }
        }

        try {
            connection.close();
        } catch (IOException e) {
            logger.warn("Failed to close NewsFeed connection: {}", e.getMessage());
        } finally {
            connection = null;
        }
    }

    @Override
    public void reset() {
        logger.info("Resetting NewsFeed session");

        stop();
        start();
    }

    @Override
    public ByteString stimulate(Label stimulus) {
        String name = stimulus.getLabel();
        logger.info("Stimulus from AMP: {}", name);

        // Guard - stimulus should be supported
        if (!STIMULI.contains(name)) {
            sendErrorToAmp("Unsupported stimulus: " + name);
            return ByteString.EMPTY;
        }

        // Guard - connection should be active
        if (connection == null || !connection.isConnected()) {
            sendErrorToAmp("No active connection to NewsFeed server.");
            return ByteString.EMPTY;
        }

        try {
            Map<String, Object> params = extractParameters(stimulus);
            normalizeOutgoingParameters(name, params);

            String json = connection.sendMessage(name, params);

            return sendStimulusConfirmation(stimulus, json);
        } catch (Exception e) {
            logger.error("Stimulate failed", e);
            sendErrorToAmp("Stimulate failed: " + e.getMessage());
            return ByteString.EMPTY;
        }
    }

    public ByteString sendStimulusConfirmation(Label stimulus, String json) {
        ByteString physicalLabel = ByteString.copyFromUtf8(json);
        long timestamp = AxiniProtobuf.timestamp();

        Label confirmation = AxiniProtobuf.createLabel(
                stimulus,
                physicalLabel,
                timestamp,
                stimulus.getCorrelationId()
        );

        adapterCore.sendStimulusConfirmation(confirmation);

        return physicalLabel;
    }

    public void sendResponseToAmp(String message) {
        logger.info("Forwarding response to AMP: {}", message);

        try {
            JsonNode json = mapper.readTree(message);

            // Guard - message must have 'name' field
            JsonNode nameNode = json.get("name");
            if (nameNode == null || !nameNode.isTextual()) {
                throw new IllegalArgumentException("Incoming SUT message has no valid 'name' field");
            }

            // Guard - response must be supported
            String responseName = nameNode.asText();
            if (!RESPONSES.contains(responseName)) {
                throw new IllegalArgumentException("Unsupported response from SUT: " + responseName);
            }

            Label logicalResponse = buildResponseLabel(responseName, json);
            ByteString physicalResponse = ByteString.copyFromUtf8(message);

            adapterCore.sendResponse(
                    logicalResponse,
                    physicalResponse,
                    AxiniProtobuf.timestamp()
            );
        } catch (Exception e) {
            logger.error("Failed to parse/forward response", e);
            sendErrorToAmp("Failed to parse/forward response: " + e.getMessage());
        }
    }

    public void sendErrorToAmp(String error) {
        logger.error("Sending error to AMP: {}", error);

        Label logicalResponse = AxiniProtobuf.createResponse("Fault", CHANNEL);
        ByteString physicalResponse = ByteString.copyFromUtf8(error);

        adapterCore.sendResponse(
                logicalResponse,
                physicalResponse,
                AxiniProtobuf.timestamp()
        );
    }

    @Override
    public List<Label> getSupportedLabels() {
        List<Label> labels = new ArrayList<>();

        // ===
        //  Stimuli
        // ===

        labels.add(AxiniProtobuf.createStimulus(
                "ProtocolRequest",
                CHANNEL,
                List.of(
                        AxiniProtobuf.createParameter("ProtocolVersions",
                                AxiniProtobuf.createStringValue("")),
                        AxiniProtobuf.createParameter("CorrelationId",
                                AxiniProtobuf.createIntValue(0))
                )
        ));

        labels.add(AxiniProtobuf.createStimulus(
                "SubscribeRequest",
                CHANNEL,
                List.of(
                        AxiniProtobuf.createParameter("Topics",
                                AxiniProtobuf.createStringValue("")),
                        AxiniProtobuf.createParameter("CorrelationId",
                                AxiniProtobuf.createIntValue(0))
                )
        ));

        labels.add(AxiniProtobuf.createStimulus("Unsubscribe", CHANNEL));
        labels.add(AxiniProtobuf.createStimulus("StopSession", CHANNEL));

        // ===
        //  Responses
        // ===

        labels.add(AxiniProtobuf.createResponse(
                "ProtocolResponse",
                CHANNEL,
                List.of(
                        AxiniProtobuf.createParameter("ProtocolVersion",
                                AxiniProtobuf.createStringValue("")),
                        AxiniProtobuf.createParameter("CorrelationId",
                                AxiniProtobuf.createIntValue(0))
                )
        ));

        labels.add(AxiniProtobuf.createResponse(
                "AvailableTopics",
                CHANNEL,
                List.of(
                        AxiniProtobuf.createParameter("Topics",
                                AxiniProtobuf.createStringValue(""))
                )
        ));

        labels.add(AxiniProtobuf.createResponse(
                "SubscribeResponse",
                CHANNEL,
                List.of(
                        AxiniProtobuf.createParameter("Topics",
                                AxiniProtobuf.createStringValue("")),
                        AxiniProtobuf.createParameter("CorrelationId",
                                AxiniProtobuf.createIntValue(0))
                )
        ));

        labels.add(AxiniProtobuf.createResponse(
                "Notify",
                CHANNEL,
                List.of(
                        AxiniProtobuf.createParameter("SequenceNumber",
                                AxiniProtobuf.createIntValue(0)),
                        AxiniProtobuf.createParameter("IsSnapshot",
                                AxiniProtobuf.createBooleanValue(false)),
                        AxiniProtobuf.createParameter("Topic",
                                AxiniProtobuf.createStringValue("")),
                        AxiniProtobuf.createParameter("Content",
                                AxiniProtobuf.createStringValue(""))
                )
        ));

        labels.add(AxiniProtobuf.createResponse(
                "NewTopicAvailable",
                CHANNEL,
                List.of(
                        AxiniProtobuf.createParameter("Topic",
                                AxiniProtobuf.createStringValue(""))
                )
        ));

        labels.add(AxiniProtobuf.createResponse("Fault", CHANNEL));

        return labels;
    }

    private Map<String, Object> extractParameters(Label stimulus) {
        Map<String, Object> params = new HashMap<>();

        for (Label.Parameter p : stimulus.getParametersList()) {
            if (p.getValue().hasString()) {
                params.put(p.getName(), p.getValue().getString());
            } else if (p.getValue().hasInteger()) {
                params.put(p.getName(), p.getValue().getInteger());
            } else if (p.getValue().hasBoolean()) {
                params.put(p.getName(), p.getValue().getBoolean());
            }
        }

        return params;
    }

    private void normalizeOutgoingParameters(String messageName, Map<String, Object> params) {
        // ProtocolVersions to list
        // e.g.: "1.0,2.1" -> ["1.0", "2.1"]
        Object protocolVersions = params.get("ProtocolVersions");
        if (protocolVersions != null) {
            params.put("ProtocolVersions", csvToList((String) protocolVersions));
        }

        // Topics to list
        // e.g.: "general,technology" -> ["general", "technology"]
        Object topics = params.get("Topics");
        if (topics != null) {
            params.put("Topics", csvToList((String) topics));
        }
    }

    private List<String> csvToList(String raw) {
        List<String> result = new ArrayList<>();

        if (raw == null || raw.trim().isEmpty()) {
            return result;
        }

        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }

        return result;
    }

    private Label buildResponseLabel(String responseName, JsonNode json) {
        JsonNode parametersNode = json.get("parameters");

        // Case - no parameters
        if (parametersNode == null || !parametersNode.isObject()) {
            return AxiniProtobuf.createResponse(responseName, CHANNEL);
        }

        // Case - with parameters
        List<Label.Parameter> params = new ArrayList<>();
        Iterator<String> fieldNames = parametersNode.fieldNames();

        while (fieldNames.hasNext()) {
            String key = fieldNames.next();
            JsonNode value = parametersNode.get(key);

            if (value == null || value.isNull()) {
                continue;
            }

            if (value.isTextual()) {
                params.add(AxiniProtobuf.createParameter(
                        key,
                        AxiniProtobuf.createStringValue(value.asText())
                ));
            } else if (value.isInt() || value.isLong()) {
                params.add(AxiniProtobuf.createParameter(
                        key,
                        AxiniProtobuf.createIntValue(value.asInt())
                ));
            } else if (value.isBoolean()) {
                params.add(AxiniProtobuf.createParameter(
                        key,
                        AxiniProtobuf.createBooleanValue(value.asBoolean())
                ));
            } else if (value.isArray()) {
                List<String> values = new ArrayList<>();
                for (JsonNode n : value) {
                    values.add(n.asText());
                }

                params.add(AxiniProtobuf.createParameter(
                        key,
                        AxiniProtobuf.createStringValue(String.join(",", values))
                ));
            } else {
                params.add(AxiniProtobuf.createParameter(
                        key,
                        AxiniProtobuf.createStringValue(value.toString())
                ));
            }
        }

        return AxiniProtobuf.createResponse(responseName, CHANNEL, params);
    }
}