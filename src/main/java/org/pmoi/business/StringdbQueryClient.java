package org.pmoi.business;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.pmoi.ApplicationParameters;
import org.pmoi.Args;

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

    public Map<String, String> getProteinNetwork(String symbol) {
        LOGGER.info("Searching StringDB for gene " + symbol);
        String url = String.format("https://string-db.org/api/xml/interaction_partners?species=9606&required_score=%s&identifiers=%s",
                Args.getInstance().getStringDBScore(), symbol);
        Map<String, String> map = new HashMap<>();
        // see if there is an entry in StringDB for the gene
        URLConnection connection;
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
                LOGGER.warn(String.format("Network I/O error while connecting to StringDB. GENE:%s. Retrying ... (%d/%d)",
                        symbol, ++count, ApplicationParameters.getInstance().getMaxTries()));
                if (count == ApplicationParameters.getInstance().getMaxTries()) {
                    LOGGER.error(String.format("StringDB data not retrieved for GENE:%s", symbol));
                    return map;
                }
            }
        }
    }
}
