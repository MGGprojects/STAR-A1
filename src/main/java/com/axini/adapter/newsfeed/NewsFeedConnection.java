package com.axini.adapter.newsfeed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class NewsFeedConnection {

    private static final Logger logger =
            LoggerFactory.getLogger(com.axini.adapter.newsfeed.NewsFeedConnection.class);

    private static final String DELIM = "<!>";

    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;

    private Thread listenerThread;

    private final ObjectMapper mapper = new ObjectMapper();

    private com.axini.adapter.newsfeed.NewsFeedHandler handler;

    public void registerHandler(com.axini.adapter.newsfeed.NewsFeedHandler handler) {
        this.handler = handler;
    }

    public synchronized void connect(String host, int port) throws IOException {
        logger.info("Connecting to SUT at {}:{}", host, port);

        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

        startListenerThread();
    }

    public synchronized boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    private void startListenerThread() {
        listenerThread = new Thread(() -> {
            try {
                while (isConnected()) {
                    JsonNode message = receiveMessage();
                    if (message != null) {
                        onMessage(message);
                    } else {
                        break;
                    }
                }
            } catch (IOException ex) {
                logger.info("NewsFeed socket closed: {}", ex.getMessage());
            } catch (Exception ex) {
                logger.error("Unexpected connection error", ex);
                if (handler != null) {
                    handler.sendErrorToAmp("Unexpected connection error: " + ex.getMessage());
                }
            }
        }, "newsfeed-listener");

        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void onMessage(JsonNode message) {
        logger.info("Received from SUT: {}", message);

        if (handler != null) {
            handler.sendResponseToAmp(message.toString());
        }
    }

    public synchronized void selectManufacturer(String manufacturer) throws IOException {
        logger.info("Selecting manufacturer: {}", manufacturer);
        writeRaw("MANUFACTURER:" + manufacturer + DELIM);
    }

    public synchronized void sendStopServer() throws IOException {
        logger.info("Sending STOP to NewsFeed server");
        writeRaw("STOP" + DELIM);
    }

    public synchronized void sendStopSession() throws IOException {
        sendMessage("StopSession", Map.of());
    }

    public synchronized void sendUnsubscribe() throws IOException {
        sendMessage("Unsubscribe", Map.of());
    }

    public synchronized String sendMessage(String name, Map<String, Object> parameters) throws IOException {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("name", name);
        message.put("parameters", parameters);

        String json = mapper.writeValueAsString(message);

        logger.info("Sending to SUT: {}", json);

        writeRaw(json + DELIM);
        return json;
    }

    public JsonNode receiveMessage() throws IOException {
        StringBuilder sb = new StringBuilder();
        int ch;
        while ((ch = in.read()) != -1) {
            sb.append((char) ch);
            if (sb.toString().endsWith("<!>")) {
                break;
            }
        }
        String raw = sb.toString().replace("<!>", "");
        return mapper.readTree(raw);
    }

    private void writeRaw(String raw) throws IOException {
        if (!isConnected()) {
            throw new IOException("Not connected to NewsFeed server.");
        }

        out.write(raw);
        out.flush();
    }

    public synchronized void close() throws IOException {
        logger.info("Closing NewsFeed connection");

        Socket currentSocket = socket;
        socket = null;

        if (currentSocket != null && !currentSocket.isClosed()) {
            currentSocket.close();
        }

        if (listenerThread != null) {
            listenerThread.interrupt();
        }
    }
}