import com.fasterxml.jackson.databind.JsonNode;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Main {

    public static void main(String[] args) throws Exception {

        NewsFeedAdapter adapter = new NewsFeedAdapter();

        adapter.connect("localhost", 2222);
        adapter.selectManufacturer("Inixa");

        Map<String, Object> params = new HashMap<>();
        params.put("ProtocolVersions", Arrays.asList("1.0", "2.0"));
        params.put("CorrelationId", 13);

        adapter.sendMessage("ProtocolRequest", params);

        JsonNode response = adapter.receiveMessage();

        System.out.println("Response received:");
        System.out.println(response.toPrettyString());

        adapter.stopSession();
        adapter.close();
    }
}