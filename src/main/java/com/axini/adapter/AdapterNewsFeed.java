package com.axini.adapter;

import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axini.adapter.generic.*;
import com.axini.adapter.newsfeed.*;

// Adapter: starts the BrokerConnection and AdapterCore.
public class AdapterNewsFeed {
    private static Logger logger = LoggerFactory.getLogger(AdapterNewsFeed.class);

    public static void runTest(String name, String url, String token) {
        try {
            URI serverUri = new URI(url);
            BrokerConnection brokerConnection =
                new BrokerConnection(serverUri, token);
            Handler handler = new NewsFeedHandler();
            AdapterCore adapterCore =
                new AdapterCore(name, brokerConnection, handler);

            brokerConnection.registerAdapterCore(adapterCore);
            handler.registerAdapterCore(adapterCore);
            adapterCore.start();
        }
        catch (URISyntaxException ex) {
            logger.error("URISyntaxException: " + ex.getMessage());
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("usage: java Adapter <name> <url> <token>");
            System.exit(1);
        }

        String name  = args[0];
        String url   = args[1];
        String token = args[2];

        String abbr_token = token.substring(0, Math.min(token.length(), 40));

        logger.info("name:  " + name);
        logger.info("url:   " + url);
        logger.info("token: " + abbr_token + "...");

        runTest(name, url, token);
    }
}
