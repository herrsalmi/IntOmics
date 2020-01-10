package org.pmoi.business;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.pmoi.MainEntry;
import org.pmoi.handler.HttpConnector;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * project FunctionalAnalysis
 * Created by ayyoub on 3/12/18.
 */
public class StringdbQueryClient {

    private static final Logger LOGGER = LogManager.getRootLogger();

    public StringdbQueryClient() {
    }

    public String search(List<String> geneId) {
        StringBuilder url = new StringBuilder("https://string-db.org/api/svg/network?species=9606&identifiers=");
        // add genes to url
        geneId.forEach(e -> url.append(e).append("%0d"));
        // delete the last %0d
        url.delete(url.length() - 3, url.length());

        url.append("&add_white_nodes=10&network_flavor=evidence");

        HttpConnector connector = new HttpConnector();
        String out = null;
        try {
            out = connector.getContent(new URL(url.toString()));
        } catch (java.io.IOException e) {
            LOGGER.error("An error occurred while connecting to StringDB");
        }
        return out;
    }

    public Map<String, String> getProteinNetwork(String symbol) {
        LOGGER.info("Searching StringDB for gene " + symbol);
        String url = String.format("https://string-db.org/api/xml/interaction_partners?species=9606&required_score=%s&identifiers=%s",
                MainEntry.STRINGDB_SCORE, symbol);
        Map<String, String> map = new HashMap<>();
        // see if there is an entry in StringDB for the gene
        URLConnection connection = null;
        try {
            connection = new URL(url).openConnection();
            HttpURLConnection httpConnection = (HttpURLConnection) connection;
            int responseCode = httpConnection.getResponseCode();
            if (responseCode == 500) {
                LOGGER.warn(String.format("No entry in StringDB for GENE:%s", symbol));
                return map;
            }
        } catch (IOException ignored) {
        }

        int count = 0;

        while (true) {
            try {
                SAXBuilder saxBuilder = new SAXBuilder();
                Document document = saxBuilder.build(url);
                List<Element> el = document.getRootElement().getChildren();
                if (el != null && !el.isEmpty()) {
                    el.forEach(l -> map.put(l.getChildText("preferredName_B"), l.getChildText("score")));
                }
                return map;
            } catch (JDOMException | IOException e) {
                LOGGER.warn(String.format("Network I/O error while connecting to StringDB. GENE:%s. Retrying ... (%d/%d)", symbol, ++count, MainEntry.MAX_TRIES));
                if (count == MainEntry.MAX_TRIES) {
                    LOGGER.error(String.format("StringDB data not retrieved for GENE:%s", symbol));
                    return map;
                }
            }
        }
    }
}
