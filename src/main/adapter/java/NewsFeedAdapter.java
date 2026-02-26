import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class NewsFeedAdapter {

    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    private final ObjectMapper objectMapper;

    public NewsFeedAdapter() {
        this.objectMapper = new ObjectMapper();
    }

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    public void selectManufacturer(String manufacturer) throws IOException {
        out.write("MANUFACTURER:" + manufacturer + "<!>");
        out.flush();
    }

    public void sendMessage(String name, Map<String, Object> parameters) throws IOException {
        Map<String, Object> message = new HashMap<>();
        message.put("name", name);
        message.put("parameters", parameters);

        String json = objectMapper.writeValueAsString(message);

        out.write(json + "<!>");
        out.flush();
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

        String rawMessage = sb.toString().replace("<!>", "");

        return objectMapper.readTree(rawMessage);
    }

    public void stopSession() throws IOException {
        out.write("STOP<!>");
        out.flush();
    }

    public void close() throws IOException {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}